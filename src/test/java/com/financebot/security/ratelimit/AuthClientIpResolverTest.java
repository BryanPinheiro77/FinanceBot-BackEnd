package com.financebot.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthClientIpResolverTest {
    @Test
    void ignoresForwardingHeadersFromAnUntrustedConnection() {
        MockHttpServletRequest request = request("198.51.100.10", "203.0.113.8");
        request.addHeader("Forwarded", "for=203.0.113.9");
        assertThat(resolver().resolve(request)).isEqualTo("198.51.100.10");
    }

    @Test
    void stripsOnlyTrustedHopsFromRightToLeftIncludingRepeatedHeaders() {
        MockHttpServletRequest request = request("10.0.0.2", "203.0.113.99, 198.51.100.10");
        request.addHeader("X-Forwarded-For", "10.0.0.3");
        assertThat(resolver("10.0.0.0/24").resolve(request)).isEqualTo("198.51.100.10");
    }

    @Test
    void ignoresMalformedClientControlledPrefixesBeforeTheFirstUntrustedHop() {
        assertThat(resolver("10.0.0.2").resolve(request("10.0.0.2", "example.com, 198.51.100.10")))
                .isEqualTo("198.51.100.10");
    }

    @Test
    void ignoresHeadersWhenPeerIsOutsideConfiguredSubnet() {
        assertThat(resolver("10.0.0.0/24").resolve(request("10.0.1.2", "203.0.113.8")))
                .isEqualTo("10.0.1.2");
    }

    @Test
    void normalizesEquivalentIpv6AndIpv4MappedAddresses() {
        AuthClientIpResolver resolver = resolver("2001:db8:1::/64");
        assertThat(resolver.resolve(request("2001:db8:1::1", "2001:db8:2::a")))
                .isEqualTo(resolver.resolve(request("2001:db8:1::2", "2001:0db8:0002:0:0:0:0:a")));
        assertThat(resolver.resolve(request("::ffff:198.51.100.10", null))).isEqualTo("198.51.100.10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com", "127.1", "999.1.1.1", "198.51.100.1,", "unknown",
            "[2001:db8::1]", "fe80::1%eth0", "203.0.113.1:8080", "a:b:c"})
    void malformedForwardingChainUsesThePeerWithoutDnsLookups(String header) {
        assertThat(resolver("10.0.0.2").resolve(request("10.0.0.2", header))).isEqualTo("10.0.0.2");
    }

    @Test
    void rejectsOversizedChainsAndUsesPeerWhenHeaderIsAbsent() {
        AuthClientIpResolver resolver = resolver("10.0.0.2");
        assertThat(resolver.resolve(request("10.0.0.2", "198.51.100.1,".repeat(21) + "198.51.100.2")))
                .isEqualTo("10.0.0.2");
        assertThat(resolver.resolve(request("10.0.0.2", "1".repeat(2049)))).isEqualTo("10.0.0.2");
        assertThat(resolver.resolve(request("10.0.0.2", null))).isEqualTo("10.0.0.2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"example.com", "0.0.0.0/0", "::/0", "10.0.0.0/no", "10.0.0.0/33"})
    void rejectsInvalidOrUnrestrictedTrustedProxies(String cidr) {
        assertThatThrownBy(() -> resolver(cidr)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preventsNativeOrFrameworkForwardingFromHidingTheConnectionPeer() {
        assertThatThrownBy(() -> new AuthClientIpResolver(new AuthRateLimitProperties(), "framework"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthClientIpResolver(new AuthRateLimitProperties(), "native"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AuthClientIpResolver resolver(String... trusted) {
        AuthRateLimitProperties properties = new AuthRateLimitProperties();
        properties.setTrustedProxies(List.of(trusted));
        return new AuthClientIpResolver(properties, "none");
    }

    private MockHttpServletRequest request(String peer, String forwarded) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (forwarded != null) request.addHeader("X-Forwarded-For", forwarded);
        return request;
    }
}
