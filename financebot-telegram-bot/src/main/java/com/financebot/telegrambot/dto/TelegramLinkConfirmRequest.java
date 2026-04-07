package com.financebot.telegrambot.dto;

public record TelegramLinkConfirmRequest(
        String linkCode,
        Long telegramId,
        String telegramUsername
) {
}
