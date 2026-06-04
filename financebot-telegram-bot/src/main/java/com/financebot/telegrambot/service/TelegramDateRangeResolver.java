package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.ParsedDateRange;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;

@Component
public class TelegramDateRangeResolver {

    public ParsedDateRange resolve(String normalizedText) {
        LocalDate today = LocalDate.now();

        if (normalizedText == null || normalizedText.isBlank()) {
            return currentMonthRange(today);
        }

        if (containsLast30Days(normalizedText)) {
            return new ParsedDateRange(today.minusDays(29), today);
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

        if (containsCurrentWeek(normalizedText)) {
            LocalDate startOfCurrentWeek = today.with(DayOfWeek.MONDAY);
            return new ParsedDateRange(startOfCurrentWeek, today);
        }

        if (containsLastMonth(normalizedText)) {
            YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
            return new ParsedDateRange(lastMonth.atDay(1), lastMonth.atEndOfMonth());
        }

        if (containsCurrentMonth(normalizedText)) {
            return currentMonthRange(today);
        }

        return currentMonthRange(today);
    }

    public boolean hasExplicitRangeHint(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }

        return containsLast30Days(normalizedText)
                || containsLast7Days(normalizedText)
                || containsYesterday(normalizedText)
                || containsToday(normalizedText)
                || containsLastWeek(normalizedText)
                || containsCurrentWeek(normalizedText)
                || containsLastMonth(normalizedText)
                || containsCurrentMonth(normalizedText);
    }

    private ParsedDateRange currentMonthRange(LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        return new ParsedDateRange(currentMonth.atDay(1), currentMonth.atEndOfMonth());
    }

    private boolean containsToday(String text) {
        return containsWord(text, "hoje")
                || containsWord(text, "hj");
    }

    private boolean containsYesterday(String text) {
        return containsWord(text, "ontem")
                || containsWord(text, "ont");
    }

    private boolean containsCurrentMonth(String text) {
        return text.contains("esse mes")
                || text.contains("este mes")
                || text.contains("no mes")
                || text.contains("nesse mes")
                || text.contains("neste mes")
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

    private boolean containsCurrentWeek(String text) {
        return text.contains("essa semana")
                || text.contains("esta semana")
                || text.contains("semana atual");
    }

    private boolean containsLast7Days(String text) {
        return text.contains("ultimos 7 dias")
                || text.contains("7 dias");
    }

    private boolean containsLast30Days(String text) {
        return text.contains("ultimos 30 dias")
                || text.contains("30 dias");
    }

    private boolean containsWord(String text, String word) {
        Pattern pattern = Pattern.compile("(^|\\b)" + Pattern.quote(word) + "(\\b|$)");
        return pattern.matcher(text).find();
    }
}