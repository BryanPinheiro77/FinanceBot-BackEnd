package com.financebot.telegrambot.dto;

public record TelegramDefaultAccountResponse(
        Long accountId,
        String accountName
) {
}