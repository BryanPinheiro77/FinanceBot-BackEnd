package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramReminderIntentEvalTest {

    private final TelegramIntentService service = new TelegramIntentService(
            new TelegramDateRangeResolver(),
            new TelegramNaturalLanguageVocabulary()
    );

    @ParameterizedTest(name = "{0}")
    @MethodSource("reminderMessages")
    void interpretsNaturalLanguageReminder(String message, LocalDate expectedDate, String expectedDescription) {
        ParsedTelegramMessage parsed = service.parse(message);

        assertThat(parsed.intentType()).isEqualTo(TelegramIntentType.CREATE_REMINDER);
        assertThat(parsed.date()).isEqualTo(expectedDate);
        assertThat(parsed.description()).isEqualTo(expectedDescription);
    }

    private static Stream<Arguments> reminderMessages() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of("me lembre dia 10 de pagar o aluguel", nextDay(today, 10), "pagar o aluguel"),
                Arguments.of("me lembra hoje de conferir o saldo", today, "conferir o saldo"),
                Arguments.of("me lembre amanhã de pagar a luz", today.plusDays(1), "pagar a luz"),
                Arguments.of("me lembrar em 2030-05-20 de renovar o seguro", LocalDate.of(2030, 5, 20), "renovar o seguro"),
                Arguments.of("lembrete 20/05/2030 para renovar o seguro", LocalDate.of(2030, 5, 20), "renovar o seguro"),
                Arguments.of("crie um lembrete para o dia 25 de enviar a fatura", nextDay(today, 25), "enviar a fatura"),
                Arguments.of("por favor, me lembre dia 1 de revisar o orçamento", nextDay(today, 1), "revisar o orcamento"),
                Arguments.of("lembre me amanhã de comprar remédio", today.plusDays(1), "comprar remedio"),
                Arguments.of("lembrar me dia 31 de fechar as contas", nextDay(today, 31), "fechar as contas"),
                Arguments.of("lembrete para hoje: separar os comprovantes", today, "separar os comprovantes"),
                Arguments.of("me avise dois dias antes da internet vencer", null, "internet")
        );
    }

    private static LocalDate nextDay(LocalDate today, int day) {
        YearMonth month = YearMonth.from(today);
        while (true) {
            if (day <= month.lengthOfMonth()) {
                LocalDate candidate = month.atDay(day);
                if (!candidate.isBefore(today)) {
                    return candidate;
                }
            }
            month = month.plusMonths(1);
        }
    }
}
