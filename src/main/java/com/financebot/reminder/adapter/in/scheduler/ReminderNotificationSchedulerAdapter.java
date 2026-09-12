package com.financebot.reminder.adapter.in.scheduler;

import com.financebot.common.observability.FinanceBotMetrics;
import com.financebot.reminder.application.usecase.PublishPendingReminderNotificationsUseCase;
import com.financebot.reminder.application.usecase.ReminderPublicationResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ReminderNotificationSchedulerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReminderNotificationSchedulerAdapter.class);

    private final PublishPendingReminderNotificationsUseCase useCase;
    private final FinanceBotMetrics metrics;

    @Scheduled(
            fixedDelayString = "${financebot.reminders.publish-interval:60000}",
            initialDelayString = "${financebot.reminders.publish-interval:60000}"
    )
    public void publishPending() {
        ReminderPublicationResult result = useCase.execute(LocalDate.now());
        metrics.recordReminderPublications(result.published(), result.failed());
        if (result.published() > 0 || result.failed() > 0) {
            LOGGER.info("Reminder notification publication finished: published={}, failed={}",
                    result.published(), result.failed());
        }
    }
}
