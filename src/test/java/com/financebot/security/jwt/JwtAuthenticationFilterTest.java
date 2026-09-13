package com.financebot.security.jwt;

import com.financebot.security.userdetails.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock JwtService jwtService;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipPublicRoutes() throws Exception {
        TestableFilter filter = new TestableFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = request("/auth/login", null);

        filter.invoke(request, new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(eq(request), any());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void shouldSkipRequestWithoutBearerToken() throws Exception {
        TestableFilter filter = new TestableFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = request("/transactions", "Basic abc");

        filter.invoke(request, new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(eq(request), any());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void shouldAuthenticateValidBearerToken() throws Exception {
        TestableFilter filter = new TestableFilter(jwtService, userDetailsService);
        UserDetails details = org.springframework.security.core.userdetails.User.withUsername("a@b.com")
                .password("secret").roles("USER").build();
        MockHttpServletRequest request = request("/transactions", "Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("a@b.com");
        when(userDetailsService.loadUserByUsername("a@b.com")).thenReturn(details);
        when(jwtService.isTokenValid("token", details)).thenReturn(true);

        filter.invoke(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("a@b.com");
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void shouldClearContextWhenTokenIsInvalid() throws Exception {
        TestableFilter filter = new TestableFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = request("/transactions", "Bearer invalid");
        when(jwtService.extractUsername("invalid")).thenThrow(new io.jsonwebtoken.JwtException("invalid"));

        filter.invoke(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(eq(request), any());
    }

    @Test
    void shouldKeepExistingAuthentication() throws Exception {
        TestableFilter filter = new TestableFilter(jwtService, userDetailsService);
        var existing = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("existing", null);
        SecurityContextHolder.getContext().setAuthentication(existing);
        MockHttpServletRequest request = request("/transactions", "Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("a@b.com");

        filter.invoke(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    private MockHttpServletRequest request(String path, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(path);
        if (authorization != null) request.addHeader("Authorization", authorization);
        return request;
    }

    private static class TestableFilter extends JwtAuthenticationFilter {
        TestableFilter(JwtService jwtService, CustomUserDetailsService service) {
            super(jwtService, service);
        }

        void invoke(MockHttpServletRequest request, MockHttpServletResponse response, FilterChain chain)
                throws Exception {
            doFilterInternal(request, response, chain);
        }
    }
}
