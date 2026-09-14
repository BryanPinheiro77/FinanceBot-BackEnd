package com.financebot.alert.port;

import com.financebot.transaction.repository.ExpenseByCategoryAndDateProjection;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseHistoryPort {

    List<ExpenseByCategoryAndDateProjection> findExpensesByCategoryAndDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
