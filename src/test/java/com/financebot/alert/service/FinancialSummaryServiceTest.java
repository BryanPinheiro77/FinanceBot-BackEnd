package com.financebot.alert.service;

import com.financebot.alert.port.SummaryTransactionPort;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialSummaryServiceTest {

    private final FakeSummaryPort port = new FakeSummaryPort();
    private FinancialSummaryService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new FinancialSummaryService(
                port,
                Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC)
        );
        user = new User();
        user.setId(1L);
        port.income = new BigDecimal("2000");
        port.expense = new BigDecimal("1200");
    }

    @Test
    void shouldBuildPreviousCompletedWeekSummary() {
        var summary = service.previousCompletedWeek(user);

        assertThat(summary.periodType()).isEqualTo("WEEKLY");
        assertThat(summary.startDate()).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(summary.endDate()).isEqualTo(LocalDate.of(2026, 9, 13));
        assertThat(summary.balance()).isEqualByComparingTo("800");
        assertThat(summary.explanation()).contains("Resumo semanal");
    }

    @Test
    void shouldRejectUserWithoutId() {
        assertThatThrownBy(() -> service.previousCompletedWeek(new User()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with id is required");
    }

    private static final class FakeSummaryPort implements SummaryTransactionPort {
        private BigDecimal income;
        private BigDecimal expense;

        @Override
        public BigDecimal sumAmountByUserAndTypeBetweenDates(
                Long userId, TransactionType type, LocalDate startDate, LocalDate endDate
        ) {
            return type == TransactionType.INCOME ? income : expense;
        }
    }
}
