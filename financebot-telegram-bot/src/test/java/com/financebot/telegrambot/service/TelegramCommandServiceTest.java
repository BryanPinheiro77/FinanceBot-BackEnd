package com.financebot.telegrambot.service;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.dto.request.InstallmentPurchaseCapacityRequest;
import com.financebot.telegrambot.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramCommandServiceTest {

    @Mock
    private FinanceBotApiClient financeBotApiClient;

    @Mock
    private TelegramIntentService telegramIntentService;

    @Mock
    private TelegramPendingConfirmationService telegramPendingConfirmationService;

    @Mock
    private TelegramQueryContextService telegramQueryContextService;

    private TelegramCommandService telegramCommandService;

    @BeforeEach
    void setUp() {
        telegramCommandService = new TelegramCommandService(
                financeBotApiClient,
                telegramIntentService,
                telegramPendingConfirmationService,
                telegramQueryContextService,
                new TelegramMessageFormatter()
        );
    }

    @Test
    @DisplayName("deve consultar viabilidade de compra parcelada e formatar resposta")
    void shouldHandleInstallmentPurchaseCapacityQuery() {
        ParsedTelegramMessage parsedMessage = new ParsedTelegramMessage(
                TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY,
                null,
                null,
                null,
                "consigo comprar algo de 2400 parcelado em 12x?",
                null,
                null,
                null,
                null,
                12,
                null,
                new BigDecimal("2400")
        );

        when(telegramIntentService.parse(any())).thenReturn(parsedMessage);
        when(telegramQueryContextService.applyQueryContext(eq(123L), any(), eq(parsedMessage))).thenReturn(parsedMessage);
        when(financeBotApiClient.getInstallmentPurchaseCapacity(any()))
                .thenReturn(new InstallmentPurchaseCapacityResponse(
                        new BigDecimal("2400"),
                        12,
                        new BigDecimal("200.00"),
                        "ALERTA",
                        "A compra pode caber, mas aumenta o comprometimento do orçamento desde a parcela atual."
                ));

        String result = telegramCommandService.handleMessage(
                "consigo comprar algo de 2400 parcelado em 12x?",
                123L,
                "bryan",
                "Bryan"
        );

        ArgumentCaptor<InstallmentPurchaseCapacityRequest> requestCaptor =
                ArgumentCaptor.forClass(InstallmentPurchaseCapacityRequest.class);

        verify(financeBotApiClient).getInstallmentPurchaseCapacity(requestCaptor.capture());
        verify(telegramPendingConfirmationService, never()).savePending(any(), any());

        InstallmentPurchaseCapacityRequest request = requestCaptor.getValue();
        assertThat(request.telegramId()).isEqualTo(123L);
        assertThat(request.totalAmount()).isEqualByComparingTo("2400");
        assertThat(request.totalInstallments()).isEqualTo(12);

        assertThat(result).contains("Análise de compra parcelada");
        assertThat(result).contains("R$");
        assertThat(result).contains("12x");
        assertThat(result).contains("Alerta");
    }

    @Test
    @DisplayName("deve mapear erro 400 da api na consulta de viabilidade")
    void shouldMapBadRequestErrorFromInstallmentPurchaseCapacityQuery() {
        ParsedTelegramMessage parsedMessage = new ParsedTelegramMessage(
                TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY,
                null,
                null,
                null,
                "consigo comprar algo de 2400 parcelado em 12x?",
                null,
                null,
                null,
                null,
                12,
                null,
                new BigDecimal("2400")
        );

        when(telegramIntentService.parse(any())).thenReturn(parsedMessage);
        when(telegramQueryContextService.applyQueryContext(eq(123L), any(), eq(parsedMessage))).thenReturn(parsedMessage);
        when(financeBotApiClient.getInstallmentPurchaseCapacity(any()))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        String result = telegramCommandService.handleMessage(
                "consigo comprar algo de 2400 parcelado em 12x?",
                123L,
                "bryan",
                "Bryan"
        );

        assertThat(result).contains("solicitação está inválida");
    }
}
