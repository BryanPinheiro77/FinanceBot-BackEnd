package com.financebot.account.service;

import com.financebot.account.domain.AccountType;
import com.financebot.account.dto.request.UpdateAccountRequest;
import com.financebot.account.mapper.AccountMapper;
import com.financebot.account.repository.AccountRepository;
import com.financebot.transaction.repository.TransactionRepository;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AccountService accountService;

    @Nested
    @DisplayName("not found")
    class NotFoundTests {

        @Test
        @DisplayName("deve lançar erro quando conta não for encontrada ao buscar por id")
        void shouldThrowWhenAccountIsNotFoundOnFindById() {
            User user = buildUser();

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.findById(10L, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verify(accountRepository).findByIdAndUserId(10L, 1L);
            verifyNoInteractions(transactionRepository, accountMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando conta não for encontrada ao atualizar")
        void shouldThrowWhenAccountIsNotFoundOnUpdate() {
            User user = buildUser();

            UpdateAccountRequest request = new UpdateAccountRequest(
                    "Conta principal",
                    AccountType.CHECKING_ACCOUNT,
                    new BigDecimal("100.00")
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.update(10L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verify(accountRepository).findByIdAndUserId(10L, 1L);
            verify(accountRepository, never()).save(any());
            verifyNoInteractions(transactionRepository, accountMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando conta não for encontrada ao deletar")
        void shouldThrowWhenAccountIsNotFoundOnDelete() {
            User user = buildUser();

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.delete(10L, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verify(accountRepository).findByIdAndUserId(10L, 1L);
            verify(accountRepository, never()).delete(any());
            verifyNoInteractions(transactionRepository, accountMapper);
        }
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("bryan@email.com");
        return user;
    }
}