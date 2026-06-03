package com.financebot.telegrambot.conversation.application.port.out;

import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;

import java.util.Optional;

public interface TelegramConversationContextStore {

    void save(Long telegramId, TelegramConversationContext context);

    Optional<TelegramConversationContext> findByTelegramId(Long telegramId);

    void deleteByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);
}