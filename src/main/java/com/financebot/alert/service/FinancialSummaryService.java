package com.financebot.alert.service;

import com.financebot.alert.domain.FinancialSummary;
import com.financebot.alert.port.SummaryTransactionPort;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

@Service
public class FinancialSummaryService {

    private final SummaryTransactionPort summaryTransactionPort;
    private final Clock clock;

    public FinancialSummaryService(SummaryTransactionPort summaryTransactionPort, Clock clock) {
        this.summaryTransactionPort = summaryTransactionPort;
        this.clock = clock;
    }

    public FinancialSummary previousCompletedWeek(User user) {
        validateUser(user);
        LocalDate currentWeekStart = LocalDate.now(clock)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return buildSummary(user, "WEEKLY", currentWeekStart.minusWeeks(1), currentWeekStart.minusDays(1));
    }

    public FinancialSummary previousCompletedMonth(User user) {
        validateUser(user);
        YearMonth month = YearMonth.now(clock).minusMonths(1);
        return buildSummary(user, "MONTHLY", month.atDay(1), month.atEndOfMonth());
    }

    private FinancialSummary buildSummary(User user, String periodType, LocalDate startDate, LocalDate endDate) {
        BigDecimal income = valueOrZero(summaryTransactionPort.sumAmountByUserAndTypeBetweenDates(
                user.getId(), TransactionType.INCOME, startDate, endDate));
        BigDecimal expense = valueOrZero(summaryTransactionPort.sumAmountByUserAndTypeBetweenDates(
                user.getId(), TransactionType.EXPENSE, startDate, endDate));
        BigDecimal balance = income.subtract(expense);
        String periodLabel = "WEEKLY".equals(periodType) ? "semanal" : "mensal";
        String explanation = "Resumo " + periodLabel + " de " + startDate
                + " a " + endDate + ": receitas de R$ " + income
                + ", despesas de R$ " + expense + " e saldo de R$ " + balance + ".";
        return new FinancialSummary(periodType, startDate, endDate, income, expense, balance, explanation);
    }

    private void validateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User with id is required");
        }
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
