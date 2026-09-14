package com.financebot.alert.service;

import com.financebot.alert.domain.AtypicalExpenseAlert;
import com.financebot.alert.port.ExpenseHistoryPort;
import com.financebot.transaction.repository.ExpenseByCategoryAndDateProjection;
import com.financebot.user.domain.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AtypicalExpenseDetector {

    private static final int REFERENCE_MONTHS = 3;
    private static final BigDecimal VARIATION_THRESHOLD = new BigDecimal("1.50");
    private static final BigDecimal MINIMUM_DIFFERENCE = new BigDecimal("100.00");
    private static final int MAX_ALERTS = 3;

    private final ExpenseHistoryPort expenseHistoryPort;
    private final Clock clock;

    public AtypicalExpenseDetector(ExpenseHistoryPort expenseHistoryPort, Clock clock) {
        this.expenseHistoryPort = expenseHistoryPort;
        this.clock = clock;
    }

    public List<AtypicalExpenseAlert> detect(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User with id is required");
        }

        YearMonth observedMonth = YearMonth.now(clock).minusMonths(1);
        YearMonth firstReferenceMonth = observedMonth.minusMonths(REFERENCE_MONTHS);
        List<ExpenseByCategoryAndDateProjection> expenses = expenseHistoryPort
                .findExpensesByCategoryAndDateBetween(
                        user.getId(),
                        firstReferenceMonth.atDay(1),
                        observedMonth.atEndOfMonth()
                );

        Map<String, Map<YearMonth, BigDecimal>> monthlyAmounts = groupByCategoryAndMonth(expenses);
        List<AtypicalExpenseAlert> alerts = new ArrayList<>();

        monthlyAmounts.forEach((category, amountsByMonth) -> {
            BigDecimal observedAmount = amountsByMonth.getOrDefault(observedMonth, BigDecimal.ZERO);
            List<BigDecimal> referenceAmounts = new ArrayList<>();
            for (int offset = REFERENCE_MONTHS; offset >= 1; offset--) {
                YearMonth referenceMonth = observedMonth.minusMonths(offset);
                BigDecimal amount = amountsByMonth.get(referenceMonth);
                if (amount == null) {
                    return;
                }
                referenceAmounts.add(amount);
            }

            BigDecimal historicalAverage = referenceAmounts.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(REFERENCE_MONTHS), 2, RoundingMode.HALF_UP);
            BigDecimal difference = observedAmount.subtract(historicalAverage);
            if (historicalAverage.signum() <= 0
                    || difference.compareTo(MINIMUM_DIFFERENCE) < 0
                    || observedAmount.compareTo(historicalAverage.multiply(VARIATION_THRESHOLD)) < 0) {
                return;
            }

            BigDecimal variationPercentage = difference
                    .multiply(BigDecimal.valueOf(100))
                    .divide(historicalAverage, 2, RoundingMode.HALF_UP);
            alerts.add(new AtypicalExpenseAlert(
                    category,
                    observedMonth,
                    observedAmount.setScale(2, RoundingMode.HALF_UP),
                    historicalAverage,
                    variationPercentage,
                    "A categoria " + category + " ficou " + variationPercentage
                            + "% acima da média dos três meses anteriores."
            ));
        });

        return alerts.stream()
                .sorted(Comparator.comparing(AtypicalExpenseAlert::variationPercentage).reversed())
                .limit(MAX_ALERTS)
                .toList();
    }

    private Map<String, Map<YearMonth, BigDecimal>> groupByCategoryAndMonth(
            List<ExpenseByCategoryAndDateProjection> expenses
    ) {
        Map<String, Map<YearMonth, BigDecimal>> grouped = new HashMap<>();
        if (expenses == null) {
            return grouped;
        }

        expenses.forEach(expense -> {
            if (expense == null || expense.getCategoryName() == null || expense.getDate() == null
                    || expense.getAmount() == null) {
                return;
            }
            grouped.computeIfAbsent(expense.getCategoryName(), ignored -> new HashMap<>())
                    .merge(YearMonth.from(expense.getDate()), expense.getAmount(), BigDecimal::add);
        });
        return grouped;
    }
}
