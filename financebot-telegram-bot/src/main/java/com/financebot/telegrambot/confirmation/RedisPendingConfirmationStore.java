package com.financebot.telegrambot.confirmation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "telegram.state.store", havingValue = "redis")
public class RedisPendingConfirmationStore implements PendingConfirmationStore {
    private static final String PREFIX = "financebot:telegram:confirmation:";
    private static final DefaultRedisScript<Long> DELETE_IF_KEY = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if value and cjson.decode(value).key == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> SAVE = new DefaultRedisScript<>("""
            local old = redis.call('GET', KEYS[1])
            if old and cjson.decode(old).attempted then return 0 end
            redis.call('PSETEX', KEYS[1], ARGV[2], ARGV[1])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<String> BEGIN = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if not value then return nil end
            local envelope = cjson.decode(value)
            envelope.attempted = true
            -- O payload é uma string opaca: Lua não converte valores monetários em doubles.
            local frozen = cjson.encode(envelope)
            redis.call('SET', KEYS[1], frozen, 'KEEPTTL')
            return frozen
            """, String.class);
    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public RedisPendingConfirmationStore(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }
    public boolean save(Long id, PendingConfirmation value) {
        Duration ttl = Duration.between(Instant.now(), value.expiresAt());
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("Confirmação expirada");
        return Long.valueOf(1).equals(redis.execute(SAVE, List.of(PREFIX + id),
                json.writeValueAsString(new Stored(value.key(), json.writeValueAsString(value), value.attempted())),
                Long.toString(Math.max(1, ttl.toMillis()))));
    }
    public PendingConfirmation begin(Long id) {
        String value = redis.execute(BEGIN, List.of(PREFIX + id));
        return decode(value);
    }
    public PendingConfirmation find(Long id) {
        String value = redis.opsForValue().get(PREFIX + id);
        return decode(value);
    }
    private PendingConfirmation decode(String value) {
        if (value == null) return null;
        Stored stored = json.readValue(value, Stored.class);
        PendingConfirmation payload = json.readValue(stored.payload(), PendingConfirmation.class);
        PendingConfirmation confirmation = new PendingConfirmation(stored.key(), payload.transaction(),
                payload.expiresAt(), stored.attempted());
        return confirmation.expiresAt().isAfter(Instant.now()) ? confirmation : null;
    }
    private record Stored(String key, String payload, boolean attempted) { }

    public void delete(Long id) { redis.delete(PREFIX + id); }
    public void deleteIfKey(Long id, String key) {
        redis.execute(DELETE_IF_KEY, List.of(PREFIX + id), key);
    }
}
