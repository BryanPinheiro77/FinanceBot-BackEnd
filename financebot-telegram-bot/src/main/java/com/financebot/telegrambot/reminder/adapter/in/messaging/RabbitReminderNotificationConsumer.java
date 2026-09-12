package com.financebot.telegrambot.reminder.adapter.in.messaging;

import com.financebot.telegrambot.dto.response.PendingReminderResponse;
import com.financebot.telegrambot.observability.CorrelationIds;
import com.financebot.telegrambot.observability.FinanceBotMetrics;
import com.financebot.telegrambot.reminder.application.SendPendingRemindersUseCase;
import com.financebot.telegrambot.reminder.application.ReminderDeliveryResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
public class RabbitReminderNotificationConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitReminderNotificationConsumer.class);

    private final SendPendingRemindersUseCase useCase;
    private final FinanceBotMetrics metrics;

    @RabbitListener(queues = ReminderMessagingTopology.REMINDER_NOTIFICATION_QUEUE)
    public void consume(ReminderNotificationMessage message) {
        String correlationId = message != null && message.reminderId() != null
                ? "reminder-" + message.reminderId()
                : CorrelationIds.currentOrCreate();
        MDC.put(CorrelationIds.MDC_KEY, correlationId);
        try {
            consumeWithCorrelation(message);
        } finally {
            MDC.remove(CorrelationIds.MDC_KEY);
        }
    }

    private void consumeWithCorrelation(ReminderNotificationMessage message) {
        if (message == null || message.reminderId() == null || message.telegramId() == null
                || message.description() == null || message.description().isBlank()) {
            LOGGER.warn("Discarding invalid reminder notification message");
            metrics.recordReminderDelivery("invalid");
            return;
        }
        PendingReminderResponse reminder;
        try {
            reminder = new PendingReminderResponse(
                    message.reminderId(),
                    message.telegramId(),
                    message.description(),
                    LocalDate.parse(message.reminderDate())
            );
        } catch (DateTimeParseException | NullPointerException exception) {
            LOGGER.warn("Discarding invalid reminder notification message");
            metrics.recordReminderDelivery("invalid");
            return;
        }

        ReminderDeliveryResult result = useCase.execute(reminder);
        metrics.recordReminderDelivery(result.name().toLowerCase(java.util.Locale.ROOT));
        switch (result) {
            case DELIVERED -> LOGGER.info("Reminder notification delivered: reminderId={}", message.reminderId());
            case RELEASED -> LOGGER.warn("Reminder notification delivery failed and was released: reminderId={}",
                    message.reminderId());
            case SKIPPED -> LOGGER.info("Stale reminder notification skipped: reminderId={}", message.reminderId());
        }
    }
}
