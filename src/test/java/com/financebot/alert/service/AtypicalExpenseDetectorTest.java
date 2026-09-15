package com.financebot.alert.service;

import com.financebot.alert.port.ExpenseHistoryPort;
import com.financebot.transaction.repository.ExpenseByCategoryAndDateProjection;
import com.financebot.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtypicalExpenseDetectorTest {

    private final FakeExpenseHistoryPort expenseHistoryPort = new FakeExpenseHistoryPort();
    private AtypicalExpenseDetector detector;
    private User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC);
        detector = new AtypicalExpenseDetector(expenseHistoryPort, clock);
        user = new User();
        user.setId(1L);
    }

    @Test
    void shouldDetectExpenseAboveHistoricalPattern() {
        expenseHistoryPort.expenses = expenses("Mercado", List.of("200", "200", "200", "350"));

        var alerts = detector.detect(user);

        assertThat(alerts).singleElement().satisfies(alert -> {
            assertThat(alert.categoryName()).isEqualTo("Mercado");
            assertThat(alert.observedMonth()).isEqualTo(YearMonth.of(2026, 8));
            assertThat(alert.observedAmount()).isEqualByComparingTo("350");
            assertThat(alert.historicalAverage()).isEqualByComparingTo("200");
            assertThat(alert.variationPercentage()).isEqualByComparingTo("75.00");
            assertThat(alert.explanation()).contains("75.00%");
        });
    }

    @Test
    void shouldIgnoreCategoryWithoutCompleteHistory() {
        expenseHistoryPort.expenses = expenses("Mercado", List.of("200", "200", "200"));

        assertThat(detector.detect(user)).isEmpty();
    }

    @Test
    void shouldIgnoreIncreaseBelowThreshold() {
        expenseHistoryPort.expenses = expenses("Mercado", List.of("200", "200", "200", "290"));

        assertThat(detector.detect(user)).isEmpty();
    }

    @Test
    void shouldOrderAndLimitAlerts() {
        List<ExpenseByCategoryAndDateProjection> expenses = new ArrayList<>();
        expenses.addAll(expenses("Casa", List.of("100", "100", "100", "300")));
        expenses.addAll(expenses("Lazer", List.of("100", "100", "100", "250")));
        expenses.addAll(expenses("Mercado", List.of("200", "200", "200", "350")));
        expenses.addAll(expenses("Transporte", List.of("100", "100", "100", "220")));
        expenseHistoryPort.expenses = expenses;

        var alerts = detector.detect(user);

        assertThat(alerts).hasSize(3);
        assertThat(alerts).extracting(alert -> alert.categoryName())
                .containsExactly("Casa", "Lazer", "Transporte");
    }

    @Test
    void shouldRejectUserWithoutId() {
        User userWithoutId = new User();

        assertThatThrownBy(() -> detector.detect(userWithoutId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with id is required");
    }

    private List<ExpenseByCategoryAndDateProjection> expenses(String category, List<String> amounts) {
        List<ExpenseByCategoryAndDateProjection> result = new ArrayList<>();
        for (int index = 0; index < amounts.size(); index++) {
            YearMonth month = YearMonth.of(2026, 5 + index);
            result.add(projection(category, month.atDay(10), amounts.get(index)));
        }
        return result;
    }

    private ExpenseByCategoryAndDateProjection projection(String category, LocalDate date, String amount) {
        return new ExpenseByCategoryAndDateProjection() {
            @Override
            public BigDecimal getAmount() {
                return new BigDecimal(amount);
            }

            @Override
            public LocalDate getDate() {
                return date;
            }

            @Override
            public String getCategoryName() {
                return category;
            }
        };
    }

    private static final class FakeExpenseHistoryPort implements ExpenseHistoryPort {
        private List<ExpenseByCategoryAndDateProjection> expenses = List.of();

        @Override
        public List<ExpenseByCategoryAndDateProjection> findExpensesByCategoryAndDateBetween(
                Long userId,
                LocalDate startDate,
                LocalDate endDate
        ) {
            return expenses;
        }
    }
}
