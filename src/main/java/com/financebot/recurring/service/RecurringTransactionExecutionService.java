package com.financebot.recurring.service;

import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecurringTransactionExecutionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Materializes every occurrence that is due up to the reference date.
     * The pessimistic lock keeps two scheduler instances from creating the same occurrence.
     */
    @Transactional
    public int executeDueTransaction(Long recurringTransactionId, LocalDate referenceDate) {
        RecurringTransaction recurring = recurringTransactionRepository.findByIdForUpdate(recurringTransactionId)
                .orElse(null);
        if (recurring == null || !recurring.isDueOnOrBefore(referenceDate)) {
            return 0;
        }

        int created = 0;
        while (recurring.isDueOnOrBefore(referenceDate)) {
            Transaction transaction = new Transaction();
            transaction.setAmount(recurring.getAmount());
            transaction.setDescription(recurring.getDescription());
            transaction.setDate(recurring.getNextExecutionDate());
            transaction.setType(recurring.getType());
            transaction.setSourceType(recurring.getSourceType());
            transaction.setInstallment(false);
            transaction.setUser(recurring.getUser());
            transaction.setAccount(recurring.getAccount());
            transaction.setCategory(recurring.getCategory());
            transactionRepository.save(transaction);

            recurring.setLastExecutedAt(LocalDateTime.now());
            recurring.advanceNextExecutionDate();
            created++;
        }

        if (recurring.getEndDate() != null && recurring.getNextExecutionDate().isAfter(recurring.getEndDate())) {
            recurring.setActive(false);
        }
        recurringTransactionRepository.save(recurring);
        return created;
    }
}
