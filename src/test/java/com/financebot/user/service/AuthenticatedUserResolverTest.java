package com.financebot.user.service;

import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserResolverTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Nested
    @DisplayName("resolve")
    class ResolveTests {

        @Test
        @DisplayName("deve retornar usuario autenticado quando authentication for valida")
        void shouldResolveAuthenticatedUser() {
            User user = new User();
            user.setId(1L);
            user.setEmail("bryan@email.com");

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));

            User result = authenticatedUserResolver.resolve(authentication);

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("deve lancar erro quando authentication for nulo")
        void shouldThrowWhenAuthenticationIsNull() {
            assertThatThrownBy(() -> authenticatedUserResolver.resolve(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Authenticated user is invalid");

            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("deve lancar erro quando authentication nao possuir nome")
        void shouldThrowWhenAuthenticationNameIsNull() {
            when(authentication.getName()).thenReturn(null);

            assertThatThrownBy(() -> authenticatedUserResolver.resolve(authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Authenticated user is invalid");

            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("deve lancar erro quando authentication possuir nome em branco")
        void shouldThrowWhenAuthenticationNameIsBlank() {
            when(authentication.getName()).thenReturn("   ");

            assertThatThrownBy(() -> authenticatedUserResolver.resolve(authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Authenticated user is invalid");

            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("deve lancar erro quando usuario autenticado nao existir")
        void shouldThrowWhenAuthenticatedUserDoesNotExist() {
            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticatedUserResolver.resolve(authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Authenticated user not found");
        }
    }
}