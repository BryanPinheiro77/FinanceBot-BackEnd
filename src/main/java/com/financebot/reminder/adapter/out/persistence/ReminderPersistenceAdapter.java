package com.financebot.reminder.adapter.out.persistence;

import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.reminder.application.port.out.ReminderPersistencePort;
import com.financebot.reminder.application.port.out.ReminderReferencePort;
import com.financebot.reminder.domain.Reminder;
import com.financebot.reminder.domain.ReminderRecurrence;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReminderPersistenceAdapter implements ReminderPersistencePort, ReminderReferencePort {
    private final SpringDataReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    @Override
    public Reminder save(Reminder reminder) {
        ReminderJpaEntity entity = reminder.getId() == null ? new ReminderJpaEntity()
                : reminderRepository.findByIdForUpdate(reminder.getId()).orElseThrow();
        entity.setDescription(reminder.getDescription());
        entity.setReminderDate(reminder.getReminderDate());
        entity.setDaysBefore(reminder.getDaysBefore());
        entity.setActive(reminder.isActive());
        entity.setSentAt(reminder.getSentAt());
        entity.setClaimedAt(reminder.getClaimedAt());
        if (entity.getUser() == null) entity.setUser(userRepository.getReferenceById(reminder.getUserId()));
        if (reminder.getRecurringTransactionId() != null && entity.getRecurringTransaction() == null) {
            entity.setRecurringTransaction(recurringTransactionRepository.getReferenceById(reminder.getRecurringTransactionId()));
        }
        return toDomain(reminderRepository.save(entity));
    }

    @Override public Optional<Reminder> findById(Long id) { return reminderRepository.findByIdForUpdate(id).map(this::toDomain); }
    @Override public Optional<Reminder> findByIdAndUserId(Long id, Long userId) { return reminderRepository.findByIdAndUserId(id, userId).map(this::toDomain); }
    @Override public List<Reminder> findAllByUserId(Long userId) { return reminderRepository.findAllByUserIdOrderByReminderDateAsc(userId).stream().map(this::toDomain).toList(); }

    @Override
    public List<Reminder> claimPending(LocalDate date, LocalDateTime staleBefore, LocalDateTime claimedAt, int limit) {
        List<ReminderJpaEntity> entities = reminderRepository.findPendingForClaim(date, staleBefore, PageRequest.of(0, limit));
        entities.forEach(entity -> entity.setClaimedAt(claimedAt));
        return reminderRepository.saveAll(entities).stream().map(this::toDomain).toList();
    }

    @Override public void delete(Reminder reminder) { reminderRepository.deleteById(reminder.getId()); }
    @Override public Optional<Long> findUserIdByTelegramId(Long telegramId) { return userRepository.findByTelegramId(telegramId).map(User::getId); }
    @Override public Optional<ReminderRecurrence> findRecurrenceByIdAndUserId(Long id, Long userId) { return recurringTransactionRepository.findByIdAndUserId(id, userId).map(this::toReference); }

    @Override
    public Optional<ReminderRecurrence> findActiveRecurrenceByDescription(Long userId, String description) {
        String normalized = normalize(description);
        return recurringTransactionRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .filter(item -> normalize(item.getDescription()).contains(normalized)
                        || normalized.contains(normalize(item.getDescription())))
                .min(Comparator.comparing(RecurringTransaction::getNextExecutionDate))
                .map(this::toReference);
    }

    private ReminderRecurrence toReference(RecurringTransaction item) {
        return new ReminderRecurrence(item.getId(), item.getDescription(), item.getNextExecutionDate(), item.getFrequency(), item.isActive());
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT);
    }

    private Reminder toDomain(ReminderJpaEntity entity) {
        Reminder reminder = new Reminder();
        reminder.setId(entity.getId()); reminder.setDescription(entity.getDescription());
        reminder.setReminderDate(entity.getReminderDate()); reminder.setDaysBefore(entity.getDaysBefore());
        reminder.setActive(entity.isActive()); reminder.setSentAt(entity.getSentAt());
        reminder.setClaimedAt(entity.getClaimedAt()); reminder.setCreatedAt(entity.getCreatedAt());
        reminder.setUserId(entity.getUser().getId()); reminder.setTelegramId(entity.getUser().getTelegramId());
        reminder.setRecurringTransactionId(entity.getRecurringTransaction() == null ? null : entity.getRecurringTransaction().getId());
        return reminder;
    }
}
