package com.financebot.telegrambot.conversation.domain;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;

import java.time.Instant;
import java.util.Set;

public record TelegramConversationContext(
        TelegramConversationContextType type,
        TelegramIntentType intentType,
        ParsedTelegramMessage parsedMessage,
        String originalMessage,
        Set<TelegramConversationMissingField> missingFields,
        Instant createdAt
) {
}