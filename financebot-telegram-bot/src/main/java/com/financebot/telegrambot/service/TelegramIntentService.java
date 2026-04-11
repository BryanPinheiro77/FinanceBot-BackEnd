package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.ParsedDateRange;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramIntentService {

    private final TelegramDateRangeResolver telegramDateRangeResolver;
    private final TelegramNaturalLanguageVocabulary telegramNaturalLanguageVocabulary;

    public TelegramIntentService(
            TelegramDateRangeResolver telegramDateRangeResolver,
            TelegramNaturalLanguageVocabulary telegramNaturalLanguageVocabulary
    ) {
        this.telegramDateRangeResolver = telegramDateRangeResolver;
        this.telegramNaturalLanguageVocabulary = telegramNaturalLanguageVocabulary;
    }

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+[\\.,]?\\d{0,2})");
    private static final Pattern EXPLICIT_ACCOUNT_PATTERN = Pattern.compile(
            "\\b(?:conta|cartao)\\s+(?:da|do|de)?\\s*([a-zA-Z0-9\\s]+?)(?=\\b(?:hoje|ontem|amanha|esse mes|este mes|mes passado|semana passada|ultimos 7 dias|e|,|\\?|$))"
    );

    private static final Pattern NATURAL_ACCOUNT_PATTERN = Pattern.compile(
            "\\b(?:na|no)\\s+([a-zA-Z][a-zA-Z0-9\\s]{1,30}?)(?=\\b(?:hoje|ontem|amanha|esse mes|este mes|mes passado|semana passada|ultimos 7 dias|e|,|\\?|$))"
    );
    private static final Pattern INSTALLMENT_PATTERN = Pattern.compile("\\b(?:parcelad[oa]\\s+em\\s+|em\\s+)(\\d{1,3})x\\b");

    public ParsedTelegramMessage parse(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return unknown(messageText);
        }

        String normalized = telegramNaturalLanguageVocabulary.normalize(messageText);

        if (isMonthAnalysisQuery(normalized)) {
            ParsedDateRange dateRange = telegramDateRangeResolver.resolve(normalized);

            return new ParsedTelegramMessage(
                    TelegramIntentType.QUERY_MONTH_ANALYSIS,
                    null,
                    null,
                    LocalDate.now(),
                    messageText,
                    null,
                    null,
                    dateRange.startDate(),
                    dateRange.endDate(),
                    null,
                    null
            );
        }

        if (isInstallmentCountQuery(normalized)) {
            ParsedDateRange dateRange = telegramDateRangeResolver.resolve(normalized);

            return new ParsedTelegramMessage(
                    TelegramIntentType.QUERY_INSTALLMENT_COUNT,
                    null,
                    null,
                    LocalDate.now(),
                    messageText,
                    null,
                    null,
                    dateRange.startDate(),
                    dateRange.endDate(),
                    null,
                    null
            );
        }

        if (isActiveInstallmentsQuery(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.QUERY_ACTIVE_INSTALLMENTS,
                    null,
                    null,
                    LocalDate.now(),
                    messageText,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (isInstallmentRemainingQuery(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.QUERY_INSTALLMENT_REMAINING,
                    null,
                    null,
                    LocalDate.now(),
                    messageText,
                    null,
                    null,
                    null,
                    null,
                    null,
                    extractInstallmentQueryTarget(normalized)
            );
        }

        if (isInstallmentEndDateQuery(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.QUERY_INSTALLMENT_END_DATE,
                    null,
                    null,
                    LocalDate.now(),
                    messageText,
                    null,
                    null,
                    null,
                    null,
                    null,
                    extractInstallmentQueryTarget(normalized)
            );
        }

        if (isTransactionTotalQuery(normalized)) {
            ParsedDateRange dateRange = telegramDateRangeResolver.resolve(normalized);

            return new ParsedTelegramMessage(
                    TelegramIntentType.QUERY_TRANSACTION_TOTAL,
                    null,
                    null,
                    LocalDate.now(),
                    messageText,
                    extractCategoryName(normalized),
                    extractAccountName(normalized),
                    dateRange.startDate(),
                    dateRange.endDate(),
                    null,
                    null
            );
        }

        if (looksLikeInstallmentExpense(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.CREATE_INSTALLMENT_EXPENSE,
                    extractAmount(normalized),
                    extractInstallmentDescription(normalized),
                    extractDate(normalized),
                    messageText,
                    extractCategoryName(normalized),
                    extractAccountName(normalized),
                    null,
                    null,
                    extractInstallmentCount(normalized),
                    null
            );
        }

        if (looksLikeExpense(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.CREATE_EXPENSE,
                    extractAmount(normalized),
                    extractDescriptionForTransaction(normalized),
                    extractDate(normalized),
                    messageText,
                    extractCategoryName(normalized),
                    extractAccountName(normalized),
                    null,
                    null,
                    null,
                    null
            );
        }

        if (looksLikeIncome(normalized)) {
            return new ParsedTelegramMessage(
                    TelegramIntentType.CREATE_INCOME,
                    extractAmount(normalized),
                    extractDescriptionForTransaction(normalized),
                    extractDate(normalized),
                    messageText,
                    extractCategoryName(normalized),
                    extractAccountName(normalized),
                    null,
                    null,
                    null,
                    null
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
                messageText,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private boolean isTransactionTotalQuery(String text) {
        return text.contains("quanto gastei")
                || text.contains("quanto recebi")
                || text.contains("quanto entrou")
                || text.contains("total gasto")
                || text.contains("total recebido")
                || text.contains("gastei quanto")
                || text.contains("recebi quanto")
                || text.contains("entrou quanto");
    }

    private boolean isMonthAnalysisQuery(String text) {
        return text.contains("analise")
                || text.contains("análise")
                || text.contains("resumo financeiro");
    }

    private boolean looksLikeExpense(String text) {
        return text.contains("gastei")
                || text.contains("paguei")
                || text.contains("comprei")
                || text.contains("despesa");
    }

    private boolean looksLikeInstallmentExpense(String text) {
        Integer installmentCount = extractInstallmentCount(text);

        return looksLikeExpense(text)
                && installmentCount != null
                && installmentCount >= 2;
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
        String normalized = telegramNaturalLanguageVocabulary.normalize(text);

        String cleaned = normalized
                .replaceAll("\\b(gastei|paguei|comprei|despesa|recebi|ganhei|entrou|entrada|reais|real)\\b", "")
                .replaceAll("\\bpor\\b", "")
                .replaceAll("\\b(hoje|ontem|amanha|mes|esse mes|este mes|mes passado|semana passada|ultimos 7 dias)\\b", "")
                .replaceAll("\\bparcelad[oa]\\s+em\\s+\\d{1,3}x\\b", "")
                .replaceAll("\\bem\\s+\\d{1,3}x\\b", "")
                .replaceAll("(\\d+[\\.,]?\\d{0,2})", "")
                .replaceAll("\\b(?:da conta|do cartao|na conta|no cartao)\\b.*", "")
                .trim();

        String extractedAccount = extractAccountName(normalized);
        if (extractedAccount != null) {
            String normalizedAccount = telegramNaturalLanguageVocabulary.normalize(extractedAccount);
            cleaned = cleaned.replaceAll("\\bna\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
            cleaned = cleaned.replaceAll("\\bno\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
            cleaned = cleaned.replaceAll("\\bconta\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
            cleaned = cleaned.replaceAll("\\bcartao\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
        }

        cleaned = telegramNaturalLanguageVocabulary.stripLeadingDescriptionNoise(
                cleaned.replaceAll("\\s+", " ").trim()
        );

        if (cleaned.isBlank()) {
            return null;
        }

        return cleaned;
    }

    private String extractInstallmentDescription(String text) {
        String description = extractDescriptionForTransaction(text);

        if (description == null || description.isBlank()) {
            return "Despesa parcelada";
        }

        return description;
    }

    private Integer extractInstallmentCount(String text) {
        Matcher matcher = INSTALLMENT_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String extractInstallmentQueryTarget(String text) {
        String normalized = telegramNaturalLanguageVocabulary.normalize(text);

        String cleaned = normalized
                .replaceAll("\\bquando acaba o parcelamento\\b", "")
                .replaceAll("\\bquando termina o parcelamento\\b", "")
                .replaceAll("\\bquando acaba minha parcela\\b", "")
                .replaceAll("\\bquando acaba meu parcelamento\\b", "")
                .replaceAll("\\bquando termina minha parcela\\b", "")
                .replaceAll("\\bquando termina meu parcelamento\\b", "")
                .replaceAll("\\bquantas parcelas faltam\\b", "")
                .replaceAll("\\bquantas faltam\\b", "")
                .replaceAll("\\bfaltam quantas parcelas\\b", "")
                .replaceAll("\\bquantas parcelas restam\\b", "")
                .replaceAll("\\brestam quantas parcelas\\b", "")
                .replaceAll("\\b(parcelamento|parcela|parcelas)\\b", "")
                .replaceAll("\\b(do|da|de|meu|minha|o|a)\\b", "")
                .replaceAll("[\\?]", "")
                .replaceAll("\\s+", " ")
                .trim();

        cleaned = telegramNaturalLanguageVocabulary.stripLeadingQueryTargetNoise(cleaned);
        return cleaned.isBlank() ? null : cleaned;
    }

    public String extractCategoryName(String text) {
        String normalized = telegramNaturalLanguageVocabulary.normalize(text);
        String accountName = extractAccountName(normalized);

        if (accountName != null) {
            String normalizedAccount = telegramNaturalLanguageVocabulary.normalize(accountName);
            normalized = normalized.replaceAll("\\b(?:na|no|conta|cartao)\\s+" + Pattern.quote(normalizedAccount) + "\\b", " ");
            normalized = normalized.replaceAll("\\b" + Pattern.quote(normalizedAccount) + "\\b", " ");
        }

        return telegramNaturalLanguageVocabulary.findCategoryName(normalized);
    }

    public String extractAccountName(String text) {
        String explicitAccount = extractAccountByPattern(EXPLICIT_ACCOUNT_PATTERN, text);
        if (explicitAccount != null) {
            return explicitAccount;
        }

        String naturalAccount = extractAccountByPattern(NATURAL_ACCOUNT_PATTERN, text);
        if (naturalAccount != null) {
            return naturalAccount;
        }

        return null;
    }

    private LocalDate extractDate(String text) {
        LocalDate today = LocalDate.now();

        if (text.contains("ontem")) {
            return today.minusDays(1);
        }

        if (text.contains("amanha")) {
            return today.plusDays(1);
        }

        return today;
    }

    private String extractAccountByPattern(Pattern pattern, String text) {
        String normalized = telegramNaturalLanguageVocabulary.normalize(text);
        Matcher matcher = pattern.matcher(normalized);

        if (!matcher.find()) {
            return null;
        }

        String account = matcher.group(1)
                .replaceAll("\\b(hoje|ontem|amanha|esse mes|este mes|mes passado|semana passada|ultimos 7 dias)\\b", "")
                .trim();

        account = trimTrailingConnector(account);

        if (account.isBlank()) {
            return null;
        }

        return telegramNaturalLanguageVocabulary.resolveAccountName(account);
    }

    private String trimTrailingConnector(String text) {
        return text.replaceAll("\\b(e|em|com)\\b\\s*$", "").trim();
    }

    private boolean isInstallmentCountQuery(String text) {
        return (text.contains("quantas parcelas") || text.contains("quantos parcelamentos"))
                && (text.contains("tenho")
                || text.contains("nesse mes")
                || text.contains("neste mes")
                || text.contains("esse mes")
                || text.contains("este mes")
                || text.contains("mes passado")
                || text.contains("hoje")
                || text.contains("ontem"));
    }

    private boolean isActiveInstallmentsQuery(String text) {
        return text.contains("parcelamentos ativos")
                || text.contains("parcelamento ativo")
                || text.contains("parcelas ativas")
                || text.contains("parcela ativa")
                || text.contains("tenho parcelamentos ativos");
    }

    private boolean isInstallmentRemainingQuery(String text) {
        return text.contains("quantas parcelas faltam")
                || text.contains("quantas faltam")
                || text.contains("faltam quantas parcelas")
                || text.contains("quantas parcelas restam")
                || text.contains("restam quantas parcelas");
    }

    private boolean isInstallmentEndDateQuery(String text) {
        return text.contains("quando acaba o parcelamento")
                || text.contains("quando termina o parcelamento")
                || text.contains("quando acaba minha parcela")
                || text.contains("quando acaba meu parcelamento")
                || text.contains("quando termina minha parcela")
                || text.contains("quando termina meu parcelamento");
    }
}
