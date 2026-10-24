package com.financebot.security.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthRateLimitService {
    private final AuthRateLimitProperties properties;
    private final RedisAuthRateLimitCounter counter;

    public long acquire(String operation, String clientIp) {
        if (!properties.isEnabled()) return 0;
        AuthRateLimitProperties.Rule rule = switch (operation) {
            case "login" -> properties.getLogin();
            case "register" -> properties.getRegister();
            default -> throw new IllegalArgumentException("Operação de autenticação inválida");
        };
        return counter.acquire(operation, clientIp, rule);
    }
}
