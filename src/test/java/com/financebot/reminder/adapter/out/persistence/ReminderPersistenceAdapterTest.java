package com.financebot.reminder.adapter.out.persistence;

import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.reminder.domain.Reminder;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderPersistenceAdapterTest {
    @Mock private SpringDataReminderRepository reminderRepository;
    @Mock private UserRepository userRepository;
    @Mock private RecurringTransactionRepository recurringTransactionRepository;

    @Test
    void claimsPendingRemindersAndMapsTelegramRecipient() {
        User user = new User();
        user.setId(10L);
        user.setTelegramId(123L);
        ReminderJpaEntity entity = new ReminderJpaEntity();
        entity.setId(1L); entity.setUser(user); entity.setActive(true);
        entity.setDescription("Pagar aluguel"); entity.setReminderDate(LocalDate.now());
        when(reminderRepository.findPendingForClaim(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(entity));
        when(reminderRepository.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        ReminderPersistenceAdapter adapter = new ReminderPersistenceAdapter(
                reminderRepository, userRepository, recurringTransactionRepository
        );
        LocalDateTime claimedAt = LocalDateTime.now();

        List<Reminder> claimed = adapter.claimPending(
                LocalDate.now(), claimedAt.minusMinutes(5), claimedAt, 100
        );

        assertThat(claimed).singleElement().satisfies(reminder -> {
            assertThat(reminder.getTelegramId()).isEqualTo(123L);
            assertThat(reminder.getClaimedAt()).isEqualTo(claimedAt);
        });
        verify(reminderRepository).findPendingForClaim(any(), any(), eq(Pageable.ofSize(100)));
    }

    @Test
    void prioritizesExactRecurrenceDescription() {
        RecurringTransaction exact = recurrence(1L, "Internet");
        RecurringTransaction partial = recurrence(2L, "Internet casa");
        when(recurringTransactionRepository.findAllByUserIdAndActiveTrue(10L))
                .thenReturn(List.of(partial, exact));
        ReminderPersistenceAdapter adapter = new ReminderPersistenceAdapter(
                reminderRepository, userRepository, recurringTransactionRepository);

        assertThat(adapter.findActiveRecurrenceByDescription(10L, "internet"))
                .get().extracting(com.financebot.reminder.domain.ReminderRecurrence::id).isEqualTo(1L);
    }

    @Test
    void rejectsAmbiguousPartialRecurrenceDescription() {
        when(recurringTransactionRepository.findAllByUserIdAndActiveTrue(10L)).thenReturn(List.of(
                recurrence(1L, "Internet casa"), recurrence(2L, "Internet escritório")));
        ReminderPersistenceAdapter adapter = new ReminderPersistenceAdapter(
                reminderRepository, userRepository, recurringTransactionRepository);

        assertThat(adapter.findActiveRecurrenceByDescription(10L, "internet")).isEmpty();
    }

    private RecurringTransaction recurrence(Long id, String description) {
        RecurringTransaction recurrence = new RecurringTransaction();
        recurrence.setId(id);
        recurrence.setDescription(description);
        recurrence.setActive(true);
        recurrence.setNextExecutionDate(LocalDate.of(2026, 10, 10));
        recurrence.setFrequency(com.financebot.recurring.domain.RecurrenceFrequency.MONTHLY);
        return recurrence;
    }
}
