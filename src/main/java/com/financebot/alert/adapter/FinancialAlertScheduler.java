package com.financebot.alert.adapter;

import com.financebot.alert.application.FinancialAlertNotificationEvent;
import com.financebot.alert.application.FinancialAlertNotificationPublisher;
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
import java.time.LocalDate;
import java.time.YearMonth;

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
    private final FinancialAlertNotificationPublisher publisher;
    private final Clock clock;
    @Value("${financebot.alerts.enabled:true}")
    private boolean alertsEnabled;

    @Scheduled(cron = "${financebot.alerts.scheduler.cron:0 0 9 * * *}")
    public void publishAlerts() {
        if (!alertsEnabled) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        for (User user : userRepository.findAll()) {
            if (user.getTelegramId() == null) {
                continue;
            }
            try {
                atypicalExpenseDetector.detect(user).forEach(alert -> publishAtypical(user, alert));
                publishExcessiveInstallment(user);
                publishTightBudget(user);
                if (today.getDayOfWeek().getValue() == 1) {
                    publishSummary(user, financialSummaryService.previousCompletedWeek(user));
                }
                if (today.getDayOfMonth() == 1) {
                    publishSummary(user, financialSummaryService.previousCompletedMonth(user));
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Could not generate financial alerts for userId={}", user.getId(), exception);
            }
        }
    }

    private void publishAtypical(User user, AtypicalExpenseAlert alert) {
        String key = "atypical-expense:" + alert.categoryName() + ":" + alert.observedMonth();
        publish(user, "Gasto fora do padrão", alert.explanation(), key);
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
        publish(user, "Resumo financeiro " + summary.periodType().toLowerCase(),
                summary.explanation(), summary.periodType().toLowerCase() + ":" + summary.endDate());
    }

    private void publish(User user, String title, String body, String deduplicationKey) {
        String notificationId = user.getId() + ":" + deduplicationKey;
        publisher.publish(new FinancialAlertNotificationEvent(
                notificationId, user.getTelegramId(), title, body, deduplicationKey
        ));
    }
}
