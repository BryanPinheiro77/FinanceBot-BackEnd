package com.financebot.telegrambot.conversation.application;

import com.financebot.telegrambot.conversation.application.port.out.TelegramConversationContextStore;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TelegramConversationContextService {

    private final TelegramConversationContextStore telegramConversationContextStore;

    public TelegramConversationContextService(
            TelegramConversationContextStore telegramConversationContextStore
    ) {
        this.telegramConversationContextStore = telegramConversationContextStore;
    }

    public void savePendingContext(Long telegramId, TelegramConversationContext context) {
        telegramConversationContextStore.save(telegramId, context);
    }

    public Optional<TelegramConversationContext> findPendingContext(Long telegramId) {
        return telegramConversationContextStore.findByTelegramId(telegramId);
    }

    public boolean hasPendingContext(Long telegramId) {
        return telegramConversationContextStore.existsByTelegramId(telegramId);
    }

    public void clearPendingContext(Long telegramId) {
        telegramConversationContextStore.deleteByTelegramId(telegramId);
    }
}