package com.financebot.telegrambot.conversation.application;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TelegramInstallmentDueDayResolver {

    private static final Pattern DUE_DAY_PATTERN = Pattern.compile(
            "(?:^|\\b)(?:dia\\s+|vencimento\\s+dia\\s+|vence\\s+dia\\s+|todo\\s+dia\\s+)?(\\d{1,2})(?:\\b|$)"
    );

    private final Clock clock;

    public TelegramInstallmentDueDayResolver() {
        this(Clock.systemDefaultZone());
    }

    TelegramInstallmentDueDayResolver(Clock clock) {
        this.clock = clock;
    }

    public Optional<LocalDate> resolve(String messageText) {
        String normalizedMessage = normalize(messageText);
        Matcher matcher = DUE_DAY_PATTERN.matcher(normalizedMessage);

        if (!matcher.find()) {
            return Optional.empty();
        }

        int dueDay = Integer.parseInt(matcher.group(1));

        if (dueDay < 1 || dueDay > 31) {
            return Optional.empty();
        }

        LocalDate today = LocalDate.now(clock);

        for (int monthOffset = 0; monthOffset < 12; monthOffset++) {
            YearMonth yearMonth = YearMonth.from(today).plusMonths(monthOffset);

            if (!isValidDayForMonth(dueDay, yearMonth)) {
                continue;
            }

            LocalDate dueDate = yearMonth.atDay(dueDay);

            if (!dueDate.isBefore(today)) {
                return Optional.of(dueDate);
            }
        }

        return Optional.empty();
    }

    private boolean isValidDayForMonth(int day, YearMonth yearMonth) {
        return day <= yearMonth.lengthOfMonth();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
