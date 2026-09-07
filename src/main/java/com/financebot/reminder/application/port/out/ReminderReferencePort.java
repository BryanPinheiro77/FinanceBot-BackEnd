package com.financebot.reminder.application.port.out;

import com.financebot.reminder.domain.ReminderRecurrence;

import java.util.Optional;

public interface ReminderReferencePort {
    Optional<Long> findUserIdByTelegramId(Long telegramId);
    Optional<ReminderRecurrence> findRecurrenceByIdAndUserId(Long id, Long userId);
    Optional<ReminderRecurrence> findActiveRecurrenceByDescription(Long userId, String description);
}
