package com.financebot.dashboard.port;

import com.financebot.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DashboardTransactionPort {
    List<Total> summarizeForDashboard(Long userId, LocalDate startDate, LocalDate endDate);

    record Total(LocalDate date, TransactionType type, Long categoryId, String categoryName,
                 BigDecimal amount, Long count) {
    }
}
