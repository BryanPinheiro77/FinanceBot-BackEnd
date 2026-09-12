package com.financebot.telegrambot.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class FinanceBotMetrics {
    private final MeterRegistry meterRegistry;

    public FinanceBotMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordTelegramMessage(String result) {
        meterRegistry.counter("financebot.telegram.messages", "result", result).increment();
    }

    public void recordReminderDelivery(String result) {
        meterRegistry.counter("financebot.reminders.deliveries", "result", result).increment();
    }

    public void recordAiInterpretation(String result, Duration duration) {
        meterRegistry.counter("financebot.ai.interpretations", "result", result).increment();
        meterRegistry.timer("financebot.ai.interpretation.duration", "result", result).record(duration);
    }
}
