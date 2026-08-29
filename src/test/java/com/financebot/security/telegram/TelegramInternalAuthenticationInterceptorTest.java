package com.financebot.security.telegram;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramInternalAuthenticationInterceptorTest {

    private static final String EXPECTED_TOKEN = "test-telegram-internal-token";

    private final TelegramInternalAuthenticationInterceptor interceptor =
            new TelegramInternalAuthenticationInterceptor(EXPECTED_TOKEN);

    @Test
    void shouldAllowRequestWithValidToken() throws Exception {
        MockHttpServletRequest request = requestWithToken(EXPECTED_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldRejectRequestWithInvalidToken() throws Exception {
        MockHttpServletRequest request = requestWithToken("invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldRejectAllRequestsWhenExpectedTokenIsNotConfigured() throws Exception {
        TelegramInternalAuthenticationInterceptor unconfiguredInterceptor =
                new TelegramInternalAuthenticationInterceptor("");
        MockHttpServletRequest request = requestWithToken("any-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = unconfiguredInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TelegramInternalAuthenticationInterceptor.INTERNAL_TOKEN_HEADER, token);
        return request;
    }
}
