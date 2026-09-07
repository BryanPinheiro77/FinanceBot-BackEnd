package com.financebot.reminder.service;

import com.financebot.recurring.domain.RecurrenceFrequency;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.reminder.domain.Reminder;
import com.financebot.reminder.dto.request.CreateReminderRequest;
import com.financebot.reminder.dto.response.ReminderResponse;
import com.financebot.reminder.mapper.ReminderMapper;
import com.financebot.reminder.repository.ReminderRepository;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private RecurringTransactionRepository recurringRepository;
    @Mock
    private AuthenticatedUserResolver userResolver;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;

    private ReminderService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ReminderService(
                reminderRepository, recurringRepository, userResolver, userRepository, new ReminderMapper()
        );
        user = new User();
        user.setId(10L);
    }

    @Test
    void shouldCreateStandaloneReminderWithExplicitDate() {
        when(userResolver.resolve(authentication)).thenReturn(user);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder reminder = invocation.getArgument(0);
            reminder.setId(1L);
            return reminder;
        });

        ReminderResponse response = service.create(
                new CreateReminderRequest("Pagar aluguel", LocalDate.of(2026, 10, 8), 2, null),
                authentication
        );

        assertThat(response.reminderDate()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(response.daysBefore()).isEqualTo(2);
    }

    @Test
    void shouldDeriveDateFromRecurringTransactionWhenDateIsOmitted() {
        when(userResolver.resolve(authentication)).thenReturn(user);
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setId(4L);
        recurring.setAmount(new BigDecimal("120"));
        recurring.setDescription("Internet");
        recurring.setFrequency(RecurrenceFrequency.MONTHLY);
        recurring.setNextExecutionDate(LocalDate.of(2026, 10, 10));
        when(recurringRepository.findByIdAndUserId(4L, 10L)).thenReturn(Optional.of(recurring));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReminderResponse response = service.create(
                new CreateReminderRequest("Avisar internet", null, 3, 4L), authentication
        );

        assertThat(response.reminderDate()).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(response.recurringTransactionId()).isEqualTo(4L);
    }

    @Test
    void shouldRequireDateForStandaloneReminder() {
        when(userResolver.resolve(authentication)).thenReturn(user);
        assertThatThrownBy(() -> service.create(
                new CreateReminderRequest("Sem data", null, 0, null), authentication
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldScheduleNextOccurrenceWhenRecurringReminderIsSent() {
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setId(4L);
        recurring.setFrequency(RecurrenceFrequency.MONTHLY);
        recurring.setNextExecutionDate(LocalDate.now());
        recurring.setActive(true);
        Reminder reminder = new Reminder();
        reminder.setId(2L);
        reminder.setActive(true);
        reminder.setDaysBefore(2);
        reminder.setReminderDate(LocalDate.now().minusDays(2));
        reminder.setRecurringTransaction(recurring);
        when(reminderRepository.findById(2L)).thenReturn(Optional.of(reminder));

        service.markSent(2L);

        assertThat(reminder.getSentAt()).isNull();
        assertThat(reminder.getReminderDate()).isEqualTo(LocalDate.now().plusMonths(1).minusDays(2));
        verify(reminderRepository).save(reminder);
    }

    @Test
    void shouldAdvanceReminderBeforeRecurringTransactionExecutionDate() {
        LocalDate currentReminderDate = LocalDate.now();
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setFrequency(RecurrenceFrequency.MONTHLY);
        recurring.setNextExecutionDate(currentReminderDate.plusDays(2));
        recurring.setActive(true);
        Reminder reminder = new Reminder();
        reminder.setId(3L);
        reminder.setActive(true);
        reminder.setDaysBefore(2);
        reminder.setReminderDate(currentReminderDate);
        reminder.setRecurringTransaction(recurring);
        when(reminderRepository.findById(3L)).thenReturn(Optional.of(reminder));

        service.markSent(3L);

        assertThat(reminder.getReminderDate())
                .isEqualTo(currentReminderDate.plusDays(2).plusMonths(1).minusDays(2));
        verify(reminderRepository).save(reminder);
    }
}
