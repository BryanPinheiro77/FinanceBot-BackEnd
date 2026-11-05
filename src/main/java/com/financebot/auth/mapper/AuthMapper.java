package com.financebot.auth.mapper;

import com.financebot.auth.dto.response.AuthResponse;
import com.financebot.auth.dto.response.SessionTokens;
import com.financebot.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    private static final String TOKEN_TYPE = "Bearer";

    public AuthResponse toResponse(SessionTokens tokens, User user) {
        return new AuthResponse(
                tokens.token(),
                TOKEN_TYPE,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                tokens.refreshToken(),
                tokens.sessionExpiresAt(),
                tokens.lastActivityAt()
        );
    }
}