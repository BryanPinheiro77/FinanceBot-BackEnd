package com.financebot.auth.mapper;

import com.financebot.auth.dto.response.AuthResponse;
import com.financebot.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    private static final String TOKEN_TYPE = "Bearer";

    public AuthResponse toResponse(String token, User user) {
        return new AuthResponse(
                token,
                TOKEN_TYPE,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}