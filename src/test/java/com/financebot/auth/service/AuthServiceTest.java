package com.financebot.auth.service;

import com.financebot.auth.dto.request.LoginRequest;
import com.financebot.auth.dto.request.RegisterRequest;
import com.financebot.auth.dto.response.AuthResponse;
import com.financebot.auth.mapper.AuthMapper;
import com.financebot.category.service.CategoryService;
import com.financebot.common.exception.UnauthorizedException;
import com.financebot.common.exception.ValidationException;
import com.financebot.security.jwt.JwtService;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock CategoryService categoryService;
    @Mock AuthMapper authMapper;
    @InjectMocks AuthService authService;

    @Test
    void shouldRegisterNormalizedUserAndCreateDefaults() {
        RegisterRequest request = new RegisterRequest();
        request.setName("  Bryan ");
        request.setEmail(" Bryan@Email.COM ");
        request.setPassword("secret");
        User saved = user(1L, "bryan@email.com");
        AuthResponse expected = new AuthResponse("token", "Bearer", 1L, "Bryan", "bryan@email.com", "USER");
        when(userRepository.existsByEmail("bryan@email.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken(saved)).thenReturn("token");
        when(authMapper.toResponse("token", saved)).thenReturn(expected);

        AuthResponse response = authService.register(request);

        assertThat(response).isEqualTo(expected);
        verify(categoryService).createDefaultCategoriesForUser(saved);
        verify(passwordEncoder).encode("secret");
        assertThat(saved.getEmail()).isEqualTo("bryan@email.com");
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Bryan");
        request.setEmail("Bryan@Email.com");
        request.setPassword("secret");
        when(userRepository.existsByEmail("bryan@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Já existe um usuário cadastrado com este email");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginWithNormalizedEmailAndValidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" Bryan@Email.com ");
        request.setPassword("secret");
        User user = user(1L, "bryan@email.com");
        AuthResponse expected = new AuthResponse("token", "Bearer", 1L, "Bryan", "bryan@email.com", "USER");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token");
        when(authMapper.toResponse("token", user)).thenReturn(expected);

        assertThat(authService.login(request)).isEqualTo(expected);
    }

    @Test
    void shouldRejectUnknownOrInvalidPasswordOnLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@email.com");
        request.setPassword("wrong");
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email ou senha inválidos");

        User user = user(1L, "user@email.com");
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Email ou senha inválidos");
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("Bryan");
        user.setEmail(email);
        user.setPassword("encoded");
        return user;
    }
}
