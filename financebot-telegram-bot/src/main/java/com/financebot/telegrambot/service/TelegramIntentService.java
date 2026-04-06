package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramIntentService {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+[\\.,]?\\d{0,2})");

    public ParsedTelegramMessage parse(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return unknown(messageText);
        }

        String normalized = normalize(messageText);

        if (isMonthAnalysisQuery(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.QUERY_MONTH_ANALYSIS,
                    null,
                    null,
                    LocalDate.now(),
                    messageText
            );
        }

        if (isMonthExpenseQuery(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.QUERY_MONTH_EXPENSE_TOTAL,
                    null,
                    null,
                    LocalDate.now(),
                    messageText
            );
        }

        if (isMonthIncomeQuery(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.QUERY_MONTH_INCOME_TOTAL,
                    null,
                    null,
                    LocalDate.now(),
                    messageText
            );
        }

        if (looksLikeExpense(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.CREATE_EXPENSE,
                    extractAmount(normalized),
                    extractDescriptionForTransaction(normalized),
                    extractDate(normalized),
                    messageText
            );
        }

        if (looksLikeIncome(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.CREATE_INCOME,
                    extractAmount(normalized),
                    extractDescriptionForTransaction(normalized),
                    extractDate(normalized),
                    messageText
            );
        }

        return unknown(messageText);
    }

    private ParsedTelegramMessage unknown(String messageText) {
        return new ParsedTelegramMessage(
                TelegramIntentType.UNKNOWN,
                null,
                null,
                null,
                messageText
        );
    }

    private boolean isMonthExpenseQuery(String text) {
        return (text.contains("quanto gastei") || text.contains("quanto ja gastei") || text.contains("total gasto"))
                && text.contains("mes");
    }

    private boolean isMonthIncomeQuery(String text) {
        return (text.contains("quanto recebi") || text.contains("total recebido") || text.contains("quanto entrou"))
                && text.contains("mes");
    }

    private boolean isMonthAnalysisQuery(String text) {
        return (text.contains("analise") || text.contains("resumo financeiro"))
                && text.contains("mes");
    }

    private boolean looksLikeExpense(String text) {
        return text.contains("gastei")
                || text.contains("paguei")
                || text.contains("comprei")
                || text.contains("despesa");
    }

    private boolean looksLikeIncome(String text) {
        return text.contains("recebi")
                || text.contains("ganhei")
                || text.contains("entrou")
                || text.contains("entrada");
    }

    private BigDecimal extractAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        String value = matcher.group(1);

        if (value.contains(",") && value.contains(".")) {
            value = value.replace(".", "").replace(",", ".");
        } else if (value.contains(",")) {
            value = value.replace(",", ".");
        }

        return new BigDecimal(value);
    }

    private String extractDescriptionForTransaction(String text) {
        String cleaned = text
                .replaceAll("\\b(gastei|paguei|comprei|despesa|recebi|ganhei|entrou|entrada)\\b", "")
                .replaceAll("\\b(hoje|ontem|amanha|amanhã)\\b", "")
                .replaceAll("(\\d+[\\.,]?\\d{0,2})", "")
                .trim();

        cleaned = cleaned.replaceAll("\\s+", " ");

        if (cleaned.isBlank()) {
            return null;
        }

        return cleaned;
    }

    private LocalDate extractDate(String text) {
        LocalDate today = LocalDate.now();

        if (text.contains("ontem")) {
            return today.minusDays(1);
        }

        if (text.contains("amanha") || text.contains("amanhã")) {
            return today.plusDays(1);
        }

        return today;
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replace("á", "a")
                .replace("à", "a")
                .replace("ã", "a")
                .replace("â", "a")
                .replace("é", "e")
                .replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ô", "o")
                .replace("õ", "o")
                .replace("ú", "u")
                .replaceAll("\\s+", " ")
                .trim();
    }
}