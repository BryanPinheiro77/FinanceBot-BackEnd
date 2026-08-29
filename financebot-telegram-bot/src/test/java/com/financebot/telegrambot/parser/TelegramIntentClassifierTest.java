package com.financebot.telegrambot.parser;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramIntentClassifierTest {

    private final TelegramIntentClassifier classifier = new TelegramIntentClassifier(
            text -> text.contains("12x") ? 12 : null,
            text -> text.contains("2000") ? new BigDecimal("2000") : null,
            text -> text.contains("paguei 5") ? 6 : null
    );

    @Test
    void shouldClassifyInstallmentPurchaseCapacity() {
        assertThat(classifier.isInstallmentPurchaseCapacityQuery(
                "consigo comprar algo de 2000 parcelado em 12x"
        )).isTrue();
    }

    @Test
    void shouldRejectInstallmentPurchaseWithoutRequiredValues() {
        assertThat(classifier.isInstallmentPurchaseCapacityQuery(
                "consigo comprar parcelado"
        )).isFalse();
    }

    @Test
    void shouldClassifyExistingInstallmentExpense() {
        assertThat(classifier.looksLikeExistingInstallmentExpense(
                "comprei um celular em 12x e ja paguei 5"
        )).isTrue();
    }

    @Test
    void shouldClassifyIncomeAndExpenseSeparately() {
        assertThat(classifier.looksLikeIncome("recebi 300")).isTrue();
        assertThat(classifier.looksLikeExpense("gastei 300")).isTrue();
    }
}
