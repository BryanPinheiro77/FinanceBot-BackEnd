package com.financebot.user.dto.response;

import java.time.LocalDateTime;

public record TelegramLinkCodeResponse(
        String telegramLinkCode,
        LocalDateTime expiresAt,
        String message
) {
}