package com.financebot.user.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CurrentUserResponse(
        Long id,
        String name,
        String email,
        BigDecimal monthlyBaseIncome,
        Long telegramId,
        boolean telegramLinked,
        String telegramLinkCode,
        LocalDateTime telegramLinkCodeExpiresAt
) {
}