package com.financebot.telegrambot.service;

import com.financebot.telegrambot.ai.application.model.AiInterpretation;
import com.financebot.telegrambot.ai.application.port.out.AiInterpretationPort;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramIntentServiceAiFallbackTest {

    @Test
    void usesAiAsPrimaryInterpreterWhenItReturnsAValidResponse() {
        AiInterpretationPort port = message -> Optional.of(new AiInterpretation(
                TelegramIntentType.CREATE_EXPENSE, new BigDecimal("80"), null, null,
                "aluguel", LocalDate.of(2026, 8, 29), "Moradia", null,
                null, null, null, null
        ));
        TelegramIntentService service = service(port);

        ParsedTelegramMessage parsed = service.parse("fiz uma movimentação de 80 referente ao aluguel ontem");

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_EXPENSE);
        assertThat(parsed.amount()).isEqualByComparingTo("80");
        assertThat(parsed.description()).isEqualTo("aluguel");
    }

    @Test
    void aiHasPriorityOverDeterministicParser() {
        AiInterpretationPort port = message -> Optional.of(new AiInterpretation(
                TelegramIntentType.CREATE_INCOME, new BigDecimal("1500"), null, null,
                "salário", LocalDate.of(2026, 8, 30), "Salário", null,
                null, null, null, null
        ));
        TelegramIntentService service = service(port);

        ParsedTelegramMessage parsed = service.parse("gastei 80 no mercado");

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_INCOME);
        assertThat(parsed.amount()).isEqualByComparingTo("1500");
    }

    @Test
    void fallsBackToUnknownWhenAiIsUnavailable() {
        TelegramIntentService service = service(message -> Optional.empty());

        ParsedTelegramMessage parsed = service.parse("uma mensagem ambígua sem dados");

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.UNKNOWN);
    }

    @Test
    void rejectsInvalidAiResponseBeforeItReachesTheFinancialFlow() {
        AiInterpretationPort port = message -> Optional.of(new AiInterpretation(
                TelegramIntentType.CREATE_EXPENSE, new BigDecimal("-80"), null, null,
                "aluguel", null, null, null, null, null, null, null
        ));
        TelegramIntentService service = service(port);

        ParsedTelegramMessage parsed = service.parse("fiz uma movimentação referente ao aluguel ontem");

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.UNKNOWN);
    }

    private TelegramIntentService service(AiInterpretationPort port) {
        return new TelegramIntentService(
                new TelegramDateRangeResolver(),
                new TelegramNaturalLanguageVocabulary(),
                port
        );
    }
}
