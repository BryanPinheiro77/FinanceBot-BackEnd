package com.financebot.telegrambot.dto.response;

public record TelegramDefaultAccountResponse(
        Long accountId,
        String accountName
) {
}