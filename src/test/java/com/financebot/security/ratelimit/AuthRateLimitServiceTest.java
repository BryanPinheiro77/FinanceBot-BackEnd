package com.financebot.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthRateLimitServiceTest {
    @Test
    void disabledLimiterDoesNotAccessRedis() {
        AuthRateLimitProperties properties = new AuthRateLimitProperties();
        properties.setEnabled(false);
        RedisAuthRateLimitCounter counter = mock(RedisAuthRateLimitCounter.class);
        assertThat(new AuthRateLimitService(properties, counter).acquire("login", "198.51.100.1")).isZero();
        verifyNoInteractions(counter);
    }

    @Test
    void rejectsInvalidLimitsAtStartup() {
        new ApplicationContextRunner().withUserConfiguration(Config.class)
                .withPropertyValues("app.auth-rate-limit.login.max-requests=0",
                        "app.auth-rate-limit.register.window-seconds=86401")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AuthRateLimitProperties.class)
    static class Config { }
}
