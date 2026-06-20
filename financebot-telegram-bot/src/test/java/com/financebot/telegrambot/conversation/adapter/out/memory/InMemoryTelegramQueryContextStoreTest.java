package com.financebot.telegrambot.conversation.adapter.out.memory;

import com.financebot.telegrambot.conversation.config.TelegramConversationProperties;
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

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTelegramQueryContextStoreTest {

    private static final Instant NOW = Instant.parse("2026-06-03T12:00:00Z");
    private static final TelegramConversationProperties PROPERTIES = new TelegramConversationProperties(
            Duration.ofMinutes(30),
            Duration.ofMinutes(10)
    );

    private InMemoryTelegramQueryContextStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTelegramQueryContextStore(
                PROPERTIES,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldSaveAndFindQueryContextByTelegramId() {
        Long telegramId = 123L;
        ParsedTelegramMessage parsedMessage = parsedMessage();

        store.save(telegramId, parsedMessage);

        Optional<ParsedTelegramMessage> result = store.findByTelegramId(telegramId);

        assertThat(result).contains(parsedMessage);
    }

    @Test
    void shouldReturnEmptyWhenQueryContextDoesNotExist() {
        Optional<ParsedTelegramMessage> result = store.findByTelegramId(123L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldDeleteQueryContextByTelegramId() {
        Long telegramId = 123L;
        store.save(telegramId, parsedMessage());

        store.deleteByTelegramId(telegramId);

        assertThat(store.findByTelegramId(telegramId)).isEmpty();
    }

    @Test
    void shouldCheckIfQueryContextExistsByTelegramId() {
        Long telegramId = 123L;

        assertThat(store.existsByTelegramId(telegramId)).isFalse();

        store.save(telegramId, parsedMessage());

        assertThat(store.existsByTelegramId(telegramId)).isTrue();
    }

    @Test
    void shouldExpireQueryContextWhenTtlIsReached() {
        InMemoryTelegramQueryContextStore expiredStore =
                new InMemoryTelegramQueryContextStore(
                        new TelegramConversationProperties(Duration.ofMinutes(30), Duration.ZERO),
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );
        Long telegramId = 123L;

        expiredStore.save(telegramId, parsedMessage());

        assertThat(expiredStore.findByTelegramId(telegramId)).isEmpty();
        assertThat(expiredStore.existsByTelegramId(telegramId)).isFalse();
    }

    private ParsedTelegramMessage parsedMessage() {
        return new ParsedTelegramMessage(
                TelegramIntentType.QUERY_TRANSACTION_TOTAL,
                null,
                null,
                LocalDate.now(),
                "quanto gastei com alimentação esse mês?",
                "Alimentação",
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                null,
                null,
                null,
                null,
                null
        );
    }
}
