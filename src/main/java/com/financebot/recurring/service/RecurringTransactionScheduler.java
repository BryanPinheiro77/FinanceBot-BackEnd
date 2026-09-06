package com.financebot.recurring.service;

import com.financebot.recurring.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionScheduler {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RecurringTransactionExecutionService executionService;

    @Scheduled(cron = "${financebot.recurring.scheduler.cron:0 0 0 * * *}")
    public void executeDueTransactions() {
        LocalDate today = LocalDate.now();
        recurringTransactionRepository.findAllByActiveTrueAndNextExecutionDateLessThanEqual(today)
                .forEach(recurring -> {
                    int created = executionService.executeDueTransaction(recurring.getId(), today);
                    if (created > 0) {
                        log.info("Generated {} recurring transaction(s) for recurring transaction {}", created, recurring.getId());
                    }
                });
    }
}
