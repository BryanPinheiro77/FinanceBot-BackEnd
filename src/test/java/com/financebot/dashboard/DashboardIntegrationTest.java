package com.financebot.dashboard;

import com.financebot.account.domain.Account;
import com.financebot.account.domain.AccountType;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "financebot.recurring.scheduler.cron=-", "financebot.alerts.scheduler.cron=-",
        "financebot.alerts.publish-interval=86400000", "financebot.reminders.publish-interval=86400000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired UserRepository users;

    @Test
    void aggregatesExactCentsDailyCategoriesAndInstallmentsOnlyInsideMonth() throws Exception {
        Actor actor = actor("first");
        Category food = category(actor, "Alimentação de teste", CategoryType.EXPENSE);
        Category travel = category(actor, "Transporte de teste", CategoryType.EXPENSE);
        Category salary = category(actor, "Salário de teste", CategoryType.INCOME);
        transaction(actor, salary, "2024-02-01", "1000.01", false);
        transaction(actor, food, "2024-02-01", "0.10", false);
        transaction(actor, food, "2024-02-01", "0.20", false);
        transaction(actor, food, "2024-02-29", "20.00", true);
        transaction(actor, travel, "2024-02-15", "30.00", false);
        transaction(actor, food, "2024-01-31", "888.00", false);
        transaction(actor, food, "2024-03-01", "999.00", true);
        food.setActive(false); // O histórico inclui categorias desativadas.
        em.flush();
        em.clear();

        JsonNode result = summary(actor, "2024-02");
        assertThat(result.path("month").asString()).isEqualTo("2024-02");
        assertThat(result.path("startDate").asString()).isEqualTo("2024-02-01");
        assertThat(result.path("endDate").asString()).isEqualTo("2024-02-29");
        assertThat(result.path("totalIncome").decimalValue()).isEqualByComparingTo("1000.01");
        assertThat(result.path("totalExpense").decimalValue()).isEqualByComparingTo("50.30");
        assertThat(result.path("balance").decimalValue()).isEqualByComparingTo("949.71");
        assertThat(result.path("transactionCount").asLong()).isEqualTo(5);
        assertThat(result.path("daily").size()).isEqualTo(29);
        assertThat(result.path("daily").get(0).path("totalExpense").decimalValue()).isEqualByComparingTo("0.30");
        assertThat(result.path("daily").get(1).path("totalIncome").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(result.path("daily").get(28).path("totalExpense").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(result.path("expensesByCategory").size()).isEqualTo(2);
        assertThat(result.path("expensesByCategory").get(0).path("categoryName").asString()).isEqualTo("Transporte de teste");
        assertThat(result.path("expensesByCategory").get(1).path("totalExpense").decimalValue()).isEqualByComparingTo("20.30");
    }

    @Test
    void emptyMonthHasZeroTotalsAndEveryCalendarDayWithoutUsingBaseIncomeOrInitialBalance() throws Exception {
        Actor actor = actor("empty");
        actor.user.setMonthlyBaseIncome(new BigDecimal("5000.00"));
        actor.account.setInitialBalance(new BigDecimal("9000.00"));
        JsonNode result = summary(actor, "2025-02");
        for (String name : new String[]{"totalIncome", "totalExpense", "balance"}) {
            assertThat(result.path(name).decimalValue()).isEqualByComparingTo("0.00");
        }
        assertThat(result.path("transactionCount").asLong()).isZero();
        assertThat(result.path("daily").size()).isEqualTo(28);
        assertThat(result.path("expensesByCategory").isEmpty()).isTrue();
    }

    @Test
    void isolatesBothUsersEvenWhenCallerSuppliesAnotherUserId() throws Exception {
        Actor first = actor("first");
        Actor second = actor("second");
        transaction(first, category(first, "Categoria", CategoryType.EXPENSE), "2026-11-05", "10.01", false);
        transaction(second, category(second, "Categoria", CategoryType.EXPENSE), "2026-11-05", "88.88", false);
        JsonNode one = json.readTree(mvc.perform(get("/dashboard/monthly-summary").param("month", "2026-11")
                        .param("userId", second.user.getId().toString()).header("Authorization", "Bearer " + first.token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(one.path("totalExpense").decimalValue()).isEqualByComparingTo("10.01");
        assertThat(summary(second, "2026-11").path("totalExpense").decimalValue()).isEqualByComparingTo("88.88");
        assertThat(one.path("expensesByCategory").size()).isEqualTo(1);
    }

    @Test
    void preservesNegativeMonthlyResult() throws Exception {
        Actor actor = actor("negative");
        transaction(actor, category(actor, "Despesa", CategoryType.EXPENSE), "2026-11-01", "25.50", false);
        assertThat(summary(actor, "2026-11").path("balance").decimalValue()).isEqualByComparingTo("-25.50");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2026-00", "2026-13", "2026-1", "2026-11-05", "0000-01", "10000-01", "invalid"})
    void rejectsMalformedMonths(String month) throws Exception {
        Actor actor = actor("invalid");
        mvc.perform(get("/dashboard/monthly-summary").param("month", month)
                        .header("Authorization", "Bearer " + actor.token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresMonthAndRealAuthentication() throws Exception {
        mvc.perform(get("/dashboard/monthly-summary").param("month", "2026-11"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/dashboard/monthly-summary").param("month", "2026-11")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        Actor actor = actor("required");
        mvc.perform(get("/dashboard/monthly-summary").header("Authorization", "Bearer " + actor.token))
                .andExpect(status().isBadRequest());
    }

    private JsonNode summary(Actor actor, String month) throws Exception {
        return json.readTree(mvc.perform(get("/dashboard/monthly-summary").param("month", month)
                        .header("Authorization", "Bearer " + actor.token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private Actor actor(String name) throws Exception {
        JsonNode auth = json.readTree(mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "email", name + "@example.test",
                                "password", "integration-test-only"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        User user = users.findByEmail(name + "@example.test").orElseThrow();
        Account account = new Account();
        account.setUser(user);
        account.setName("Conta de teste");
        account.setType(AccountType.CHECKING_ACCOUNT);
        em.persist(account);
        return new Actor(auth.path("token").asString(), user, account);
    }

    private Category category(Actor actor, String name, CategoryType type) {
        Category category = new Category();
        category.setUser(actor.user);
        category.setName(name);
        category.setType(type);
        em.persist(category);
        return category;
    }

    private void transaction(Actor actor, Category category, String date, String amount, boolean installment) {
        Transaction transaction = new Transaction();
        transaction.setUser(actor.user);
        transaction.setAccount(actor.account);
        transaction.setCategory(category);
        transaction.setType(TransactionType.valueOf(category.getType().name()));
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDescription("Lançamento de teste");
        transaction.setDate(LocalDate.parse(date));
        transaction.setInstallment(installment);
        em.persist(transaction);
    }

    private record Actor(String token, User user, Account account) { }
}
