package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.transaction.application.command.CreateInstallmentTransactionCommand;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.SaveTransactionPort;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.domain.installment.InstallmentPlan;
import com.financebot.transaction.domain.installment.InstallmentPlanFactory;
import com.financebot.transaction.domain.installment.InstallmentPlanItem;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.domain.User;
import com.financebot.user.service.UserResourceResolver;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateInstallmentTransactionUseCaseTest {

    private static final String INSTALLMENT_GROUP_ID = "installment-group-123";

    @Mock
    private SaveTransactionPort saveTransactionPort;

    @Mock
    private UserResourceResolver userResourceResolver;

    @Mock
    private TransactionCategoryValidator transactionCategoryValidator;

    @Mock
    private InstallmentPlanFactory installmentPlanFactory;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private CreateInstallmentTransactionUseCase createInstallmentTransactionUseCase;

    @Test
    @DisplayName("deve criar parcelamento com sucesso a partir do plano de domínio")
    void shouldCreateInstallmentSuccessfullyFromDomainPlan() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);
        Category category = buildCategory(20L, CategoryType.EXPENSE);

        CreateInstallmentTransactionCommand command = buildInstallmentCommand(user);

        TransactionResponse response1 = mock(TransactionResponse.class);
        TransactionResponse response2 = mock(TransactionResponse.class);
        TransactionResponse response3 = mock(TransactionResponse.class);

        mockInstallmentCreationDependencies(user, account, category, command, response1, response2, response3);

        InstallmentTransactionResponse result = createInstallmentTransactionUseCase.execute(command);

        List<Transaction> savedTransactions = captureSavedInstallments();

        assertInstallmentAmounts(savedTransactions);
        assertInstallmentDescriptions(savedTransactions);
        assertInstallmentDates(savedTransactions);
        assertInstallmentCommonData(savedTransactions, user, account, category);
        assertInstallmentMetadata(savedTransactions, result);
        assertInstallmentResponse(result, response1, response2, response3);

        verify(installmentPlanFactory).create(
                command.totalAmount(),
                command.description(),
                command.firstInstallmentDate(),
                command.type(),
                command.totalInstallments()
        );
        verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("deve lançar erro quando plano de parcelamento rejeitar receita")
    void shouldThrowWhenInstallmentPlanRejectsIncomeTransaction() {
        User user = buildUser(1L, "bryan@email.com");

        CreateInstallmentTransactionCommand command = new CreateInstallmentTransactionCommand(
                new BigDecimal("1000.00"),
                "Salário parcelado",
                LocalDate.of(2026, 4, 10),
                TransactionType.INCOME,
                SourceType.WEB,
                10L,
                20L,
                3,
                user
        );

        when(installmentPlanFactory.create(
                command.totalAmount(),
                command.description(),
                command.firstInstallmentDate(),
                command.type(),
                command.totalInstallments()
        )).thenThrow(new IllegalArgumentException("Installment transactions are allowed only for expenses"));

        assertThatThrownBy(() -> createInstallmentTransactionUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Installment transactions are allowed only for expenses");

        verifyNoInteractions(userResourceResolver, transactionCategoryValidator, transactionMapper);
        verify(saveTransactionPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("deve lançar erro quando plano de parcelamento rejeitar total menor que dois")
    void shouldThrowWhenInstallmentPlanRejectsTotalInstallmentsLessThanTwo() {
        User user = buildUser(1L, "bryan@email.com");

        CreateInstallmentTransactionCommand command = new CreateInstallmentTransactionCommand(
                new BigDecimal("1000.00"),
                "Notebook",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB,
                10L,
                20L,
                1,
                user
        );

        when(installmentPlanFactory.create(
                command.totalAmount(),
                command.description(),
                command.firstInstallmentDate(),
                command.type(),
                command.totalInstallments()
        )).thenThrow(new IllegalArgumentException("Total installments must be at least 2"));

        assertThatThrownBy(() -> createInstallmentTransactionUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total installments must be at least 2");

        verifyNoInteractions(userResourceResolver, transactionCategoryValidator, transactionMapper);
        verify(saveTransactionPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("deve lançar erro quando conta não pertencer ao usuário no parcelamento")
    void shouldThrowWhenAccountIsNotFoundForUserOnCreateInstallment() {
        User user = buildUser(1L, "bryan@email.com");

        CreateInstallmentTransactionCommand command = buildInstallmentCommand(user);
        InstallmentPlan plan = buildInstallmentPlan();

        when(installmentPlanFactory.create(
                command.totalAmount(),
                command.description(),
                command.firstInstallmentDate(),
                command.type(),
                command.totalInstallments()
        )).thenReturn(plan);
        when(userResourceResolver.resolveAccount(10L, 1L))
                .thenThrow(new EntityNotFoundException("Account not found"));

        assertThatThrownBy(() -> createInstallmentTransactionUseCase.execute(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Account not found");

        verify(userResourceResolver, never()).resolveCategory(any(), any());
        verifyNoInteractions(transactionCategoryValidator, transactionMapper);
        verify(saveTransactionPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("deve lançar erro quando categoria não pertencer ao usuário no parcelamento")
    void shouldThrowWhenCategoryIsNotFoundForUserOnCreateInstallment() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);

        CreateInstallmentTransactionCommand command = buildInstallmentCommand(user);
        InstallmentPlan plan = buildInstallmentPlan();

        when(installmentPlanFactory.create(
                command.totalAmount(),
                command.description(),
                command.firstInstallmentDate(),
                command.type(),
                command.totalInstallments()
        )).thenReturn(plan);
        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L))
                .thenThrow(new EntityNotFoundException("Category not found"));

        assertThatThrownBy(() -> createInstallmentTransactionUseCase.execute(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Category not found");

        verifyNoInteractions(transactionCategoryValidator, transactionMapper);
        verify(saveTransactionPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("deve lançar erro quando categoria não combinar com o tipo no parcelamento")
    void shouldThrowWhenCategoryTypeDoesNotMatchTransactionTypeOnCreateInstallment() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);
        Category category = buildCategory(20L, CategoryType.INCOME);

        CreateInstallmentTransactionCommand command = buildInstallmentCommand(user);
        InstallmentPlan plan = buildInstallmentPlan();

        when(installmentPlanFactory.create(
                command.totalAmount(),
                command.description(),
                command.firstInstallmentDate(),
                command.type(),
                command.totalInstallments()
        )).thenReturn(plan);
        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);

        doThrow(new IllegalArgumentException("Category type does not match transaction type"))
                .when(transactionCategoryValidator)
                .validate(category, TransactionType.EXPENSE);

        assertThatThrownBy(() -> createInstallmentTransactionUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category type does not match transaction type");

        verifyNoInteractions(transactionMapper);
        verify(saveTransactionPort, never()).saveAll(anyList());
    }

    private CreateInstallmentTransactionCommand buildInstallmentCommand(User user) {
        return new CreateInstallmentTransactionCommand(
                new BigDecimal("1000.00"),
                "Notebook",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB,
                10L,
                20L,
                3,
                user
        );
    }

    private void mockInstallmentCreationDependencies(
            User user,
            Account account,
            Category category,
            CreateInstallmentTransactionCommand command,
            TransactionResponse response1,
            TransactionResponse response2,
            TransactionResponse response3
    ) {
        InstallmentPlan plan = buildInstallmentPlan();

        when(installmentPlanFactory.create(
                command.totalAmount(),
                command.description(),
                command.firstInstallmentDate(),
                command.type(),
                command.totalInstallments()
        )).thenReturn(plan);

        when(userResourceResolver.resolveAccount(command.accountId(), user.getId())).thenReturn(account);
        when(userResourceResolver.resolveCategory(command.categoryId(), user.getId())).thenReturn(category);

        when(saveTransactionPort.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(response1, response2, response3);
    }

    private InstallmentPlan buildInstallmentPlan() {
        List<InstallmentPlanItem> items = List.of(
                new InstallmentPlanItem(
                        new BigDecimal("333.33"),
                        "Notebook - 1/3",
                        LocalDate.of(2026, 4, 10),
                        1,
                        3,
                        INSTALLMENT_GROUP_ID
                ),
                new InstallmentPlanItem(
                        new BigDecimal("333.33"),
                        "Notebook - 2/3",
                        LocalDate.of(2026, 5, 10),
                        2,
                        3,
                        INSTALLMENT_GROUP_ID
                ),
                new InstallmentPlanItem(
                        new BigDecimal("333.34"),
                        "Notebook - 3/3",
                        LocalDate.of(2026, 6, 10),
                        3,
                        3,
                        INSTALLMENT_GROUP_ID
                )
        );

        return new InstallmentPlan(
                INSTALLMENT_GROUP_ID,
                3,
                items
        );
    }

    private List<Transaction> captureSavedInstallments() {
        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);

        verify(saveTransactionPort).saveAll(captor.capture());

        return captor.getValue();
    }

    private void assertInstallmentAmounts(List<Transaction> savedTransactions) {
        assertThat(savedTransactions).hasSize(3);
        assertThat(savedTransactions.get(0).getAmount()).isEqualByComparingTo("333.33");
        assertThat(savedTransactions.get(1).getAmount()).isEqualByComparingTo("333.33");
        assertThat(savedTransactions.get(2).getAmount()).isEqualByComparingTo("333.34");
    }

    private void assertInstallmentDescriptions(List<Transaction> savedTransactions) {
        assertThat(savedTransactions.get(0).getDescription()).isEqualTo("Notebook - 1/3");
        assertThat(savedTransactions.get(1).getDescription()).isEqualTo("Notebook - 2/3");
        assertThat(savedTransactions.get(2).getDescription()).isEqualTo("Notebook - 3/3");
    }

    private void assertInstallmentDates(List<Transaction> savedTransactions) {
        assertThat(savedTransactions.get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(savedTransactions.get(1).getDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(savedTransactions.get(2).getDate()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    private void assertInstallmentCommonData(
            List<Transaction> savedTransactions,
            User user,
            Account account,
            Category category
    ) {
        Transaction first = savedTransactions.get(0);

        assertThat(first.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(first.getSourceType()).isEqualTo(SourceType.WEB);
        assertThat(first.getUser()).isEqualTo(user);
        assertThat(first.getAccount()).isEqualTo(account);
        assertThat(first.getCategory()).isEqualTo(category);
    }

    private void assertInstallmentMetadata(
            List<Transaction> savedTransactions,
            InstallmentTransactionResponse result
    ) {
        Transaction first = savedTransactions.get(0);
        Transaction second = savedTransactions.get(1);
        Transaction third = savedTransactions.get(2);

        assertThat(first.getInstallment()).isTrue();
        assertThat(second.getInstallment()).isTrue();
        assertThat(third.getInstallment()).isTrue();

        assertThat(first.getInstallmentNumber()).isEqualTo(1);
        assertThat(second.getInstallmentNumber()).isEqualTo(2);
        assertThat(third.getInstallmentNumber()).isEqualTo(3);

        assertThat(first.getTotalInstallments()).isEqualTo(3);
        assertThat(second.getTotalInstallments()).isEqualTo(3);
        assertThat(third.getTotalInstallments()).isEqualTo(3);

        assertThat(first.getInstallmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(second.getInstallmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(third.getInstallmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);

        assertThat(result.installmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
    }

    private void assertInstallmentResponse(
            InstallmentTransactionResponse result,
            TransactionResponse response1,
            TransactionResponse response2,
            TransactionResponse response3
    ) {
        assertThat(result.installmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(result.totalInstallments()).isEqualTo(3);
        assertThat(result.transactions()).containsExactly(response1, response2, response3);
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