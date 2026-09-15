package com.financebot.alert.adapter;

import com.financebot.alert.service.FinancialNotificationService;
import com.financebot.alert.domain.NotificationKind;

import com.financebot.alert.domain.AtypicalExpenseAlert;
import com.financebot.alert.domain.ExcessiveInstallmentAlert;
import com.financebot.alert.domain.FinancialSummary;
import com.financebot.alert.domain.TightBudgetAlert;
import com.financebot.alert.service.AtypicalExpenseDetector;
import com.financebot.alert.service.ExcessiveInstallmentDetector;
import com.financebot.alert.service.FinancialSummaryService;
import com.financebot.alert.service.TightBudgetDetector;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

@Component
@RequiredArgsConstructor
public class FinancialAlertScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinancialAlertScheduler.class);

    private final UserRepository userRepository;
    private final AtypicalExpenseDetector atypicalExpenseDetector;
    private final ExcessiveInstallmentDetector excessiveInstallmentDetector;
    private final TightBudgetDetector tightBudgetDetector;
    private final FinancialSummaryService financialSummaryService;
    private final FinancialAnalysisService financialAnalysisService;
    private final FinancialNotificationService notificationService;
    private final Clock clock;
    @Value("${financebot.alerts.enabled:true}")
    private boolean alertsEnabled;

    @Scheduled(cron = "${financebot.alerts.scheduler.cron:0 0 9 * * *}")
    public void publishAlerts() {
        if (!alertsEnabled) {
            return;
        }
        int page = 0;
        Slice<User> users;
        do {
            users = userRepository.findByTelegramIdIsNotNullOrderByIdAsc(PageRequest.of(page++, 100));
            for (User user : users) {
                if (user.isFinancialAlertsEnabled()) {
                    safely(user, () -> atypicalExpenseDetector.detect(user).forEach(alert -> publishAtypical(user, alert)));
                    safely(user, () -> publishExcessiveInstallment(user));
                    safely(user, () -> publishTightBudget(user));
                }
                // Sempre considerar o último período fechado: recupera uma execução perdida.
                if (user.isWeeklySummaryEnabled()) {
                    safely(user, () -> publishSummary(user, financialSummaryService.previousCompletedWeek(user)));
                }
                if (user.isMonthlySummaryEnabled()) {
                    safely(user, () -> publishSummary(user, financialSummaryService.previousCompletedMonth(user)));
                }
            }
        } while (users.hasNext());
    }

    @Scheduled(fixedDelayString = "${financebot.alerts.publish-interval:60000}",
               initialDelayString = "${financebot.alerts.publish-interval:60000}")
    public void publishPending() {
        notificationService.publishDue();
    }

    private void safely(User user, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            LOGGER.warn("Financial notification generation failed for userId={}", user.getId());
        }
    }

    private void publishAtypical(User user, AtypicalExpenseAlert alert) {
        String key = "atypical-expense:" + alert.categoryName() + ":" + alert.observedMonth();
        notificationService.enqueue(user, NotificationKind.ALERT, "Gasto fora do padrão", alert.explanation(),
                key, alert.observedMonth().plusMonths(2).atDay(1).atStartOfDay());
    }

    private void publishExcessiveInstallment(User user) {
        ExcessiveInstallmentAlert alert = excessiveInstallmentDetector.detect(user);
        if (alert != null) {
            publish(user, "Parcelas em excesso", alert.explanation(), "excessive-installments:" + YearMonth.now(clock));
        }
    }

    private void publishTightBudget(User user) {
        TightBudgetAlert alert = tightBudgetDetector.detect(financialAnalysisService.getFinancialCommitment(user));
        if (alert != null) {
            publish(user, "Orçamento apertado", alert.explanation(), "tight-budget:" + YearMonth.now(clock));
        }
    }

    private void publishSummary(User user, FinancialSummary summary) {
        if (user.getCreatedAt() != null && user.getCreatedAt().toLocalDate().isAfter(summary.endDate())) {
            return;
        }
        NotificationKind kind = NotificationKind.valueOf(summary.periodType());
        String label = kind == NotificationKind.WEEKLY ? "semanal" : "mensal";
        notificationService.enqueue(user, kind, "Resumo financeiro " + label, summary.explanation(),
                summary.periodType() + ":" + summary.endDate(),
                summary.endDate().plusDays(kind == NotificationKind.WEEKLY ? 14 : 45).atStartOfDay());
    }

    private void publish(User user, String title, String body, String periodKey) {
        notificationService.enqueue(user, NotificationKind.ALERT, title, body, periodKey,
                YearMonth.now(clock).plusMonths(1).atDay(1).atStartOfDay());
    }
}
