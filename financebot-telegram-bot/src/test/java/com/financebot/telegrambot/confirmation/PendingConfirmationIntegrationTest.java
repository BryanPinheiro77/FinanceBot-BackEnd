package com.financebot.telegrambot.confirmation;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.conversation.application.TelegramConversationContextService;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.handler.TelegramPendingOperationHandler;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CyclicBarrier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers
class PendingConfirmationIntegrationTest {
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    LettuceConnectionFactory factory;
    StringRedisTemplate redis;
    TelegramPendingConfirmationService service;
    FinanceBotApiClient api;

    @BeforeEach void setup() {
        factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        service = restart();
        service.clearPending(123L);
        api = mock(FinanceBotApiClient.class);
    }
    @AfterEach void close() { factory.destroy(); }

    TelegramPendingConfirmationService restart() {
        return new TelegramPendingConfirmationService(new RedisPendingConfirmationStore(redis, JsonMapper.builder().build()));
    }
    TelegramPendingOperationHandler handler(TelegramPendingConfirmationService pending) {
        var formatter = new TelegramMessageFormatter();
        return new TelegramPendingOperationHandler(api, pending, mock(TelegramPendingQueryService.class), formatter,
                new TelegramBotErrorMapper(formatter), mock(TelegramConversationContextService.class));
    }

    @Test void lostResponseAndRestartReuseExactlyTheSameKeyAndPayload() {
        service.savePending(123L, transaction("Compra"));
        PendingConfirmation original = service.getConfirmation(123L);
        doThrow(new IllegalStateException("Resposta perdida")).doNothing()
                .when(api).createTransaction(any(), eq(original.key()));
        assertThat(handler(service).handleConfirmation(123L)).contains("tentar confirmar novamente");
        var restarted = restart();
        assertThat(restarted.getConfirmation(123L).key()).isEqualTo(original.key());
        assertThat(restarted.getConfirmation(123L).transaction()).isEqualTo(original.transaction());
        assertThat(restarted.getConfirmation(123L).attempted()).isTrue();
        assertThatThrownBy(() -> restarted.savePending(123L, transaction("Editada")))
                .isInstanceOf(PendingConfirmationLockedException.class);
        assertThat(handler(restarted).handleConfirmation(123L)).contains("sucesso");
        verify(api, times(2)).createTransaction(any(), eq(original.key()));
        assertThat(service.hasPending(123L)).isFalse();
    }

