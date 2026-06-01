package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.transaction.application.command.UpdateTransactionCommand;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.FindTransactionPort;
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
import java.util.Optional;

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
class UpdateTransactionUseCaseTest {

    @Mock
    private FindTransactionPort findTransactionPort;

    @Mock
    private SaveTransactionPort saveTransactionPort;

    @Mock
    private UserResourceResolver userResourceResolver;

    @Mock
    private TransactionCategoryValidator transactionCategoryValidator;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private UpdateTransactionUseCase updateTransactionUseCase;

    @Test
    @DisplayName("deve atualizar transação com sucesso")
    void shouldUpdateSuccessfully() {
        User user = buildUser(1L, "bryan@email.com");
        Transaction transaction = new Transaction();
        Account account = buildAccount(10L);
        Category category = buildCategory(20L, CategoryType.INCOME);
        TransactionResponse response = mock(TransactionResponse.class);

        UpdateTransactionCommand command = buildUpdateTransactionCommand(user);

        when(findTransactionPort.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(transaction));
        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);
        when(saveTransactionPort.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionResponse result = updateTransactionUseCase.execute(command);

        assertThat(result).isEqualTo(response);
        assertThat(transaction.getAccount()).isEqualTo(account);
        assertThat(transaction.getCategory()).isEqualTo(category);

        verify(findTransactionPort).findByIdAndUserId(77L, 1L);
        verify(userResourceResolver).resolveAccount(10L, 1L);
        verify(userResourceResolver).resolveCategory(20L, 1L);
        verify(transactionCategoryValidator).validate(category, TransactionType.INCOME);
        verify(transactionMapper).updateEntity(command, transaction);
        verify(saveTransactionPort).save(transaction);
        verify(transactionMapper).toResponse(transaction);
    }

    @Test
    @DisplayName("deve lançar erro quando transação a atualizar não existir")
    void shouldThrowWhenTransactionToUpdateDoesNotExist() {
        User user = buildUser(1L, "bryan@email.com");
        UpdateTransactionCommand command = buildUpdateTransactionCommand(user);

        when(findTransactionPort.findByIdAndUserId(77L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateTransactionUseCase.execute(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Transaction not found");

        verifyNoInteractions(userResourceResolver, transactionCategoryValidator, transactionMapper, saveTransactionPort);
    }

    @Test
    @DisplayName("deve lançar erro quando conta informada no update não existir para o usuário")
    void shouldThrowWhenAccountForUpdateIsNotFound() {
        User user = buildUser(1L, "bryan@email.com");
        Transaction transaction = new Transaction();
        UpdateTransactionCommand command = buildUpdateTransactionCommand(user);

        when(findTransactionPort.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(transaction));
        when(userResourceResolver.resolveAccount(10L, 1L))
                .thenThrow(new EntityNotFoundException("Account not found"));

        assertThatThrownBy(() -> updateTransactionUseCase.execute(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Account not found");

        verify(userResourceResolver, never()).resolveCategory(any(), any());
        verifyNoInteractions(transactionCategoryValidator);
        verify(transactionMapper, never()).updateEntity(any(UpdateTransactionCommand.class), any(Transaction.class));
        verify(saveTransactionPort, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar erro quando categoria informada no update não existir para o usuário")
    void shouldThrowWhenCategoryForUpdateIsNotFound() {
        User user = buildUser(1L, "bryan@email.com");
        Transaction transaction = new Transaction();
        Account account = buildAccount(10L);
        UpdateTransactionCommand command = buildUpdateTransactionCommand(user);

        when(findTransactionPort.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(transaction));
        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L))
                .thenThrow(new EntityNotFoundException("Category not found"));

        assertThatThrownBy(() -> updateTransactionUseCase.execute(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Category not found");

        verifyNoInteractions(transactionCategoryValidator);
        verify(transactionMapper, never()).updateEntity(any(UpdateTransactionCommand.class), any(Transaction.class));
        verify(saveTransactionPort, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar erro quando categoria não combinar com o tipo no update")
    void shouldThrowWhenCategoryTypeDoesNotMatchOnUpdate() {
        User user = buildUser(1L, "bryan@email.com");
        Transaction transaction = new Transaction();
        Account account = buildAccount(10L);
        Category category = buildCategory(20L, CategoryType.EXPENSE);
        UpdateTransactionCommand command = buildUpdateTransactionCommand(user);

        when(findTransactionPort.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(transaction));
        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);

        doThrow(new IllegalArgumentException("Category type does not match transaction type"))
                .when(transactionCategoryValidator)
                .validate(category, TransactionType.INCOME);

        assertThatThrownBy(() -> updateTransactionUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category type does not match transaction type");

        verify(transactionMapper, never()).updateEntity(any(UpdateTransactionCommand.class), any(Transaction.class));
        verify(saveTransactionPort, never()).save(any());
    }

    private UpdateTransactionCommand buildUpdateTransactionCommand(User user) {
        return new UpdateTransactionCommand(
                77L,
                new BigDecimal("500.00"),
                "Salário",
                LocalDate.of(2026, 4, 5),
                TransactionType.INCOME,
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