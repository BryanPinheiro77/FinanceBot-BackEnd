package com.financebot.telegram.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.telegram.service.TelegramAccountResolverService;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramUserApplicationUseCaseTest {
    @Mock UserRepository userRepository;
    @Mock TelegramAccountResolverService accountResolver;
    @InjectMocks TelegramUserApplicationUseCase useCase;

    @Test
    void shouldReturnProfileAndUpdateMonthlyIncome() {
        User user = user();
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertThat(useCase.getProfile(10L).monthlyBaseIncome()).isEqualByComparingTo("5000");
        var response = useCase.updateMonthlyIncome(10L, new BigDecimal("6200"));

        assertThat(response.monthlyBaseIncome()).isEqualByComparingTo("6200");
        verify(userRepository).save(user);
    }

    @Test
    void shouldDisconnectTelegramLink() {
        User user = user();
        user.setTelegramLinkCode("code");
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        useCase.disconnect(10L);

        assertThat(user.getTelegramId()).isNull();
        assertThat(user.getTelegramLinkCode()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void shouldResolveDefaultAccount() {
        User user = user();
        Account account = new Account();
        account.setId(3L);
        account.setName("Carteira");
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(accountResolver.getOrCreateDefaultAccount(user)).thenReturn(account);

        var response = useCase.getDefaultAccount(10L);

        assertThat(response.accountId()).isEqualTo(3L);
        assertThat(response.accountName()).isEqualTo("Carteira");
    }

    @Test
    void shouldRejectUnknownUsers() {
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getProfile(10L))
                .isInstanceOf(TelegramUserNotFoundException.class);
        assertThatThrownBy(() -> useCase.getDefaultAccount(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuário não encontrado para este Telegram.");
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setName("Bryan");
        user.setEmail("bryan@email.com");
        user.setTelegramId(10L);
        user.setMonthlyBaseIncome(new BigDecimal("5000"));
        return user;
    }
}
