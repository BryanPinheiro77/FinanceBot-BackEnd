package com.financebot.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private JwtService jwtService;
    private UserDetails user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
        user = org.springframework.security.core.userdetails.User.withUsername("bryan@email.com")
                .password("secret").roles("USER").build();
    }

    @Test
    void shouldGenerateAndExtractUsername() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("bryan@email.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void shouldIncludeExtraClaimsAndRejectDifferentUser() {
        String token = jwtService.generateToken(Map.of("role", "ADMIN"), user);
        UserDetails other = org.springframework.security.core.userdetails.User.withUsername("other@email.com")
                .password("secret").roles("USER").build();

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(role).isEqualTo("ADMIN");
        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
        String token = jwtService.generateToken(user);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, user))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> jwtService.extractUsername("malformed-token"))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
