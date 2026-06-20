package com.financebot.telegrambot.support;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.dto.response.TelegramDefaultAccountResponse;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramPreviewAccountResolverTest {

    @Mock
    private FinanceBotApiClient financeBotApiClient;

    private TelegramPreviewAccountResolver telegramPreviewAccountResolver;

    @BeforeEach
    void setUp() {
        telegramPreviewAccountResolver = new TelegramPreviewAccountResolver(financeBotApiClient);
    }

    @Test
    @DisplayName("deve preservar conta explicita sem buscar conta padrao")
    void shouldKeepExplicitAccountWithoutFetchingDefaultAccount() {
        PendingTelegramTransaction pending = pendingTransaction("Nubank");

        TelegramPreviewAccountResolver.ResolvedPreviewAccount result =
                telegramPreviewAccountResolver.resolve(pending, 123L);

        assertThat(result.displayName()).isEqualTo("Nubank");
        assertThat(result.persistedName()).isEqualTo("Nubank");
        verify(financeBotApiClient, never()).getDefaultAccount(123L);
    }

    @Test
    @DisplayName("deve usar conta padrao quando nao existe conta explicita")
    void shouldUseDefaultAccountWhenThereIsNoExplicitAccount() {
        PendingTelegramTransaction pending = pendingTransaction(null);
        when(financeBotApiClient.getDefaultAccount(123L))
                .thenReturn(new TelegramDefaultAccountResponse(10L, "Carteira"));

        TelegramPreviewAccountResolver.ResolvedPreviewAccount result =
                telegramPreviewAccountResolver.resolve(pending, 123L);

        assertThat(result.displayName()).isEqualTo("Carteira");
        assertThat(result.persistedName()).isEqualTo("Carteira");
    }

    @Test
    @DisplayName("deve retornar fallback quando busca da conta padrao falha")
    void shouldReturnFallbackWhenDefaultAccountLookupFails() {
        PendingTelegramTransaction pending = pendingTransaction(null);
        when(financeBotApiClient.getDefaultAccount(123L))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        TelegramPreviewAccountResolver.ResolvedPreviewAccount result =
                telegramPreviewAccountResolver.resolve(pending, 123L);

        assertThat(result.displayName()).isEqualTo("conta padrão");
        assertThat(result.persistedName()).isNull();
    }

    private PendingTelegramTransaction pendingTransaction(String accountName) {
        return new PendingTelegramTransaction(
                TelegramIntentType.CREATE_EXPENSE,
                new BigDecimal("50.00"),
                null,
                "mercado",
                LocalDate.of(2026, 6, 1),
                "Mercado",
                accountName,
                null,
                null,
                "gastei 50 no mercado"
        );
    }
}
