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
    @DisplayName("deve interpretar parcelamento existente com parcelas ja pagas")
    void shouldParseExistingInstallmentWithPaidInstallments() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "comprei um iPhone de 6000 em 10x e já paguei 5 parcelas"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE);
        assertThat(parsed.totalAmount()).isEqualByComparingTo(new BigDecimal("6000"));
        assertThat(parsed.description()).isEqualTo("iphone");
        assertThat(parsed.totalInstallments()).isEqualTo(10);
        assertThat(parsed.firstRemainingInstallmentNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("deve interpretar parcelamento existente com valor mensal")
    void shouldParseExistingInstallmentWithMonthlyAmount() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "tenho um financiamento de 300 por mes em 10x e ja paguei 5 parcelas"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE);
        assertThat(parsed.totalAmount()).isNull();
        assertThat(parsed.monthlyAmount()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(parsed.totalInstallments()).isEqualTo(10);
        assertThat(parsed.firstRemainingInstallmentNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("deve interpretar parcelamento existente com valor mensal escrito em reais")
    void shouldParseExistingInstallmentWithMonthlyAmountInReais() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "tenho um financiamento de 300 reais por mes em 10x e ja paguei 5 parcelas"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE);
        assertThat(parsed.totalAmount()).isNull();
        assertThat(parsed.monthlyAmount()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(parsed.totalInstallments()).isEqualTo(10);
        assertThat(parsed.firstRemainingInstallmentNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("deve interpretar parcelamento existente com parcelas pagas sem palavra parcelas")
    void shouldParseExistingInstallmentWithPaidInstallmentsWithoutInstallmentWord() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "comprei um iphone parcelado em 10x e ja paguei 5"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE);
        assertThat(parsed.amount()).isNull();
        assertThat(parsed.totalAmount()).isNull();
        assertThat(parsed.description()).isEqualTo("iphone");
        assertThat(parsed.totalInstallments()).isEqualTo(10);
        assertThat(parsed.firstRemainingInstallmentNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("deve interpretar parcelamento existente com parcela atual por extenso")
    void shouldParseExistingInstallmentWithCurrentInstallmentInWords() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "comprei um iPhone de 6000 em 10x e estou pagando a sexta parcela"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE);
        assertThat(parsed.totalAmount()).isEqualByComparingTo(new BigDecimal("6000"));
        assertThat(parsed.totalInstallments()).isEqualTo(10);
        assertThat(parsed.firstRemainingInstallmentNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("deve interpretar parcelamento existente com parcela atual numerica sem palavra parcela")
    void shouldParseExistingInstallmentWithCurrentInstallmentNumberWithoutInstallmentWord() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "comprei um celular de 3000 parcelado em 10x e estou pagando a 6"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE);
        assertThat(parsed.totalAmount()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(parsed.description()).isEqualTo("celular");
        assertThat(parsed.totalInstallments()).isEqualTo(10);
        assertThat(parsed.firstRemainingInstallmentNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("deve interpretar parcelamento existente com quinta parcela atual sem palavra parcela")
    void shouldParseExistingInstallmentWithFifthCurrentInstallmentWithoutInstallmentWord() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "comprei um celular de 3000 parcelado em 10x e estou pagando a 5"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE);
        assertThat(parsed.totalAmount()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(parsed.description()).isEqualTo("celular");
        assertThat(parsed.totalInstallments()).isEqualTo(10);
        assertThat(parsed.firstRemainingInstallmentNumber()).isEqualTo(5);
    }

    @Test
    @DisplayName("nao deve usar parcelas pagas como valor do parcelamento existente")
    void shouldNotUsePaidInstallmentsAsExistingInstallmentAmount() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "tenho um parcelamento de 10x e ja paguei 5"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE);
        assertThat(parsed.amount()).isNull();
        assertThat(parsed.totalAmount()).isNull();
        assertThat(parsed.totalInstallments()).isEqualTo(10);
        assertThat(parsed.firstRemainingInstallmentNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("deve interpretar parcelamento existente com parcela atual numerica")
    void shouldParseExistingInstallmentWithCurrentInstallmentNumber() {
        ParsedTelegramMessage parsed = telegramIntentService.parse(
                "tenho um financiamento de 3000 em 10x e estou na 6ª parcela"
        );

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE);
        assertThat(parsed.totalAmount()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(parsed.description()).isEqualTo("Despesa parcelada");
        assertThat(parsed.totalInstallments()).isEqualTo(10);
        assertThat(parsed.firstRemainingInstallmentNumber()).isEqualTo(6);
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
