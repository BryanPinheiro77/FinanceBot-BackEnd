package com.financebot.telegrambot.dto.response;

public record TelegramLinkConfirmResponse(
        boolean success,
        String message
) {
}
