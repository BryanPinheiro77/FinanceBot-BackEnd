package com.financebot.alert.port;

import com.financebot.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SummaryTransactionPort {

    BigDecimal sumAmountByUserAndTypeBetweenDates(
            Long userId,
            TransactionType type,
            LocalDate startDate,
            LocalDate endDate
    );
}
