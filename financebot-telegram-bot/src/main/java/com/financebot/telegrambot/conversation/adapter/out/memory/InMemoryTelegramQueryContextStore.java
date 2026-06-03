package com.financebot.telegrambot.conversation.adapter.out.memory;

import com.financebot.telegrambot.conversation.application.port.out.TelegramQueryContextStore;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
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
public class InMemoryTelegramQueryContextStore implements TelegramQueryContextStore {

    private final Map<Long, ParsedTelegramMessage> queryContexts = new ConcurrentHashMap<>();

    @Override
    public void save(Long telegramId, ParsedTelegramMessage parsedMessage) {
        queryContexts.put(telegramId, parsedMessage);
    }

    @Override
    public Optional<ParsedTelegramMessage> findByTelegramId(Long telegramId) {
        return Optional.ofNullable(queryContexts.get(telegramId));
    }

    @Override
    public void deleteByTelegramId(Long telegramId) {
        queryContexts.remove(telegramId);
    }

    @Override
    public boolean existsByTelegramId(Long telegramId) {
        return queryContexts.containsKey(telegramId);
    }
}
