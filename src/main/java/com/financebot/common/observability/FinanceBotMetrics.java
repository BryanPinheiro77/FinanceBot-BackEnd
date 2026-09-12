package com.financebot.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class FinanceBotMetrics {
    private final MeterRegistry meterRegistry;

    public FinanceBotMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordReminderPublications(int published, int failed) {
        meterRegistry.counter("financebot.reminders.publications", "result", "published").increment(published);
        meterRegistry.counter("financebot.reminders.publications", "result", "failed").increment(failed);
    }

    public void recordRecurringTransactions(int created) {
        meterRegistry.counter("financebot.recurring.transactions", "result", "created").increment(created);
    }
}
