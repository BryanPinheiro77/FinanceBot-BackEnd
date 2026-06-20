package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.support.TelegramPendingEditParser;
import com.financebot.telegrambot.support.TelegramPreviewAccountResolver;
import com.financebot.telegrambot.support.TelegramTextNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramPendingEditHandlerTest {

    private static final Long TELEGRAM_ID = 123L;

    @Mock
    private TelegramPendingConfirmationService telegramPendingConfirmationService;

    @Mock
    private FinanceBotApiClient financeBotApiClient;

    private TelegramPendingEditHandler handler;

    @BeforeEach
    void setUp() {
        TelegramTextNormalizer textNormalizer = new TelegramTextNormalizer();

        handler = new TelegramPendingEditHandler(
                telegramPendingConfirmationService,
                new TelegramMessageFormatter(),
                new TelegramPendingEditParser(textNormalizer),
                new TelegramPreviewAccountResolver(financeBotApiClient)
        );
    }

    @Test
    @DisplayName("deve atualizar primeira parcela restante de parcelamento existente")
    void shouldUpdateFirstRemainingInstallmentNumberForExistingInstallment() {
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID))
                .thenReturn(existingInstallmentPending());

        String result = handler.handleEdit(TELEGRAM_ID, "muda para a parcela 8");

        ArgumentCaptor<PendingTelegramTransaction> pendingCaptor =
                ArgumentCaptor.forClass(PendingTelegramTransaction.class);
        verify(telegramPendingConfirmationService).savePending(
                eq(TELEGRAM_ID),
                pendingCaptor.capture()
        );

        PendingTelegramTransaction updated = pendingCaptor.getValue();
        assertThat(updated.firstRemainingInstallmentNumber()).isEqualTo(8);
        assertThat(updated.totalInstallments()).isEqualTo(10);
        assertThat(result).contains("Próxima parcela:</b> 8/10");
        assertThat(result).contains("Parcelas pagas:</b> 7");
    }

    @Test
    @DisplayName("deve atualizar valor mensal sem preencher valor total no parcelamento existente mensal")
    void shouldUpdateMonthlyAmountWithoutSettingTotalAmountForMonthlyExistingInstallment() {
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID))
                .thenReturn(existingInstallmentPendingWithMonthlyAmount());

        String result = handler.handleEdit(TELEGRAM_ID, "muda valor para 350");

        ArgumentCaptor<PendingTelegramTransaction> pendingCaptor =
                ArgumentCaptor.forClass(PendingTelegramTransaction.class);
        verify(telegramPendingConfirmationService).savePending(
                eq(TELEGRAM_ID),
                pendingCaptor.capture()
        );

        PendingTelegramTransaction updated = pendingCaptor.getValue();
        assertThat(updated.amount()).isNull();
        assertThat(updated.monthlyAmount()).isEqualByComparingTo("350");
        assertThat(result).contains("Valor total:</b> R$ 3.500,00");
        assertThat(result).contains("Valor da parcela:</b> R$ 350,00");
    }

    @Test
    @DisplayName("deve preservar valor mensal quando edicao direta de numero for ambigua")
    void shouldPreserveMonthlyAmountSemanticsWhenBareAmountEditIsAmbiguous() {
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID))
                .thenReturn(existingInstallmentPendingWithMonthlyAmount());

        handler.handleEdit(TELEGRAM_ID, "muda pra 6200");

        ArgumentCaptor<PendingTelegramTransaction> pendingCaptor =
                ArgumentCaptor.forClass(PendingTelegramTransaction.class);
        verify(telegramPendingConfirmationService).savePending(
                eq(TELEGRAM_ID),
                pendingCaptor.capture()
        );

        PendingTelegramTransaction updated = pendingCaptor.getValue();
        assertThat(updated.amount()).isNull();
        assertThat(updated.monthlyAmount()).isEqualByComparingTo("6200");
    }

    @Test
    @DisplayName("deve trocar parcelamento existente mensal para valor total quando edicao for explicita")
    void shouldSwitchMonthlyExistingInstallmentToTotalAmountWhenEditIsExplicit() {
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID))
                .thenReturn(existingInstallmentPendingWithMonthlyAmount());

        String result = handler.handleEdit(TELEGRAM_ID, "muda o valor total para 6200");

        ArgumentCaptor<PendingTelegramTransaction> pendingCaptor =
                ArgumentCaptor.forClass(PendingTelegramTransaction.class);
        verify(telegramPendingConfirmationService).savePending(
                eq(TELEGRAM_ID),
                pendingCaptor.capture()
        );

        PendingTelegramTransaction updated = pendingCaptor.getValue();
        assertThat(updated.amount()).isEqualByComparingTo("6200");
        assertThat(updated.monthlyAmount()).isNull();
        assertThat(result).contains("Valor total:</b> R$ 6.200,00");
        assertThat(result).contains("Valor da parcela:</b> R$ 620,00");
    }

    @Test
    @DisplayName("deve trocar parcelamento existente total para valor mensal quando edicao for explicita")
    void shouldSwitchTotalExistingInstallmentToMonthlyAmountWhenEditIsExplicit() {
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID))
                .thenReturn(existingInstallmentPending());

        String result = handler.handleEdit(TELEGRAM_ID, "muda o valor mensal para 650");

        ArgumentCaptor<PendingTelegramTransaction> pendingCaptor =
                ArgumentCaptor.forClass(PendingTelegramTransaction.class);
        verify(telegramPendingConfirmationService).savePending(
                eq(TELEGRAM_ID),
                pendingCaptor.capture()
        );

        PendingTelegramTransaction updated = pendingCaptor.getValue();
        assertThat(updated.amount()).isNull();
        assertThat(updated.monthlyAmount()).isEqualByComparingTo("650");
        assertThat(result).contains("Valor total:</b> R$ 6.500,00");
        assertThat(result).contains("Valor da parcela:</b> R$ 650,00");
    }

    @Test
    @DisplayName("deve preservar valor total quando edicao de valor for ambigua")
    void shouldPreserveTotalAmountSemanticsWhenAmountEditIsAmbiguous() {
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID))
                .thenReturn(existingInstallmentPending());

        handler.handleEdit(TELEGRAM_ID, "muda valor para 5000");

        ArgumentCaptor<PendingTelegramTransaction> pendingCaptor =
                ArgumentCaptor.forClass(PendingTelegramTransaction.class);
        verify(telegramPendingConfirmationService).savePending(
                eq(TELEGRAM_ID),
                pendingCaptor.capture()
        );

        PendingTelegramTransaction updated = pendingCaptor.getValue();
        assertThat(updated.amount()).isEqualByComparingTo("5000");
        assertThat(updated.monthlyAmount()).isNull();
    }

    @Test
    @DisplayName("deve rejeitar primeira parcela restante maior que o total")
    void shouldRejectFirstRemainingInstallmentGreaterThanTotalInstallments() {
        when(telegramPendingConfirmationService.getPending(TELEGRAM_ID))
                .thenReturn(existingInstallmentPending());

        String result = handler.handleEdit(TELEGRAM_ID, "muda para a parcela 11");

        assertThat(result).contains("o valor informado não é válido");
        verify(telegramPendingConfirmationService, never()).savePending(
                eq(TELEGRAM_ID),
                any()
        );
    }

    private PendingTelegramTransaction existingInstallmentPending() {
        return new PendingTelegramTransaction(
                TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE,
                new BigDecimal("6000.00"),
                null,
                "iPhone",
                LocalDate.of(2026, 6, 15),
                "Eletrônicos",
                "Nubank",
                10,
                6,
                "comprei um iPhone de 6000 em 10x e ja paguei 5 parcelas"
        );
    }

    private PendingTelegramTransaction existingInstallmentPendingWithMonthlyAmount() {
        return new PendingTelegramTransaction(
                TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE,
                null,
                new BigDecimal("300.00"),
                "Financiamento",
                LocalDate.of(2026, 6, 15),
                "Casa",
                "Nubank",
                10,
                6,
                "tenho um financiamento de 300 por mes em 10x e ja paguei 5 parcelas"
        );
    }
}
