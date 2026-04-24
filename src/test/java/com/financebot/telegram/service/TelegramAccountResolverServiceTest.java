package com.financebot.telegram.service;

import com.financebot.account.domain.Account;
import com.financebot.account.domain.AccountType;
import com.financebot.account.repository.AccountRepository;
import com.financebot.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramAccountResolverServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TelegramAccountResolverService telegramAccountResolverService;

    @Nested
    @DisplayName("resolve")
    class ResolveTests {

        @Test
        @DisplayName("deve retornar conta explicita existente")
        void shouldReturnExistingExplicitAccount() {
            User user = buildUser(1L);
            Account expectedAccount = buildAccount("Nubank", user, false);

            when(accountRepository.findByUserIdAndNameIgnoreCase(user.getId(), "Nubank"))
                    .thenReturn(Optional.of(expectedAccount));

            Account result = telegramAccountResolverService.resolve(user, " Nubank ");

            assertThat(result).isEqualTo(expectedAccount);

            verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account.class));
        }

        @Test
        @DisplayName("deve criar conta explicita quando ela nao existir")
        void shouldCreateExplicitAccountWhenItDoesNotExist() {
            User user = buildUser(1L);
            Account savedAccount = buildAccount("Banco Inter", user, false);

            when(accountRepository.findByUserIdAndNameIgnoreCase(user.getId(), "banco inter"))
                    .thenReturn(Optional.empty());

            when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
                    .thenReturn(savedAccount);

            Account result = telegramAccountResolverService.resolve(user, " banco inter ");

            assertThat(result).isEqualTo(savedAccount);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());

            Account accountToSave = captor.getValue();

            assertThat(accountToSave.getName()).isEqualTo("Banco Inter");
            assertThat(accountToSave.getType()).isEqualTo(AccountType.CHECKING_ACCOUNT);
            assertThat(accountToSave.getInitialBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(accountToSave.isDefaultAccount()).isFalse();
            assertThat(accountToSave.getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("deve retornar conta padrao existente quando conta explicita nao for informada")
        void shouldReturnExistingDefaultAccountWhenExplicitAccountIsNull() {
            User user = buildUser(1L);
            Account defaultAccount = buildAccount("Banco Principal", user, true);

            when(accountRepository.findByUserIdAndDefaultAccountTrue(user.getId()))
                    .thenReturn(Optional.of(defaultAccount));

            Account result = telegramAccountResolverService.resolve(user, null);

            assertThat(result).isEqualTo(defaultAccount);

            verify(accountRepository, never()).findAllByUserId(user.getId());
            verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account.class));
        }

        @Test
        @DisplayName("deve retornar conta padrao existente quando conta explicita estiver em branco")
        void shouldReturnExistingDefaultAccountWhenExplicitAccountIsBlank() {
            User user = buildUser(1L);
            Account defaultAccount = buildAccount("Banco Principal", user, true);

            when(accountRepository.findByUserIdAndDefaultAccountTrue(user.getId()))
                    .thenReturn(Optional.of(defaultAccount));

            Account result = telegramAccountResolverService.resolve(user, "   ");

            assertThat(result).isEqualTo(defaultAccount);

            verify(accountRepository, never()).findAllByUserId(user.getId());
            verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account.class));
        }

        @Test
        @DisplayName("deve transformar unica conta existente em conta padrao")
        void shouldSetSingleExistingAccountAsDefaultAccount() {
            User user = buildUser(1L);
            Account existingAccount = buildAccount("Carteira", user, false);
            Account savedAccount = buildAccount("Carteira", user, true);

            when(accountRepository.findByUserIdAndDefaultAccountTrue(user.getId()))
                    .thenReturn(Optional.empty());

            when(accountRepository.findAllByUserId(user.getId()))
                    .thenReturn(List.of(existingAccount));

            when(accountRepository.save(existingAccount))
                    .thenReturn(savedAccount);

            Account result = telegramAccountResolverService.resolve(user, null);

            assertThat(result).isEqualTo(savedAccount);
            assertThat(existingAccount.isDefaultAccount()).isTrue();

            verify(accountRepository).save(existingAccount);
        }

        @Test
        @DisplayName("deve criar conta padrao quando usuario nao possuir contas")
        void shouldCreateDefaultAccountWhenUserHasNoAccounts() {
            User user = buildUser(1L);
            Account savedAccount = buildAccount("Banco Principal", user, true);

            when(accountRepository.findByUserIdAndDefaultAccountTrue(user.getId()))
                    .thenReturn(Optional.empty());

            when(accountRepository.findAllByUserId(user.getId()))
                    .thenReturn(List.of());

            when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
                    .thenReturn(savedAccount);

            Account result = telegramAccountResolverService.resolve(user, null);

            assertThat(result).isEqualTo(savedAccount);

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());

            Account accountToSave = captor.getValue();

            assertThat(accountToSave.getName()).isEqualTo("Banco Principal");
            assertThat(accountToSave.getType()).isEqualTo(AccountType.CHECKING_ACCOUNT);
            assertThat(accountToSave.getInitialBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(accountToSave.isDefaultAccount()).isTrue();
            assertThat(accountToSave.getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("deve usar primeira conta como padrao quando houver varias contas sem padrao")
        void shouldUseFirstAccountAsDefaultWhenThereAreMultipleAccountsWithoutDefault() {
            User user = buildUser(1L);
            Account firstAccount = buildAccount("Conta 1", user, false);
            Account secondAccount = buildAccount("Conta 2", user, false);
            Account savedAccount = buildAccount("Conta 1", user, true);

            when(accountRepository.findByUserIdAndDefaultAccountTrue(user.getId()))
                    .thenReturn(Optional.empty());

            when(accountRepository.findAllByUserId(user.getId()))
                    .thenReturn(List.of(firstAccount, secondAccount));

            when(accountRepository.save(firstAccount))
                    .thenReturn(savedAccount);

            Account result = telegramAccountResolverService.resolve(user, null);

            assertThat(result).isEqualTo(savedAccount);
            assertThat(firstAccount.isDefaultAccount()).isTrue();
            assertThat(secondAccount.isDefaultAccount()).isFalse();

            verify(accountRepository).save(firstAccount);
        }
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Account buildAccount(String name, User user, boolean defaultAccount) {
        Account account = new Account();
        account.setName(name);
        account.setType(AccountType.CHECKING_ACCOUNT);
        account.setInitialBalance(BigDecimal.ZERO);
        account.setDefaultAccount(defaultAccount);
        account.setUser(user);
        return account;
    }
}