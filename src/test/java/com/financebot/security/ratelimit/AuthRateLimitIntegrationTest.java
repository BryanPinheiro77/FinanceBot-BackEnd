package com.financebot.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_rate_limit;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "app.auth-rate-limit.enabled=true",
        "app.auth-rate-limit.login.max-requests=3",
        "app.auth-rate-limit.login.window-seconds=60",
        "app.auth-rate-limit.register.max-requests=2",
        "app.auth-rate-limit.register.window-seconds=60",
        "app.auth-rate-limit.trusted-proxies=10.0.0.0/24",
        "financebot.recurring.scheduler.cron=-",
        "financebot.alerts.scheduler.cron=-",
        "financebot.alerts.publish-interval=86400000",
        "financebot.reminders.publish-interval=86400000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthRateLimitIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.username", () -> "default");
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired StringRedisTemplate redis;
    @Autowired RedisAuthRateLimitCounter counter;

    @BeforeEach
    void clearDisposableTestContainer() {
        // A conexão aponta exclusivamente para o container temporário criado acima.
        try (var connection = redis.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void limitsInvalidPayloadsBeforeControllerValidationAndIgnoresSpoofedHeaders() throws Exception {
        for (int index = 0; index < 3; index++) {
            mvc.perform(login("198.51.100.1").header("X-Forwarded-For", "203.0.113." + index)
                            .header("Forwarded", "for=203.0.113." + index))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(login("198.51.100.1").header("X-Forwarded-For", "203.0.113.99")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", matchesPattern("[1-9][0-9]*")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("status").value(429))
                .andExpect(jsonPath("title").value("Too many requests"));
        mvc.perform(login("198.51.100.2")).andExpect(status().isBadRequest());
    }

    @Test
    void loginFailuresAndSuccessesConsumeTheSameBudget() throws Exception {
        mvc.perform(post("/auth/register").with(request -> {request.setRemoteAddr("198.51.100.1"); return request;})
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Test","email":"limiter@example.test","password":"test-only-password"}
                        """))
                .andExpect(status().isCreated());
        for (int index = 0; index < 3; index++) {
            mvc.perform(post("/auth/login").with(request -> {request.setRemoteAddr("198.51.100.1"); return request;})
                            .contentType(MediaType.APPLICATION_JSON).content("""
                            {"email":"limiter@example.test","password":"%s"}
                            """.formatted(index == 1 ? "test-only-password" : "incorrect-password")))
                    .andExpect(status().is(index == 1 ? 200 : 401));
        }
        mvc.perform(login("198.51.100.1")).andExpect(status().isTooManyRequests());
    }

    @Test
    void registrationHasItsOwnBudgetAndDoesNotLimitOtherRoutesOrPreflight() throws Exception {
        for (int index = 0; index < 2; index++) {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(options("/auth/register").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
    }

    @Test
    void trustedProxyUsesTheLastUntrustedHopAndSeparatesClients() throws Exception {
        for (int index = 0; index < 3; index++) {
            mvc.perform(login("10.0.0.2").header("X-Forwarded-For", "203.0.113." + index + ", 198.51.100.1"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(login("10.0.0.2").header("X-Forwarded-For", "203.0.113.99, 198.51.100.1"))
                .andExpect(status().isTooManyRequests());
        mvc.perform(login("10.0.0.2").header("X-Forwarded-For", "198.51.100.2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void countersAreSharedBetweenInstancesAndRejectionsDoNotExtendExpiration() throws Exception {
        var rule = new AuthRateLimitProperties.Rule(1, 1);
        String client = UUID.randomUUID().toString();
        assertThat(counter.acquire("login", client, rule)).isZero();
        RedisAuthRateLimitCounter otherInstance = new RedisAuthRateLimitCounter(redis);
        Thread.sleep(100);
        String key = redis.keys("financebot:auth-rate-limit:v1:login:*").iterator().next();
        long ttlBeforeRejection = redis.getExpire(key, TimeUnit.MILLISECONDS);
        assertThat(otherInstance.acquire("login", client, rule)).isEqualTo(1);
        assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS)).isLessThanOrEqualTo(ttlBeforeRejection);
        Thread.sleep(1100);
        assertThat(otherInstance.acquire("login", client, rule)).isZero();
        assertThat(counter.acquire("register", client, rule)).isZero();
    }

    @Test
    void concurrentRequestsAllowExactlyTheConfiguredLimit() throws Exception {
        var rule = new AuthRateLimitProperties.Rule(7, 60);
        String client = UUID.randomUUID().toString();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<Long>> results = new ArrayList<>();
            for (int index = 0; index < 40; index++) {
                results.add(executor.submit((Callable<Long>) () -> {
                    start.await();
                    return counter.acquire("login", client, rule);
                }));
            }
            start.countDown();
            int allowed = 0;
            for (Future<Long> result : results) {
                if (result.get(10, TimeUnit.SECONDS) == 0) allowed++;
            }
            assertThat(allowed).isEqualTo(7);
        }
    }

    @Test
    void redisKeysDoNotContainTheRawIpAndAlwaysExpire() {
        counter.acquire("login", "198.51.100.1", new AuthRateLimitProperties.Rule(3, 60));
        var keys = redis.keys("financebot:auth-rate-limit:v1:*");
        assertThat(keys).hasSize(1);
        String key = keys.iterator().next();
        assertThat(key).doesNotContain("198.51.100.1");
        assertThat(redis.getExpire(key)).isBetween(1L, 60L);
    }

    @Test
    void repairsCounterWithoutTtlInsteadOfBlockingPermanently() {
        var rule = new AuthRateLimitProperties.Rule(1, 60);
        counter.acquire("login", "198.51.100.1", rule);
        String key = redis.keys("financebot:auth-rate-limit:v1:*").iterator().next();
        redis.persist(key);
        assertThat(counter.acquire("login", "198.51.100.1", rule)).isEqualTo(60);
        assertThat(redis.getExpire(key)).isBetween(1L, 60L);
    }

    private MockHttpServletRequestBuilder login(String peer) {
        return post("/auth/login").with(request -> {request.setRemoteAddr(peer); return request;})
                .contentType(MediaType.APPLICATION_JSON).content("{}");
    }
}
