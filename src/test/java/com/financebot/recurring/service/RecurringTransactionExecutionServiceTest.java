package com.financebot.recurring.service;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.recurring.domain.RecurrenceFrequency;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionExecutionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RecurringTransactionExecutionService service;

    @Test
    void shouldCreateAllOverdueOccurrencesAndAdvanceSchedule() {
        RecurringTransaction recurring = recurring(LocalDate.of(2026, 4, 1), null);
        when(recurringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(recurring));

        int created = service.executeDueTransaction(1L, LocalDate.of(2026, 6, 1));

        assertThat(created).isEqualTo(3);
        assertThat(recurring.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(recurring.getLastExecutedAt()).isNotNull();
        verify(transactionRepository, times(3)).save(any(Transaction.class));
        verify(recurringRepository).save(recurring);
    }

    @Test
    void shouldDeactivateAfterLastOccurrence() {
        RecurringTransaction recurring = recurring(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));
        when(recurringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(recurring));

        int created = service.executeDueTransaction(1L, LocalDate.of(2026, 6, 30));

        assertThat(created).isEqualTo(1);
        assertThat(recurring.isActive()).isFalse();
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldNotCreateWhenTransactionIsNotDue() {
        RecurringTransaction recurring = recurring(LocalDate.of(2026, 7, 1), null);
        when(recurringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(recurring));

        assertThat(service.executeDueTransaction(1L, LocalDate.of(2026, 6, 30))).isZero();

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(recurringRepository, never()).save(any(RecurringTransaction.class));
    }

    private RecurringTransaction recurring(LocalDate startDate, LocalDate endDate) {
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setId(1L);
        recurring.setAmount(new BigDecimal("120.00"));
        recurring.setDescription("Internet");
        recurring.setType(TransactionType.EXPENSE);
        recurring.setSourceType(SourceType.WEB);
        recurring.setFrequency(RecurrenceFrequency.MONTHLY);
        recurring.setStartDate(startDate);
        recurring.setEndDate(endDate);
        recurring.setNextExecutionDate(startDate);
        recurring.setActive(true);
        recurring.setUser(new User());
        recurring.setAccount(new Account());
        recurring.setCategory(new Category());
        return recurring;
    }
}
