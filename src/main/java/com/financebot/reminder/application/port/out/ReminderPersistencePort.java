package com.financebot.reminder.application.port.out;

import com.financebot.reminder.domain.Reminder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReminderPersistencePort {
    Reminder save(Reminder reminder);
    Optional<Reminder> findById(Long id);
    Optional<Reminder> findByIdAndUserId(Long id, Long userId);
    List<Reminder> findAllByUserId(Long userId);
    List<Reminder> claimPending(LocalDate date, LocalDateTime staleBefore, LocalDateTime claimedAt, int limit);
    void delete(Reminder reminder);
}
