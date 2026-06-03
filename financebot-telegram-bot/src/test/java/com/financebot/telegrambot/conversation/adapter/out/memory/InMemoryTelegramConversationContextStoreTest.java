package com.financebot.telegrambot.conversation.adapter.out.memory;

import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContextType;
import com.financebot.telegrambot.conversation.domain.TelegramConversationMissingField;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTelegramConversationContextStoreTest {

    private InMemoryTelegramConversationContextStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTelegramConversationContextStore();
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
