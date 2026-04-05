package com.financebot.user.dto.response;

public record TelegramLinkConfirmResponse(
        boolean success,
        String message
) {
}
