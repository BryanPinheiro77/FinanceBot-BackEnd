package com.financebot.common.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceBotMetricsTest {
    @Test
    void recordsReminderAndRecurringResults() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        FinanceBotMetrics metrics = new FinanceBotMetrics(registry);

        metrics.recordReminderPublications(2, 1);
        metrics.recordRecurringTransactions(3);

        assertThat(registry.counter("financebot.reminders.publications", "result", "published").count())
                .isEqualTo(2);
        assertThat(registry.counter("financebot.reminders.publications", "result", "failed").count())
                .isEqualTo(1);
        assertThat(registry.counter("financebot.recurring.transactions", "result", "created").count())
                .isEqualTo(3);
    }
}
