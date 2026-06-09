package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.transaction.application.command.CreateExistingInstallmentTransactionCommand;
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
import org.mockito.Spy;
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
class CreateExistingInstallmentTransactionUseCaseTest {

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

    @Spy
    private InstallmentTransactionBuilder installmentTransactionBuilder;

    @InjectMocks
    private CreateExistingInstallmentTransactionUseCase createExistingInstallmentTransactionUseCase;

    @Test
    @DisplayName("deve criar parcelamento existente com sucesso a partir do plano de parcelas restantes")
    void shouldCreateExistingInstallmentSuccessfullyFromRemainingPlan() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);
        Category category = buildCategory(20L, CategoryType.EXPENSE);

        CreateExistingInstallmentTransactionCommand command = buildExistingInstallmentCommand(user);

        TransactionResponse response1 = mock(TransactionResponse.class);
        TransactionResponse response2 = mock(TransactionResponse.class);
        TransactionResponse response3 = mock(TransactionResponse.class);

        mockExistingInstallmentCreationDependencies(
                user,
                account,
                category,
                command,
                response1,
                response2,
                response3
        );

        InstallmentTransactionResponse result = createExistingInstallmentTransactionUseCase.execute(command);

        List<Transaction> savedTransactions = captureSavedInstallments();

        assertRemainingInstallmentAmounts(savedTransactions);
        assertRemainingInstallmentDescriptions(savedTransactions);
        assertRemainingInstallmentDates(savedTransactions);
        assertRemainingInstallmentCommonData(savedTransactions, user, account, category);
        assertRemainingInstallmentMetadata(savedTransactions, result);
        assertRemainingInstallmentResponse(result, response1, response2, response3);

        verify(installmentPlanFactory).createRemaining(
                command.totalAmount(),
                command.description(),
                command.firstRemainingInstallmentDate(),
                command.type(),
                command.totalInstallments(),
                command.firstRemainingInstallmentNumber()
        );
        verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("deve lançar erro quando plano restante rejeitar primeira parcela inválida")
    void shouldThrowWhenRemainingPlanRejectsInvalidFirstRemainingInstallment() {
        User user = buildUser(1L, "bryan@email.com");
        CreateExistingInstallmentTransactionCommand command = new CreateExistingInstallmentTransactionCommand(
                new BigDecimal("1000.00"),
                "Notebook",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB,
                10L,
                20L,
                3,
                4,
                user
        );

        when(installmentPlanFactory.createRemaining(
                command.totalAmount(),
                command.description(),
                command.firstRemainingInstallmentDate(),
                command.type(),
                command.totalInstallments(),
                command.firstRemainingInstallmentNumber()
        )).thenThrow(new IllegalArgumentException(
                "First remaining installment number cannot be greater than total installments"
        ));

        assertThatThrownBy(() -> createExistingInstallmentTransactionUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("First remaining installment number cannot be greater than total installments");

        verifyNoInteractions(userResourceResolver, transactionCategoryValidator, transactionMapper);
        verify(saveTransactionPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("deve lançar erro quando conta não pertencer ao usuário no parcelamento existente")
    void shouldThrowWhenAccountIsNotFoundForUserOnCreateExistingInstallment() {
        User user = buildUser(1L, "bryan@email.com");
        CreateExistingInstallmentTransactionCommand command = buildExistingInstallmentCommand(user);
        InstallmentPlan plan = buildRemainingInstallmentPlan();

        when(installmentPlanFactory.createRemaining(
                command.totalAmount(),
                command.description(),
                command.firstRemainingInstallmentDate(),
                command.type(),
                command.totalInstallments(),
                command.firstRemainingInstallmentNumber()
        )).thenReturn(plan);
        when(userResourceResolver.resolveAccount(10L, 1L))
                .thenThrow(new EntityNotFoundException("Account not found"));

        assertThatThrownBy(() -> createExistingInstallmentTransactionUseCase.execute(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Account not found");

        verify(userResourceResolver, never()).resolveCategory(any(), any());
        verifyNoInteractions(transactionCategoryValidator, transactionMapper);
        verify(saveTransactionPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("deve lançar erro quando categoria não pertencer ao usuário no parcelamento existente")
    void shouldThrowWhenCategoryIsNotFoundForUserOnCreateExistingInstallment() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);
        CreateExistingInstallmentTransactionCommand command = buildExistingInstallmentCommand(user);
        InstallmentPlan plan = buildRemainingInstallmentPlan();

        when(installmentPlanFactory.createRemaining(
                command.totalAmount(),
                command.description(),
                command.firstRemainingInstallmentDate(),
                command.type(),
                command.totalInstallments(),
                command.firstRemainingInstallmentNumber()
        )).thenReturn(plan);
        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L))
                .thenThrow(new EntityNotFoundException("Category not found"));

        assertThatThrownBy(() -> createExistingInstallmentTransactionUseCase.execute(command))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Category not found");

        verifyNoInteractions(transactionCategoryValidator, transactionMapper);
        verify(saveTransactionPort, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("deve lançar erro quando categoria não combinar com tipo no parcelamento existente")
    void shouldThrowWhenCategoryTypeDoesNotMatchTransactionTypeOnCreateExistingInstallment() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);
        Category category = buildCategory(20L, CategoryType.INCOME);
        CreateExistingInstallmentTransactionCommand command = buildExistingInstallmentCommand(user);
        InstallmentPlan plan = buildRemainingInstallmentPlan();

        when(installmentPlanFactory.createRemaining(
                command.totalAmount(),
                command.description(),
                command.firstRemainingInstallmentDate(),
                command.type(),
                command.totalInstallments(),
                command.firstRemainingInstallmentNumber()
        )).thenReturn(plan);
        when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);

        doThrow(new IllegalArgumentException("Category type does not match transaction type"))
                .when(transactionCategoryValidator)
                .validate(category, TransactionType.EXPENSE);

        assertThatThrownBy(() -> createExistingInstallmentTransactionUseCase.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category type does not match transaction type");

        verifyNoInteractions(transactionMapper);
        verify(saveTransactionPort, never()).saveAll(anyList());
    }

    private CreateExistingInstallmentTransactionCommand buildExistingInstallmentCommand(User user) {
        return new CreateExistingInstallmentTransactionCommand(
                new BigDecimal("6000.00"),
                "iPhone",
                LocalDate.of(2026, 6, 15),
                TransactionType.EXPENSE,
                SourceType.WEB,
                10L,
                20L,
                10,
                6,
                user
        );
    }

    private void mockExistingInstallmentCreationDependencies(
            User user,
            Account account,
            Category category,
            CreateExistingInstallmentTransactionCommand command,
            TransactionResponse response1,
            TransactionResponse response2,
            TransactionResponse response3
    ) {
        InstallmentPlan plan = buildRemainingInstallmentPlan();

        when(installmentPlanFactory.createRemaining(
                command.totalAmount(),
                command.description(),
                command.firstRemainingInstallmentDate(),
                command.type(),
                command.totalInstallments(),
                command.firstRemainingInstallmentNumber()
        )).thenReturn(plan);

        when(userResourceResolver.resolveAccount(command.accountId(), user.getId())).thenReturn(account);
        when(userResourceResolver.resolveCategory(command.categoryId(), user.getId())).thenReturn(category);

        when(saveTransactionPort.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(response1, response2, response3);
    }

    private InstallmentPlan buildRemainingInstallmentPlan() {
        List<InstallmentPlanItem> items = List.of(
                new InstallmentPlanItem(
                        new BigDecimal("600.00"),
                        "iPhone - 6/10",
                        LocalDate.of(2026, 6, 15),
                        6,
                        10,
                        INSTALLMENT_GROUP_ID
                ),
                new InstallmentPlanItem(
                        new BigDecimal("600.00"),
                        "iPhone - 7/10",
                        LocalDate.of(2026, 7, 15),
                        7,
                        10,
                        INSTALLMENT_GROUP_ID
                ),
                new InstallmentPlanItem(
                        new BigDecimal("600.00"),
                        "iPhone - 8/10",
                        LocalDate.of(2026, 8, 15),
                        8,
                        10,
                        INSTALLMENT_GROUP_ID
                )
        );

        return new InstallmentPlan(
                INSTALLMENT_GROUP_ID,
                10,
                items
        );
    }

    private List<Transaction> captureSavedInstallments() {
        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);

        verify(saveTransactionPort).saveAll(captor.capture());

        return captor.getValue();
    }

    private void assertRemainingInstallmentAmounts(List<Transaction> savedTransactions) {
        assertThat(savedTransactions).hasSize(3);
        assertThat(savedTransactions.get(0).getAmount()).isEqualByComparingTo("600.00");
        assertThat(savedTransactions.get(1).getAmount()).isEqualByComparingTo("600.00");
        assertThat(savedTransactions.get(2).getAmount()).isEqualByComparingTo("600.00");
    }

    private void assertRemainingInstallmentDescriptions(List<Transaction> savedTransactions) {
        assertThat(savedTransactions.get(0).getDescription()).isEqualTo("iPhone - 6/10");
        assertThat(savedTransactions.get(1).getDescription()).isEqualTo("iPhone - 7/10");
        assertThat(savedTransactions.get(2).getDescription()).isEqualTo("iPhone - 8/10");
    }

    private void assertRemainingInstallmentDates(List<Transaction> savedTransactions) {
        assertThat(savedTransactions.get(0).getDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(savedTransactions.get(1).getDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(savedTransactions.get(2).getDate()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    private void assertRemainingInstallmentCommonData(
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

    private void assertRemainingInstallmentMetadata(
            List<Transaction> savedTransactions,
            InstallmentTransactionResponse result
    ) {
        Transaction first = savedTransactions.get(0);
        Transaction second = savedTransactions.get(1);
        Transaction third = savedTransactions.get(2);

        assertThat(first.getInstallment()).isTrue();
        assertThat(second.getInstallment()).isTrue();
        assertThat(third.getInstallment()).isTrue();

        assertThat(first.getInstallmentNumber()).isEqualTo(6);
        assertThat(second.getInstallmentNumber()).isEqualTo(7);
        assertThat(third.getInstallmentNumber()).isEqualTo(8);

        assertThat(first.getTotalInstallments()).isEqualTo(10);
        assertThat(second.getTotalInstallments()).isEqualTo(10);
        assertThat(third.getTotalInstallments()).isEqualTo(10);

        assertThat(first.getInstallmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(second.getInstallmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(third.getInstallmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);

        assertThat(result.installmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
    }

    private void assertRemainingInstallmentResponse(
            InstallmentTransactionResponse result,
            TransactionResponse response1,
            TransactionResponse response2,
            TransactionResponse response3
    ) {
        assertThat(result.installmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(result.totalInstallments()).isEqualTo(10);
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