    @Test void concurrentConfirmationsUseTheSameKey() throws Exception {
        service.savePending(123L, transaction("Compra"));
        String key = service.getConfirmation(123L).key();
        var barrier = new CyclicBarrier(2);
        doAnswer(invocation -> { barrier.await(5, TimeUnit.SECONDS); return null; })
                .when(api).createTransaction(any(), eq(key));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> handler(service).handleConfirmation(123L));
            var b = executor.submit(() -> handler(restart()).handleConfirmation(123L));
            assertThat(a.get(10, TimeUnit.SECONDS)).contains("sucesso");
            assertThat(b.get(10, TimeUnit.SECONDS)).contains("sucesso");
        }
        verify(api, times(2)).createTransaction(any(), eq(key));
        assertThat(service.hasPending(123L)).isFalse();
    }

    @Test void oldSuccessCannotDeleteAReplacedOperation() {
        service.savePending(123L, transaction("Antiga"));
        String oldKey = service.getConfirmation(123L).key();
        doAnswer(invocation -> { service.clearPending(123L); service.savePending(123L, transaction("Nova")); return null; })
                .when(api).createTransaction(any(), eq(oldKey));
        handler(service).handleConfirmation(123L);
        assertThat(service.getPending(123L).description()).isEqualTo("Nova");
        assertThat(service.getConfirmation(123L).key()).isNotEqualTo(oldKey);
    }

    @Test void expirationDoesNotRefreshOnReadsOrRetries() {
        service.savePending(123L, transaction("Compra"));
        long ttl = redis.getExpire("financebot:telegram:confirmation:123", TimeUnit.MILLISECONDS);
        assertThat(ttl).isBetween(850000L, 900000L);
        PendingConfirmation original = service.getConfirmation(123L);
        assertThat(restart().getConfirmation(123L).expiresAt()).isEqualTo(original.expiresAt());
        redis.expire("financebot:telegram:confirmation:123", java.time.Duration.ZERO);
        assertThat(service.getConfirmation(123L)).isNull();
        verifyNoInteractions(api);
    }

    @Test void memoryStoreExpiresAndDeletesOnlyMatchingKey() {
        var memory = new InMemoryPendingConfirmationStore();
        memory.save(123L, new PendingConfirmation("old-key", transaction("Antiga"), Instant.now().plusSeconds(30), false));
        memory.save(123L, new PendingConfirmation("new-key", transaction("Nova"), Instant.now().plusSeconds(30), false));
        memory.deleteIfKey(123L, "old-key");
        assertThat(memory.find(123L).key()).isEqualTo("new-key");
        memory.deleteIfKey(123L, "new-key");
        assertThat(memory.find(123L)).isNull();
        memory.save(123L, new PendingConfirmation("expired-key", transaction("Expirada"), Instant.now().minusSeconds(1), false));
        assertThat(memory.find(123L)).isNull();
    }

    @Test void failedCleanupKeepsTheKeyForAnotherReplay() {
        service.savePending(123L, transaction("Compra"));
        var wrapped = spy(service);
        String key = service.getConfirmation(123L).key();
        doThrow(new IllegalStateException("Redis indisponível")).doCallRealMethod()
                .when(wrapped).clearConfirmed(123L, key);
        assertThat(handler(wrapped).handleConfirmation(123L)).contains("tentar confirmar novamente");
        assertThat(handler(wrapped).handleConfirmation(123L)).contains("sucesso");
        verify(api, times(2)).createTransaction(any(), eq(key));
        assertThat(service.hasPending(123L)).isFalse();
    }

    @Test void freezingPreservesExactMoneyAndAllNullableFields() {
        var pending = new PendingTelegramTransaction(TelegramIntentType.CREATE_EXPENSE,
                new BigDecimal("123456789012345.67"), null, "Compra \"especial\" 🎉", LocalDate.of(2026, 10, 24),
                null, "Conta", null, null, null);
        service.savePending(123L, pending);
        assertThat(service.beginConfirmation(123L).transaction()).isEqualTo(pending);
        assertThat(restart().getPending(123L)).isEqualTo(pending);
        assertThatThrownBy(() -> service.savePending(123L, transaction("Outra")))
                .isInstanceOf(PendingConfirmationLockedException.class);
    }

    @Test void redisFailureDoesNotSendARequestWithANewKey() {
        var failing = mock(PendingConfirmationStore.class);
        when(failing.begin(123L)).thenThrow(new IllegalStateException("Redis indisponível"));
        assertThatThrownBy(() -> handler(new TelegramPendingConfirmationService(failing)).handleConfirmation(123L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(api);
    }

    @Test void memoryAttemptCannotBeEditedAndExplicitCancellationWarnsAboutPriorAttempt() {
        var memory = new TelegramPendingConfirmationService(new InMemoryPendingConfirmationStore());
        memory.savePending(123L, transaction("Compra"));
        memory.beginConfirmation(123L);
        assertThatThrownBy(() -> memory.savePending(123L, transaction("Editada")))
                .isInstanceOf(PendingConfirmationLockedException.class);
        assertThat(handler(memory).handleCancellation(123L)).contains("pode ter sido salva");
        assertThat(memory.hasPending(123L)).isFalse();
    }

    @Test void editingAndAllNewPreviewsExplainWhyAnAttemptedConfirmationCannotBeReplaced() {
        var memory = new TelegramPendingConfirmationService(new InMemoryPendingConfirmationStore());
        memory.savePending(123L, transaction("Original"));
        String key = memory.beginConfirmation(123L).key();
        var resolver = new com.financebot.telegrambot.support.TelegramPreviewAccountResolver(api);
        var formatter = new com.financebot.telegrambot.formatter.TelegramTransactionMessageFormatter();
        var edit = new com.financebot.telegrambot.handler.TelegramPendingEditHandler(memory, formatter,
                new com.financebot.telegrambot.support.TelegramPendingEditParser(
                        new com.financebot.telegrambot.support.TelegramTextNormalizer()), resolver);
        assertThat(edit.handleEdit(123L, "muda valor para 350")).isEqualTo(PendingConfirmationLockedException.MESSAGE);
        var preview = new com.financebot.telegrambot.handler.TelegramTransactionPreviewHandler(memory, formatter,
                new com.financebot.telegrambot.mapper.PendingTelegramTransactionMapper(), resolver,
                mock(TelegramConversationContextService.class));
        for (var intent : new TelegramIntentType[]{TelegramIntentType.CREATE_EXPENSE,
                TelegramIntentType.CREATE_INSTALLMENT_EXPENSE, TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE}) {
            var parsed = new com.financebot.telegrambot.dto.ParsedTelegramMessage(intent, new BigDecimal("100.00"),
                    "Nova", LocalDate.of(2026, 10, 24), "mensagem", "Categoria", "Conta", null, null,
                    5, 3, null, null, null);
            assertThat(preview.handlePreview(123L, parsed, false)).isEqualTo(PendingConfirmationLockedException.MESSAGE);
        }
        assertThat(memory.getConfirmation(123L).key()).isEqualTo(key);
        assertThat(memory.getPending(123L).description()).isEqualTo("Original");
        verifyNoInteractions(api);
    }

    static PendingTelegramTransaction transaction(String description) {
        return new PendingTelegramTransaction(TelegramIntentType.CREATE_EXPENSE, new BigDecimal("10.00"), null,
                description, LocalDate.of(2026, 10, 24), "Categoria", "Conta", null, null, "mensagem");
    }
}
