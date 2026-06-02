package com.financebot.telegrambot.service;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.dto.request.CreateInstallmentTransactionFromTelegramRequest;
import com.financebot.telegrambot.dto.request.CreateTransactionFromTelegramRequest;
import com.financebot.telegrambot.dto.request.InstallmentPurchaseCapacityRequest;
import com.financebot.telegrambot.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.telegrambot.dto.response.TelegramDefaultAccountResponse;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.mapper.PendingTelegramTransactionMapper;
import com.financebot.telegrambot.router.TelegramCommandRouter;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import com.financebot.telegrambot.support.TelegramCommandMatcher;
import com.financebot.telegrambot.support.TelegramTextNormalizer;
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
import java.time.LocalDate;

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
    private TelegramPendingQueryService telegramPendingQueryService;

    @Mock
    private TelegramQueryContextService telegramQueryContextService;

    private TelegramCommandService telegramCommandService;

    @BeforeEach
    void setUp() {
        TelegramMessageFormatter telegramMessageFormatter = new TelegramMessageFormatter();
        TelegramTextNormalizer telegramTextNormalizer = new TelegramTextNormalizer();

        TelegramCommandRouter telegramCommandRouter = new TelegramCommandRouter(
                financeBotApiClient,
                telegramIntentService,
                telegramPendingConfirmationService,
                telegramPendingQueryService,
                telegramQueryContextService,
                telegramMessageFormatter,
                new PendingTelegramTransactionMapper(),
                new TelegramCommandMatcher(telegramTextNormalizer),
                new TelegramBotErrorMapper(telegramMessageFormatter),
                telegramTextNormalizer
        );

        telegramCommandService = new TelegramCommandService(telegramCommandRouter);
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
        when(telegramQueryContextService.applyQueryContext(eq(123L), any(), eq(parsedMessage)))
                .thenReturn(parsedMessage);
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
        when(telegramQueryContextService.applyQueryContext(eq(123L), any(), eq(parsedMessage)))
                .thenReturn(parsedMessage);
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

    @Test
    @DisplayName("deve gerar preview de despesa e salvar PendingTelegramTransaction")
    void shouldGenerateExpensePreviewAndSavePendingTransaction() {
        ParsedTelegramMessage parsedMessage = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_EXPENSE,
                new BigDecimal("50.00"),
                "mercado",
                LocalDate.of(2026, 6, 1),
                "gastei 50 no mercado pelo nubank",
                "Mercado",
                "Nubank",
                null,
                null,
                null,
                null,
                null
        );

        when(telegramIntentService.parse(any())).thenReturn(parsedMessage);
        when(telegramQueryContextService.applyQueryContext(eq(123L), any(), eq(parsedMessage)))
                .thenReturn(parsedMessage);

        String result = telegramCommandService.handleMessage(
                "gastei 50 no mercado pelo nubank",
                123L,
                "bryan",
                "Bryan"
        );

        ArgumentCaptor<PendingTelegramTransaction> pendingCaptor =
                ArgumentCaptor.forClass(PendingTelegramTransaction.class);

        verify(telegramPendingConfirmationService).savePending(eq(123L), pendingCaptor.capture());
        verify(telegramPendingQueryService, never()).savePending(any(), any());

        PendingTelegramTransaction pending = pendingCaptor.getValue();

        assertThat(pending.intentType()).isEqualTo(TelegramIntentType.CREATE_EXPENSE);
        assertThat(pending.amount()).isEqualByComparingTo("50.00");
        assertThat(pending.description()).isEqualTo("mercado");
        assertThat(pending.date()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(pending.categoryName()).isEqualTo("Mercado");
        assertThat(pending.accountName()).isEqualTo("Nubank");

        assertThat(result).contains("Entendi esta despesa");
        assertThat(result).contains("Mercado");
        assertThat(result).contains("Nubank");
    }

    @Test
    @DisplayName("deve confirmar despesa usando dados do PendingTelegramTransaction")
    void shouldConfirmExpenseUsingPendingTransactionData() {
        PendingTelegramTransaction pending = new PendingTelegramTransaction(
                TelegramIntentType.CREATE_EXPENSE,
                new BigDecimal("50.00"),
                "mercado",
                LocalDate.of(2026, 6, 1),
                "Mercado",
                "Nubank",
                null,
                "gastei 50 no mercado pelo nubank"
        );

        when(telegramPendingConfirmationService.getPending(123L)).thenReturn(pending);

        String result = telegramCommandService.handleMessage(
                "confirmar",
                123L,
                "bryan",
                "Bryan"
        );

        ArgumentCaptor<CreateTransactionFromTelegramRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTransactionFromTelegramRequest.class);

        verify(financeBotApiClient).createTransaction(requestCaptor.capture());
        verify(telegramPendingConfirmationService).clearPending(123L);

        CreateTransactionFromTelegramRequest request = requestCaptor.getValue();

        assertThat(request.telegramId()).isEqualTo(123L);
        assertThat(request.type()).isEqualTo("EXPENSE");
        assertThat(request.amount()).isEqualByComparingTo("50.00");
        assertThat(request.description()).isEqualTo("mercado");
        assertThat(request.date()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(request.categoryName()).isEqualTo("Mercado");
        assertThat(request.accountName()).isEqualTo("Nubank");

        assertThat(result).contains("Transação registrada com sucesso");
    }

    @Test
    @DisplayName("deve gerar preview de parcelamento e salvar PendingTelegramTransaction")
    void shouldGenerateInstallmentPreviewAndSavePendingTransaction() {
        ParsedTelegramMessage parsedMessage = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
                new BigDecimal("1200.00"),
                "notebook",
                LocalDate.of(2026, 6, 1),
                "comprei notebook de 1200 em 12x no nubank",
                "Eletrônicos",
                "Nubank",
                null,
                null,
                12,
                null,
                null
        );

        when(telegramIntentService.parse(any())).thenReturn(parsedMessage);
        when(telegramQueryContextService.applyQueryContext(eq(123L), any(), eq(parsedMessage)))
                .thenReturn(parsedMessage);

        String result = telegramCommandService.handleMessage(
                "comprei notebook de 1200 em 12x no nubank",
                123L,
                "bryan",
                "Bryan"
        );

        ArgumentCaptor<PendingTelegramTransaction> pendingCaptor =
                ArgumentCaptor.forClass(PendingTelegramTransaction.class);

        verify(telegramPendingConfirmationService).savePending(eq(123L), pendingCaptor.capture());

        PendingTelegramTransaction pending = pendingCaptor.getValue();

        assertThat(pending.intentType()).isEqualTo(TelegramIntentType.CREATE_INSTALLMENT_EXPENSE);
        assertThat(pending.amount()).isEqualByComparingTo("1200.00");
        assertThat(pending.description()).isEqualTo("notebook");
        assertThat(pending.date()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(pending.categoryName()).isEqualTo("Eletrônicos");
        assertThat(pending.accountName()).isEqualTo("Nubank");
        assertThat(pending.totalInstallments()).isEqualTo(12);

        assertThat(result).contains("Entendi este parcelamento");
        assertThat(result).contains("12x");
        assertThat(result).contains("Nubank");
    }

    @Test
    @DisplayName("deve confirmar parcelamento usando dados do PendingTelegramTransaction")
    void shouldConfirmInstallmentUsingPendingTransactionData() {
        PendingTelegramTransaction pending = new PendingTelegramTransaction(
                TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
                new BigDecimal("1200.00"),
                "notebook",
                LocalDate.of(2026, 6, 1),
                "Eletrônicos",
                "Nubank",
                12,
                "comprei notebook de 1200 em 12x no nubank"
        );

        when(telegramPendingConfirmationService.getPending(123L)).thenReturn(pending);

        String result = telegramCommandService.handleMessage(
                "sim",
                123L,
                "bryan",
                "Bryan"
        );

        ArgumentCaptor<CreateInstallmentTransactionFromTelegramRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateInstallmentTransactionFromTelegramRequest.class);

        verify(financeBotApiClient).createInstallmentTransaction(requestCaptor.capture());
        verify(telegramPendingConfirmationService).clearPending(123L);

        CreateInstallmentTransactionFromTelegramRequest request = requestCaptor.getValue();

        assertThat(request.telegramId()).isEqualTo(123L);
        assertThat(request.totalAmount()).isEqualByComparingTo("1200.00");
        assertThat(request.description()).isEqualTo("notebook");
        assertThat(request.firstInstallmentDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(request.categoryName()).isEqualTo("Eletrônicos");
        assertThat(request.accountName()).isEqualTo("Nubank");
        assertThat(request.totalInstallments()).isEqualTo(12);

        assertThat(result).contains("Parcelamento registrado com sucesso");
    }

    @Test
    @DisplayName("deve salvar conta padrao resolvida no PendingTelegramTransaction")
    void shouldSaveResolvedDefaultAccountInPendingTransaction() {
        ParsedTelegramMessage parsedMessage = new ParsedTelegramMessage(
                TelegramIntentType.CREATE_EXPENSE,
                new BigDecimal("80.00"),
                "gasolina",
                LocalDate.of(2026, 6, 1),
                "paguei 80 de gasolina",
                "Combustível",
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(telegramIntentService.parse(any())).thenReturn(parsedMessage);
        when(telegramQueryContextService.applyQueryContext(eq(123L), any(), eq(parsedMessage)))
                .thenReturn(parsedMessage);
        when(financeBotApiClient.getDefaultAccount(123L))
                .thenReturn(new TelegramDefaultAccountResponse(10L, "Carteira"));

        String result = telegramCommandService.handleMessage(
                "paguei 80 de gasolina",
                123L,
                "bryan",
                "Bryan"
        );

        ArgumentCaptor<PendingTelegramTransaction> pendingCaptor =
                ArgumentCaptor.forClass(PendingTelegramTransaction.class);

        verify(telegramPendingConfirmationService).savePending(eq(123L), pendingCaptor.capture());

        PendingTelegramTransaction pending = pendingCaptor.getValue();

        assertThat(pending.accountName()).isEqualTo("Carteira");
        assertThat(result).contains("Carteira");
    }
}