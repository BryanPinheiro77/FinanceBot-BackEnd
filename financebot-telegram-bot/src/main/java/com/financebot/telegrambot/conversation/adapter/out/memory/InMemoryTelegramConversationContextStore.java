package com.financebot.telegrambot.conversation.adapter.out.memory;

import com.financebot.telegrambot.conversation.application.port.out.TelegramConversationContextStore;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(
        name = "telegram.state.store",
        havingValue = "memory",
        matchIfMissing = true
)
public class InMemoryTelegramConversationContextStore implements TelegramConversationContextStore {

    private final Map<Long, TelegramConversationContext> contexts = new ConcurrentHashMap<>();

    @Override
    public void save(Long telegramId, TelegramConversationContext context) {
        contexts.put(telegramId, context);
    }

    @Override
    public Optional<TelegramConversationContext> findByTelegramId(Long telegramId) {
        return Optional.ofNullable(contexts.get(telegramId));
    }

    @Override
    public void deleteByTelegramId(Long telegramId) {
        contexts.remove(telegramId);
    }

    @Override
    public boolean existsByTelegramId(Long telegramId) {
        return contexts.containsKey(telegramId);
    }
}
