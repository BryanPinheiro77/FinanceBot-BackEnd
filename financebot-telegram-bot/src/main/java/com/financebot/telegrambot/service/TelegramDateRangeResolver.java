package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.ParsedDateRange;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

@Component
public class TelegramDateRangeResolver {

    public ParsedDateRange resolve(String normalizedText) {
        LocalDate today = LocalDate.now();

        if (normalizedText == null || normalizedText.isBlank()) {
            YearMonth currentMonth = YearMonth.from(today);
            return new ParsedDateRange(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        }

        if (containsLast7Days(normalizedText)) {
            return new ParsedDateRange(today.minusDays(6), today);
        }

        if (containsYesterday(normalizedText)) {
            LocalDate yesterday = today.minusDays(1);
            return new ParsedDateRange(yesterday, yesterday);
        }

        if (containsToday(normalizedText)) {
            return new ParsedDateRange(today, today);
        }

        if (containsLastWeek(normalizedText)) {
            LocalDate startOfCurrentWeek = today.with(DayOfWeek.MONDAY);
            LocalDate startOfLastWeek = startOfCurrentWeek.minusWeeks(1);
            LocalDate endOfLastWeek = startOfLastWeek.plusDays(6);

            return new ParsedDateRange(startOfLastWeek, endOfLastWeek);
        }

        if (containsLastMonth(normalizedText)) {
            YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
            return new ParsedDateRange(lastMonth.atDay(1), lastMonth.atEndOfMonth());
        }

        if (containsCurrentMonth(normalizedText)) {
            YearMonth currentMonth = YearMonth.from(today);
            return new ParsedDateRange(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        }

        YearMonth currentMonth = YearMonth.from(today);
        return new ParsedDateRange(currentMonth.atDay(1), currentMonth.atEndOfMonth());
    }

    private boolean containsToday(String text) {
        return text.contains("hoje");
    }

    private boolean containsYesterday(String text) {
        return text.contains("ontem");
    }

    private boolean containsCurrentMonth(String text) {
        return text.contains("esse mes")
                || text.contains("este mes")
                || text.contains("no mes")
                || text.contains("nesse mes")
                || text.contains("mes atual");
    }

    private boolean containsLastMonth(String text) {
        return text.contains("mes passado")
                || text.contains("ultimo mes")
                || text.contains("mes anterior");
    }

    private boolean containsLastWeek(String text) {
        return text.contains("semana passada")
                || text.contains("ultima semana")
                || text.contains("semana anterior");
    }

    private boolean containsLast7Days(String text) {
        return text.contains("ultimos 7 dias")
                || text.contains("últimos 7 dias")
                || text.contains("7 dias");
    }
}