package com.financebot.reminder.application.usecase;

import com.financebot.recurring.domain.RecurrenceFrequency;
import com.financebot.reminder.application.port.out.ReminderPersistencePort;
import com.financebot.reminder.application.port.out.ReminderReferencePort;
import com.financebot.reminder.application.command.CreateReminderCommand;
import com.financebot.reminder.application.command.CreateTelegramReminderCommand;
import com.financebot.reminder.application.command.UpdateReminderCommand;
import com.financebot.reminder.domain.Reminder;
import com.financebot.reminder.domain.ReminderRecurrence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderUseCaseTest {
    @Mock private ReminderPersistencePort persistencePort;
    @Mock private ReminderReferencePort referencePort;
    private ReminderUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReminderUseCase(persistencePort, referencePort);
    }

    @Test
    void createsStandaloneReminder() {
        when(persistencePort.save(any())).thenAnswer(call -> call.getArgument(0));
        Reminder result = useCase.create(new CreateReminderCommand(
                "Pagar aluguel", LocalDate.of(2026, 10, 8), 0, null, 10L
        ));
        assertThat(result.getReminderDate()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(result.getUserId()).isEqualTo(10L);
    }

    @Test
    void createsReminderLinkedToRecurrence() {
        ReminderRecurrence recurrence = new ReminderRecurrence(
                4L, "Internet", LocalDate.of(2026, 10, 10), RecurrenceFrequency.MONTHLY, true
        );
        when(referencePort.findRecurrenceByIdAndUserId(4L, 10L)).thenReturn(Optional.of(recurrence));
        when(persistencePort.save(any())).thenAnswer(call -> call.getArgument(0));

        Reminder result = useCase.create(new CreateReminderCommand("Internet", null, 3, 4L, 10L));

        assertThat(result.getReminderDate()).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(result.getRecurringTransactionId()).isEqualTo(4L);
    }

    @Test
    void createsTelegramReminderLinkedByDescription() {
        ReminderRecurrence recurrence = new ReminderRecurrence(
                4L, "Internet", LocalDate.of(2026, 10, 10), RecurrenceFrequency.MONTHLY, true
        );
        when(referencePort.findUserIdByTelegramId(123L)).thenReturn(Optional.of(10L));
        when(referencePort.findActiveRecurrenceByDescription(10L, "internet")).thenReturn(Optional.of(recurrence));
        when(persistencePort.save(any())).thenAnswer(call -> call.getArgument(0));

        Reminder result = useCase.createForTelegram(
                new CreateTelegramReminderCommand(123L, "internet", null, 2, "internet")
        );

        assertThat(result.getReminderDate()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(result.getRecurringTransactionId()).isEqualTo(4L);
    }

    @Test
    void requiresDateForStandaloneReminder() {
        assertThatThrownBy(() -> useCase.create(new CreateReminderCommand("Sem data", null, 0, null, 10L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void claimsPendingRemindersInBoundedBatch() {
        Reminder reminder = new Reminder();
        when(persistencePort.claimPending(any(), any(), any(), org.mockito.ArgumentMatchers.eq(100)))
                .thenReturn(List.of(reminder));

        assertThat(useCase.claimPending(LocalDate.now())).containsExactly(reminder);
        verify(persistencePort).claimPending(any(LocalDate.class), any(LocalDateTime.class),
                any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(100));
    }

    @Test
    void acceptsOnlyTheCurrentlyClaimedReminderDelivery() {
        LocalDate date = LocalDate.of(2026, 9, 9);
        Reminder reminder = new Reminder();
        reminder.setId(3L);
        reminder.setReminderDate(date);
        reminder.setClaimedAt(LocalDateTime.now());
        when(persistencePort.findById(3L)).thenReturn(Optional.of(reminder));

        assertThat(useCase.isCurrentDelivery(3L, date)).isTrue();
        assertThat(useCase.isCurrentDelivery(3L, date.plusDays(1))).isFalse();
    }

    @Test
    void rejectsDeliveryThatIsNoLongerClaimed() {
        LocalDate date = LocalDate.of(2026, 9, 9);
        Reminder reminder = new Reminder();
        reminder.setId(3L);
        reminder.setReminderDate(date);
        when(persistencePort.findById(3L)).thenReturn(Optional.of(reminder));

        assertThat(useCase.isCurrentDelivery(3L, date)).isFalse();
    }

    @Test
    void advancesRecurringReminderAfterSending() {
        LocalDate reminderDate = LocalDate.now();
        Reminder reminder = new Reminder();
        reminder.setId(3L); reminder.setUserId(10L); reminder.setRecurringTransactionId(4L);
        reminder.setDaysBefore(2); reminder.setReminderDate(reminderDate);
        ReminderRecurrence recurrence = new ReminderRecurrence(
                4L, "Internet", reminderDate.plusDays(2), RecurrenceFrequency.MONTHLY, true
        );
        when(persistencePort.findById(3L)).thenReturn(Optional.of(reminder));
        when(referencePort.findRecurrenceByIdAndUserId(4L, 10L)).thenReturn(Optional.of(recurrence));
        when(persistencePort.save(reminder)).thenReturn(reminder);

        useCase.markSent(3L);

        assertThat(reminder.getReminderDate()).isEqualTo(reminderDate.plusDays(2).plusMonths(1).minusDays(2));
        assertThat(reminder.getClaimedAt()).isNull();
    }

    @Test
    void doesNotProcessReminderAlreadySent() {
        Reminder reminder = new Reminder();
        reminder.setId(5L);
        reminder.setActive(false);
        reminder.setSentAt(LocalDateTime.now());
        when(persistencePort.findById(5L)).thenReturn(Optional.of(reminder));

        useCase.markSent(5L);

        verify(persistencePort).findById(5L);
        verifyNoMoreInteractions(persistencePort, referencePort);
    }

    @Test
    void updatesAndDeletesReminderOwnedByUser() {
        Reminder reminder = new Reminder();
        reminder.setId(7L);
        when(persistencePort.findByIdAndUserId(7L, 10L)).thenReturn(Optional.of(reminder));
        when(persistencePort.save(reminder)).thenReturn(reminder);

        Reminder updated = useCase.update(7L, new UpdateReminderCommand(
                "  Atualizado ", LocalDate.of(2026, 10, 10), 2, true, 10L));

        assertThat(updated.getDescription()).isEqualTo("Atualizado");
        assertThat(updated.getSentAt()).isNull();
        useCase.delete(7L, 10L);
        verify(persistencePort).delete(reminder);
    }

    @Test
    void rejectsUnknownReminderAndTelegramReferences() {
        when(persistencePort.findByIdAndUserId(7L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.update(7L,
                new UpdateReminderCommand("x", LocalDate.now(), 0, false, 10L)))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);

        when(referencePort.findUserIdByTelegramId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.createForTelegram(
                new CreateTelegramReminderCommand(99L, "x", LocalDate.now(), 0, null)))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    void marksStandaloneReminderSentAndReleasesClaim() {
        Reminder reminder = new Reminder();
        reminder.setId(8L);
        reminder.setActive(true);
        reminder.setReminderDate(LocalDate.now());
        when(persistencePort.findById(8L)).thenReturn(Optional.of(reminder));
        when(persistencePort.save(reminder)).thenReturn(reminder);

        useCase.markSent(8L);
        assertThat(reminder.getSentAt()).isNotNull();

        reminder.setClaimedAt(LocalDateTime.now());
        useCase.releaseClaim(8L);
        assertThat(reminder.getClaimedAt()).isNull();
    }
}
