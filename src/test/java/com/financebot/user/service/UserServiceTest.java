package com.financebot.user.service;

import com.financebot.user.domain.User;
import com.financebot.user.dto.request.TelegramLinkConfirmRequest;
import com.financebot.user.dto.request.UpdateMonthlyBaseIncomeRequest;
import com.financebot.user.dto.response.CurrentUserResponse;
import com.financebot.user.dto.response.TelegramLinkCodeResponse;
import com.financebot.user.dto.response.TelegramLinkConfirmResponse;
import com.financebot.user.mapper.UserMapper;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("getMe")
    class GetMeTests {

        @Test
        @DisplayName("deve retornar dados do usuario autenticado")
        void shouldReturnCurrentUserData() {
            User user = buildUser();
            user.setMonthlyBaseIncome(new BigDecimal("3500.00"));
            user.setOnboardingCompleted(false);
            user.setTelegramId(123456L);
            user.setTelegramLinkCode("FIN-ABC123");
            user.setTelegramLinkCodeExpiresAt(LocalDateTime.now().plusMinutes(10));

            mockAuthenticatedUser(user);

            CurrentUserResponse response = userService.getMe(authentication);

            assertThat(response.id()).isEqualTo(user.getId());
            assertThat(response.name()).isEqualTo(user.getName());
            assertThat(response.email()).isEqualTo(user.getEmail());
            assertThat(response.monthlyBaseIncome()).isEqualByComparingTo("3500.00");
            assertThat(response.onboardingCompleted()).isFalse();
            assertThat(response.telegramId()).isEqualTo(123456L);
            assertThat(response.telegramLinked()).isTrue();
            assertThat(response.telegramLinkCode()).isEqualTo("FIN-ABC123");
            assertThat(response.telegramLinkCodeExpiresAt()).isEqualTo(user.getTelegramLinkCodeExpiresAt());

            verify(authenticatedUserResolver).resolve(authentication);
        }

        @Test
        @DisplayName("deve retornar telegramLinked falso quando usuario nao tiver Telegram vinculado")
        void shouldReturnTelegramLinkedFalseWhenUserHasNoTelegramId() {
            User user = buildUser();
            user.setTelegramId(null);

            mockAuthenticatedUser(user);

            CurrentUserResponse response = userService.getMe(authentication);

            assertThat(response.telegramLinked()).isFalse();
            assertThat(response.telegramId()).isNull();

            verify(authenticatedUserResolver).resolve(authentication);
        }
    }

    @Nested
    @DisplayName("updateMonthlyBaseIncome")
    class UpdateMonthlyBaseIncomeTests {

        @Test
        @DisplayName("deve atualizar renda mensal base do usuario autenticado")
        void shouldUpdateMonthlyBaseIncome() {
            User user = buildUser();

            UpdateMonthlyBaseIncomeRequest request = new UpdateMonthlyBaseIncomeRequest(
                    new BigDecimal("4200.00")
            );

            mockAuthenticatedUser(user);

            userService.updateMonthlyBaseIncome(request, authentication);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();

            assertThat(savedUser).isEqualTo(user);
            assertThat(savedUser.getMonthlyBaseIncome()).isEqualByComparingTo("4200.00");

            verify(authenticatedUserResolver).resolve(authentication);
        }
    }

    @Nested
    @DisplayName("completeOnboarding")
    class CompleteOnboardingTests {

        @Test
        @DisplayName("deve marcar onboarding como concluido para usuario autenticado")
        void shouldCompleteOnboarding() {
            User user = buildUser();
            user.setOnboardingCompleted(false);

            mockAuthenticatedUser(user);

            userService.completeOnboarding(authentication);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();

            assertThat(savedUser).isEqualTo(user);
            assertThat(savedUser.getOnboardingCompleted()).isTrue();

            verify(authenticatedUserResolver).resolve(authentication);
        }
    }

    @Nested
    @DisplayName("generateTelegramLinkCode")
    class GenerateTelegramLinkCodeTests {

        @Test
        @DisplayName("deve gerar codigo de vinculo do Telegram")
        void shouldGenerateTelegramLinkCode() {
            User user = buildUser();

            mockAuthenticatedUser(user);

            TelegramLinkCodeResponse response = userService.generateTelegramLinkCode(authentication);

            assertThat(response.telegramLinkCode()).startsWith("FIN-");
            assertThat(response.telegramLinkCode()).hasSize(10);
            assertThat(response.expiresAt()).isAfter(LocalDateTime.now());
            assertThat(response.message()).isEqualTo("Send this code to the Telegram bot to link your account.");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();

            assertThat(savedUser.getTelegramLinkCode()).isEqualTo(response.telegramLinkCode());
            assertThat(savedUser.getTelegramLinkCodeExpiresAt()).isEqualTo(response.expiresAt());

            verify(authenticatedUserResolver).resolve(authentication);
        }
    }

    @Nested
    @DisplayName("confirmTelegramLink")
    class ConfirmTelegramLinkTests {

        @Test
        @DisplayName("deve confirmar vinculo do Telegram com sucesso")
        void shouldConfirmTelegramLinkSuccessfully() {
            User user = buildUser();
            user.setTelegramLinkCode("FIN-ABC123");
            user.setTelegramLinkCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            TelegramLinkConfirmRequest request = new TelegramLinkConfirmRequest(
                    "FIN-ABC123",
                    987654L,
                    "bryan_telegram"
            );

            when(userRepository.findByTelegramLinkCode("FIN-ABC123"))
                    .thenReturn(Optional.of(user));

            when(userRepository.existsByTelegramId(987654L))
                    .thenReturn(false);

            TelegramLinkConfirmResponse response = userService.confirmTelegramLink(request);

            assertThat(response.success()).isTrue();
            assertThat(response.message()).isEqualTo("Conta conectada com sucesso ao Telegram.");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();

            assertThat(savedUser.getTelegramId()).isEqualTo(987654L);
            assertThat(savedUser.getTelegramLinkCode()).isNull();
            assertThat(savedUser.getTelegramLinkCodeExpiresAt()).isNull();
        }

        @Test
        @DisplayName("deve retornar sucesso quando Telegram ja estava vinculado ao mesmo usuario")
        void shouldReturnSuccessWhenTelegramWasAlreadyLinkedToSameUser() {
            User user = buildUser();
            user.setTelegramId(987654L);
            user.setTelegramLinkCode("FIN-ABC123");
            user.setTelegramLinkCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            TelegramLinkConfirmRequest request = new TelegramLinkConfirmRequest(
                    "FIN-ABC123",
                    987654L,
                    "bryan_telegram"
            );

            when(userRepository.findByTelegramLinkCode("FIN-ABC123"))
                    .thenReturn(Optional.of(user));

            TelegramLinkConfirmResponse response = userService.confirmTelegramLink(request);

            assertThat(response.success()).isTrue();
            assertThat(response.message()).isEqualTo("Sua conta já estava conectada a este Telegram.");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();

            assertThat(savedUser.getTelegramId()).isEqualTo(987654L);
            assertThat(savedUser.getTelegramLinkCode()).isNull();
            assertThat(savedUser.getTelegramLinkCodeExpiresAt()).isNull();

            verify(userRepository, never()).existsByTelegramId(anyLong());
        }

        @Test
        @DisplayName("deve lancar erro quando codigo de vinculo nao existir")
        void shouldThrowWhenLinkCodeDoesNotExist() {
            TelegramLinkConfirmRequest request = new TelegramLinkConfirmRequest(
                    "FIN-INVALID",
                    987654L,
                    "bryan_telegram"
            );

            when(userRepository.findByTelegramLinkCode("FIN-INVALID"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.confirmTelegramLink(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Código de vínculo inválido");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("deve lancar erro quando codigo de vinculo nao tiver data de expiracao")
        void shouldThrowWhenLinkCodeExpirationIsNull() {
            User user = buildUser();
            user.setTelegramLinkCode("FIN-ABC123");
            user.setTelegramLinkCodeExpiresAt(null);

            TelegramLinkConfirmRequest request = new TelegramLinkConfirmRequest(
                    "FIN-ABC123",
                    987654L,
                    "bryan_telegram"
            );

            when(userRepository.findByTelegramLinkCode("FIN-ABC123"))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.confirmTelegramLink(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Código de vínculo expirado");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("deve lancar erro quando codigo de vinculo estiver expirado")
        void shouldThrowWhenLinkCodeIsExpired() {
            User user = buildUser();
            user.setTelegramLinkCode("FIN-ABC123");
            user.setTelegramLinkCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

            TelegramLinkConfirmRequest request = new TelegramLinkConfirmRequest(
                    "FIN-ABC123",
                    987654L,
                    "bryan_telegram"
            );

            when(userRepository.findByTelegramLinkCode("FIN-ABC123"))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.confirmTelegramLink(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Código de vínculo expirado");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("deve lancar erro quando Telegram ja estiver vinculado a outra conta")
        void shouldThrowWhenTelegramIsAlreadyLinkedToAnotherAccount() {
            User user = buildUser();
            user.setTelegramLinkCode("FIN-ABC123");
            user.setTelegramLinkCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            TelegramLinkConfirmRequest request = new TelegramLinkConfirmRequest(
                    "FIN-ABC123",
                    987654L,
                    "bryan_telegram"
            );

            when(userRepository.findByTelegramLinkCode("FIN-ABC123"))
                    .thenReturn(Optional.of(user));

            when(userRepository.existsByTelegramId(987654L))
                    .thenReturn(true);

            assertThatThrownBy(() -> userService.confirmTelegramLink(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Este Telegram já está vinculado a outra conta");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("disconnectTelegram")
    class DisconnectTelegramTests {

        @Test
        @DisplayName("deve desconectar Telegram do usuario autenticado")
        void shouldDisconnectTelegram() {
            User user = buildUser();
            user.setTelegramId(987654L);
            user.setTelegramLinkCode("FIN-ABC123");
            user.setTelegramLinkCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            mockAuthenticatedUser(user);

            userService.disconnectTelegram(authentication);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User savedUser = captor.getValue();

            assertThat(savedUser.getTelegramId()).isNull();
            assertThat(savedUser.getTelegramLinkCode()).isNull();
            assertThat(savedUser.getTelegramLinkCodeExpiresAt()).isNull();

            verify(authenticatedUserResolver).resolve(authentication);
        }
    }

    private void mockAuthenticatedUser(User user) {
        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Bryan");
        user.setEmail("bryan@email.com");
        user.setMonthlyBaseIncome(new BigDecimal("3000.00"));
        user.setOnboardingCompleted(false);
        return user;
    }
}