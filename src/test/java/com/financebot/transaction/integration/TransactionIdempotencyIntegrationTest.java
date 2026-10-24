package com.financebot.transaction.integration;

import com.financebot.account.domain.Account;
import com.financebot.account.domain.AccountType;
import com.financebot.account.repository.AccountRepository;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.security.jwt.JwtService;
import com.financebot.transaction.idempotency.TransactionIdempotencyService;
import com.financebot.user.domain.User;
import com.financebot.user.domain.UserRole;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "telegram.internal-token=test-internal-token",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.datasource.hikari.maximum-pool-size=8",
        "spring.datasource.hikari.connection-init-sql=SET lock_timeout = '15s'; SET statement_timeout = '20s'",
        "financebot.recurring.scheduler.cron=-", "financebot.alerts.scheduler.cron=-",
        "financebot.alerts.publish-interval=86400000", "financebot.reminders.publish-interval=86400000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class TransactionIdempotencyIntegrationTest {
    private static final String KEY = "test-request-0001";

    @Container
    static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:17-alpine")
            .withEnv("POSTGRES_DB", "idempotency_test").withEnv("POSTGRES_USER", "test_user")
            .withEnv("POSTGRES_PASSWORD", "test-only-password").withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/idempotency_test");
        registry.add("spring.datasource.username", () -> "test_user");
        registry.add("spring.datasource.password", () -> "test-only-password");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TransactionIdempotencyService idempotency;
    @Autowired com.financebot.transaction.idempotency.TransactionIdempotencyProperties idempotencyProperties;
    private Actor first;
    private Actor second;

    @BeforeEach
    void prepareCommittedActors() {
        removeFailureConstraints();
        // Banco exclusivo do container acima. Não há transação de teste envolvendo as requisições.
        jdbc.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        var transaction = new TransactionTemplate(transactionManager);
        first = transaction.execute(status -> actor("first"));
        second = transaction.execute(status -> actor("second"));
    }

    @AfterEach
    void cleanup() {
        removeFailureConstraints();
    }

    @ParameterizedTest
    @EnumSource(Endpoint.class)
    void repeatsReturnOriginalResponseEvenAfterFinancialDataChanges(Endpoint endpoint) throws Exception {
        String request = body(endpoint, first);
        String original = create(endpoint, first, KEY, request, 201);
        jdbc.update("UPDATE transactions SET description = 'Edição posterior', amount = 777.00 WHERE user_id = ?",
                first.userId);

        assertThat(create(endpoint, first, KEY, request, 201)).isEqualTo(original);
        assertThat(transactionCount()).isEqualTo(endpoint.rows);
        assertThat(markerCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT min(amount) FROM transactions", BigDecimal.class))
                .isEqualByComparingTo("777.00");
    }

    @ParameterizedTest
    @EnumSource(Endpoint.class)
    void conflictingPayloadReturns409AndKeepsOriginalResult(Endpoint endpoint) throws Exception {
        String request = body(endpoint, first);
        String original = create(endpoint, first, KEY, request, 201);
        var changed = json.readTree(request).deepCopy();
        ((tools.jackson.databind.node.ObjectNode) changed).put("description", "Outro conteúdo");
        String error = create(endpoint, first, KEY, json.writeValueAsString(changed), 409);
        assertThat(json.readTree(error).path("title").asString()).isEqualTo("Idempotency conflict");
        assertThat(create(endpoint, first, KEY, request, 201)).isEqualTo(original);
        assertThat(transactionCount()).isEqualTo(endpoint.rows);
    }

    @Test
    void typedPayloadNormalizationIgnoresJsonOrderAndEquivalentDecimalScales() throws Exception {
        String original = create(Endpoint.TRANSACTION, first, KEY, body(Endpoint.TRANSACTION, first), 201);
        String reordered = """
                {"categoryId":%d,"accountId":%d,"sourceType":"WEB","type":"EXPENSE",
                 "date":"2026-01-01","description":"Compra privada","amount":100.0000}
                """.formatted(first.categoryId, first.accountId);
        assertThat(create(Endpoint.TRANSACTION, first, KEY, reordered, 201)).isEqualTo(original);
        assertThat(transactionCount()).isEqualTo(1);
    }

    @Test
    void identicalKeysAreIsolatedByUserAndOperation() throws Exception {
        String original = create(Endpoint.TRANSACTION, first, KEY, body(Endpoint.TRANSACTION, first), 201);
        String other = create(Endpoint.TRANSACTION, second, KEY, body(Endpoint.TRANSACTION, second), 201);
        assertThat(json.readTree(original).path("transaction").path("id"))
                .isNotEqualTo(json.readTree(other).path("transaction").path("id"));
        create(Endpoint.INSTALLMENT, first, KEY, body(Endpoint.INSTALLMENT, first), 201);
        create(Endpoint.EXISTING, first, KEY, body(Endpoint.EXISTING, first), 201);
        assertThat(markerCount()).isEqualTo(4);
        assertThat(transactionCount()).isEqualTo(8);
    }

    @ParameterizedTest
    @EnumSource(Endpoint.class)
    void requestsWithoutKeyKeepTheExistingCreationContract(Endpoint endpoint) throws Exception {
        create(endpoint, first, null, body(endpoint, first), 201);
        create(endpoint, first, null, body(endpoint, first), 201);
        assertThat(transactionCount()).isEqualTo(endpoint.rows * 2);
        assertThat(markerCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "spaces are invalid", "test-key,another-key", "chave-inválida"})
    void malformedKeysDoNotWriteAnything(String key) throws Exception {
        create(Endpoint.TRANSACTION, first, key, body(Endpoint.TRANSACTION, first), 400);
        assertThat(transactionCount()).isZero();
        assertThat(markerCount()).isZero();
    }

    @Test
    void oversizedKeysAndUnauthenticatedCallsAreRejected() throws Exception {
        create(Endpoint.TRANSACTION, first, "a".repeat(129), body(Endpoint.TRANSACTION, first), 400);
        mvc.perform(post("/transactions").header("Idempotency-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content(body(Endpoint.TRANSACTION, first)))
                .andExpect(status().isUnauthorized());
        assertThat(markerCount()).isZero();
        assertThat(transactionCount()).isZero();
    }

    @Test
    void validationFailureDoesNotConsumeKey() throws Exception {
        String request = body(Endpoint.TRANSACTION, first);
        create(Endpoint.TRANSACTION, first, KEY, request.replace("100.00", "0.00"), 400);
        assertThat(markerCount()).isZero();
        create(Endpoint.TRANSACTION, first, KEY, request, 201);
        assertThat(transactionCount()).isEqualTo(1);
    }

    @Test
    void businessFailureRollsBackClaimAndEarlierInstallment() throws Exception {
        jdbc.execute("ALTER TABLE transactions ADD CONSTRAINT test_idempotency_failure CHECK (installment_number <> 2)");
        assertCheckFailure(() -> create(Endpoint.INSTALLMENT, first, KEY, body(Endpoint.INSTALLMENT, first), 201));
        assertThat(markerCount()).isZero();
        assertThat(transactionCount()).isZero();
        removeFailureConstraints();
        String original = create(Endpoint.INSTALLMENT, first, KEY, body(Endpoint.INSTALLMENT, first), 201);
        assertThat(create(Endpoint.INSTALLMENT, first, KEY, body(Endpoint.INSTALLMENT, first), 201)).isEqualTo(original);
        assertThat(transactionCount()).isEqualTo(3);
    }

    @Test
    void failureSavingResponseRollsBackAlreadyCreatedTransactions() throws Exception {
        jdbc.execute("ALTER TABLE transaction_idempotency_requests ADD CONSTRAINT test_idempotency_failure "
                + "CHECK (response_encrypted IS NULL)");
        assertCheckFailure(() -> create(Endpoint.INSTALLMENT, first, KEY, body(Endpoint.INSTALLMENT, first), 201));
        assertThat(markerCount()).isZero();
        assertThat(transactionCount()).isZero();
        removeFailureConstraints();
        create(Endpoint.INSTALLMENT, first, KEY, body(Endpoint.INSTALLMENT, first), 201);
        assertThat(transactionCount()).isEqualTo(3);
    }

    @ParameterizedTest
    @MethodSource("concurrentRequests")
    void concurrentRequestsWaitAndReplayOneCommittedOperation(Endpoint endpoint, boolean expired) throws Exception {
        String request = body(endpoint, first);
        if (expired) {
            create(endpoint, first, KEY, request, 201);
            expireAllMarkers();
        }
        try (var holder = dataSource.getConnection(); var executor = Executors.newFixedThreadPool(2)) {
            holder.setAutoCommit(false);
            String lock = expired
                    ? "SELECT user_id FROM transaction_idempotency_requests WHERE user_id = ? FOR UPDATE"
                    : "SELECT id FROM users WHERE id = ? FOR UPDATE";
            try (var statement = holder.prepareStatement(lock)) {
                statement.setLong(1, first.userId);
                try (var result = statement.executeQuery()) { assertThat(result.next()).isTrue(); }
            }
            try {
                var a = executor.submit(() -> create(endpoint, first, KEY, request, 201));
                var b = executor.submit(() -> create(endpoint, first, KEY, request, 201));
                await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(25)).untilAsserted(() ->
                        assertThat(jdbc.queryForObject("""
                                SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()
                                  AND wait_event_type = 'Lock'
                                  AND query ILIKE 'INSERT INTO transaction_idempotency_requests%'
                                """, Integer.class)).isEqualTo(2));
                holder.commit();
                assertThat(a.get(20, TimeUnit.SECONDS)).isEqualTo(b.get(20, TimeUnit.SECONDS));
            } finally {
                holder.rollback();
            }
        }
        assertThat(transactionCount()).isEqualTo(endpoint.rows * (expired ? 2 : 1));
        assertThat(markerCount()).isEqualTo(1);
    }

    static Stream<Arguments> concurrentRequests() {
        return Stream.of(Endpoint.values()).flatMap(endpoint -> Stream.of(false, true)
                .map(expired -> Arguments.of(endpoint, expired)));
    }

    @Test
    void expiredKeyStartsNewOperationButDoesNotDeleteExistingTransactions() throws Exception {
        String original = create(Endpoint.TRANSACTION, first, KEY, body(Endpoint.TRANSACTION, first), 201);
        expireAllMarkers();
        String next = create(Endpoint.TRANSACTION, first, KEY, body(Endpoint.TRANSACTION, first), 201);
        assertThat(json.readTree(next).path("transaction").path("id"))
                .isNotEqualTo(json.readTree(original).path("transaction").path("id"));
        assertThat(markerCount()).isEqualTo(1);
        assertThat(transactionCount()).isEqualTo(2);
    }

    @Test
    void cleanupSkipsLockedRecordsAndOnlyDeletesExpiredMarkers() throws Exception {
        create(Endpoint.TRANSACTION, first, KEY, body(Endpoint.TRANSACTION, first), 201);
        create(Endpoint.TRANSACTION, second, KEY, body(Endpoint.TRANSACTION, second), 201);
        expireAllMarkers();
        try (var holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            try (var statement = holder.prepareStatement("SELECT user_id FROM transaction_idempotency_requests "
                    + "WHERE user_id = ? FOR UPDATE")) {
                statement.setLong(1, first.userId);
                try (var result = statement.executeQuery()) { assertThat(result.next()).isTrue(); }
            }
            try {
                assertThat(idempotency.purgeExpired()).isEqualTo(1);
                assertThat(markerCount()).isEqualTo(1);
            } finally {
                holder.rollback();
            }
        }
        assertThat(idempotency.purgeExpired()).isEqualTo(1);
        assertThat(transactionCount()).isEqualTo(2);
    }

    @Test
    void storedResponseIsEncryptedAndCannotBeCopiedBetweenUsers() throws Exception {
        create(Endpoint.TRANSACTION, first, KEY, body(Endpoint.TRANSACTION, first), 201);
        create(Endpoint.TRANSACTION, second, KEY, body(Endpoint.TRANSACTION, second), 201);
        String ciphertext = jdbc.queryForObject("SELECT response_encrypted FROM transaction_idempotency_requests "
                + "WHERE user_id = ?", String.class, second.userId);
        assertThat(ciphertext).startsWith("v2.").doesNotContain("Compra privada");
        assertThat(jdbc.queryForObject("SELECT key_hash FROM transaction_idempotency_requests WHERE user_id = ?",
                String.class, first.userId)).hasSize(64).isNotEqualTo(KEY);
        jdbc.update("UPDATE transaction_idempotency_requests SET response_encrypted = ? WHERE user_id = ?",
                ciphertext, first.userId);
        String unavailable = create(Endpoint.TRANSACTION, first, KEY, body(Endpoint.TRANSACTION, first), 503);
        assertThat(unavailable).doesNotContain("Compra privada", ciphertext);
        assertThat(transactionCount()).isEqualTo(2);
    }

    @Test
    void corsAllowsIdempotencyHeader() throws Exception {
        mvc.perform(options("/transactions").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Idempotency-Key, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Idempotency-Key")));
    }

    @ParameterizedTest
    @EnumSource(Endpoint.class)
    void telegramReplaysAndConflictsUseCommittedUserScopedMarkers(Endpoint endpoint) throws Exception {
        linkTelegram(first, 123L);
        String body = telegramBody(endpoint, 123L);
        telegramCreate(endpoint, KEY, body, 200);
        telegramCreate(endpoint, KEY, body, 200);
        telegramCreate(endpoint, KEY, body.replace("Compra privada", "Outra compra"), 409);
        assertThat(transactionCount()).isEqualTo(endpoint.rows);
        assertThat(markerCount()).isEqualTo(1);
        // Replay independe da resolução atual de contas/categorias.
        jdbc.update("UPDATE accounts SET name = 'Renomeada' WHERE id = ?", first.accountId);
        telegramCreate(endpoint, KEY, body, 200);
        assertThat(transactionCount()).isEqualTo(endpoint.rows);
    }

    @ParameterizedTest
    @EnumSource(Endpoint.class)
    void telegramRequiresInternalAuthenticationAndKeepsLegacyContract(Endpoint endpoint) throws Exception {
        linkTelegram(first, 123L);
        String body = telegramBody(endpoint, 123L);
        mvc.perform(post("/telegram" + endpoint.path).contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", KEY).content(body))
                .andExpect(status().isUnauthorized());
        assertThat(markerCount()).isZero();
        telegramCreate(endpoint, null, body, 200);
        telegramCreate(endpoint, null, body, 200);
        assertThat(transactionCount()).isEqualTo(endpoint.rows * 2);
        assertThat(markerCount()).isZero();
    }

    @Test
    void telegramKeysAreIsolatedFromRestAndOtherLinkedUsers() throws Exception {
        linkTelegram(first, 123L);
        linkTelegram(second, 456L);
        telegramCreate(Endpoint.TRANSACTION, KEY, telegramBody(Endpoint.TRANSACTION, 123L), 200);
        telegramCreate(Endpoint.TRANSACTION, KEY, telegramBody(Endpoint.TRANSACTION, 456L), 200);
        create(Endpoint.TRANSACTION, first, KEY, body(Endpoint.TRANSACTION, first), 201);
        assertThat(transactionCount()).isEqualTo(3);
        assertThat(markerCount()).isEqualTo(3);
    }

    @ParameterizedTest
    @EnumSource(Endpoint.class)
    void concurrentTelegramConfirmationsCommitOneOperation(Endpoint endpoint) throws Exception {
        linkTelegram(first, 123L);
        try (var holder = dataSource.getConnection(); var executor = Executors.newFixedThreadPool(2)) {
            holder.setAutoCommit(false);
            try (var lock = holder.prepareStatement("SELECT id FROM users WHERE id = ? FOR UPDATE")) {
                lock.setLong(1, first.userId);
                lock.executeQuery().close();
            }
            try {
                var a = executor.submit(() -> telegramCreate(endpoint, KEY, telegramBody(endpoint, 123L), 200));
                var b = executor.submit(() -> telegramCreate(endpoint, KEY, telegramBody(endpoint, 123L), 200));
                await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(jdbc.queryForObject("""
                        SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()
                          AND wait_event_type = 'Lock'
                          AND query ILIKE 'INSERT INTO transaction_idempotency_requests%'
                        """, Integer.class)).isEqualTo(2));
                holder.commit();
                assertThat(a.get(20, TimeUnit.SECONDS)).isEmpty();
                assertThat(b.get(20, TimeUnit.SECONDS)).isEmpty();
            } finally { holder.rollback(); }
        }
        assertThat(transactionCount()).isEqualTo(endpoint.rows);
        assertThat(markerCount()).isEqualTo(1);
    }

    @Test
    void telegramFailureRollsBackMarkerAndAllowsRetry() throws Exception {
        linkTelegram(first, 123L);
        jdbc.execute("ALTER TABLE transactions ADD CONSTRAINT test_idempotency_failure CHECK (amount < 0)");
        assertCheckFailure(() -> telegramCreate(Endpoint.TRANSACTION, KEY,
                telegramBody(Endpoint.TRANSACTION, 123L), 200));
        assertThat(transactionCount()).isZero();
        assertThat(markerCount()).isZero();
        removeFailureConstraints();
        telegramCreate(Endpoint.TRANSACTION, KEY, telegramBody(Endpoint.TRANSACTION, 123L), 200);
        assertThat(transactionCount()).isEqualTo(1);
    }

    @Test
    void telegramRetentionOutlastsPendingWindowEvenWhenRestTtlIsMinimal() throws Exception {
        linkTelegram(first, 123L);
        long previous = idempotencyProperties.getTtlSeconds();
        try {
            idempotencyProperties.setTtlSeconds(60);
            telegramCreate(Endpoint.TRANSACTION, KEY, telegramBody(Endpoint.TRANSACTION, 123L), 200);
            assertThat(jdbc.queryForObject("SELECT EXTRACT(EPOCH FROM (expires_at-created_at)) "
                    + "FROM transaction_idempotency_requests", BigDecimal.class)).isEqualByComparingTo("3600");
        } finally { idempotencyProperties.setTtlSeconds(previous); }
    }

    @Test
    void telegramRejectsInvalidKeysAndUnknownLinksWithoutCreatingMarkers() throws Exception {
        linkTelegram(first, 123L);
        telegramCreate(Endpoint.TRANSACTION, "bad", telegramBody(Endpoint.TRANSACTION, 123L), 400);
        telegramCreate(Endpoint.TRANSACTION, KEY, telegramBody(Endpoint.TRANSACTION, 999L), 404);
        assertThat(markerCount()).isZero();
        assertThat(transactionCount()).isZero();
    }

    private void linkTelegram(Actor actor, long telegramId) {
        jdbc.update("UPDATE users SET telegram_id = ? WHERE id = ?", telegramId, actor.userId);
    }

    private String telegramBody(Endpoint endpoint, long telegramId) {
        var tree = (tools.jackson.databind.node.ObjectNode) json.readTree(body(endpoint, first));
        tree.remove("accountId"); tree.remove("categoryId"); tree.remove("sourceType");
        tree.put("telegramId", telegramId); tree.put("accountName", "Conta"); tree.put("categoryName", "Categoria");
        return json.writeValueAsString(tree);
    }

    private String telegramCreate(Endpoint endpoint, String key, String body, int expected) throws Exception {
        var request = post("/telegram" + endpoint.path).contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Service-Token", "test-internal-token").content(body);
        if (key != null) request.header("Idempotency-Key", key);
        return mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
    }

    private String create(Endpoint endpoint, Actor actor, String key, String body, int status) throws Exception {
        var request = post(endpoint.path).header("Authorization", actor.token)
                .contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) request.header("Idempotency-Key", key);
        return mvc.perform(request).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
    }

    private String body(Endpoint endpoint, Actor actor) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("description", "Compra privada");
        values.put("type", "EXPENSE"); values.put("sourceType", "WEB");
        values.put("accountId", actor.accountId); values.put("categoryId", actor.categoryId);
        if (endpoint == Endpoint.TRANSACTION) {
            values.put("amount", new BigDecimal("100.00")); values.put("date", "2026-01-01");
        } else {
            values.put("totalAmount", new BigDecimal("100.00"));
            values.put("totalInstallments", endpoint == Endpoint.EXISTING ? 5 : 3);
            values.put(endpoint == Endpoint.EXISTING ? "firstRemainingInstallmentDate" : "firstInstallmentDate", "2026-01-01");
            if (endpoint == Endpoint.EXISTING) values.put("firstRemainingInstallmentNumber", 3);
        }
        return json.writeValueAsString(values);
    }

    private Actor actor(String name) {
        User user = new User();
        user.setName(name); user.setEmail(name + "@example.test");
        user.setPassword("test-only-fixture"); user.setRole(UserRole.USER); user = users.save(user);
        Account account = new Account();
        account.setName("Conta"); account.setType(AccountType.CHECKING_ACCOUNT);
        account.setInitialBalance(new BigDecimal("1000.00")); account.setUser(user); account = accounts.save(account);
        Category category = new Category();
        category.setName("Categoria"); category.setType(CategoryType.EXPENSE); category.setUser(user);
        category = categories.save(category);
        return new Actor(user.getId(), account.getId(), category.getId(), "Bearer " + jwt.generateToken(user));
    }

    private long transactionCount() { return jdbc.queryForObject("SELECT count(*) FROM transactions", Long.class); }
    private long markerCount() { return jdbc.queryForObject("SELECT count(*) FROM transaction_idempotency_requests", Long.class); }

    private void expireAllMarkers() {
        jdbc.update("UPDATE transaction_idempotency_requests SET created_at = CURRENT_TIMESTAMP - INTERVAL '2 days', "
                + "expires_at = CURRENT_TIMESTAMP - INTERVAL '1 day'");
    }

    private void removeFailureConstraints() {
        jdbc.execute("ALTER TABLE transactions DROP CONSTRAINT IF EXISTS test_idempotency_failure");
        jdbc.execute("ALTER TABLE transaction_idempotency_requests DROP CONSTRAINT IF EXISTS test_idempotency_failure");
    }

    private void assertCheckFailure(ThrowingAction action) {
        Throwable failure = catchThrowable(action::run);
        assertThat(failure).isNotNull();
        Throwable root = NestedExceptionUtils.getMostSpecificCause(failure);
        assertThat(root).isInstanceOf(SQLException.class);
        assertThat(((SQLException) root).getSQLState()).isEqualTo("23514");
    }

    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }
    private record Actor(Long userId, Long accountId, Long categoryId, String token) { }
    private enum Endpoint {
        TRANSACTION("/transactions", 1), INSTALLMENT("/transactions/installments", 3),
        EXISTING("/transactions/installments/existing", 3);
        final String path;
        final int rows;
        Endpoint(String path, int rows) { this.path = path; this.rows = rows; }
    }
}
