package com.financebot.telegrambot.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceBotMetricsTest {
    @Test
    void recordsTelegramReminderAndAiResults() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        FinanceBotMetrics metrics = new FinanceBotMetrics(registry);

        metrics.recordTelegramMessage("success");
        metrics.recordReminderDelivery("delivered");
        metrics.recordAiInterpretation("success", Duration.ofMillis(150));

        assertThat(registry.counter("financebot.telegram.messages", "result", "success").count()).isEqualTo(1);
        assertThat(registry.counter("financebot.reminders.deliveries", "result", "delivered").count()).isEqualTo(1);
        assertThat(registry.counter("financebot.ai.interpretations", "result", "success").count()).isEqualTo(1);
        assertThat(registry.timer("financebot.ai.interpretation.duration", "result", "success").count()).isEqualTo(1);
    }
}
