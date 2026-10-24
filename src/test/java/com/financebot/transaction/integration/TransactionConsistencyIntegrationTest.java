package com.financebot.transaction.integration;

import com.financebot.account.domain.Account;
import com.financebot.account.domain.AccountType;
import com.financebot.account.repository.AccountRepository;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.recurring.domain.RecurrenceFrequency;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.recurring.service.RecurringTransactionExecutionService;
import com.financebot.security.jwt.JwtService;
import com.financebot.transaction.application.command.CreateExistingInstallmentTransactionCommand;
import com.financebot.transaction.application.command.CreateInstallmentTransactionCommand;
import com.financebot.transaction.application.command.UpdateTransactionCommand;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.usecase.CreateExistingInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.UpdateTransactionUseCase;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.repository.TransactionRepository;
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

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.datasource.hikari.maximum-pool-size=6",
        "spring.datasource.hikari.connection-init-sql=SET lock_timeout = '15s'; SET statement_timeout = '20s'",
        "financebot.recurring.scheduler.cron=-",
        "financebot.alerts.scheduler.cron=-",
        "financebot.alerts.publish-interval=86400000",
        "financebot.reminders.publish-interval=86400000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class TransactionConsistencyIntegrationTest {
    private static final LocalDate JANUARY = LocalDate.of(2026, 1, 1);
    private static final LocalDate MARCH = LocalDate.of(2026, 3, 1);

    @Container
    static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:17-alpine")
            .withEnv("POSTGRES_DB", "consistency_test")
            .withEnv("POSTGRES_USER", "test_user")
            .withEnv("POSTGRES_PASSWORD", "test-only-password")
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost()
                + ":" + POSTGRES.getMappedPort(5432) + "/consistency_test");
        registry.add("spring.datasource.username", () -> "test_user");
        registry.add("spring.datasource.password", () -> "test-only-password");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired TransactionRepository transactions;
    @Autowired RecurringTransactionRepository recurringTransactions;
    @Autowired CreateInstallmentTransactionUseCase createInstallment;
    @Autowired CreateExistingInstallmentTransactionUseCase createExistingInstallment;
    @Autowired UpdateTransactionUseCase updateTransaction;
    @Autowired RecurringTransactionExecutionService executeRecurring;
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired tools.jackson.databind.ObjectMapper json;

    private Fixture fixture;

    @BeforeEach
    void createCommittedFixtureInDisposableDatabase() {
        dropFailureConstraint();
        // Somente o banco exclusivo do container de teste, nunca um datasource externo.
        jdbc.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        fixture = new TransactionTemplate(transactionManager).execute(status -> createFixture());
    }

    @AfterEach
    void removeFailureInjection() {
        dropFailureConstraint();
    }

    @ParameterizedTest
    @EnumSource(InstallmentKind.class)
    void installmentFailureRollsBackEarlierInsertAndCanBeRetried(InstallmentKind kind) {
        List<Map<String, Object>> before = transactionSnapshot();
        BigDecimal balanceBefore = balance();
        int rejectedNumber = kind == InstallmentKind.NEW ? 2 : 4;
        addFailureConstraint("installment_number IS NULL OR installment_number <> " + rejectedNumber);

        assertCheckFailure(() -> createInstallments(kind));

        assertThat(transactionSnapshot()).isEqualTo(before);
        assertThat(balance()).isEqualByComparingTo(balanceBefore);
        dropFailureConstraint();
        InstallmentTransactionResponse result = createInstallments(kind);

        List<Integer> expectedNumbers = kind == InstallmentKind.NEW ? List.of(1, 2, 3) : List.of(3, 4, 5);
        assertThat(jdbc.queryForList("SELECT installment_number FROM transactions WHERE installment_group_id = ? "
                        + "ORDER BY installment_number", Integer.class, result.installmentGroupId()))
                .isEqualTo(expectedNumbers);
        BigDecimal expectedTotal = new BigDecimal(kind == InstallmentKind.NEW ? "100.00" : "60.00");
        assertThat(jdbc.queryForObject("SELECT sum(amount) FROM transactions WHERE installment_group_id = ?",
                BigDecimal.class, result.installmentGroupId())).isEqualByComparingTo(expectedTotal);
        assertThat(transactions.count()).isEqualTo(4);
        assertThat(balance()).isEqualByComparingTo(balanceBefore.subtract(expectedTotal));
    }

    @ParameterizedTest
    @EnumSource(InstallmentKind.class)
    void insufficientTotalCannotGenerateZeroOrNegativeInstallments(InstallmentKind kind) throws Exception {
        List<Map<String, Object>> before = transactionSnapshot();
        mvc.perform(installmentRequest(kind, "0.03", 5)).andExpect(status().isBadRequest());
        assertThat(transactionSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @EnumSource(InstallmentKind.class)
    void fractionalCentTotalIsRejectedWithoutLosingMoneyInPersistence(InstallmentKind kind) throws Exception {
        List<Map<String, Object>> before = transactionSnapshot();
        mvc.perform(installmentRequest(kind, "1.001", 3)).andExpect(status().isBadRequest());
        assertThat(transactionSnapshot()).isEqualTo(before);
    }

    @Test
    void fractionalMonthlyAmountCannotBeHiddenByMultiplication() throws Exception {
        List<Map<String, Object>> before = transactionSnapshot();
        mvc.perform(post("/transactions/installments/existing")
                        .header("Authorization", "Bearer " + jwt.generateToken(fixture.user))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"monthlyAmount":0.015,"description":"Compra pequena",
                         "firstRemainingInstallmentDate":"2026-01-01","type":"EXPENSE","sourceType":"WEB",
                         "accountId":%d,"categoryId":%d,"totalInstallments":2,"firstRemainingInstallmentNumber":1}
                        """.formatted(fixture.accountId, fixture.expenseCategoryId)))
                .andExpect(status().isBadRequest());
        assertThat(transactionSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @EnumSource(InstallmentKind.class)
    void roundingKeepsAllInstallmentsPositiveAndPreservesTotalInDatabase(InstallmentKind kind) throws Exception {
        mvc.perform(installmentRequest(kind, "0.15", 10)).andExpect(status().isCreated());
        List<BigDecimal> amounts = jdbc.queryForList("SELECT amount FROM transactions WHERE is_installment = true "
                + "ORDER BY installment_number", BigDecimal.class);
        assertThat(amounts).hasSize(kind == InstallmentKind.NEW ? 10 : 8)
                .allSatisfy(amount -> assertThat(amount).isGreaterThanOrEqualTo(new BigDecimal("0.01")));
        BigDecimal expected = new BigDecimal(kind == InstallmentKind.NEW ? "0.15" : "0.11");
        assertThat(amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(expected);
    }

    @Test
    void installmentPrecheckMatchesTheAmountsActuallyCommitted() throws Exception {
        LocalDate firstDate = YearMonth.now().plusMonths(1).atDay(1).minusMonths(9);
        String body = json.writeValueAsString(new CreateInstallmentTransactionRequest(new BigDecimal("0.15"),
                "Compra pequena", firstDate, TransactionType.EXPENSE, SourceType.WEB,
                fixture.accountId, fixture.expenseCategoryId, 10));
        String token = "Bearer " + jwt.generateToken(fixture.user);
        var preview = json.readTree(mvc.perform(post("/analysis/financial-commitment/precheck/installment")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/transactions/installments").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        var actual = json.readTree(mvc.perform(get("/analysis/financial-commitment").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(preview.path("nextMonthProjectedExpense").decimalValue())
                .isEqualByComparingTo(actual.path("nextMonthProjectedExpense").decimalValue());
        assertThat(preview.path("totalFutureInstallments").decimalValue())
                .isEqualByComparingTo(actual.path("totalFutureInstallments").decimalValue());
    }

    @Test
    void failedUpdateRestoresAllPreviouslyCommittedFields() {
        List<Map<String, Object>> before = transactionSnapshot();
        BigDecimal balanceBefore = balance();
        addFailureConstraint("description <> 'Falha de teste'");

        assertCheckFailure(() -> updateTransaction.execute(new UpdateTransactionCommand(fixture.transactionId,
                new BigDecimal("150.00"), "Falha de teste", MARCH, TransactionType.EXPENSE, SourceType.BOT_TEXT,
                fixture.accountId, fixture.expenseCategoryId, fixture.user)));

        assertThat(transactionSnapshot()).isEqualTo(before);
        assertThat(balance()).isEqualByComparingTo(balanceBefore);
        dropFailureConstraint();
        updateTransaction.execute(new UpdateTransactionCommand(fixture.transactionId, new BigDecimal("150.00"),
                "Edição válida", MARCH, TransactionType.EXPENSE, SourceType.WEB,
                fixture.accountId, fixture.expenseCategoryId, fixture.user));
        assertThat(jdbc.queryForObject("SELECT description FROM transactions WHERE id = ?", String.class,
                fixture.transactionId)).isEqualTo("Edição válida");
        assertThat(balance()).isEqualByComparingTo("850.00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidHttpUpdatePreservesCommittedTransactionAndBalance(boolean incompatibleCategory) throws Exception {
        List<Map<String, Object>> before = transactionSnapshot();
        BigDecimal balanceBefore = balance();
        mvc.perform(put("/transactions/" + fixture.transactionId)
                        .header("Authorization", "Bearer " + jwt.generateToken(fixture.user))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"amount":%s,"description":"Tentativa inválida","date":"2026-03-01",
                         "type":"EXPENSE","sourceType":"WEB","accountId":%d,"categoryId":%d}
                        """.formatted(incompatibleCategory ? "150.00" : "0", fixture.accountId,
                                incompatibleCategory ? fixture.incomeCategoryId : fixture.expenseCategoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("title").value("Invalid request"));

        assertThat(transactionSnapshot()).isEqualTo(before);
        assertThat(balance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    void recurringFailureRollsBackOccurrencesAndScheduleThenRetryCreatesEachOccurrenceOnce() {
        List<Map<String, Object>> before = transactionSnapshot();
        Map<String, Object> scheduleBefore = scheduleSnapshot();
        BigDecimal balanceBefore = balance();
        addFailureConstraint("transaction_date <> DATE '2026-02-01'");

        assertCheckFailure(() -> executeRecurring.executeDueTransaction(fixture.recurringId, MARCH));

        assertThat(transactionSnapshot()).isEqualTo(before);
        assertThat(scheduleSnapshot()).isEqualTo(scheduleBefore);
        assertThat(balance()).isEqualByComparingTo(balanceBefore);
        dropFailureConstraint();
        assertThat(executeRecurring.executeDueTransaction(fixture.recurringId, MARCH)).isEqualTo(3);
        assertThat(executeRecurring.executeDueTransaction(fixture.recurringId, MARCH)).isZero();
        assertCompletedRecurrence();
    }

    @Test
    void concurrentExecutionsWaitForTheSameRowAndDoNotDuplicateOccurrences() throws Exception {
        // A trava externa garante sobreposição; o teste não depende de sorte no escalonamento das threads.
        try (var holder = dataSource.getConnection(); var executor = Executors.newFixedThreadPool(2)) {
            holder.setAutoCommit(false);
            try (var statement = holder.prepareStatement("SELECT id FROM recurring_transactions WHERE id = ? FOR UPDATE")) {
                statement.setLong(1, fixture.recurringId);
                try (var result = statement.executeQuery()) { assertThat(result.next()).isTrue(); }
            }
            try {
                var first = executor.submit(() -> executeRecurring.executeDueTransaction(fixture.recurringId, MARCH));
                var second = executor.submit(() -> executeRecurring.executeDueTransaction(fixture.recurringId, MARCH));
                await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(25)).untilAsserted(() ->
                        assertThat(jdbc.queryForObject("""
                                SELECT count(*) FROM pg_stat_activity
                                WHERE datname = current_database() AND pid <> pg_backend_pid()
                                  AND wait_event_type = 'Lock' AND query ILIKE 'select%recurring_transactions%'
                                """, Integer.class)).isEqualTo(2));
                holder.commit();
                assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder(0, 3);
            } finally {
                holder.rollback();
                // Libera os workers mesmo se a verificação do bloqueio falhar, antes de fechar o executor.
            }
        }
        assertCompletedRecurrence();
    }

    private void assertCompletedRecurrence() {
        assertThat(jdbc.query("SELECT transaction_date FROM transactions WHERE description = 'Internet de teste' "
                + "ORDER BY transaction_date", (rs, row) -> rs.getDate(1).toLocalDate()))
                .containsExactly(JANUARY, JANUARY.plusMonths(1), MARCH);
        assertThat(transactions.count()).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT active FROM recurring_transactions WHERE id = ?", Boolean.class,
                fixture.recurringId)).isFalse();
        assertThat(jdbc.queryForObject("SELECT next_execution_date FROM recurring_transactions WHERE id = ?",
                LocalDate.class, fixture.recurringId)).isEqualTo(MARCH.plusMonths(1));
        assertThat(jdbc.queryForObject("SELECT last_executed_at FROM recurring_transactions WHERE id = ?",
                java.sql.Timestamp.class, fixture.recurringId)).isNotNull();
        assertThat(balance()).isEqualByComparingTo("615.00");
    }

    private InstallmentTransactionResponse createInstallments(InstallmentKind kind) {
        if (kind == InstallmentKind.NEW) {
            return createInstallment.execute(new CreateInstallmentTransactionCommand(new BigDecimal("100.00"),
                    "Compra de teste", JANUARY, TransactionType.EXPENSE, SourceType.WEB,
                    fixture.accountId, fixture.expenseCategoryId, 3, fixture.user));
        }
        return createExistingInstallment.execute(new CreateExistingInstallmentTransactionCommand(
                new BigDecimal("100.00"), null, "Compra de teste", JANUARY,
                TransactionType.EXPENSE, SourceType.WEB, fixture.accountId, fixture.expenseCategoryId, 5, 3, fixture.user));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder installmentRequest(
            InstallmentKind kind, String amount, int count) {
        boolean existing = kind == InstallmentKind.EXISTING;
        return post(existing ? "/transactions/installments/existing" : "/transactions/installments")
                .header("Authorization", "Bearer " + jwt.generateToken(fixture.user))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"totalAmount":%s,"description":"Compra pequena","%s":"2026-01-01",
                         "type":"EXPENSE","sourceType":"WEB","accountId":%d,"categoryId":%d,
                         "totalInstallments":%d%s}
                        """.formatted(amount, existing ? "firstRemainingInstallmentDate" : "firstInstallmentDate",
                        fixture.accountId, fixture.expenseCategoryId, count,
                        existing ? ",\"firstRemainingInstallmentNumber\":3" : ""));
    }

    private List<Map<String, Object>> transactionSnapshot() {
        return jdbc.queryForList("SELECT * FROM transactions ORDER BY id");
    }

    private Map<String, Object> scheduleSnapshot() {
        return jdbc.queryForMap("SELECT * FROM recurring_transactions WHERE id = ?", fixture.recurringId);
    }

    private BigDecimal balance() {
        return jdbc.queryForObject("""
                SELECT a.initial_balance + coalesce(sum(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END), 0)
                FROM accounts a LEFT JOIN transactions t ON t.account_id = a.id WHERE a.id = ? GROUP BY a.id
                """, BigDecimal.class, fixture.accountId);
    }

    private void assertCheckFailure(Runnable action) {
        Throwable failure = catchThrowable(action::run);
        assertThat(failure).isNotNull();
        Throwable root = NestedExceptionUtils.getMostSpecificCause(failure);
        assertThat(root).isInstanceOf(SQLException.class);
        assertThat(((SQLException) root).getSQLState()).isEqualTo("23514");
    }

    private void addFailureConstraint(String expression) {
        // Expressões constantes do teste, aplicadas somente ao PostgreSQL temporário.
        jdbc.execute("ALTER TABLE transactions ADD CONSTRAINT test_transaction_failure CHECK (" + expression + ")");
    }

    private void dropFailureConstraint() {
        jdbc.execute("ALTER TABLE transactions DROP CONSTRAINT IF EXISTS test_transaction_failure");
    }

    private Fixture createFixture() {
        User user = new User();
        user.setName("Usuário de teste"); user.setEmail("consistency@example.test");
        user.setPassword("test-only-fixture"); user.setRole(UserRole.USER);
        user = users.save(user);
        Account account = new Account();
        account.setName("Conta de teste"); account.setType(AccountType.CHECKING_ACCOUNT);
        account.setInitialBalance(new BigDecimal("1000.00")); account.setUser(user);
        account = accounts.save(account);
        Category expense = category(user, CategoryType.EXPENSE);
        Category income = category(user, CategoryType.INCOME);
        Transaction original = new Transaction();
        original.setAmount(new BigDecimal("25.00")); original.setDescription("Registro anterior");
        original.setDate(JANUARY.minusMonths(1)); original.setType(TransactionType.EXPENSE);
        original.setSourceType(SourceType.WEB); original.setUser(user);
        original.setAccount(account); original.setCategory(expense);
        original = transactions.save(original);
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setDescription("Internet de teste"); recurring.setAmount(new BigDecimal("120.00"));
        recurring.setType(TransactionType.EXPENSE); recurring.setSourceType(SourceType.WEB);
        recurring.setFrequency(RecurrenceFrequency.MONTHLY); recurring.setStartDate(JANUARY);
        recurring.setEndDate(MARCH); recurring.setNextExecutionDate(JANUARY); recurring.setActive(true);
        recurring.setUser(user); recurring.setAccount(account); recurring.setCategory(expense);
        recurring = recurringTransactions.save(recurring);
        return new Fixture(user, account.getId(), expense.getId(), income.getId(), original.getId(), recurring.getId());
    }

    private Category category(User user, CategoryType type) {
        Category category = new Category();
        category.setName("Categoria " + type); category.setType(type); category.setUser(user);
        return categories.save(category);
    }

    private record Fixture(User user, Long accountId, Long expenseCategoryId, Long incomeCategoryId,
                           Long transactionId, Long recurringId) { }
    private enum InstallmentKind { NEW, EXISTING }
}
