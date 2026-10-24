package com.financebot.security.ratelimit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
public class RedisAuthRateLimitCounter {
    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();
    static {
        SCRIPT.setLocation(new ClassPathResource("redis/auth-rate-limit.lua"));
        SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;

    public RedisAuthRateLimitCounter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Retorna zero quando permitido ou o Retry-After em segundos quando bloqueado. */
    public long acquire(String operation, String clientIp, AuthRateLimitProperties.Rule rule) {
        String key = "financebot:auth-rate-limit:v1:" + operation + ":" + digest(clientIp);
        Long remainingMillis = redis.execute(SCRIPT, List.of(key),
                Integer.toString(rule.getMaxRequests()), Long.toString(rule.getWindowSeconds() * 1000));
        if (remainingMillis == null || remainingMillis < 0) {
            throw new IllegalStateException("Resposta inválida do contador de autenticação");
        }
        return (remainingMillis + 999) / 1000;
    }

    private String digest(String clientIp) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(clientIp.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
