package com.financebot.analysis.service;

import com.financebot.account.repository.AccountRepository;
import com.financebot.analysis.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private FinancialAnalysisService financialAnalysisService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("bryan@email.com");
        user.setMonthlyBaseIncome(new BigDecimal("5000"));
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
}
