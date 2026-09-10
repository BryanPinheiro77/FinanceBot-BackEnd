package com.financebot.telegrambot.reminder.adapter.in.messaging;

import com.financebot.telegrambot.dto.response.PendingReminderResponse;
import com.financebot.telegrambot.reminder.application.SendPendingRemindersUseCase;
import com.financebot.telegrambot.reminder.application.ReminderDeliveryResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
public class RabbitReminderNotificationConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitReminderNotificationConsumer.class);

    private final SendPendingRemindersUseCase useCase;

    @RabbitListener(queues = ReminderMessagingTopology.REMINDER_NOTIFICATION_QUEUE)
    public void consume(ReminderNotificationMessage message) {
        if (message == null || message.reminderId() == null || message.telegramId() == null
                || message.description() == null || message.description().isBlank()) {
            LOGGER.warn("Discarding invalid reminder notification message");
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
            return;
        }

        ReminderDeliveryResult result = useCase.execute(reminder);
        switch (result) {
            case DELIVERED -> LOGGER.info("Reminder notification delivered: reminderId={}", message.reminderId());
            case RELEASED -> LOGGER.warn("Reminder notification delivery failed and was released: reminderId={}",
                    message.reminderId());
            case SKIPPED -> LOGGER.info("Stale reminder notification skipped: reminderId={}", message.reminderId());
        }
    }
}
