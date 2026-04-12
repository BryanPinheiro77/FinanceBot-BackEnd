package com.financebot.telegrambot.dto.request;

public record TelegramLinkConfirmRequest(
        String linkCode,
        Long telegramId,
        String telegramUsername
) {
}
