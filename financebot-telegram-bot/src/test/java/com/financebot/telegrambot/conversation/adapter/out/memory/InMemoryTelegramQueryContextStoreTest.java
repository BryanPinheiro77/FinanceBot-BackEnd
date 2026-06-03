package com.financebot.telegrambot.conversation.adapter.out.memory;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTelegramQueryContextStoreTest {

    private InMemoryTelegramQueryContextStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTelegramQueryContextStore();
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
                null
        );
    }
}
