package com.financebot.account.dto;

import com.financebot.account.domain.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        LocalDateTime createdAt
) {
}