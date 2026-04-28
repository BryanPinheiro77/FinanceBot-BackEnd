package com.financebot.recurring.mapper;

import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.dto.response.RecurringTransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionMapper {

    public RecurringTransactionResponse toResponse(RecurringTransaction recurringTransaction) {
        return new RecurringTransactionResponse(
                recurringTransaction.getId(),
                recurringTransaction.getDescription(),
                recurringTransaction.getAmount(),
                recurringTransaction.getType(),
                recurringTransaction.getSourceType(),
                recurringTransaction.getFrequency(),
                recurringTransaction.getStartDate(),
                recurringTransaction.getEndDate(),
                recurringTransaction.getNextExecutionDate(),
                recurringTransaction.isActive(),
                recurringTransaction.getLastExecutedAt(),
                recurringTransaction.getAccount().getId(),
                recurringTransaction.getCategory().getId()
        );
    }
}
