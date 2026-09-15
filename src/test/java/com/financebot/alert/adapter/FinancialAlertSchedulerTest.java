package com.financebot.alert.adapter;

import com.financebot.alert.service.FinancialNotificationService;
import com.financebot.alert.domain.*;
import com.financebot.alert.service.*;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class FinancialAlertSchedulerTest {
    @Mock UserRepository users;
    @Mock AtypicalExpenseDetector atypical;
    @Mock ExcessiveInstallmentDetector installments;
    @Mock TightBudgetDetector budget;
    @Mock FinancialSummaryService summaries;
    @Mock FinancialAnalysisService analysis;
    @Mock FinancialNotificationService notifications;
    private FinancialAlertScheduler scheduler;
    private User user;

    @BeforeEach
    void setup() {
        scheduler = new FinancialAlertScheduler(users, atypical, installments, budget, summaries,
                analysis, notifications, Clock.fixed(Instant.parse("2026-09-15T09:00:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(scheduler, "alertsEnabled", true);
        user = new User(); user.setId(1L); user.setTelegramId(123L);
    }

    @Test
    void recoversLatestWeeklyAndMonthlySummariesOnTuesday() {
        when(users.findByTelegramIdIsNotNullOrderByIdAsc(any())).thenReturn(new SliceImpl<>(List.of(user)));
        when(summaries.previousCompletedWeek(user)).thenReturn(summary("WEEKLY", LocalDate.of(2026, 9, 13)));
        when(summaries.previousCompletedMonth(user)).thenReturn(summary("MONTHLY", LocalDate.of(2026, 8, 31)));
        scheduler.publishAlerts();
        verify(notifications).enqueue(eq(user), eq(NotificationKind.WEEKLY), anyString(), anyString(),
                eq("WEEKLY:2026-09-13"), eq(LocalDate.of(2026, 9, 27).atStartOfDay()));
        verify(notifications).enqueue(eq(user), eq(NotificationKind.MONTHLY), anyString(), anyString(),
                eq("MONTHLY:2026-08-31"), eq(LocalDate.of(2026, 10, 15).atStartOfDay()));
    }

    @Test
    void failingRiskAnalysisDoesNotBlockSummaries() {
        when(users.findByTelegramIdIsNotNullOrderByIdAsc(any())).thenReturn(new SliceImpl<>(List.of(user)));
        when(analysis.getFinancialCommitment(user)).thenThrow(new IllegalStateException());
        when(summaries.previousCompletedWeek(user)).thenReturn(summary("WEEKLY", LocalDate.of(2026, 9, 13)));
        when(summaries.previousCompletedMonth(user)).thenReturn(summary("MONTHLY", LocalDate.of(2026, 8, 31)));
        scheduler.publishAlerts();
        verify(notifications, times(2)).enqueue(eq(user), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void skipsAllGenerationWhenGloballyDisabled() {
        ReflectionTestUtils.setField(scheduler, "alertsEnabled", false);
        scheduler.publishAlerts();
        verifyNoInteractions(users, atypical, summaries, notifications);
    }

    @Test
    void optedOutUserDoesNotRunRules() {
        user.setFinancialAlertsEnabled(false); user.setWeeklySummaryEnabled(false); user.setMonthlySummaryEnabled(false);
        when(users.findByTelegramIdIsNotNullOrderByIdAsc(any())).thenReturn(new SliceImpl<>(List.of(user)));
        scheduler.publishAlerts();
        verifyNoInteractions(atypical, installments, budget, analysis, summaries, notifications);
    }

    @Test
    void processesEligibleUsersInPages() {
        when(users.findByTelegramIdIsNotNullOrderByIdAsc(PageRequest.of(0, 100)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 100), true));
        when(users.findByTelegramIdIsNotNullOrderByIdAsc(PageRequest.of(1, 100)))
                .thenReturn(new SliceImpl<>(List.of()));
        scheduler.publishAlerts();
        verify(users).findByTelegramIdIsNotNullOrderByIdAsc(PageRequest.of(1, 100));
    }

    @Test
    void doesNotSendSummaryForPeriodBeforeRegistration() {
        user.setCreatedAt(LocalDateTime.of(2026, 9, 15, 8, 0));
        user.setFinancialAlertsEnabled(false);
        when(users.findByTelegramIdIsNotNullOrderByIdAsc(any())).thenReturn(new SliceImpl<>(List.of(user)));
        when(summaries.previousCompletedWeek(user)).thenReturn(summary("WEEKLY", LocalDate.of(2026, 9, 13)));
        when(summaries.previousCompletedMonth(user)).thenReturn(summary("MONTHLY", LocalDate.of(2026, 8, 31)));
        scheduler.publishAlerts();
        verifyNoInteractions(notifications);
    }

    private FinancialSummary summary(String kind, LocalDate end) {
        return new FinancialSummary(kind, end.minusDays(6), end, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, "Resumo");
    }
}
