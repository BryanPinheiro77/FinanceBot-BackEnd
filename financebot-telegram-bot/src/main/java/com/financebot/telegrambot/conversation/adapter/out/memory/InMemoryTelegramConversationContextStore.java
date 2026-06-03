package com.financebot.telegrambot.conversation.adapter.out.memory;

import com.financebot.telegrambot.conversation.application.port.out.TelegramConversationContextStore;
import com.financebot.telegrambot.conversation.config.TelegramConversationProperties;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;
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
public class InMemoryTelegramConversationContextStore implements TelegramConversationContextStore {

    private final Map<Long, StoredConversationContext> contexts = new ConcurrentHashMap<>();
    private final TelegramConversationProperties telegramConversationProperties;
    private final Clock clock;

    @Autowired
    public InMemoryTelegramConversationContextStore(
            TelegramConversationProperties telegramConversationProperties
    ) {
        this(telegramConversationProperties, Clock.systemUTC());
    }

    InMemoryTelegramConversationContextStore(
            TelegramConversationProperties telegramConversationProperties,
            Clock clock
    ) {
        this.telegramConversationProperties = telegramConversationProperties;
        this.clock = clock;
    }

    @Override
    public void save(Long telegramId, TelegramConversationContext context) {
        contexts.put(
                telegramId,
                new StoredConversationContext(
                        context,
                        Instant.now(clock).plus(telegramConversationProperties.contextTtl())
                )
        );
    }

    @Override
    public Optional<TelegramConversationContext> findByTelegramId(Long telegramId) {
        StoredConversationContext storedContext = contexts.get(telegramId);

        if (storedContext == null) {
            return Optional.empty();
        }

        if (storedContext.isExpired(Instant.now(clock))) {
            contexts.remove(telegramId);
            return Optional.empty();
        }

        return Optional.of(storedContext.context());
    }

    @Override
    public void deleteByTelegramId(Long telegramId) {
        contexts.remove(telegramId);
    }

    @Override
    public boolean existsByTelegramId(Long telegramId) {
        return findByTelegramId(telegramId).isPresent();
    }

    private record StoredConversationContext(
            TelegramConversationContext context,
            Instant expiresAt
    ) {

        private boolean isExpired(Instant now) {
            return !expiresAt.isAfter(now);
        }
    }
}
