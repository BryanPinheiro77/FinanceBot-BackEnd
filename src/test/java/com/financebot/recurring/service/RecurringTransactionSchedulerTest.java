package com.financebot.recurring.service;

import com.financebot.common.observability.FinanceBotMetrics;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionSchedulerTest {
    @Mock RecurringTransactionRepository repository;
    @Mock RecurringTransactionExecutionService executionService;
    @Mock FinanceBotMetrics metrics;
    @InjectMocks RecurringTransactionScheduler scheduler;

    @Test
    void shouldExecuteDueTransactionsAndRecordCreatedCount() {
        RecurringTransaction first = new RecurringTransaction();
        first.setId(1L);
        RecurringTransaction second = new RecurringTransaction();
        second.setId(2L);
        when(repository.findAllByActiveTrueAndNextExecutionDateLessThanEqual(any()))
                .thenReturn(List.of(first, second));
        when(executionService.executeDueTransaction(eq(1L), any())).thenReturn(2);
        when(executionService.executeDueTransaction(eq(2L), any())).thenReturn(0);

        scheduler.executeDueTransactions();

        verify(metrics).recordRecurringTransactions(2);
        verify(metrics).recordRecurringTransactions(0);
        verify(executionService).executeDueTransaction(eq(1L), any());
        verify(executionService).executeDueTransaction(eq(2L), any());
    }

    @Test
    void shouldDoNothingWhenThereAreNoDueTransactions() {
        when(repository.findAllByActiveTrueAndNextExecutionDateLessThanEqual(any())).thenReturn(List.of());

        scheduler.executeDueTransactions();

        verifyNoInteractions(executionService, metrics);
    }
}
