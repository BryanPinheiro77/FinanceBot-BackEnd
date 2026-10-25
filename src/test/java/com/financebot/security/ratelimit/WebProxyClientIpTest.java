package com.financebot.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebProxyClientIpTest {
    private static final byte[] FIXTURE_KEY = new byte[32];

    @Test
    void authenticatesClientIpWithoutChangingTheUserAuthentication() throws Exception {
        var request = signedRequest("203.0.113.8", Instant.now().getEpochSecond());
        assertThat(resolver().resolve(request)).isEqualTo("203.0.113.8");
        assertThat(new AuthClientIpResolver(new AuthRateLimitProperties(), "none").resolve(request))
                .isEqualTo("198.51.100.2");
    }

    @Test
    void alteredIpPathOrMethodCannotReuseASignature() throws Exception {
        var request = signedRequest("203.0.113.8", Instant.now().getEpochSecond());
        request.setRequestURI("/auth/register");
        assertThat(resolver().resolve(request)).isEqualTo("198.51.100.2");
        request.setRequestURI("/auth/login");
        request.setMethod("GET");
        assertThat(resolver().resolve(request)).isEqualTo("198.51.100.2");
        request.setMethod("POST");
        request.removeHeader("X-FinanceBot-Client-IP");
        request.addHeader("X-FinanceBot-Client-IP", "203.0.113.9");
        assertThat(resolver().resolve(request)).isEqualTo("198.51.100.2");
    }

    @ParameterizedTest
    @ValueSource(longs = {-120, 120})
    void rejectsOldAndFutureAssertions(long offset) throws Exception {
        assertThat(resolver().resolve(signedRequest("203.0.113.8", Instant.now().getEpochSecond() + offset)))
                .isEqualTo("198.51.100.2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com", "203.0.113.8, 203.0.113.9", "127.1", "999.1.1.1"})
    void rejectsNonLiteralOrAmbiguousAddressesEvenWithAValidSignature(String ip) throws Exception {
        assertThat(resolver().resolve(signedRequest(ip, Instant.now().getEpochSecond())))
                .isEqualTo("198.51.100.2");
    }

    @Test
    void rejectsRepeatedHeadersAndInvalidSignature() throws Exception {
        var request = signedRequest("203.0.113.8", Instant.now().getEpochSecond());
        request.addHeader("X-FinanceBot-Client-IP", "203.0.113.9");
        assertThat(resolver().resolve(request)).isEqualTo("198.51.100.2");
        request = signedRequest("203.0.113.8", Instant.now().getEpochSecond());
        request.removeHeader("X-FinanceBot-Proxy-Signature");
        request.addHeader("X-FinanceBot-Proxy-Signature", "0".repeat(64));
        assertThat(resolver().resolve(request)).isEqualTo("198.51.100.2");
    }

    @Test
    void invalidConfiguredKeyFailsAtStartup() {
        var properties = new AuthRateLimitProperties();
        properties.setWebProxySecret("not-base64");
        assertThatThrownBy(() -> new AuthClientIpResolver(properties, "none"))
                .isInstanceOf(IllegalArgumentException.class);
        properties.setWebProxySecret(Base64.getEncoder().encodeToString(FIXTURE_KEY).replace("=", ""));
        assertThatThrownBy(() -> new AuthClientIpResolver(properties, "none"))
                .isInstanceOf(IllegalArgumentException.class);
        properties.setWebProxySecret(Base64.getEncoder().encodeToString(new byte[16]));
        assertThatThrownBy(() -> new AuthClientIpResolver(properties, "none"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AuthClientIpResolver resolver() {
        var properties = new AuthRateLimitProperties();
        properties.setWebProxySecret(Base64.getEncoder().encodeToString(FIXTURE_KEY));
        return new AuthClientIpResolver(properties, "none");
    }

    private MockHttpServletRequest signedRequest(String ip, long timestamp) throws Exception {
        var request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("198.51.100.2");
        String payload = timestamp + "\n" + ip + "\nPOST\n/auth/login";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(FIXTURE_KEY, "HmacSHA256"));
        request.addHeader("X-FinanceBot-Client-IP", ip);
        request.addHeader("X-FinanceBot-Proxy-Timestamp", Long.toString(timestamp));
        request.addHeader("X-FinanceBot-Proxy-Signature",
                HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))));
        return request;
    }
}
