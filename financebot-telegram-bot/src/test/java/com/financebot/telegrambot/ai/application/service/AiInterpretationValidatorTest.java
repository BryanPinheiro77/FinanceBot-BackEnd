package com.financebot.telegrambot.ai.application.service;

import com.financebot.telegrambot.ai.application.model.AiInterpretation;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AiInterpretationValidatorTest {

    @Test
    void acceptsValidSimpleExpense() {
        AiInterpretation interpretation = new AiInterpretation(
                TelegramIntentType.CREATE_EXPENSE, new BigDecimal("80"), null, null,
                "mercado", null, "Mercado", null, null, null, null, null
        );

        assertThat(AiInterpretationValidator.isValid(interpretation)).isTrue();
    }

    @Test
    void rejectsInstallmentWithBothTotalAndMonthlyAmounts() {
        AiInterpretation interpretation = new AiInterpretation(
                TelegramIntentType.CREATE_INSTALLMENT_EXPENSE, null,
                new BigDecimal("1200"), new BigDecimal("100"), "notebook", null,
                null, null, 12, null, null, null
        );

        assertThat(AiInterpretationValidator.isValid(interpretation)).isFalse();
    }

    @Test
    void rejectsUnknownIntentAndInvalidInstallmentNumber() {
        AiInterpretation unknown = new AiInterpretation(
                TelegramIntentType.UNKNOWN, null, null, null, null, null,
                null, null, null, null, null, null
        );
        AiInterpretation invalidNumber = new AiInterpretation(
                TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE, null,
                new BigDecimal("1000"), null, "compra", null, null, null,
                10, 11, null, null
        );

        assertThat(AiInterpretationValidator.isValid(unknown)).isFalse();
        assertThat(AiInterpretationValidator.isValid(invalidNumber)).isFalse();
    }
}
