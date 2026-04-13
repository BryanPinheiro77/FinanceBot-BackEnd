package com.financebot.telegrambot.dto.response;

import java.math.BigDecimal;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        BigDecimal monthlyBaseIncome,
        Long telegramId
) {
}