package com.financebot.telegrambot.conversation.adapter.out.redis;

import com.financebot.telegrambot.conversation.config.TelegramConversationProperties;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTelegramQueryContextStoreTest {

    private static final Long TELEGRAM_ID = 123L;
    private static final String KEY = "financebot:telegram:query-context:123";
    private static final String JSON = "{\"intentType\":\"QUERY_TRANSACTION_TOTAL\"}";
    private static final Duration CONTEXT_TTL = Duration.ofMinutes(30);
    private static final Duration QUERY_CONTEXT_TTL = Duration.ofMinutes(10);

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JsonMapper jsonMapper;

    private RedisTelegramQueryContextStore store;

    @BeforeEach
    void setUp() {
        TelegramConversationProperties properties = new TelegramConversationProperties(
                CONTEXT_TTL,
                QUERY_CONTEXT_TTL
        );
        store = new RedisTelegramQueryContextStore(stringRedisTemplate, jsonMapper, properties);
    }

    @Test
    void shouldSaveQueryContextWithTtl() throws JacksonException {
        ParsedTelegramMessage parsedMessage = parsedMessage();
        when(jsonMapper.writeValueAsString(parsedMessage)).thenReturn(JSON);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        store.save(TELEGRAM_ID, parsedMessage);

        verify(valueOperations).set(KEY, JSON, QUERY_CONTEXT_TTL);
    }

    @Test
    void shouldThrowWhenQueryContextSerializationFails() throws JacksonException {
        ParsedTelegramMessage parsedMessage = parsedMessage();
        JacksonException exception = new JacksonException("serialization error") {
        };
        when(jsonMapper.writeValueAsString(parsedMessage)).thenThrow(exception);

        assertThatThrownBy(() -> store.save(TELEGRAM_ID, parsedMessage))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize Telegram query context")
                .hasCause(exception);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void shouldFindQueryContextByTelegramId() throws JacksonException {
        ParsedTelegramMessage parsedMessage = parsedMessage();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(JSON);
        when(jsonMapper.readValue(JSON, ParsedTelegramMessage.class)).thenReturn(parsedMessage);

        Optional<ParsedTelegramMessage> result = store.findByTelegramId(TELEGRAM_ID);

        assertThat(result).contains(parsedMessage);
    }

    @Test
    void shouldReturnEmptyWhenQueryContextDoesNotExist() throws JacksonException {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        Optional<ParsedTelegramMessage> result = store.findByTelegramId(TELEGRAM_ID);

        assertThat(result).isEmpty();
        verify(jsonMapper, never()).readValue(JSON, ParsedTelegramMessage.class);
    }

    @Test
    void shouldReturnEmptyWhenStoredQueryContextIsBlank() throws JacksonException {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(" ");

        Optional<ParsedTelegramMessage> result = store.findByTelegramId(TELEGRAM_ID);

        assertThat(result).isEmpty();
        verify(jsonMapper, never()).readValue(JSON, ParsedTelegramMessage.class);
    }

    @Test
    void shouldThrowWhenQueryContextDeserializationFails() throws JacksonException {
        JacksonException exception = new JacksonException("deserialization error") {
        };
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(JSON);
        when(jsonMapper.readValue(JSON, ParsedTelegramMessage.class)).thenThrow(exception);

        assertThatThrownBy(() -> store.findByTelegramId(TELEGRAM_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to deserialize Telegram query context")
                .hasCause(exception);
    }

    @Test
    void shouldDeleteQueryContextByTelegramId() {
        store.deleteByTelegramId(TELEGRAM_ID);

        verify(stringRedisTemplate).delete(KEY);
    }

    @Test
    void shouldCheckIfQueryContextExistsByTelegramId() {
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
