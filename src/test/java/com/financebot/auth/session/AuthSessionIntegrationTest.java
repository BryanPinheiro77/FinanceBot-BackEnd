package com.financebot.auth.session;

import com.financebot.security.jwt.JwtService;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_sessions;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "financebot.recurring.scheduler.cron=-", "financebot.alerts.scheduler.cron=-",
        "financebot.alerts.publish-interval=86400000", "financebot.reminders.publish-interval=86400000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void registrationHashesPasswordAndIssuesShortLivedSessionTokens() throws Exception {
        JsonNode issued = register();
        String access = issued.get("token").asString();
        String email = jwt.extractUsername(access);
        assertThat(passwords.matches("test-only-password", users.findByEmail(email).orElseThrow().getPassword())).isTrue();
        Long lifetime = jwt.extractClaim(access, c -> (c.getExpiration().getTime() - c.getIssuedAt().getTime()) / 1000);
        assertThat(lifetime).isEqualTo(900);
        assertThat(issued.get("refreshToken").asString()).matches("[A-Za-z0-9_-]{43}");
        assertThat(jdbc.queryForObject("select count(*) from auth_refresh_tokens where token_hash = ?", Long.class,
                issued.get("refreshToken").asString())).isZero();
        profile(issued, 200);
    }

    @Test
    void logoutRevokesEveryAccessAndRefreshOfThatSessionAndPreservesAnotherLogin() throws Exception {
        JsonNode first = register();
        String email = jwt.extractUsername(first.get("token").asString());
        JsonNode other = json.readTree(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", email, "password", "test-only-password"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode rotated = refresh(first, 200);
        mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refreshBody(first)))
                .andExpect(status().isNoContent());
        profile(first, 401);
        profile(rotated, 401);
        refresh(rotated, 401);
        profile(other, 200);
        // Logout continua idempotente, inclusive com refresh já consumido.
        mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refreshBody(first)))
                .andExpect(status().isNoContent());
    }

    @Test
    void replayCommitsSessionRevocationEvenThoughResponseIsUnauthorized() throws Exception {
        JsonNode original = register();
        JsonNode rotated = refresh(original, 200);
        assertThat(rotated.get("refreshToken").asString()).isNotEqualTo(original.get("refreshToken").asString());
        refresh(original, 401);
        profile(rotated, 401);
        refresh(rotated, 401);
    }

    @Test
    void unknownRefreshCannotRevokeAValidSession() throws Exception {
        JsonNode issued = register();
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"}"))
                .andExpect(status().isUnauthorized());
        profile(issued, 200);
    }

    @Test
    void tokensWithoutSessionCannotBypassRevocation() throws Exception {
        JsonNode issued = register();
        var user = users.findByEmail(jwt.extractUsername(issued.get("token").asString())).orElseThrow();
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + jwt.generateToken(user)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/activity")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/session")).andExpect(status().isUnauthorized());
    }

    @Test
    void backgroundRefreshDoesNotExtendIdleDeadlineAndActivityDoes() throws Exception {
        JsonNode issued = register();
        UUID sid = sessionId(issued);
        Instant earlier = Instant.now().minusSeconds(3500);
        jdbc.update("update auth_sessions set last_activity_at = ? where id = ?", earlier, sid);
        JsonNode rotated = refresh(issued, 200);
        Instant activity = jdbc.queryForObject("select last_activity_at from auth_sessions where id = ?", Instant.class, sid);
        assertThat(activity).isEqualTo(earlier);
        mvc.perform(post("/auth/activity").header("Authorization", "Bearer " + rotated.get("token").asString()))
                .andExpect(status().isOk()).andExpect(jsonPath("idleWarningAt").exists())
                .andExpect(jsonPath("idleExpiresAt").exists());
        assertThat(jdbc.queryForObject("select last_activity_at from auth_sessions where id = ?", Instant.class, sid))
                .isAfter(earlier);
    }

    @Test
    void warningGraceIsAllowedButExpiredIdleSessionCannotBeRevived() throws Exception {
        JsonNode issued = register();
        UUID sid = sessionId(issued);
        jdbc.update("update auth_sessions set last_activity_at = ? where id = ?", Instant.now().minusSeconds(3610), sid);
        profile(issued, 200);
        jdbc.update("update auth_sessions set last_activity_at = ? where id = ?", Instant.now().minusSeconds(3661), sid);
        profile(issued, 401);
        refresh(issued, 401);
        mvc.perform(post("/auth/activity").header("Authorization", "Bearer " + issued.get("token").asString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void absoluteSessionExpiryCannotBeExtendedByRefresh() throws Exception {
        JsonNode issued = register();
        jdbc.update("update auth_sessions set expires_at = ? where id = ?", Instant.now().minusSeconds(1), sessionId(issued));
        profile(issued, 401);
        refresh(issued, 401);
    }

    @Test
    void concurrentRefreshHasOneWinnerAndReplayRevokesItsSession() throws Exception {
        JsonNode issued = register();
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> call = () -> {
                start.await(5, TimeUnit.SECONDS);
                return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(issued))).andReturn().getResponse().getStatus();
            };
            var first = pool.submit(call);
            var second = pool.submit(call);
            start.countDown();
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 401);
        }
        profile(issued, 401);
    }

    private JsonNode register() throws Exception {
        String email = "session-" + UUID.randomUUID() + "@example.test";
        return json.readTree(mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("name", "Teste", "email", email, "password", "test-only-password"))))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode refresh(JsonNode issued, int expected) throws Exception {
        return json.readTree(mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(issued)))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());
    }

    private String refreshBody(JsonNode issued) throws Exception {
        return json.writeValueAsString(java.util.Map.of("refreshToken", issued.get("refreshToken").asString()));
    }

    private void profile(JsonNode issued, int expected) throws Exception {
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + issued.get("token").asString()))
                .andExpect(status().is(expected));
    }

    private UUID sessionId(JsonNode issued) {
        return UUID.fromString(jwt.extractClaim(issued.get("token").asString(), c -> c.get("sid", String.class)));
    }
}
