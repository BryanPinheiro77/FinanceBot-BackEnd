package com.financebot.telegram.dto.response;

public record TelegramDefaultAccountResponse(
        Long accountId,
        String accountName
) {
}
