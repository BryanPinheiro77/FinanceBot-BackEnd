package com.financebot.transaction.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseByCategoryAndDateProjection {

    BigDecimal getAmount();

    LocalDate getDate();

    String getCategoryName();
}
