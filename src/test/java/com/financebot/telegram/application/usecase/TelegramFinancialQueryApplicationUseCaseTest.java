package com.financebot.telegram.application.usecase;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramFinancialQueryApplicationUseCaseTest {
    @Mock UserRepository userRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock FinancialAnalysisService financialAnalysisService;
    @InjectMocks TelegramFinancialQueryApplicationUseCase useCase;

    @Test
    void shouldReturnCurrentMonthSummary() {
        User user = user();
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByUserAndTypeBetweenDates(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(new BigDecimal("250"));

        var response = useCase.currentMonthSummary(10L, TransactionType.EXPENSE);

        assertThat(response.totalAmount()).isEqualByComparingTo("250");
        assertThat(response.type()).isEqualTo("EXPENSE");
    }

    @Test
    void shouldUseRepositoryFilterAccordingToTransactionSummaryArguments() {
        User user = user();
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.sumAmountByUserAndTypeAndDateBetweenAndCategoryAndAccount(
                eq(1L), eq(TransactionType.EXPENSE), any(), any(), eq("Casa"), eq("Conta")))
                .thenReturn(new BigDecimal("100"));

        var response = useCase.transactionSummary(10L, TransactionType.EXPENSE, "despesas", " Casa ", " Conta ",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(response.totalAmount()).isEqualByComparingTo("100");
        assertThat(response.categoryName()).isEqualTo("Casa");
        assertThat(response.accountName()).isEqualTo("Conta");
        verify(transactionRepository).sumAmountByUserAndTypeAndDateBetweenAndCategoryAndAccount(
                eq(1L), eq(TransactionType.EXPENSE), any(), any(), eq("Casa"), eq("Conta"));
    }

    @Test
    void shouldCountInstallmentsAndNormalizeNullCount() {
        User user = user();
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.countInstallmentsByUserBetweenDates(eq(1L), any(), any())).thenReturn(null);

        var response = useCase.installmentCount(10L, LocalDate.now().minusDays(2), LocalDate.now());

        assertThat(response.installmentCount()).isZero();
    }

    @Test
    void shouldRejectMissingInstallmentDates() {
        assertThatThrownBy(() -> useCase.installmentCount(10L, null, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date and end date are required");
    }

    @Test
    void shouldValidateInstallmentCapacityAndDelegate() {
        User user = user();
        InstallmentPurchaseCapacityResponse expected = new InstallmentPurchaseCapacityResponse(
                new BigDecimal("100"), 4, new BigDecimal("25"), "VIAVEL", "ok");
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(financialAnalysisService.analyzeInstallmentPurchaseCapacity(user, new BigDecimal("100"), 4))
                .thenReturn(expected);

        assertThat(useCase.installmentCapacity(10L, new BigDecimal("100"), 4)).isEqualTo(expected);
        assertThatThrownBy(() -> useCase.installmentCapacity(10L, BigDecimal.ZERO, 4))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> useCase.installmentCapacity(10L, new BigDecimal("100"), 1))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldReturnEmptyActiveInstallmentSummaryWhenThereAreNoTransactions() {
        User user = user();
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.findActiveInstallmentTransactionsByUser(eq(1L), any())).thenReturn(List.of());

        var response = useCase.activeInstallmentSummary(10L, null);

        assertThat(response.hasActiveInstallment()).isFalse();
    }

    @Test
    void shouldRejectMultipleActiveInstallmentGroups() {
        User user = user();
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.findActiveInstallmentTransactionsByUser(eq(1L), any()))
                .thenReturn(List.of(transaction("Compra A - 1/2", "a"), transaction("Compra B - 1/2", "b")));

        assertThatThrownBy(() -> useCase.activeInstallmentSummary(10L, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Multiple active installments found");
    }

    @Test
    void shouldReturnFilteredActiveInstallmentSummary() {
        User user = user();
        Transaction current = transaction("Café - 1/2", "group");
        current.setDate(LocalDate.now().minusDays(1));
        current.setInstallmentNumber(1);
        current.setTotalInstallments(2);
        Transaction future = transaction("Café - 2/2", "group");
        future.setDate(LocalDate.now().plusDays(10));
        future.setInstallmentNumber(2);
        future.setTotalInstallments(2);
        when(userRepository.findByTelegramId(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.findActiveInstallmentTransactionsByUser(eq(1L), any()))
                .thenReturn(List.of(current, future));
        when(transactionRepository.findInstallmentTransactionsByGroupIdAndUser(1L, "group"))
                .thenReturn(List.of(current, future));

        var response = useCase.activeInstallmentSummary(10L, " cafe ");

        assertThat(response.hasActiveInstallment()).isTrue();
        assertThat(response.description()).isEqualTo("Café");
        assertThat(response.totalInstallments()).isEqualTo(2);
        assertThat(response.remainingInstallments()).isEqualTo(1);
    }

    @Test
    void shouldRejectUnknownTelegramUser() {
        when(userRepository.findByTelegramId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.financialAnalysis(99L))
                .isInstanceOf(TelegramUserNotFoundException.class);
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setTelegramId(10L);
        return user;
    }

    private Transaction transaction(String description, String group) {
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setInstallmentGroupId(group);
        transaction.setDate(LocalDate.now());
        transaction.setType(TransactionType.EXPENSE);
        return transaction;
    }
}
