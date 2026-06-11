package com.financebot.telegrambot.conversation.adapter.out.memory;

import com.financebot.telegrambot.conversation.config.TelegramConversationProperties;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContextType;
import com.financebot.telegrambot.conversation.domain.TelegramConversationMissingField;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTelegramConversationContextStoreTest {

    private static final Instant NOW = Instant.parse("2026-06-03T12:00:00Z");
    private static final TelegramConversationProperties PROPERTIES = new TelegramConversationProperties(
            Duration.ofMinutes(30),
            Duration.ofMinutes(10)
    );

    private InMemoryTelegramConversationContextStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTelegramConversationContextStore(
                PROPERTIES,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldSaveAndFindContextByTelegramId() {
        Long telegramId = 123L;
        TelegramConversationContext context = createContext();

        store.save(telegramId, context);

        Optional<TelegramConversationContext> result = store.findByTelegramId(telegramId);

        assertThat(result).contains(context);
    }

    @Test
    void shouldReturnEmptyWhenContextDoesNotExist() {
        Optional<TelegramConversationContext> result = store.findByTelegramId(123L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldDeleteContextByTelegramId() {
        Long telegramId = 123L;
        store.save(telegramId, createContext());

        store.deleteByTelegramId(telegramId);

        assertThat(store.findByTelegramId(telegramId)).isEmpty();
    }

    @Test
    void shouldCheckIfContextExistsByTelegramId() {
        Long telegramId = 123L;

        assertThat(store.existsByTelegramId(telegramId)).isFalse();

        store.save(telegramId, createContext());

        assertThat(store.existsByTelegramId(telegramId)).isTrue();
    }

    @Test
    void shouldExpireContextWhenTtlIsReached() {
        InMemoryTelegramConversationContextStore expiredStore =
                new InMemoryTelegramConversationContextStore(
                        new TelegramConversationProperties(Duration.ZERO, Duration.ofMinutes(10)),
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );
        Long telegramId = 123L;

        expiredStore.save(telegramId, createContext());

        assertThat(expiredStore.findByTelegramId(telegramId)).isEmpty();
        assertThat(expiredStore.existsByTelegramId(telegramId)).isFalse();
    }

    private TelegramConversationContext createContext() {
        ParsedTelegramMessage parsedMessage = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
                null,
                "iPhone",
                LocalDate.now(),
                "comprei um iPhone de 6000 em 10x",
                null,
                null,
                null,
                null,
                10,
                null,
                null,
                null
        );

        return new TelegramConversationContext(
                TelegramConversationContextType.PENDING_MISSING_INFORMATION,
                TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
                parsedMessage,
                "comprei um iPhone de 6000 em 10x",
                Set.of(TelegramConversationMissingField.INSTALLMENT_DUE_DAY),
                Instant.now()
        );
    }
}
