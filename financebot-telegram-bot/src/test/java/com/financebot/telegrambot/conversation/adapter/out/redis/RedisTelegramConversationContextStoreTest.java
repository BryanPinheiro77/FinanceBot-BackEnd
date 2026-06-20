package com.financebot.telegrambot.conversation.adapter.out.redis;

import com.financebot.telegrambot.conversation.config.TelegramConversationProperties;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContextType;
import com.financebot.telegrambot.conversation.domain.TelegramConversationMissingField;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTelegramConversationContextStoreTest {

    private static final Long TELEGRAM_ID = 123L;
    private static final String KEY = "financebot:telegram:conversation:123";
    private static final String JSON = "{\"type\":\"PENDING_MISSING_INFORMATION\"}";
    private static final Duration CONTEXT_TTL = Duration.ofMinutes(30);
    private static final Duration QUERY_CONTEXT_TTL = Duration.ofMinutes(10);

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JsonMapper jsonMapper;

    private RedisTelegramConversationContextStore store;

    @BeforeEach
    void setUp() {
        TelegramConversationProperties properties = new TelegramConversationProperties(
                CONTEXT_TTL,
                QUERY_CONTEXT_TTL
        );
        store = new RedisTelegramConversationContextStore(stringRedisTemplate, jsonMapper, properties);
    }

    @Test
    void shouldSaveContextWithTtl() throws JacksonException {
        TelegramConversationContext context = createContext();
        when(jsonMapper.writeValueAsString(context)).thenReturn(JSON);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        store.save(TELEGRAM_ID, context);

        verify(valueOperations).set(KEY, JSON, CONTEXT_TTL);
    }

    @Test
    void shouldThrowWhenContextSerializationFails() throws JacksonException {
        TelegramConversationContext context = createContext();
        JacksonException exception = new JacksonException("serialization error") {
        };
        when(jsonMapper.writeValueAsString(context)).thenThrow(exception);

        assertThatThrownBy(() -> store.save(TELEGRAM_ID, context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize Telegram conversation context")
                .hasCause(exception);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void shouldFindContextByTelegramId() throws JacksonException {
        TelegramConversationContext context = createContext();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(JSON);
        when(jsonMapper.readValue(JSON, TelegramConversationContext.class)).thenReturn(context);

        Optional<TelegramConversationContext> result = store.findByTelegramId(TELEGRAM_ID);

        assertThat(result).contains(context);
    }

    @Test
    void shouldReturnEmptyWhenContextDoesNotExist() throws JacksonException {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        Optional<TelegramConversationContext> result = store.findByTelegramId(TELEGRAM_ID);

        assertThat(result).isEmpty();
        verify(jsonMapper, never()).readValue(JSON, TelegramConversationContext.class);
    }

    @Test
    void shouldReturnEmptyWhenStoredContextIsBlank() throws JacksonException {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(" ");

        Optional<TelegramConversationContext> result = store.findByTelegramId(TELEGRAM_ID);

        assertThat(result).isEmpty();
        verify(jsonMapper, never()).readValue(JSON, TelegramConversationContext.class);
    }

    @Test
    void shouldThrowWhenContextDeserializationFails() throws JacksonException {
        JacksonException exception = new JacksonException("deserialization error") {
        };
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(JSON);
        when(jsonMapper.readValue(JSON, TelegramConversationContext.class)).thenThrow(exception);

        assertThatThrownBy(() -> store.findByTelegramId(TELEGRAM_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to deserialize Telegram conversation context")
                .hasCause(exception);
    }

    @Test
    void shouldDeleteContextByTelegramId() {
        store.deleteByTelegramId(TELEGRAM_ID);

        verify(stringRedisTemplate).delete(KEY);
    }

    @Test
    void shouldCheckIfContextExistsByTelegramId() {
        when(stringRedisTemplate.hasKey(KEY)).thenReturn(true);

        boolean exists = store.existsByTelegramId(TELEGRAM_ID);

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenRedisHasKeyReturnsNull() {
        when(stringRedisTemplate.hasKey(KEY)).thenReturn(null);

        boolean exists = store.existsByTelegramId(TELEGRAM_ID);

        assertThat(exists).isFalse();
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
