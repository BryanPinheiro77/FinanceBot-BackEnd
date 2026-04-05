package com.financebot.telegrambot.dto;

public record TelegramLinkConfirmResponse(
        boolean success,
        String message
) {
}
