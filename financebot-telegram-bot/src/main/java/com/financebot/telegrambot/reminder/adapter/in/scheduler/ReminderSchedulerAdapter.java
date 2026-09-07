package com.financebot.telegrambot.reminder.adapter.in.scheduler;

import com.financebot.telegrambot.reminder.application.SendPendingRemindersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderSchedulerAdapter {
    private final SendPendingRemindersUseCase useCase;

    @Scheduled(
            fixedDelayString = "${financebot.reminders.poll-interval:60000}",
            initialDelayString = "${financebot.reminders.poll-interval:60000}"
    )
    public void process() {
        useCase.execute();
    }
}
