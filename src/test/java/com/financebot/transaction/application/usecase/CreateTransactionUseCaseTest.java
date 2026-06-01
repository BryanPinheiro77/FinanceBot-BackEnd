package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.transaction.application.command.CreateTransactionCommand;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.SaveTransactionPort;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.domain.User;
import com.financebot.user.service.UserResourceResolver;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTransactionUseCaseTest {

    @Mock
    private SaveTransactionPort saveTransactionPort;

    @Mock
    private UserResourceResolver userResourceResolver;

    @Mock
    private TransactionCategoryValidator transactionCategoryValidator;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private CreateTransactionUseCase createTransactionUseCase;

    @Test
    @DisplayName("deve criar transação com sucesso quando dados forem válidos")
    void shouldCreateTransactionSuccessfully() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);
        Category category = buildCategory(20L, CategoryType.EXPENSE);

        CreateTransactionCommand command = buildCreateTransactionCommand(user);

        Transaction transaction = new Transaction();
        Transaction savedTransaction = new Transaction();
        TransactionResponse response = mock(TransactionResponse.class);

        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);
        when(transactionMapper.toEntity(command)).thenReturn(transaction);
        when(saveTransactionPort.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

        TransactionResponse result = createTransactionUseCase.execute(command);

        assertThat(result).isEqualTo(response);
        assertTransactionLinkedToUserResources(transaction, user, account, category);
        assertTransactionMarkedAsNonInstallment(transaction);

        verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
        verify(transactionMapper).toEntity(command);
        verify(saveTransactionPort).save(transaction);
        verify(transactionMapper).toResponse(savedTransaction);
    }

    @Test
    @DisplayName("deve lançar erro quando conta não pertencer ao usuário")
    void shouldThrowWhenAccountIsNotFoundForUser() {
        User user = buildUser(1L, "bryan@email.com");
        CreateTransactionCommand command = buildCreateTransactionCommand(user);

        when(userResourceResolver.resolveAccount(10L, 1L))
                .thenThrow(new EntityNotFoundException("Account not found"));

        assertThatThrownBy(() -> createTransactionUseCase.execute(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Account not found");

        verify(userResourceResolver, never()).resolveCategory(any(), any());
        verifyNoInteractions(transactionCategoryValidator, saveTransactionPort, transactionMapper);
    }

    @Test
    @DisplayName("deve lançar erro quando categoria não pertencer ao usuário")
    void shouldThrowWhenCategoryIsNotFoundForUser() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);
        CreateTransactionCommand command = buildCreateTransactionCommand(user);

        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L))
                .thenThrow(new EntityNotFoundException("Category not found"));

        assertThatThrownBy(() -> createTransactionUseCase.execute(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Category not found");

        verifyNoInteractions(transactionCategoryValidator, saveTransactionPort, transactionMapper);
    }

    @Test
    @DisplayName("deve lançar erro quando categoria não combinar com o tipo da transação")
    void shouldThrowWhenCategoryTypeDoesNotMatchTransactionType() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);
        Category category = buildCategory(20L, CategoryType.INCOME);
        CreateTransactionCommand command = buildCreateTransactionCommand(user);

        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);

        doThrow(new IllegalArgumentException("Category type does not match transaction type"))
                .when(transactionCategoryValidator)
                .validate(category, TransactionType.EXPENSE);

        assertThatThrownBy(() -> createTransactionUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category type does not match transaction type");

        verifyNoInteractions(saveTransactionPort, transactionMapper);
    }

    private void assertTransactionLinkedToUserResources(
            Transaction transaction,
            User user,
            Account account,
            Category category
    ) {
        assertThat(transaction.getUser()).isEqualTo(user);
        assertThat(transaction.getAccount()).isEqualTo(account);
        assertThat(transaction.getCategory()).isEqualTo(category);
    }

    private void assertTransactionMarkedAsNonInstallment(Transaction transaction) {
        assertThat(transaction.getInstallment()).isFalse();
        assertThat(transaction.getInstallmentNumber()).isNull();
        assertThat(transaction.getTotalInstallments()).isNull();
        assertThat(transaction.getInstallmentGroupId()).isNull();
    }

    private CreateTransactionCommand buildCreateTransactionCommand(User user) {
        return new CreateTransactionCommand(
                new BigDecimal("150.00"),
                "Mercado",
                LocalDate.of(2026, 4, 4),
                TransactionType.EXPENSE,
                SourceType.WEB,
                10L,
                20L,
                user
        );
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private Account buildAccount(Long id) {
        Account account = new Account();
        account.setId(id);
        account.setName("Conta teste");
        return account;
    }

    private Category buildCategory(Long id, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setName("Categoria teste");
        category.setType(type);
        return category;
    }
}