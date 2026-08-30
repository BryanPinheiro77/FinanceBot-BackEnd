package com.financebot.telegrambot.service;

import com.financebot.telegrambot.ai.application.model.AiInterpretation;
import com.financebot.telegrambot.ai.application.port.out.AiInterpretationPort;
import com.financebot.telegrambot.ai.application.service.AiInterpretationValidator;
import com.financebot.telegrambot.dto.ParsedDateRange;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.parser.TelegramQueryParser;
import com.financebot.telegrambot.parser.TelegramTransactionParser;
import com.financebot.telegrambot.parser.TelegramIntentClassifier;
    import org.springframework.stereotype.Service;

    import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    @Service
    public class TelegramIntentService {

        private static final String DATE_NOISE_WORDS =
                "ultimos 30 dias|ultimos 7 dias|mes passado|semana passada|essa semana|esta semana|esse mes|este mes|nesse mes|neste mes|mes atual|hoje|hj|ontem|ont|amanha|mes|30 dias";

        private static final String ACCOUNT_BOUNDARY_WORDS =
                "ultimos 30 dias|ultimos 7 dias|mes passado|semana passada|essa semana|esta semana|esse mes|este mes|nesse mes|neste mes|hoje|hj|ontem|ont|amanha|e";

        private static final String TRANSACTION_NOISE_WORDS =
                "pix recebido|me pagaram|gastei|paguei|pago|comprei|compra|despesa|recebi|ganhei|entrou|entrada|caiu|depositaram|deposito|boleto|debito|debitei|reais|real";

        private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\b(\\d+[\\.,]?\\d{0,2})\\b(?!\\s*x\\b)");

        private static final Pattern EXPLICIT_ACCOUNT_PATTERN = Pattern.compile(
                "\\b(?:conta|cartao)\\s+(?:da|do|de)?\\s*([a-zA-Z0-9\\s]+?)(?=\\s*(?:\\b(?:"
                        + ACCOUNT_BOUNDARY_WORDS
                        + ")\\b|,|\\?|$))"
        );

        private static final Pattern NATURAL_ACCOUNT_PATTERN = Pattern.compile(
                "\\b(?:na|no)\\s+([a-zA-Z][a-zA-Z0-9\\s]{1,30}?)(?=\\s*(?:\\b(?:"
                        + ACCOUNT_BOUNDARY_WORDS
                        + ")\\b|,|\\?|$))"
        );

        private static final Pattern INSTALLMENT_PATTERN = Pattern.compile("\\b(?:parcelad[oa]\\s+em\\s+|em\\s+|de\\s+)(\\d{1,3})x\\b");

        private static final String INSTALLMENT_ORDINAL_WORDS =
                "primeir[ao]|segund[ao]|terceir[ao]|quart[ao]|quint[ao]|sext[ao]|setim[ao]|oitav[ao]|non[ao]|decim[ao]";

        private static final Pattern PAID_INSTALLMENTS_PATTERN = Pattern.compile(
                "\\b(?:ja\\s+)?paguei\\s+(\\d{1,3})(?:\\s+parcelas?)?\\b"
        );

        private static final Pattern CURRENT_INSTALLMENT_NUMBER_PATTERN = Pattern.compile(
                "\\b(?:estou|to)\\s+(?:pagando|na|no)\\s+(?:a|o)?\\s*(\\d{1,3})(?:a|o|ª|º)?(?:\\s+parcela)?\\b"
        );

        private static final Pattern CURRENT_INSTALLMENT_WORD_PATTERN = Pattern.compile(
                "\\b(?:estou|to)\\s+(?:pagando|na|no)\\s+(?:a|o)?\\s*("
                        + INSTALLMENT_ORDINAL_WORDS
                        + ")(?:\\s+parcela)?\\b"
        );

        private static final Pattern INSTALLMENT_PURCHASE_AMOUNT_PATTERN = Pattern.compile(
                "\\b(?:de|por|valor(?:\\s+de)?|parcelar)\\s+(\\d+[\\.,]?\\d{0,2})\\b(?!\\s*x\\b)"
        );

        private static final Pattern MONTHLY_INSTALLMENT_AMOUNT_PATTERN = Pattern.compile(
                "\\b(?:de|por|valor(?:\\s+de)?|parcela(?:\\s+de)?|parcelas?\\s+de)?\\s*(\\d+[\\.,]?\\d{0,2})\\s*(?:reais?|rs)?\\s*(?:por\\s+mes|ao\\s+mes|mensais|mensal|/mes|por\\s+parcela|cada\\s+parcela)\\b"
        );

        private static final Pattern TRANSACTION_NOISE_PATTERN = Pattern.compile(
                "\\b(?:" + TRANSACTION_NOISE_WORDS + ")\\b"
        );

        private static final Pattern DATE_NOISE_PATTERN = Pattern.compile(
                "\\b(?:" + DATE_NOISE_WORDS + ")\\b"
        );

        private static final Pattern AMOUNT_WITH_OPTIONAL_PREPOSITION_PATTERN = Pattern.compile(
                "\\b(?:(?:de|por|valor(?:\\s+de)?)\\s+)?\\d+[\\.,]?\\d{0,2}\\b"
        );

        private static final Pattern TRAILING_DESCRIPTION_NOISE_PATTERN = Pattern.compile(
                "\\b(?:de|do|da|dos|das|por|em)\\s*$"
        );

        private final TelegramDateRangeResolver telegramDateRangeResolver;
        private final TelegramNaturalLanguageVocabulary telegramNaturalLanguageVocabulary;
        private final TelegramIntentClassifier telegramIntentClassifier;
        private final TelegramQueryParser telegramQueryParser;
        private final TelegramTransactionParser telegramTransactionParser;
        private final AiInterpretationPort aiInterpretationPort;

        public TelegramIntentService(
                TelegramDateRangeResolver telegramDateRangeResolver,
                TelegramNaturalLanguageVocabulary telegramNaturalLanguageVocabulary
        ) {
            this(telegramDateRangeResolver, telegramNaturalLanguageVocabulary, message -> Optional.empty());
        }

        public TelegramIntentService(
                TelegramDateRangeResolver telegramDateRangeResolver,
                TelegramNaturalLanguageVocabulary telegramNaturalLanguageVocabulary,
                AiInterpretationPort aiInterpretationPort
        ) {
            this.telegramDateRangeResolver = telegramDateRangeResolver;
            this.telegramNaturalLanguageVocabulary = telegramNaturalLanguageVocabulary;
            this.aiInterpretationPort = aiInterpretationPort;
            this.telegramIntentClassifier = new TelegramIntentClassifier(
                    this::extractInstallmentCount,
                    this::extractInstallmentPurchaseAmount,
                    this::extractFirstRemainingInstallmentNumber
            );
            this.telegramQueryParser = new TelegramQueryParser(this, telegramDateRangeResolver);
            this.telegramTransactionParser = new TelegramTransactionParser(this);
        }

        public ParsedTelegramMessage parse(String messageText) {
            if (messageText == null || messageText.isBlank()) {
                return unknown(messageText);
            }

            String normalized = telegramNaturalLanguageVocabulary.normalize(messageText);

            // A IA é a estratégia principal quando habilitada. O parser determinístico
            // continua sendo o fallback para indisponibilidade ou resposta inválida.
            Optional<ParsedTelegramMessage> aiParsed = aiInterpretationPort.interpret(messageText)
                    .filter(AiInterpretationValidator::isValid)
                    .map(interpretation -> toParsedMessage(interpretation, messageText));
            if (aiParsed.isPresent()) {
                return aiParsed.get();
            }

            ParsedTelegramMessage parsed = telegramQueryParser.parse(normalized, messageText);

            if (parsed == null) {
                parsed = telegramTransactionParser.parse(normalized, messageText);
            }

            // Mantém uma rota de compatibilidade durante a migração incremental das regras.
            if (parsed != null) {
                return parsed;
            }

            ParsedTelegramMessage legacyParsed = parseLegacy(messageText);
            if (legacyParsed.intentType() != TelegramIntentType.UNKNOWN) {
                return legacyParsed;
            }

            return legacyParsed;
        }

        private ParsedTelegramMessage toParsedMessage(AiInterpretation interpretation, String originalMessage) {
            return new ParsedTelegramMessage(
                    interpretation.intentType(),
                    interpretation.amount(),
                    interpretation.description(),
                    interpretation.date() != null ? interpretation.date() : LocalDate.now(),
                    originalMessage,
                    interpretation.categoryName(),
                    interpretation.accountName(),
                    interpretation.startDate(),
                    interpretation.endDate(),
                    interpretation.totalInstallments(),
                    interpretation.firstRemainingInstallmentNumber(),
                    null,
                    interpretation.totalAmount(),
                    interpretation.monthlyAmount()
            );
        }

        private ParsedTelegramMessage parseLegacy(String messageText) {
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
                        null,
                        null,
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
                        null,
                        null,
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
                        null,
                        extractInstallmentQueryTarget(normalized),
                        null,
                        null
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
                        null,
                        extractInstallmentQueryTarget(normalized),
                        null,
                        null
                );
            }

            if (isInstallmentPurchaseCapacityQuery(normalized)) {
                BigDecimal totalAmount = extractInstallmentPurchaseAmount(normalized);
                Integer totalInstallments = extractInstallmentCount(normalized);

                if (totalAmount != null && totalInstallments != null && totalInstallments >= 2) {
                    return new ParsedTelegramMessage(
                            TelegramIntentType.QUERY_INSTALLMENT_PURCHASE_CAPACITY,
                            null,
                            null,
                            LocalDate.now(),
                            messageText,
                            null,
                            null,
                            null,
                            null,
                            totalInstallments,
                            null,
                            null,
                            totalAmount,
                            null
                    );
                }
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
                        null,
                        null,
                        null,
                        null
                );
            }

            if (looksLikeExistingInstallmentExpense(normalized)) {
                BigDecimal monthlyAmount = extractMonthlyInstallmentAmount(normalized);
                BigDecimal totalAmount = monthlyAmount != null
                        ? null
                        : extractInstallmentPurchaseAmount(normalized);

                return new ParsedTelegramMessage(
                        TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE,
                        null,
                        extractInstallmentDescription(normalized),
                        extractDate(normalized),
                        messageText,
                        extractCategoryName(normalized),
                        extractAccountName(normalized),
                        null,
                        null,
                        extractInstallmentCount(normalized),
                        extractFirstRemainingInstallmentNumber(normalized),
                        null,
                        totalAmount,
                        monthlyAmount
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
                        null,
                        null,
                        null,
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
                    null,
                    null,
                    null,
                    null
            );
        }

        public boolean isInstallmentPurchaseCapacityQuery(String text) {
            return telegramIntentClassifier.isInstallmentPurchaseCapacityQuery(text);
        }

        public boolean isTransactionTotalQuery(String text) {
            return telegramIntentClassifier.isTransactionTotalQuery(text);
        }

        public boolean isMonthAnalysisQuery(String text) {
            return telegramIntentClassifier.isMonthAnalysisQuery(text);
        }

        public boolean looksLikeExpense(String text) {
            return telegramIntentClassifier.looksLikeExpense(text);
        }

        public boolean looksLikeInstallmentExpense(String text) {
            return telegramIntentClassifier.looksLikeInstallmentExpense(text);
        }

        public boolean looksLikeExistingInstallmentExpense(String text) {
            return telegramIntentClassifier.looksLikeExistingInstallmentExpense(text);
        }

        public boolean looksLikeIncome(String text) {
            return telegramIntentClassifier.looksLikeIncome(text);
        }

        public BigDecimal extractAmount(String text) {
            Matcher matcher = AMOUNT_PATTERN.matcher(text);

            if (!matcher.find()) {
                return null;
            }

            return parseAmount(matcher.group(1));
        }

        public BigDecimal extractInstallmentPurchaseAmount(String text) {
            Matcher matcher = INSTALLMENT_PURCHASE_AMOUNT_PATTERN.matcher(text);

            if (!matcher.find()) {
                return null;
            }

            return parseAmount(matcher.group(1));
        }

        public String extractDescriptionForTransaction(String text) {
            String normalized = telegramNaturalLanguageVocabulary.normalize(text);
            String cleaned = normalized;

            String extractedAccount = extractAccountName(normalized);
            if (extractedAccount != null) {
                String normalizedAccount = telegramNaturalLanguageVocabulary.normalize(extractedAccount);
                cleaned = cleaned.replaceAll("\\bna\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
                cleaned = cleaned.replaceAll("\\bno\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
                cleaned = cleaned.replaceAll("\\bconta\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
                cleaned = cleaned.replaceAll("\\bcartao\\s+" + Pattern.quote(normalizedAccount) + "\\b", "");
            }

            cleaned = cleaned
                    .replaceAll("\\b(?:ja\\s+)?paguei\\s+\\d{1,3}(?:\\s+parcelas?)?\\b", "")
                    .replaceAll("\\b(?:estou|to)\\s+(?:pagando|na|no)\\s+(?:a|o)?\\s*(?:"
                            + INSTALLMENT_ORDINAL_WORDS
                            + "|\\d{1,3}(?:a|o|ª|º)?)(?:\\s+parcela)?\\b", "")
                    .replaceAll("\\b(?:tenho|financiamento|parcelamento)\\b", "");

            cleaned = TRANSACTION_NOISE_PATTERN.matcher(cleaned).replaceAll("");
            cleaned = DATE_NOISE_PATTERN.matcher(cleaned).replaceAll("");

            cleaned = cleaned
                    .replaceAll("\\bparcelad[oa]\\s+em\\s+\\d{1,3}x\\b", "")
                    .replaceAll("\\bem\\s+\\d{1,3}x\\b", "");

            cleaned = AMOUNT_WITH_OPTIONAL_PREPOSITION_PATTERN.matcher(cleaned).replaceAll("");

            cleaned = cleaned
                    .replaceAll("\\b(?:da conta|do cartao|na conta|no cartao)\\b.*", "")
                    .replaceAll("\\b(?:ja|parcelas?|e|um|uma|o|a|os|as)\\b", "")
                    .replaceAll("\\s+", " ")
                    .trim();

            cleaned = telegramNaturalLanguageVocabulary.stripLeadingDescriptionNoise(cleaned);
            cleaned = TRAILING_DESCRIPTION_NOISE_PATTERN.matcher(cleaned).replaceAll("").trim();

            if (cleaned.isBlank()) {
                return null;
            }

            return cleaned;
        }

        public String extractInstallmentDescription(String text) {
            String description = extractDescriptionForTransaction(text);

            if (description == null || description.isBlank()) {
                return "Despesa parcelada";
            }

            return description;
        }

        public Integer extractInstallmentCount(String text) {
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

        public Integer extractFirstRemainingInstallmentNumber(String text) {
            Integer currentInstallmentNumber = extractCurrentInstallmentNumber(text);

            if (currentInstallmentNumber != null) {
                return currentInstallmentNumber;
            }

            Integer paidInstallments = extractPaidInstallments(text);

            if (paidInstallments == null) {
                return null;
            }

            return paidInstallments + 1;
        }

        private Integer extractPaidInstallments(String text) {
            Matcher matcher = PAID_INSTALLMENTS_PATTERN.matcher(text);

            if (!matcher.find()) {
                return null;
            }

            try {
                return Integer.valueOf(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private Integer extractCurrentInstallmentNumber(String text) {
            Matcher numberMatcher = CURRENT_INSTALLMENT_NUMBER_PATTERN.matcher(text);

            if (numberMatcher.find()) {
                try {
                    return Integer.valueOf(numberMatcher.group(1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            Matcher wordMatcher = CURRENT_INSTALLMENT_WORD_PATTERN.matcher(text);

            if (!wordMatcher.find()) {
                return null;
            }

            return parseInstallmentOrdinal(wordMatcher.group(1));
        }

        private Integer parseInstallmentOrdinal(String value) {
            return switch (value) {
                case "primeira", "primeiro" -> 1;
                case "segunda", "segundo" -> 2;
                case "terceira", "terceiro" -> 3;
                case "quarta", "quarto" -> 4;
                case "quinta", "quinto" -> 5;
                case "sexta", "sexto" -> 6;
                case "setima", "setimo" -> 7;
                case "oitava", "oitavo" -> 8;
                case "nona", "nono" -> 9;
                case "decima", "decimo" -> 10;
                default -> null;
            };
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
                    .replaceAll("\\bquando termina a parcela\\b", "")
                    .replaceAll("\\bquando acaba a parcela\\b", "")
                    .replaceAll("\\bquando termina parcela\\b", "")
                    .replaceAll("\\bquando acaba parcela\\b", "")
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

        public LocalDate extractDate(String text) {
            LocalDate today = LocalDate.now();

            if (containsWord(text, "ontem") || containsWord(text, "ont")) {
                return today.minusDays(1);
            }

            if (containsWord(text, "amanha")) {
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

            String account = DATE_NOISE_PATTERN.matcher(matcher.group(1)).replaceAll("").trim();
            account = trimTrailingConnector(account);

            if (account.isBlank()) {
                return null;
            }

            return telegramNaturalLanguageVocabulary.resolveAccountName(account);
        }

        private String trimTrailingConnector(String text) {
            return text.replaceAll("\\b(e|em|com)\\b\\s*$", "").trim();
        }

        private boolean containsWord(String text, String word) {
            Pattern pattern = Pattern.compile("(^|\\b)" + Pattern.quote(word) + "(\\b|$)");
            return pattern.matcher(text).find();
        }

        public boolean isInstallmentCountQuery(String text) {
            return telegramIntentClassifier.isInstallmentCountQuery(text);
        }

        public boolean isActiveInstallmentsQuery(String text) {
            return telegramIntentClassifier.isActiveInstallmentsQuery(text);
        }

        public boolean isInstallmentRemainingQuery(String text) {
            return telegramIntentClassifier.isInstallmentRemainingQuery(text);
        }

        public boolean isInstallmentEndDateQuery(String text) {
            return telegramIntentClassifier.isInstallmentEndDateQuery(text);
        }

        public BigDecimal extractMonthlyInstallmentAmount(String text) {
            Matcher matcher = MONTHLY_INSTALLMENT_AMOUNT_PATTERN.matcher(text);

            if (!matcher.find()) {
                return null;
            }

            return parseAmount(matcher.group(1));
        }

        private BigDecimal parseAmount(String value) {
            if (value.contains(",") && value.contains(".")) {
                value = value.replace(".", "").replace(",", ".");
            } else if (value.contains(",")) {
                value = value.replace(",", ".");
            }

            return new BigDecimal(value);
        }
    }
