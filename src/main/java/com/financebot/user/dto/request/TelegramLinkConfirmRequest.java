package com.financebot.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TelegramLinkConfirmRequest(
        @NotBlank
        String linkCode,

        @NotNull
        Long telegramId,

        String telegramUsername
) {
}