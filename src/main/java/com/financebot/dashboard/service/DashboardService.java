package com.financebot.dashboard.service;

import com.financebot.dashboard.dto.CategorySummaryResponse;
import com.financebot.dashboard.dto.DailySummaryResponse;
import com.financebot.dashboard.dto.MonthlySummaryResponse;
import com.financebot.dashboard.port.DashboardTransactionPort;
import com.financebot.transaction.domain.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final DashboardTransactionPort transactions;

    @Transactional(readOnly = true)
    public MonthlySummaryResponse monthlySummary(Long userId, YearMonth month) {
        var totals = transactions.summarizeForDashboard(userId, month.atDay(1), month.atEndOfMonth());
        Map<LocalDate, BigDecimal> incomeByDay = new HashMap<>();
        Map<LocalDate, BigDecimal> expenseByDay = new HashMap<>();
        Map<Long, CategorySummaryResponse> categories = new HashMap<>();
        BigDecimal income = ZERO;
        BigDecimal expense = ZERO;
        long count = 0;
        for (var total : totals) {
            count += total.count();
            if (total.type() == TransactionType.INCOME) {
                income = income.add(total.amount());
                incomeByDay.merge(total.date(), total.amount(), BigDecimal::add);
            } else {
                expense = expense.add(total.amount());
                expenseByDay.merge(total.date(), total.amount(), BigDecimal::add);
                categories.merge(total.categoryId(),
                        new CategorySummaryResponse(total.categoryId(), total.categoryName(), total.amount()),
                        (a, b) -> new CategorySummaryResponse(a.categoryId(), a.categoryName(),
                                a.totalExpense().add(b.totalExpense())));
            }
        }
        var daily = new ArrayList<DailySummaryResponse>(month.lengthOfMonth());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            daily.add(new DailySummaryResponse(date, incomeByDay.getOrDefault(date, ZERO),
                    expenseByDay.getOrDefault(date, ZERO)));
        }
        var expensesByCategory = categories.values().stream()
                .sorted(Comparator.comparing(CategorySummaryResponse::totalExpense).reversed()
                        .thenComparing(CategorySummaryResponse::categoryId))
                .toList();
        return new MonthlySummaryResponse(month.toString(), month.atDay(1), month.atEndOfMonth(),
                income, expense, income.subtract(expense), count, daily, expensesByCategory);
    }
}
