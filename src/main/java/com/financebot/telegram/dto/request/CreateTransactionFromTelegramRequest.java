package com.financebot.telegram.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionFromTelegramRequest(
        Long telegramId,
        String type,
        BigDecimal amount,
        String description,
        LocalDate date,
        String categoryName,
        String accountName
) {}