package com.financebot.user.dto.response;

import java.math.BigDecimal;

public record TelegramUserProfileResponse(
        Long id,
        String name,
        String email,
        BigDecimal monthlyBaseIncome,
        Long telegramId
) {
}