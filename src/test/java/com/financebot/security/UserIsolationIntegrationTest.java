package com.financebot.security;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user_isolation;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "financebot.recurring.scheduler.cron=-",
        "financebot.alerts.scheduler.cron=-",
        "financebot.alerts.publish-interval=86400000",
        "financebot.reminders.publish-interval=86400000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserIsolationIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private EntityManager entityManager;

    private Actor first;
    private Actor second;

    @BeforeEach
    void createTwoIndependentUsers() throws Exception {
        first = createActor("first");
        second = createActor("second");
        // Estabiliza a escala dos BigDecimals antes das comparações após releitura do banco.
        entityManager.flush();
        entityManager.clear();
    }

    @ParameterizedTest(name = "{0}: listagem isolada, segundo usuário={1}")
    @MethodSource("resourcesAndUsers")
    void listsOnlyAuthenticatedUsersResourcesEvenWithForeignUserId(Resource resource, boolean reverse)
            throws Exception {
        Actor caller = reverse ? second : first;
        Actor other = reverse ? first : second;
        JsonNode response = perform(get(resource.path).param("userId", other.userId.toString()), caller, 200);
        JsonNode items = resource.items(response);

        assertThat(items.size()).isEqualTo(resource == Resource.CATEGORY ? 9 : 1);
        assertThat(ids(items)).contains(caller.id(resource)).doesNotContain(other.id(resource));
        if (resource == Resource.TRANSACTION) {
            assertThat(response.path("totalElements").asInt()).isEqualTo(1);
        }
    }

    @ParameterizedTest(name = "{0}: consulta de recurso alheio, segundo usuário={1}")
    @MethodSource("readableResourcesAndUsers")
    void foreignAndMissingIdsReturnTheSameNotFoundError(Resource resource, boolean reverse) throws Exception {
        Actor caller = reverse ? second : first;
        Actor owner = reverse ? first : second;
        String path = resource.path + "/" + owner.id(resource);

        perform(get(path), owner, 200);
        JsonNode foreignError = perform(get(path), caller, 404);
        JsonNode missingError = perform(get(resource.path + "/" + Long.MAX_VALUE), caller, 404);

        assertThat(foreignError.path("title").asString()).isEqualTo("Resource not found");
        assertThat(foreignError.path("detail")).isEqualTo(missingError.path("detail"));
    }

    @ParameterizedTest(name = "{0} {1}: alteração alheia, segundo usuário={2}")
    @MethodSource("foreignMutations")
    void cannotUpdateOrDeleteAnotherUsersResource(Resource resource, HttpMethod method, boolean reverse)
            throws Exception {
        Actor caller = reverse ? second : first;
        Actor owner = reverse ? first : second;
        JsonNode ownerBefore = list(resource, owner);
        JsonNode callerBefore = list(resource, caller);
        MockHttpServletRequestBuilder request = request(method, resource.path + "/" + owner.id(resource));
        if (method == HttpMethod.PUT) {
            Map<String, Object> body = body(resource, caller);
            body.put(resource == Resource.ACCOUNT || resource == Resource.CATEGORY ? "name" : "description",
                    "Tentativa de alteração");
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }

        perform(request, caller, 404);

        // A releitura após flush/clear detecta alterações persistidas, inclusive soft delete.
        entityManager.flush();
        entityManager.clear();
        assertThat(list(resource, owner)).isEqualTo(ownerBefore);
        assertThat(list(resource, caller)).isEqualTo(callerBefore);
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void ownerCanUpdateAndDeleteOwnResource(Resource resource) throws Exception {
        Map<String, Object> body = body(resource, first);
        String textField = resource == Resource.ACCOUNT || resource == Resource.CATEGORY ? "name" : "description";
        body.put(textField, "Recurso próprio adicional");
        long id = createdId(resource, perform(post(resource.path).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), first, 201));
        body.put(textField, "Alteração permitida");

        JsonNode updated = perform(put(resource.path + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), first, 200);
        assertThat(updated.path(textField).asString()).isEqualTo("Alteração permitida");
        perform(delete(resource.path + "/" + id), first, 204);
        entityManager.flush();
        entityManager.clear();
        assertThat(ids(resource.items(list(resource, first)))).doesNotContain(id);
    }

    @ParameterizedTest(name = "{0}: ativação alheia, segundo usuário={1}")
    @MethodSource("recurrenceActions")
    void cannotActivateOrDeactivateAnotherUsersRecurrence(String action, boolean reverse) throws Exception {
        Actor caller = reverse ? second : first;
        Actor owner = reverse ? first : second;
        String path = Resource.RECURRING.path + "/" + owner.id(Resource.RECURRING) + "/";
        // Cada ataque tentaria mudar o estado, evitando um falso positivo por operação sem efeito.
        perform(patch(path + (action.equals("activate") ? "deactivate" : "activate")), owner, 200);
        JsonNode before = list(Resource.RECURRING, owner);

        perform(patch(path + action), caller, 404);
        entityManager.flush();
        entityManager.clear();
        assertThat(list(Resource.RECURRING, owner)).isEqualTo(before);
        perform(patch(path + action), owner, 200);
    }

    @ParameterizedTest(name = "{0}: vínculo alheio em {1}, segundo usuário={2}")
    @MethodSource("foreignReferences")
    void rejectsForeignAccountOrCategoryWithoutChangingEitherUsersData(
            WriteEndpoint endpoint, String reference, boolean reverse) throws Exception {
        Actor caller = reverse ? second : first;
        Actor owner = reverse ? first : second;
        Map<String, Object> body = endpoint.body(caller);
        body.put(reference, owner.id(reference.equals("accountId") ? Resource.ACCOUNT : Resource.CATEGORY));
        Map<Resource, JsonNode> beforeCaller = snapshot(caller);
        Map<Resource, JsonNode> beforeOwner = snapshot(owner);

        perform(request(endpoint.method, endpoint.path(caller)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), caller, 404);

        entityManager.flush();
        entityManager.clear();
        assertThat(snapshot(caller)).isEqualTo(beforeCaller);
        assertThat(snapshot(owner)).isEqualTo(beforeOwner);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cannotAttachReminderToAnotherUsersRecurrence(boolean reverse) throws Exception {
        Actor caller = reverse ? second : first;
        Actor owner = reverse ? first : second;
        Map<String, Object> body = body(Resource.REMINDER, caller);
        body.put("recurringTransactionId", owner.id(Resource.RECURRING));
        JsonNode before = list(Resource.REMINDER, caller);

        perform(post(Resource.REMINDER.path).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), caller, 404);
        entityManager.flush();
        entityManager.clear();
        assertThat(list(Resource.REMINDER, caller)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"accountId", "categoryId"})
    void foreignTransactionFiltersNeverExposeOtherUsersTransactions(String filter) throws Exception {
        for (Actor caller : new Actor[]{first, second}) {
            Actor owner = caller == first ? second : first;
            Resource reference = filter.equals("accountId") ? Resource.ACCOUNT : Resource.CATEGORY;
            JsonNode result = perform(get("/transactions").param(filter, owner.id(reference).toString()), caller, 200);
            assertThat(result.path("content").isEmpty()).isTrue();
            assertThat(result.path("totalElements").asInt()).isZero();
        }
    }

    @ParameterizedTest
    @EnumSource(Resource.class)
    void protectedResourcesRequireAuthentication(Resource resource) throws Exception {
        mvc.perform(get(resource.path)).andExpect(status().isUnauthorized());
        mvc.perform(get(resource.path).header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("resourcesAndUsers")
    void creationCannotAssignOwnershipUsingForeignUserIdInBody(Resource resource, boolean reverse) throws Exception {
        Actor caller = reverse ? second : first;
        Actor owner = reverse ? first : second;
        Map<String, Object> body = body(resource, caller);
        body.put("userId", owner.userId);
        body.put(resource == Resource.ACCOUNT || resource == Resource.CATEGORY ? "name" : "description",
                "Tentativa de definir proprietário");
        Map<Resource, JsonNode> ownerBefore = snapshot(owner);

        long id = createdId(resource, perform(post(resource.path).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), caller, 201));
        entityManager.flush();
        entityManager.clear();

        assertThat(ids(resource.items(list(resource, caller)))).contains(id);
        assertThat(snapshot(owner)).isEqualTo(ownerBefore);
    }

    private Actor createActor(String name) throws Exception {
        JsonNode auth = json.readTree(mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "email", name + "@example.test",
                                "password", "integration-test-only"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(auth.path("token").asString()).isNotBlank();
        Actor actor = new Actor(auth.path("token").asString(), auth.path("userId").asLong(),
                new EnumMap<>(Resource.class));
        for (Resource resource : Resource.values()) {
            JsonNode created = perform(post(resource.path).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(body(resource, actor))), actor, 201);
            actor.ids.put(resource, createdId(resource, created));
        }
        return actor;
    }

    private static long createdId(Resource resource, JsonNode response) {
        return (resource == Resource.TRANSACTION ? response.path("transaction") : response).path("id").asLong();
    }

    private JsonNode perform(MockHttpServletRequestBuilder request, Actor actor, int expectedStatus) throws Exception {
        var result = mvc.perform(request.header("Authorization", "Bearer " + actor.token))
                .andExpect(status().is(expectedStatus));
        if (expectedStatus == 404) {
            result.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("title").value("Resource not found"));
        }
        String content = result.andReturn().getResponse().getContentAsString();
        return content.isBlank() ? json.nullNode() : json.readTree(content);
    }

    private JsonNode list(Resource resource, Actor actor) throws Exception {
        return perform(get(resource.path), actor, 200);
    }

    private static List<Long> ids(JsonNode items) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode item : items) ids.add(item.path("id").asLong());
        return ids;
    }

    private Map<Resource, JsonNode> snapshot(Actor actor) throws Exception {
        Map<Resource, JsonNode> result = new EnumMap<>(Resource.class);
        for (Resource resource : Resource.values()) {
            result.put(resource, list(resource, actor));
        }
        return result;
    }

    private static Map<String, Object> body(Resource resource, Actor actor) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (resource == Resource.ACCOUNT) {
            body.put("name", "Conta de teste");
            body.put("type", "CHECKING_ACCOUNT");
            body.put("initialBalance", BigDecimal.ZERO);
        } else if (resource == Resource.CATEGORY) {
            body.put("name", "Categoria de teste");
            body.put("type", "EXPENSE");
        } else if (resource == Resource.REMINDER) {
            body.put("description", "Lembrete de teste");
            body.put("reminderDate", TODAY.plusDays(30).toString());
            body.put("daysBefore", 0);
            body.put("active", true);
            body.put("recurringTransactionId", actor.id(Resource.RECURRING));
        } else {
            body.put("description", "Despesa de teste");
            body.put("amount", new BigDecimal("25.00"));
            body.put("type", "EXPENSE");
            body.put("sourceType", "WEB");
            body.put("accountId", actor.id(Resource.ACCOUNT));
            body.put("categoryId", actor.id(Resource.CATEGORY));
            if (resource == Resource.RECURRING) {
                body.put("frequency", "MONTHLY");
                body.put("startDate", TODAY.plusMonths(1).toString());
                body.put("active", true);
            } else {
                body.put("date", TODAY.toString());
            }
        }
        return body;
    }

    static Stream<Arguments> resourcesAndUsers() {
        return Stream.of(Resource.values()).flatMap(resource -> Stream.of(false, true)
                .map(reverse -> Arguments.of(resource, reverse)));
    }

    static Stream<Arguments> readableResourcesAndUsers() {
        return Stream.of(Resource.values()).filter(resource -> resource != Resource.REMINDER)
                .flatMap(resource -> Stream.of(false, true).map(reverse -> Arguments.of(resource, reverse)));
    }

    static Stream<Arguments> foreignMutations() {
        return Stream.of(Resource.values()).flatMap(resource -> Stream.of(HttpMethod.PUT, HttpMethod.DELETE)
                .flatMap(method -> Stream.of(false, true).map(reverse -> Arguments.of(resource, method, reverse))));
    }

    static Stream<Arguments> recurrenceActions() {
        return Stream.of("activate", "deactivate").flatMap(action -> Stream.of(false, true)
                .map(reverse -> Arguments.of(action, reverse)));
    }

    static Stream<Arguments> foreignReferences() {
        return Stream.of(WriteEndpoint.values()).flatMap(endpoint -> Stream.of("accountId", "categoryId")
                .flatMap(reference -> Stream.of(false, true)
                        .map(reverse -> Arguments.of(endpoint, reference, reverse))));
    }

    private record Actor(String token, Long userId, Map<Resource, Long> ids) {
        Long id(Resource resource) { return ids.get(resource); }
    }

    private enum Resource {
        ACCOUNT("/accounts"), CATEGORY("/categories"), TRANSACTION("/transactions"),
        RECURRING("/recurring-transactions"), REMINDER("/reminders");

        final String path;
        Resource(String path) { this.path = path; }
        JsonNode items(JsonNode response) { return this == TRANSACTION ? response.path("content") : response; }
    }

    private enum WriteEndpoint {
        TRANSACTION(HttpMethod.POST, "/transactions", Resource.TRANSACTION),
        UPDATE_TRANSACTION(HttpMethod.PUT, "/transactions", Resource.TRANSACTION),
        RECURRING(HttpMethod.POST, "/recurring-transactions", Resource.RECURRING),
        UPDATE_RECURRING(HttpMethod.PUT, "/recurring-transactions", Resource.RECURRING),
        INSTALLMENT(HttpMethod.POST, "/transactions/installments", Resource.TRANSACTION),
        EXISTING_INSTALLMENT(HttpMethod.POST, "/transactions/installments/existing", Resource.TRANSACTION),
        TRANSACTION_PRECHECK(HttpMethod.POST, "/analysis/financial-commitment/precheck/transaction", Resource.TRANSACTION),
        INSTALLMENT_PRECHECK(HttpMethod.POST, "/analysis/financial-commitment/precheck/installment", Resource.TRANSACTION);

        final HttpMethod method;
        final String path;
        final Resource resource;

        WriteEndpoint(HttpMethod method, String path, Resource resource) {
            this.method = method;
            this.path = path;
            this.resource = resource;
        }

        String path(Actor actor) { return method == HttpMethod.PUT ? path + "/" + actor.id(resource) : path; }

        Map<String, Object> body(Actor actor) {
            Map<String, Object> body = UserIsolationIntegrationTest.body(resource, actor);
            if (this == INSTALLMENT || this == EXISTING_INSTALLMENT || this == INSTALLMENT_PRECHECK) {
                body.remove("amount");
                body.remove("date");
                body.put("totalAmount", new BigDecimal("100.00"));
                body.put("totalInstallments", 4);
                body.put(this == EXISTING_INSTALLMENT ? "firstRemainingInstallmentDate" : "firstInstallmentDate",
                        TODAY.plusMonths(1).toString());
                if (this == EXISTING_INSTALLMENT) body.put("firstRemainingInstallmentNumber", 2);
            }
            return body;
        }
    }
}
