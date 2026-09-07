package com.financebot.reminder.service;

import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.reminder.domain.Reminder;
import com.financebot.reminder.dto.request.CreateReminderRequest;
import com.financebot.reminder.dto.request.UpdateReminderRequest;
import com.financebot.reminder.dto.request.CreateTelegramReminderRequest;
import com.financebot.reminder.dto.response.PendingReminderResponse;
import com.financebot.reminder.dto.response.ReminderResponse;
import com.financebot.reminder.mapper.ReminderMapper;
import com.financebot.reminder.repository.ReminderRepository;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final UserRepository userRepository;
    private final ReminderMapper reminderMapper;

    @Transactional
    public ReminderResponse create(CreateReminderRequest request, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);
        RecurringTransaction recurring = resolveRecurring(request.recurringTransactionId(), user.getId());
        LocalDate reminderDate = resolveReminderDate(request.reminderDate(), request.daysBefore(), recurring);

        Reminder reminder = new Reminder();
        reminder.setDescription(request.description().trim());
        reminder.setReminderDate(reminderDate);
        reminder.setDaysBefore(normalizeDaysBefore(request.daysBefore()));
        reminder.setActive(true);
        reminder.setUser(user);
        reminder.setRecurringTransaction(recurring);
        return reminderMapper.toResponse(reminderRepository.save(reminder));
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> findAll(Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);
        return reminderRepository.findAllByUserIdOrderByReminderDateAsc(user.getId())
                .stream().map(reminderMapper::toResponse).toList();
    }

    @Transactional
    public ReminderResponse update(Long id, UpdateReminderRequest request, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);
        Reminder reminder = getUserReminder(id, user.getId());
        reminder.setDescription(request.description().trim());
        reminder.setReminderDate(request.reminderDate());
        reminder.setDaysBefore(request.daysBefore());
        reminder.setActive(request.active());
        if (request.active()) {
            reminder.setSentAt(null);
        }
        return reminderMapper.toResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);
        reminderRepository.delete(getUserReminder(id, user.getId()));
    }

    @Transactional(readOnly = true)
    public List<PendingReminderResponse> findPendingForTelegram(LocalDate date) {
        return reminderRepository.findPendingForTelegram(date).stream()
                .map(reminder -> new PendingReminderResponse(
                        reminder.getId(), reminder.getUser().getTelegramId(),
                        reminder.getDescription(), reminder.getReminderDate()
                )).toList();
    }

    @Transactional
    public ReminderResponse createForTelegram(CreateTelegramReminderRequest request) {
        User user = userRepository.findByTelegramId(request.telegramId())
                .orElseThrow(() -> new EntityNotFoundException("Telegram user not found"));
        Reminder reminder = new Reminder();
        reminder.setDescription(request.description().trim());
        reminder.setReminderDate(request.reminderDate());
        reminder.setDaysBefore(normalizeDaysBefore(request.daysBefore()));
        reminder.setUser(user);
        return reminderMapper.toResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public void markSent(Long id) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reminder not found"));
        if (reminder.isActive() && reminder.getSentAt() == null) {
            if (reminder.getRecurringTransaction() != null
                    && reminder.getRecurringTransaction().isActive()
                    && reminder.getRecurringTransaction().getNextExecutionDate() != null) {
                LocalDate nextDue = reminder.getRecurringTransaction().getNextExecutionDate();
                LocalDate nextReminderDate = nextDue.minusDays(reminder.getDaysBefore());
                if (!nextReminderDate.isAfter(reminder.getReminderDate())) {
                    nextDue = switch (reminder.getRecurringTransaction().getFrequency()) {
                        case DAILY -> nextDue.plusDays(1);
                        case WEEKLY -> nextDue.plusWeeks(1);
                        case MONTHLY -> nextDue.plusMonths(1);
                        case YEARLY -> nextDue.plusYears(1);
                    };
                }
                reminder.setReminderDate(nextDue.minusDays(reminder.getDaysBefore()));
            } else {
                reminder.setSentAt(LocalDateTime.now());
                reminder.setActive(false);
            }
            reminderRepository.save(reminder);
        }
    }

    private RecurringTransaction resolveRecurring(Long recurringId, Long userId) {
        if (recurringId == null) {
            return null;
        }
        return recurringTransactionRepository.findByIdAndUserId(recurringId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Recurring transaction not found"));
    }

    private LocalDate resolveReminderDate(LocalDate reminderDate, Integer daysBefore, RecurringTransaction recurring) {
        if (reminderDate != null) {
            return reminderDate;
        }
        if (recurring == null || recurring.getNextExecutionDate() == null) {
            throw new IllegalArgumentException("Reminder date is required for standalone reminders");
        }
        return recurring.getNextExecutionDate().minusDays(normalizeDaysBefore(daysBefore));
    }

    private int normalizeDaysBefore(Integer daysBefore) {
        return daysBefore == null ? 0 : daysBefore;
    }

    private Reminder getUserReminder(Long id, Long userId) {
        return reminderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Reminder not found"));
    }
}
