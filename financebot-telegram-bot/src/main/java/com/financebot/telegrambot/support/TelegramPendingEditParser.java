package com.financebot.telegrambot.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TelegramPendingEditParser {

    private final TelegramTextNormalizer telegramTextNormalizer;

    public PendingEditResult parse(String messageText) {
        String lower = telegramTextNormalizer.normalize(messageText);

        BigDecimal amount = null;
        String description = null;
        LocalDate date = null;
        String categoryName = null;
        String accountName = null;

        boolean changed = false;

        if (containsAmountEditHint(lower)) {
            BigDecimal newAmount = extractAmountFromEdit(messageText);

            if (newAmount != null && newAmount.compareTo(BigDecimal.ZERO) > 0) {
                amount = newAmount;
                changed = true;
            }
        }

        if (containsDescriptionEditHint(lower)) {
            String newDescription = extractDescriptionFromEdit(messageText);

            if (newDescription != null && !newDescription.isBlank()) {
                description = newDescription;
                changed = true;
            }
        }

        if (containsCategoryEditHint(lower)) {
            String newCategory = extractCategoryFromEdit(messageText);

            if (newCategory != null && !newCategory.isBlank()) {
                categoryName = newCategory;
                changed = true;
            }
        }

        if (containsDateEditHint(lower)) {
            LocalDate newDate = extractDateFromEdit(messageText);

            if (newDate != null) {
                date = newDate;
                changed = true;
            }
        }

        if (containsAccountEditHint(lower)) {
            String newAccount = extractAccountFromEdit(messageText);

            if (newAccount != null && !newAccount.isBlank()) {
                accountName = newAccount;
                changed = true;
            }
        }

        return new PendingEditResult(
                changed,
                amount,
                description,
                date,
                categoryName,
                accountName
        );
    }

    private boolean containsAmountEditHint(String lower) {
        return lower.contains("valor");
    }

    private boolean containsDescriptionEditHint(String lower) {
        return lower.contains("descricao");
    }

    private boolean containsCategoryEditHint(String lower) {
        return lower.contains("categoria");
    }

    private boolean containsDateEditHint(String lower) {
        return lower.contains("data")
                || lower.contains("hoje")
                || lower.contains("ontem")
                || lower.contains("amanha")
                || EDIT_DATE_SLASH_PATTERN.matcher(lower).find()
                || EDIT_DATE_DASH_PATTERN.matcher(lower).find()
                || EDIT_DAY_ONLY_PATTERN.matcher(lower).find();
    }

    private boolean containsAccountEditHint(String lower) {
        return lower.contains("conta");
    }

    private BigDecimal extractAmountFromEdit(String text) {
        try {
            String normalized = text.replace("R$", "").trim();
            String extracted = normalized.replaceAll(".*?(\\d+[\\.,]?\\d{0,2}).*", "$1");
            return parseBrazilianNumber(extracted);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractDescriptionFromEdit(String text) {
        String cleaned = telegramTextNormalizer.normalize(text)
                .replaceFirst(".*?descricao\\s+para\\s+", "")
                .replaceFirst(".*?descricao\\s+pra\\s+", "")
                .replaceFirst(".*?descricao\\s+", "")
                .trim();

        cleaned = trimAtNextEditHint(cleaned);

        return cleaned.isBlank() ? null : cleaned;
    }

    private String extractCategoryFromEdit(String text) {
        String cleaned = telegramTextNormalizer.normalize(text)
                .replaceFirst(".*?categoria\\s+para\\s+", "")
                .replaceFirst(".*?categoria\\s+pra\\s+", "")
                .replaceFirst(".*?categoria\\s+", "")
                .trim();

        cleaned = trimAtNextEditHint(cleaned);

        if (cleaned.isBlank()) {
            return null;
        }

        return capitalizeWords(cleaned);
    }

    private LocalDate extractDateFromEdit(String text) {
        String lower = telegramTextNormalizer.normalize(text);

        if (lower.contains("hoje")) {
            return LocalDate.now();
        }

        if (lower.contains("ontem")) {
            return LocalDate.now().minusDays(1);
        }

        if (lower.contains("amanha")) {
            return LocalDate.now().plusDays(1);
        }

        Matcher slashMatcher = EDIT_DATE_SLASH_PATTERN.matcher(lower);
        if (slashMatcher.find()) {
            try {
                return LocalDate.parse(slashMatcher.group(1), FLEXIBLE_SLASH_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }

        Matcher dashMatcher = EDIT_DATE_DASH_PATTERN.matcher(lower);
        if (dashMatcher.find()) {
            try {
                return LocalDate.parse(dashMatcher.group(1), FLEXIBLE_DASH_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }

        Matcher dayOnlyMatcher = EDIT_DAY_ONLY_PATTERN.matcher(lower);
        if (dayOnlyMatcher.find()) {
            try {
                int day = Integer.parseInt(dayOnlyMatcher.group(1));
                YearMonth currentMonth = YearMonth.now();

                if (day >= 1 && day <= currentMonth.lengthOfMonth()) {
                    return currentMonth.atDay(day);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String extractAccountFromEdit(String text) {
        String cleaned = telegramTextNormalizer.normalize(text)
                .replaceFirst(".*?conta\\s+para\\s+", "")
                .replaceFirst(".*?conta\\s+pra\\s+", "")
                .replaceFirst(".*?usa\\s+a\\s+conta\\s+", "")
                .replaceFirst(".*?coloca\\s+na\\s+conta\\s+", "")
                .replaceFirst(".*?coloca\\s+a\\s+conta\\s+", "")
                .replaceFirst(".*?troca\\s+a\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?troca\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?muda\\s+a\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?muda\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?altera\\s+a\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?altera\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?conta\\s+", "")
                .trim();

        cleaned = trimAtNextEditHint(cleaned);

        cleaned = cleaned.replaceFirst("^o\\s+", "")
                .replaceFirst("^a\\s+", "")
                .trim();

        if (cleaned.isBlank()) {
            return null;
        }

        return capitalizeWords(cleaned);
    }

    private BigDecimal parseBrazilianNumber(String value) {
        String normalized = value.trim()
                .replace("R$", "")
                .replace(" ", "");

        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(",", ".");
        }

        return new BigDecimal(normalized);
    }

    private String capitalizeWords(String text) {
        String[] parts = text.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(" ");
            }

            result.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase());
        }

        return result.toString();
    }

    private String trimAtNextEditHint(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String normalized = telegramTextNormalizer.normalize(text);

        String[] markers = {
                " e o valor",
                " e a valor",
                " e valor",
                " e a descricao",
                " e o descricao",
                " e descricao",
                " e a categoria",
                " e o categoria",
                " e categoria",
                " e a data",
                " e o data",
                " e data",
                " e a conta",
                " e o conta",
                " e conta",
                ", valor",
                ", descricao",
                ", categoria",
                ", data",
                ", conta"
        };

        int cutIndex = normalized.length();

        for (String marker : markers) {
            int index = normalized.indexOf(marker);
            if (index >= 0 && index < cutIndex) {
                cutIndex = index;
            }
        }

        return text.substring(0, cutIndex).trim();
    }

    private static final Pattern EDIT_DATE_SLASH_PATTERN = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");
    private static final Pattern EDIT_DATE_DASH_PATTERN = Pattern.compile("(\\d{1,2}-\\d{1,2}-\\d{4})");
    private static final Pattern EDIT_DAY_ONLY_PATTERN = Pattern.compile("\\bdia\\s+(\\d{1,2})\\b");

    private static final DateTimeFormatter FLEXIBLE_SLASH_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral('/')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('/')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter();

    private static final DateTimeFormatter FLEXIBLE_DASH_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('-')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter();

    public record PendingEditResult(
            boolean changed,
            BigDecimal amount,
            String description,
            LocalDate date,
            String categoryName,
            String accountName
    ) {
    }
}