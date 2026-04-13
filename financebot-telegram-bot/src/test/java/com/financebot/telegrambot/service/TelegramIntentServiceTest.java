package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramIntentServiceTest {

    private TelegramIntentService telegramIntentService;

    @BeforeEach
    void setUp() {
        telegramIntentService = new TelegramIntentService(
                new TelegramDateRangeResolver(),
                new TelegramNaturalLanguageVocabulary()
        );
    }

    @Test
    @DisplayName("deve interpretar compra parcelada com valor e parcelas explicitos")
    void shouldParseInstallmentPurchaseCapacityQuery() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "consigo comprar algo de 2000 parcelado em 12x?"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY);
        assertThat(parsed.totalAmount()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(parsed.totalInstallments()).isEqualTo(12);
    }

    @Test
    @DisplayName("deve interpretar compra parcelada com formulacao de compra em x vezes")
    void shouldParseInstallmentPurchaseCapacityFromPurchaseQuestion() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "consigo fazer uma compra de 3000 em 10x?"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY);
        assertThat(parsed.totalAmount()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(parsed.totalInstallments()).isEqualTo(10);
    }

    @Test
    @DisplayName("deve interpretar compra parcelada com pergunta sobre caber no orcamento")
    void shouldParseInstallmentPurchaseCapacityFromBudgetQuestion() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "se eu parcelar 2400 em 12x, cabe no meu orçamento?"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY);
        assertThat(parsed.totalAmount()).isEqualByComparingTo(new BigDecimal("2400"));
        assertThat(parsed.totalInstallments()).isEqualTo(12);
    }

    @Test
    @DisplayName("nao deve interpretar analise parcelada sem valor explicito")
    void shouldNotParseInstallmentPurchaseCapacityWithoutAmount() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "consigo comprar parcelado em 12x?"
        );

        assertThat(parsed.intentType()).isNotEqualTo(TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY);
    }

    @Test
    @DisplayName("nao deve interpretar analise parcelada sem parcelas explicitas")
    void shouldNotParseInstallmentPurchaseCapacityWithoutInstallments() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "consigo comprar algo de 2000?"
        );

        assertThat(parsed.intentType()).isNotEqualTo(TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY);
    }

    @Test
    @DisplayName("nao deve interpretar analise parcelada com apenas uma parcela")
    void shouldNotParseInstallmentPurchaseCapacityWithSingleInstallment() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "consigo comprar algo de 2000 em 1x?"
        );

        assertThat(parsed.intentType()).isNotEqualTo(TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY);
    }

    @Test
    @DisplayName("nao deve confundir criacao parcelada com analise de viabilidade")
    void shouldNotConfuseInstallmentCreationWithPurchaseCapacityAnalysis() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "gastei 2400 parcelado em 12x"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_INSTALLMENT_EXPENSE);
    }

    @Test
    @DisplayName("nao deve interpretar pergunta ambigua fora do escopo")
    void shouldNotParseOutOfScopeAmbiguousInstallmentQuestion() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "consigo fazer mais uma parcela de 10x?"
        );

        assertThat(parsed.intentType()).isNotEqualTo(TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY);
    }
}
