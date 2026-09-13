package com.financebot.account.service;

import com.financebot.account.domain.AccountType;
import com.financebot.account.domain.Account;
import com.financebot.account.dto.request.CreateAccountRequest;
import com.financebot.account.dto.request.UpdateAccountRequest;
import com.financebot.account.dto.response.AccountResponse;
import com.financebot.account.mapper.AccountMapper;
import com.financebot.account.repository.AccountRepository;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.domain.TransactionType;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
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

    @Test
    @DisplayName("deve criar conta e calcular saldo atual")
    void shouldCreateAccountAndCalculateCurrentBalance() {
        User user = buildUser();
        CreateAccountRequest request = new CreateAccountRequest(
                "  Conta principal  ", AccountType.CHECKING_ACCOUNT, new BigDecimal("100.00"));
        Account account = buildAccount(user, 10L, "Conta principal", new BigDecimal("100.00"));
        AccountResponse response = new AccountResponse(
                10L, "Conta principal", AccountType.CHECKING_ACCOUNT,
                new BigDecimal("100.00"), new BigDecimal("125.00"), null);

        when(authentication.getName()).thenReturn("bryan@email.com");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
        when(accountRepository.existsByNameIgnoreCaseAndUserId("Conta principal", 1L)).thenReturn(false);
        when(accountMapper.toEntity(request)).thenReturn(account);
        when(accountRepository.save(account)).thenReturn(account);
        when(transactionRepository.sumAmountByAccountAndUserAndType(10L, 1L, TransactionType.INCOME))
                .thenReturn(new BigDecimal("50.00"));
        when(transactionRepository.sumAmountByAccountAndUserAndType(10L, 1L, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("25.00"));
        when(accountMapper.toResponse(account, new BigDecimal("125.00"))).thenReturn(response);

        assertThat(accountService.create(request, authentication)).isSameAs(response);
        verify(accountMapper).toEntity(request);
        verify(accountRepository).save(account);
        verify(accountMapper).toResponse(account, new BigDecimal("125.00"));
    }

    @Test
    @DisplayName("deve listar contas ordenadas com saldo atual")
    void shouldFindAllAccountsWithCurrentBalance() {
        User user = buildUser();
        Account first = buildAccount(user, 10L, "Conta A", new BigDecimal("100.00"));
        Account second = buildAccount(user, 11L, "Conta B", new BigDecimal("200.00"));
        AccountResponse firstResponse = new AccountResponse(10L, "Conta A", AccountType.CHECKING_ACCOUNT,
                new BigDecimal("100.00"), new BigDecimal("110.00"), null);
        AccountResponse secondResponse = new AccountResponse(11L, "Conta B", AccountType.SAVINGS_ACCOUNT,
                new BigDecimal("200.00"), new BigDecimal("180.00"), null);

        when(authentication.getName()).thenReturn("bryan@email.com");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
        when(accountRepository.findAllByUserIdOrderByNameAsc(1L)).thenReturn(List.of(first, second));
        stubBalance(first, "10.00", "0.00");
        stubBalance(second, "0.00", "20.00");
        when(accountMapper.toResponse(first, new BigDecimal("110.00"))).thenReturn(firstResponse);
        when(accountMapper.toResponse(second, new BigDecimal("180.00"))).thenReturn(secondResponse);

        assertThat(accountService.findAll(authentication)).containsExactly(firstResponse, secondResponse);
    }

    @Test
    @DisplayName("deve buscar conta e calcular saldo atual")
    void shouldFindAccountById() {
        User user = buildUser();
        Account account = buildAccount(user, 10L, "Conta", new BigDecimal("100.00"));
        AccountResponse response = new AccountResponse(10L, "Conta", AccountType.CHECKING_ACCOUNT,
                new BigDecimal("100.00"), new BigDecimal("100.00"), null);

        when(authentication.getName()).thenReturn("bryan@email.com");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
        stubBalance(account, "0.00", "0.00");
        when(accountMapper.toResponse(account, new BigDecimal("100.00"))).thenReturn(response);

        assertThat(accountService.findById(10L, authentication)).isSameAs(response);
    }

    @Test
    @DisplayName("deve atualizar conta sem consultar duplicidade quando nome não mudou")
    void shouldUpdateAccountWithoutDuplicateCheckWhenNameIsUnchanged() {
        User user = buildUser();
        Account account = buildAccount(user, 10L, "Conta", new BigDecimal("100.00"));
        UpdateAccountRequest request = new UpdateAccountRequest(
                " conta ", AccountType.SAVINGS_ACCOUNT, new BigDecimal("150.00"));
        AccountResponse response = new AccountResponse(10L, "conta", AccountType.SAVINGS_ACCOUNT,
                new BigDecimal("150.00"), new BigDecimal("150.00"), null);

        when(authentication.getName()).thenReturn("bryan@email.com");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
        doAnswer(invocation -> {
            account.setName("conta");
            account.setType(AccountType.SAVINGS_ACCOUNT);
            account.setInitialBalance(new BigDecimal("150.00"));
            return null;
        }).when(accountMapper).updateEntity(request, account);
        when(accountRepository.save(account)).thenReturn(account);
        stubBalance(account, "0.00", "0.00");
        when(accountMapper.toResponse(account, new BigDecimal("150.00"))).thenReturn(response);

        assertThat(accountService.update(10L, request, authentication)).isSameAs(response);
        verify(accountRepository, never()).existsByNameIgnoreCaseAndUserId(any(), any());
        verify(accountMapper).updateEntity(request, account);
    }

    @Test
    @DisplayName("deve deletar conta encontrada")
    void shouldDeleteAccount() {
        User user = buildUser();
        Account account = buildAccount(user, 10L, "Conta", BigDecimal.ZERO);
        when(authentication.getName()).thenReturn("bryan@email.com");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
        when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));

        accountService.delete(10L, authentication);

        verify(accountRepository).delete(account);
        verifyNoInteractions(transactionRepository, accountMapper);
    }

    @Test
    @DisplayName("deve rejeitar nome de conta duplicado ao criar")
    void shouldRejectDuplicateAccountNameOnCreate() {
        User user = buildUser();
        CreateAccountRequest request = new CreateAccountRequest("Conta", AccountType.CHECKING_ACCOUNT, BigDecimal.ZERO);
        when(authentication.getName()).thenReturn("bryan@email.com");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
        when(accountRepository.existsByNameIgnoreCaseAndUserId("Conta", 1L)).thenReturn(true);

        assertThatThrownBy(() -> accountService.create(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account already exists for this user");
        verifyNoInteractions(accountMapper, transactionRepository);
    }

    @Test
    @DisplayName("deve rejeitar saldo inicial nulo ou negativo")
    void shouldRejectInvalidInitialBalance() {
        User user = buildUser();
        when(authentication.getName()).thenReturn("bryan@email.com");
        when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
        when(accountRepository.existsByNameIgnoreCaseAndUserId("Conta", 1L)).thenReturn(false);

        assertThatThrownBy(() -> accountService.create(
                new CreateAccountRequest("Conta", AccountType.CHECKING_ACCOUNT, null), authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Initial balance is required");
        assertThatThrownBy(() -> accountService.create(
                new CreateAccountRequest("Conta", AccountType.CHECKING_ACCOUNT, new BigDecimal("-1")), authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Initial balance cannot be negative");
    }

    @Test
    @DisplayName("deve rejeitar usuário autenticado inexistente")
    void shouldRejectMissingAuthenticatedUser() {
        when(authentication.getName()).thenReturn("missing@email.com");
        when(userRepository.findByEmail("missing@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findAll(authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Authenticated user not found");
    }

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

    private Account buildAccount(User user, Long id, String name, BigDecimal initialBalance) {
        Account account = new Account();
        account.setId(id);
        account.setUser(user);
        account.setName(name);
        account.setType(AccountType.CHECKING_ACCOUNT);
        account.setInitialBalance(initialBalance);
        return account;
    }

    private void stubBalance(Account account, String income, String expense) {
        when(transactionRepository.sumAmountByAccountAndUserAndType(
                account.getId(), 1L, TransactionType.INCOME)).thenReturn(new BigDecimal(income));
        when(transactionRepository.sumAmountByAccountAndUserAndType(
                account.getId(), 1L, TransactionType.EXPENSE)).thenReturn(new BigDecimal(expense));
    }
}
