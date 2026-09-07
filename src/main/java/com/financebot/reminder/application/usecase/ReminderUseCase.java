package com.financebot.reminder.application.usecase;

import com.financebot.reminder.application.port.out.ReminderPersistencePort;
import com.financebot.reminder.application.port.out.ReminderReferencePort;
import com.financebot.reminder.application.command.CreateReminderCommand;
import com.financebot.reminder.application.command.CreateTelegramReminderCommand;
import com.financebot.reminder.application.command.UpdateReminderCommand;
import com.financebot.reminder.domain.Reminder;
import com.financebot.reminder.domain.ReminderRecurrence;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReminderUseCase {
    private static final int CLAIM_BATCH_SIZE = 100;
    private static final int CLAIM_TIMEOUT_MINUTES = 5;

    private final ReminderPersistencePort persistencePort;
    private final ReminderReferencePort referencePort;

    @Transactional
    public Reminder create(CreateReminderCommand command) {
        ReminderRecurrence recurrence = resolveRecurrence(command.recurringTransactionId(), command.userId());
        return saveNew(command.userId(), command.description(), command.reminderDate(), command.daysBefore(), recurrence);
    }

    @Transactional(readOnly = true)
    public List<Reminder> findAll(Long userId) {
        return persistencePort.findAllByUserId(userId);
    }

    @Transactional
    public Reminder update(Long id, UpdateReminderCommand command) {
        Reminder reminder = findUserReminder(id, command.userId());
        reminder.setDescription(command.description().trim());
        reminder.setReminderDate(command.reminderDate());
        reminder.setDaysBefore(command.daysBefore());
        reminder.setActive(command.active());
        if (command.active()) reminder.setSentAt(null);
        return persistencePort.save(reminder);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        persistencePort.delete(findUserReminder(id, userId));
    }

    @Transactional
    public Reminder createForTelegram(CreateTelegramReminderCommand command) {
        Long userId = referencePort.findUserIdByTelegramId(command.telegramId())
                .orElseThrow(() -> new EntityNotFoundException("Telegram user not found"));
        ReminderRecurrence recurrence = command.recurringDescription() == null ? null
                : referencePort.findActiveRecurrenceByDescription(userId, command.recurringDescription())
                .orElseThrow(() -> new EntityNotFoundException("Recurring transaction not found"));
        return saveNew(userId, command.description(), command.reminderDate(), command.daysBefore(), recurrence);
    }

    @Transactional
    public List<Reminder> claimPending(LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        return persistencePort.claimPending(date, now.minusMinutes(CLAIM_TIMEOUT_MINUTES), now, CLAIM_BATCH_SIZE);
    }

    @Transactional
    public void markSent(Long id) {
        Reminder reminder = persistencePort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reminder not found"));
        if (!reminder.isActive() || reminder.getSentAt() != null) return;
        ReminderRecurrence recurrence = reminder.getRecurringTransactionId() == null ? null
                : referencePort.findRecurrenceByIdAndUserId(reminder.getRecurringTransactionId(), reminder.getUserId()).orElse(null);
        if (recurrence != null && recurrence.active() && recurrence.nextExecutionDate() != null) {
            LocalDate nextDue = recurrence.nextExecutionDate();
            if (!nextDue.minusDays(reminder.getDaysBefore()).isAfter(reminder.getReminderDate())) {
                nextDue = switch (recurrence.frequency()) {
                    case DAILY -> nextDue.plusDays(1);
                    case WEEKLY -> nextDue.plusWeeks(1);
                    case MONTHLY -> nextDue.plusMonths(1);
                    case YEARLY -> nextDue.plusYears(1);
                };
            }
            reminder.reschedule(nextDue.minusDays(reminder.getDaysBefore()));
        } else {
            reminder.markSent();
        }
        persistencePort.save(reminder);
    }

    @Transactional
    public void releaseClaim(Long id) {
        Reminder reminder = persistencePort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reminder not found"));
        reminder.setClaimedAt(null);
        persistencePort.save(reminder);
    }

    private Reminder saveNew(Long userId, String description, LocalDate date, Integer daysBefore,
                             ReminderRecurrence recurrence) {
        int advanceDays = daysBefore == null ? 0 : daysBefore;
        LocalDate reminderDate = date;
        if (reminderDate == null && recurrence != null) {
            reminderDate = recurrence.nextExecutionDate().minusDays(advanceDays);
        }
        if (reminderDate == null) throw new IllegalArgumentException("Reminder date is required for standalone reminders");
        Reminder reminder = new Reminder();
        reminder.setDescription(description.trim()); reminder.setReminderDate(reminderDate);
        reminder.setDaysBefore(advanceDays); reminder.setUserId(userId);
        reminder.setRecurringTransactionId(recurrence == null ? null : recurrence.id());
        return persistencePort.save(reminder);
    }

    private ReminderRecurrence resolveRecurrence(Long id, Long userId) {
        if (id == null) return null;
        return referencePort.findRecurrenceByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Recurring transaction not found"));
    }

    private Reminder findUserReminder(Long id, Long userId) {
        return persistencePort.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Reminder not found"));
    }
}
