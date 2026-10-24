package com.financebot.security.ratelimit;

import com.financebot.auth.controller.AuthController;
import com.financebot.auth.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthRateLimitInterceptorTest {
    @Test
    void redisFailureReturnsServiceUnavailableWithoutExecutingController() throws Exception {
        AuthRateLimitService service = mock(AuthRateLimitService.class);
        AuthClientIpResolver resolver = mock(AuthClientIpResolver.class);
        when(resolver.resolve(any())).thenReturn("198.51.100.1");
        when(service.acquire("login", "198.51.100.1"))
                .thenThrow(new DataAccessResourceFailureException("private connection detail"));
        var interceptor = new AuthRateLimitInterceptor(service, resolver, JsonMapper.builder().build());
        var request = new MockHttpServletRequest("POST", "/auth/login");
        var response = new MockHttpServletResponse();
        var handler = new HandlerMethod(mock(AuthController.class), AuthController.class.getMethod("login", LoginRequest.class));

        assertThat(interceptor.preHandle(request, response, handler)).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("Service unavailable").doesNotContain("private connection detail");
    }
}
