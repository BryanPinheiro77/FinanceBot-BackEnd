package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.ParsedDateRange;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramIntentService {

    private final TelegramDateRangeResolver telegramDateRangeResolver;

    public TelegramIntentService(TelegramDateRangeResolver telegramDateRangeResolver) {
        this.telegramDateRangeResolver = telegramDateRangeResolver;
    }

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+[\\.,]?\\d{0,2})");
    private static final Pattern EXPLICIT_ACCOUNT_PATTERN = Pattern.compile(
            "\\b(?:conta|cartao)\\s+(?:da|do|de)?\\s*([a-zA-Z0-9\\s]+?)(?=\\b(?:hoje|ontem|amanha|esse mes|este mes|mes passado|semana passada|ultimos 7 dias|e|,|\\?|$))"
    );

    private static final Pattern NATURAL_ACCOUNT_PATTERN = Pattern.compile(
            "\\b(?:na|no)\\s+([a-zA-Z][a-zA-Z0-9\\s]{1,30}?)(?=\\b(?:hoje|ontem|amanha|esse mes|este mes|mes passado|semana passada|ultimos 7 dias|e|,|\\?|$))"
    );
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
            "\\b(mercado|supermercado|gasolina|combustivel|farmacia|uber|ifood|salario|freela|alimentacao|outros|moradia|transporte|saude)\\b"
    );

    public ParsedTelegramMessage parse(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return unknown(messageText);
        }

        String normalized = normalize(messageText);

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
                    dateRange.endDate()
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
                    dateRange.endDate()
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
                    null
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
                    dateRange.endDate()
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
                .replaceAll("\\b(gastei|paguei|comprei|despesa|recebi|ganhei|entrou|entrada|reais|real)\\b", "")
                .replaceAll("\\b(hoje|ontem|amanha|mes|esse mes|este mes|mes passado|semana passada|ultimos 7 dias)\\b", "")
                .replaceAll("(\\d+[\\.,]?\\d{0,2})", "")
                .replaceAll("\\b(?:da conta|do cartao|na conta|no cartao)\\b.*", "")
                .trim();

        String extractedAccount = extractAccountName(text);
        if (extractedAccount != null) {
            String normalizedAccount = normalize(extractedAccount);
            cleaned = cleaned.replaceAll("\\bna\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
            cleaned = cleaned.replaceAll("\\bno\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
            cleaned = cleaned.replaceAll("\\bconta\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
            cleaned = cleaned.replaceAll("\\bcartao\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
        }

        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        if (cleaned.isBlank()) {
            return null;
        }

        return cleaned;
    }

    private String extractCategoryName(String text) {
        Matcher matcher = CATEGORY_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        String value = matcher.group(1);

        return switch (value) {
            case "mercado", "supermercado" -> "Mercado";
            case "gasolina", "combustivel" -> "Combustível";
            case "farmacia", "saude" -> "Saúde";
            case "uber", "transporte" -> "Transporte";
            case "ifood", "alimentacao" -> "Alimentação";
            case "salario" -> "Salário";
            case "freela" -> "Freelance";
            case "moradia" -> "Moradia";
            case "outros" -> "Outros";
            default -> null;
        };
    }

    private String extractAccountName(String text) {
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
                .replace("ç", "c")
                .replaceAll("\\s+", " ")
                .trim();
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

    private String extractAccountByPattern(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);

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

        if (!looksLikeKnownAccount(account)) {
            return null;
        }

        return capitalizeWords(account);
    }

    private String trimTrailingConnector(String text) {
        return text.replaceAll("\\b(e|em|com)\\b\\s*$", "").trim();
    }

    private boolean looksLikeKnownAccount(String account) {
        String normalized = normalize(account);

        return normalized.contains("nubank")
                || normalized.contains("inter")
                || normalized.contains("picpay")
                || normalized.contains("mercado pago")
                || normalized.contains("itau")
                || normalized.contains("bradesco")
                || normalized.contains("caixa")
                || normalized.contains("santander")
                || normalized.contains("banco do brasil")
                || normalized.contains("bb");
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
}