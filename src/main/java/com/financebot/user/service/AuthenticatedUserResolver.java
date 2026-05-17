package com.financebot.user.service;

import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private static final String AUTHENTICATED_USER_INVALID_MESSAGE = "Authenticated user is invalid";
    private static final String AUTHENTICATED_USER_NOT_FOUND_MESSAGE = "Authenticated user not found";

    private final UserRepository userRepository;

    public User resolve(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException(AUTHENTICATED_USER_INVALID_MESSAGE);
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException(AUTHENTICATED_USER_NOT_FOUND_MESSAGE));
    }
}