package com.financebot.analysis.service;

import com.financebot.analysis.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.category.domain.Category;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.domain.RecurrenceFrequency;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import com.financebot.user.service.UserResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Mock
    private UserResourceResolver userResourceResolver;

    @Mock
    private TransactionCategoryValidator transactionCategoryValidator;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private FinancialAnalysisService financialAnalysisService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("bryan@email.com");
        user.setMonthlyBaseIncome(new BigDecimal("5000"));
        lenient().when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
    }

    @Test
    @DisplayName("deve classificar como viavel quando a nova parcela nao pressiona o orcamento")
    void shouldClassifyAsViavel() {
        mockCurrentAnalysisData(
                new BigDecimal("800"),
                new BigDecimal("1500"),
                2L,
                List.of()
        );

        InstallmentPurchaseCapacityResponse response = financialAnalysisService.analyzeInstallmentPurchaseCapacity(
                user,
                new BigDecimal("1200"),
                12
        );

        assertThat(response.analysisResult()).isEqualTo("VIAVEL");
        assertThat(response.estimatedInstallmentAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("deve classificar como alerta quando a nova parcela eleva o comprometimento")
    void shouldClassifyAsAlerta() {
        mockCurrentAnalysisData(
                new BigDecimal("1200"),
                new BigDecimal("2800"),
                4L,
                List.of()
        );

        InstallmentPurchaseCapacityResponse response = financialAnalysisService.analyzeInstallmentPurchaseCapacity(
                user,
                new BigDecimal("2400"),
                12
        );

        assertThat(response.analysisResult()).isEqualTo("ALERTA");
        assertThat(response.estimatedInstallmentAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("deve classificar como desfavoravel quando a nova parcela deixa o saldo projetado negativo")
    void shouldClassifyAsDesfavoravel() {
        mockCurrentAnalysisData(
                new BigDecimal("2200"),
                new BigDecimal("4900"),
                6L,
                List.of()
        );

        InstallmentPurchaseCapacityResponse response = financialAnalysisService.analyzeInstallmentPurchaseCapacity(
                user,
                new BigDecimal("2400"),
                2
        );

        assertThat(response.analysisResult()).isEqualTo("DESFAVORAVEL");
        assertThat(response.estimatedInstallmentAmount()).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("deve classificar como desfavoravel quando nao houver renda de referencia")
    void shouldClassifyAsDesfavoravelWhenIncomeReferenceIsZero() {
        user.setMonthlyBaseIncome(BigDecimal.ZERO);

        mockCurrentAnalysisData(
                BigDecimal.ZERO,
                new BigDecimal("500"),
                1L,
                List.of()
        );

        when(transactionRepository.sumIncomeBetweenDatesByUser(eq(1L), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        InstallmentPurchaseCapacityResponse response = financialAnalysisService.analyzeInstallmentPurchaseCapacity(
                user,
                new BigDecimal("1200"),
                12
        );

        assertThat(response.analysisResult()).isEqualTo("DESFAVORAVEL");
    }

    @Test
    @DisplayName("deve classificar como alerta no limiar de sessenta por cento")
    void shouldClassifyAsAlertaAtSixtyPercentThreshold() {
        mockCurrentAnalysisData(
                BigDecimal.ZERO,
                new BigDecimal("2800"),
                1L,
                List.of()
        );

        InstallmentPurchaseCapacityResponse response = financialAnalysisService.analyzeInstallmentPurchaseCapacity(
                user,
                new BigDecimal("2000"),
                10
        );

        assertThat(response.analysisResult()).isEqualTo("ALERTA");
        assertThat(response.estimatedInstallmentAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("deve classificar como desfavoravel no limiar de oitenta por cento")
    void shouldClassifyAsDesfavoravelAtEightyPercentThreshold() {
        mockCurrentAnalysisData(
                BigDecimal.ZERO,
                new BigDecimal("3800"),
                1L,
                List.of()
        );

        InstallmentPurchaseCapacityResponse response = financialAnalysisService.analyzeInstallmentPurchaseCapacity(
                user,
                new BigDecimal("2000"),
                10
        );

        assertThat(response.analysisResult()).isEqualTo("DESFAVORAVEL");
        assertThat(response.estimatedInstallmentAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("deve lancar erro quando valor total for invalido")
    void shouldThrowWhenTotalAmountIsInvalid() {
        assertThatThrownBy(() -> financialAnalysisService.analyzeInstallmentPurchaseCapacity(
                user,
                BigDecimal.ZERO,
                12
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total amount must be greater than zero");
    }

    @Test
    @DisplayName("deve lancar erro quando quantidade de parcelas for invalida")
    void shouldThrowWhenTotalInstallmentsIsInvalid() {
        BigDecimal totalAmount = new BigDecimal("1200");

        assertThatThrownBy(() -> financialAnalysisService.analyzeInstallmentPurchaseCapacity(
                user,
                totalAmount,
                1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total installments must be at least 2");
    }

    @Test
    @DisplayName("deve rejeitar usuario nulo ao consultar comprometimento")
    void shouldRejectNullUserOnCommitmentQuery() {
        assertThatThrownBy(() -> financialAnalysisService.getFinancialCommitment((User) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is required");
    }

    @Test
    @DisplayName("deve resolver usuario autenticado ao consultar comprometimento")
    void shouldResolveAuthenticatedUserOnCommitmentQuery() {
        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        mockCurrentAnalysisData(new BigDecimal("300"), new BigDecimal("1000"), 2L, List.of());

        FinancialCommitmentResponse response = financialAnalysisService.getFinancialCommitment(authentication);

        assertThat(response.monthlyIncomeReference()).isEqualByComparingTo("5000");
        assertThat(response.nextMonthProjectedExpense()).isEqualByComparingTo("1000");
        verify(authenticatedUserResolver).resolve(authentication);
    }

    @Test
    @DisplayName("deve projetar receitas e despesas recorrentes ativas no proximo mes")
    void shouldProjectActiveRecurringTransactionsForNextMonth() {
        java.time.YearMonth nextMonth = java.time.YearMonth.now().plusMonths(1);
        RecurringTransaction expense = recurring(new BigDecimal("300"), TransactionType.EXPENSE,
                nextMonth.atDay(1), RecurrenceFrequency.MONTHLY, true);
        RecurringTransaction income = recurring(new BigDecimal("800"), TransactionType.INCOME,
                nextMonth.atDay(2), RecurrenceFrequency.MONTHLY, true);
        RecurringTransaction inactive = recurring(new BigDecimal("999"), TransactionType.EXPENSE,
                nextMonth.atDay(3), RecurrenceFrequency.MONTHLY, false);
        mockCurrentAnalysisData(BigDecimal.ZERO, BigDecimal.ZERO, 0L, List.of(expense, income, inactive));

        FinancialCommitmentResponse response = financialAnalysisService.getFinancialCommitment(user);

        assertThat(response.projectedRecurringExpenseNextMonth()).isEqualByComparingTo("300");
        assertThat(response.projectedRecurringIncomeNextMonth()).isEqualByComparingTo("800");
        assertThat(response.nextMonthProjectedExpense()).isEqualByComparingTo("300");
    }

    @Test
    @DisplayName("deve incluir despesa prevista para o proximo mes na previa")
    void shouldIncludeNextMonthExpenseInTransactionPreview() {
        mockCurrentAnalysisData(BigDecimal.ZERO, new BigDecimal("1000"), 2L, List.of());
        Category category = new Category();
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("250"), "mercado", java.time.YearMonth.now().plusMonths(1).atDay(5),
                TransactionType.EXPENSE, SourceType.WEB, 10L, 20L
        );
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);

        FinancialCommitmentResponse response = financialAnalysisService.previewTransactionAlert(request, authentication);

        assertThat(response.nextMonthProjectedExpense()).isEqualByComparingTo("1250");
        verify(userResourceResolver).resolveAccount(10L, 1L);
        verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("deve incluir renda prevista para o proximo mes na previa")
    void shouldIncludeNextMonthIncomeInTransactionPreview() {
        mockCurrentAnalysisData(BigDecimal.ZERO, new BigDecimal("1000"), 2L, List.of());
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("750"), "salario", java.time.YearMonth.now().plusMonths(1).atDay(5),
                TransactionType.INCOME, SourceType.WEB, 10L, 20L
        );
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(new Category());

        FinancialCommitmentResponse response = financialAnalysisService.previewTransactionAlert(request, authentication);

        assertThat(response.nextMonthProjectedIncome()).isEqualByComparingTo("5750");
        assertThat(response.projectedNetNextMonth()).isEqualByComparingTo("4750");
    }

    @Test
    @DisplayName("nao deve alterar projecao para transacao do mes atual")
    void shouldKeepProjectionForCurrentMonthTransaction() {
        mockCurrentAnalysisData(BigDecimal.ZERO, new BigDecimal("1000"), 2L, List.of());
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("750"), "compra", java.time.LocalDate.now(),
                TransactionType.EXPENSE, SourceType.WEB, 10L, 20L
        );
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(new Category());

        FinancialCommitmentResponse response = financialAnalysisService.previewTransactionAlert(request, authentication);

        assertThat(response.nextMonthProjectedExpense()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("deve projetar parcelas futuras e incrementar grupos ativos")
    void shouldProjectInstallmentsInPreview() {
        mockCurrentAnalysisData(BigDecimal.ZERO, BigDecimal.ZERO, 2L, List.of());
        CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                new BigDecimal("1000"), "celular", java.time.YearMonth.now().plusMonths(1).atDay(1),
                TransactionType.EXPENSE, SourceType.WEB, 10L, 20L, 3
        );
        when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(new Category());

        FinancialCommitmentResponse response = financialAnalysisService.previewInstallmentAlert(request, authentication);

        assertThat(response.totalFutureInstallments()).isEqualByComparingTo("1000");
        assertThat(response.nextMonthProjectedExpense()).isEqualByComparingTo("333.33");
        assertThat(response.activeInstallmentCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("deve rejeitar parcelas para receitas")
    void shouldRejectInstallmentIncomePreview() {
        CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                new BigDecimal("1000"), "salario", java.time.LocalDate.now(),
                TransactionType.INCOME, SourceType.WEB, 10L, 20L, 3
        );

        assertThatThrownBy(() -> financialAnalysisService.previewInstallmentAlert(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Installment transactions are allowed only for expenses");
    }

    @Test
    @DisplayName("deve rejeitar quantidade invalida de parcelas na previa")
    void shouldRejectInvalidInstallmentCountInPreview() {
        CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                new BigDecimal("1000"), "compra", java.time.LocalDate.now(),
                TransactionType.EXPENSE, SourceType.WEB, 10L, 20L, 1
        );

        assertThatThrownBy(() -> financialAnalysisService.previewInstallmentAlert(request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total installments must be at least 2");
    }

    private void mockCurrentAnalysisData(
            BigDecimal futureInstallments,
            BigDecimal projectedExpensesNextMonth,
            Long activeInstallmentCount,
            List<RecurringTransaction> recurringTransactions
    ) {
        when(transactionRepository.sumFutureInstallmentsByUser(eq(1L), any()))
                .thenReturn(futureInstallments);

        when(transactionRepository.sumProjectedExpensesBetweenDatesByUser(eq(1L), any(), any()))
                .thenReturn(projectedExpensesNextMonth);

        when(transactionRepository.countDistinctActiveInstallmentGroupsByUser(eq(1L), any()))
                .thenReturn(activeInstallmentCount);

        when(recurringTransactionRepository.findAllByUserIdAndActiveTrue(1L))
                .thenReturn(recurringTransactions);
    }

    private RecurringTransaction recurring(
            BigDecimal amount,
            TransactionType type,
            java.time.LocalDate nextExecutionDate,
            RecurrenceFrequency frequency,
            boolean active
    ) {
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setAmount(amount);
        recurring.setType(type);
        recurring.setFrequency(frequency);
        recurring.setStartDate(nextExecutionDate);
        recurring.setNextExecutionDate(nextExecutionDate);
        recurring.setActive(active);
        return recurring;
    }
}
