package com.financebot.telegrambot.conversation.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramInstallmentDueDayResolverTest {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    @Test
    void shouldResolveDueDayInCurrentMonthWhenDayHasNotPassed() {
        TelegramInstallmentDueDayResolver resolver = resolverAt("2026-06-03T10:00:00Z");

        Optional<LocalDate> result = resolver.resolve("dia 15");

        assertThat(result).contains(LocalDate.of(2026, 6, 15));
    }

    @Test
    void shouldResolveDueDayInNextMonthWhenDayAlreadyPassed() {
        TelegramInstallmentDueDayResolver resolver = resolverAt("2026-06-20T10:00:00Z");

        Optional<LocalDate> result = resolver.resolve("dia 15");

        assertThat(result).contains(LocalDate.of(2026, 7, 15));
    }

    @Test
    void shouldResolveCurrentDayAsToday() {
        TelegramInstallmentDueDayResolver resolver = resolverAt("2026-06-15T10:00:00Z");

        Optional<LocalDate> result = resolver.resolve("15");

        assertThat(result).contains(LocalDate.of(2026, 6, 15));
    }

    @Test
    void shouldResolveAccentedText() {
        TelegramInstallmentDueDayResolver resolver = resolverAt("2026-06-03T10:00:00Z");

        Optional<LocalDate> result = resolver.resolve("vencimento dia 15");

        assertThat(result).contains(LocalDate.of(2026, 6, 15));
    }

    @Test
    void shouldSkipInvalidMonthAndResolveNextValidMonth() {
        TelegramInstallmentDueDayResolver resolver = resolverAt("2026-02-10T10:00:00Z");

        Optional<LocalDate> result = resolver.resolve("dia 31");

        assertThat(result).contains(LocalDate.of(2026, 3, 31));
    }

    @Test
    void shouldReturnEmptyWhenMessageHasNoDay() {
        TelegramInstallmentDueDayResolver resolver = resolverAt("2026-06-03T10:00:00Z");

        Optional<LocalDate> result = resolver.resolve("na proxima fatura");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenDayIsInvalid() {
        TelegramInstallmentDueDayResolver resolver = resolverAt("2026-06-03T10:00:00Z");

        Optional<LocalDate> result = resolver.resolve("dia 32");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenMessageIsNull() {
        TelegramInstallmentDueDayResolver resolver = resolverAt("2026-06-03T10:00:00Z");

        Optional<LocalDate> result = resolver.resolve(null);

        assertThat(result).isEmpty();
    }

    private TelegramInstallmentDueDayResolver resolverAt(String instant) {
        return new TelegramInstallmentDueDayResolver(
                Clock.fixed(Instant.parse(instant), ZONE_ID)
        );
    }
}
