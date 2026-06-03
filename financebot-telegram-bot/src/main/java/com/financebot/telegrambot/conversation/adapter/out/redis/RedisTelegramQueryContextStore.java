package com.financebot.telegrambot.conversation.adapter.out.redis;

import com.financebot.telegrambot.conversation.application.port.out.TelegramQueryContextStore;
import com.financebot.telegrambot.conversation.config.TelegramConversationProperties;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

@Component
@ConditionalOnProperty(
        name = "telegram.state.store",
        havingValue = "redis"
)
public class RedisTelegramQueryContextStore implements TelegramQueryContextStore {

    private static final String KEY_PREFIX = "financebot:telegram:query-context:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;
    private final TelegramConversationProperties telegramConversationProperties;

    public RedisTelegramQueryContextStore(
            StringRedisTemplate stringRedisTemplate,
            JsonMapper jsonMapper,
            TelegramConversationProperties telegramConversationProperties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jsonMapper = jsonMapper;
        this.telegramConversationProperties = telegramConversationProperties;
    }

    @Override
    public void save(Long telegramId, ParsedTelegramMessage parsedMessage) {
        try {
            String json = jsonMapper.writeValueAsString(parsedMessage);
            stringRedisTemplate.opsForValue().set(
                    buildKey(telegramId),
                    json,
                    telegramConversationProperties.queryContextTtl()
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize Telegram query context", exception);
        }
    }

    @Override
    public Optional<ParsedTelegramMessage> findByTelegramId(Long telegramId) {
        String json = stringRedisTemplate.opsForValue().get(buildKey(telegramId));

        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonMapper.readValue(json, ParsedTelegramMessage.class));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize Telegram query context", exception);
        }
    }

    @Override
    public void deleteByTelegramId(Long telegramId) {
        stringRedisTemplate.delete(buildKey(telegramId));
    }

    @Override
    public boolean existsByTelegramId(Long telegramId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(telegramId)));
    }

    private String buildKey(Long telegramId) {
        return KEY_PREFIX + telegramId;
    }
}
