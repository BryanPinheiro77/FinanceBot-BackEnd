package com.financebot.telegrambot.conversation.adapter.out.memory;

import com.financebot.telegrambot.conversation.application.port.out.TelegramQueryContextStore;
import com.financebot.telegrambot.conversation.config.TelegramConversationProperties;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
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

    private final Map<Long, StoredQueryContext> queryContexts = new ConcurrentHashMap<>();
    private final TelegramConversationProperties telegramConversationProperties;
    private final Clock clock;

    @Autowired
    public InMemoryTelegramQueryContextStore(
            TelegramConversationProperties telegramConversationProperties
    ) {
        this(telegramConversationProperties, Clock.systemUTC());
    }

    InMemoryTelegramQueryContextStore(
            TelegramConversationProperties telegramConversationProperties,
            Clock clock
    ) {
        this.telegramConversationProperties = telegramConversationProperties;
        this.clock = clock;
    }

    @Override
    public void save(Long telegramId, ParsedTelegramMessage parsedMessage) {
        queryContexts.put(
                telegramId,
                new StoredQueryContext(
                        parsedMessage,
                        Instant.now(clock).plus(telegramConversationProperties.queryContextTtl())
                )
        );
    }

    @Override
    public Optional<ParsedTelegramMessage> findByTelegramId(Long telegramId) {
        StoredQueryContext storedContext = queryContexts.get(telegramId);

        if (storedContext == null) {
            return Optional.empty();
        }

        if (storedContext.isExpired(Instant.now(clock))) {
            queryContexts.remove(telegramId);
            return Optional.empty();
        }

        return Optional.of(storedContext.parsedMessage());
    }

    @Override
    public void deleteByTelegramId(Long telegramId) {
        queryContexts.remove(telegramId);
    }

    @Override
    public boolean existsByTelegramId(Long telegramId) {
        return findByTelegramId(telegramId).isPresent();
    }

    private record StoredQueryContext(
            ParsedTelegramMessage parsedMessage,
            Instant expiresAt
    ) {

        private boolean isExpired(Instant now) {
            return !expiresAt.isAfter(now);
        }
    }
}
