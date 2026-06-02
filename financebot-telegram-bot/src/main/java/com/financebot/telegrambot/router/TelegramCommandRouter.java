package com.financebot.telegrambot.router;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.dto.request.InstallmentPurchaseCapacityRequest;
import com.financebot.telegrambot.dto.request.TelegramInstallmentCountRequest;
import com.financebot.telegrambot.dto.request.TelegramTransactionSummaryRequest;
import com.financebot.telegrambot.dto.request.CreateInstallmentTransactionFromTelegramRequest;
import com.financebot.telegrambot.dto.request.CreateTransactionFromTelegramRequest;
import com.financebot.telegrambot.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.telegrambot.dto.response.MonthlyAmountSummaryResponse;
import com.financebot.telegrambot.dto.response.TelegramActiveInstallmentSummaryResponse;
import com.financebot.telegrambot.dto.response.TelegramActiveInstallmentsResponse;
import com.financebot.telegrambot.dto.response.TelegramDefaultAccountResponse;
import com.financebot.telegrambot.dto.response.TelegramInstallmentCountResponse;
import com.financebot.telegrambot.dto.response.TelegramTransactionSummaryResponse;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.mapper.PendingTelegramTransactionMapper;
import com.financebot.telegrambot.service.TelegramIntentService;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.service.TelegramQueryContextService;
import com.financebot.telegrambot.handler.TelegramBasicCommandHandler;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import com.financebot.telegrambot.support.TelegramCommandMatcher;
import com.financebot.telegrambot.support.TelegramTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

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
public class TelegramCommandRouter {

    private final FinanceBotApiClient financeBotApiClient;
    private final TelegramIntentService telegramIntentService;
    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramPendingQueryService telegramPendingQueryService;
    private final TelegramQueryContextService telegramQueryContextService;
    private final TelegramMessageFormatter telegramMessageFormatter;
    private final PendingTelegramTransactionMapper pendingTelegramTransactionMapper;
    private final TelegramCommandMatcher telegramCommandMatcher;
    private final TelegramBotErrorMapper telegramBotErrorMapper;
    private final TelegramTextNormalizer telegramTextNormalizer;
    private final TelegramBasicCommandHandler telegramBasicCommandHandler;

    public String route(
            String messageText,
            Long telegramId,
            String telegramUsername,
            String telegramFirstName
    ) {
        if (messageText == null || messageText.isBlank()) {
            return "Não consegui entender sua mensagem. Tente /start, /iniciar, /help ou /ajuda.";
        }

        String normalizedMessage = messageText.trim();

        if (telegramCommandMatcher.startsWithCommand(normalizedMessage, "/start", "/iniciar")) {
            return telegramBasicCommandHandler.handleStart(telegramFirstName, telegramUsername);
        }

        if (telegramCommandMatcher.startsWithCommand(normalizedMessage, "/help", "/ajuda")) {
            return telegramBasicCommandHandler.handleHelp();
        }

        if (telegramCommandMatcher.startsWithCommand(normalizedMessage, "/connect", "/conectar")) {
            return telegramBasicCommandHandler.handleConnect(normalizedMessage, telegramId, telegramUsername);
        }

        if (telegramCommandMatcher.startsWithCommand(normalizedMessage, "/me", "/perfil")) {
            return telegramBasicCommandHandler.handleMe(telegramId);
        }

        if (telegramCommandMatcher.startsWithCommand(normalizedMessage, "/disconnect", "/desconectar")) {
            return telegramBasicCommandHandler.handleDisconnect(telegramId);
        }

        if (telegramCommandMatcher.startsWithCommand(normalizedMessage, "/setincome", "/definirrenda")) {
            return telegramBasicCommandHandler.handleSetIncome(normalizedMessage, telegramId);
        }

        if (telegramCommandMatcher.startsWithCommand(normalizedMessage, "/analysis", "/analise")) {
            return telegramBasicCommandHandler.handleAnalysis(telegramId);
        }

        if (telegramCommandMatcher.startsWithCommand(normalizedMessage, "/status", "/resumo")) {
            return telegramBasicCommandHandler.handleStatus(telegramId);
        }

        if (telegramCommandMatcher.containsGreeting(normalizedMessage)) {
            return telegramBasicCommandHandler.handleGreeting(telegramFirstName, telegramUsername);
        }

        if (telegramCommandMatcher.looksLikeConnectionIntent(normalizedMessage)) {
            return telegramBasicCommandHandler.handleConnectionIntent();
        }

        if (telegramCommandMatcher.isConfirmationMessage(normalizedMessage)) {
            return handleConfirmation(telegramId);
        }

        if (telegramCommandMatcher.isCancellationMessage(normalizedMessage)) {
            return handleCancellation(telegramId);
        }

        if (telegramPendingConfirmationService.hasPending(telegramId)
                && telegramCommandMatcher.looksLikeEditMessage(normalizedMessage)) {
            return handlePendingEdit(telegramId, normalizedMessage);
        }

        ParsedTelegramMessage pendingQuery = telegramPendingQueryService.getPending(telegramId);
        if (pendingQuery != null
                && pendingQuery.intentType() != null
                && pendingQuery.intentType().name().startsWith("QUERY_INSTALLMENT_")
                && !normalizedMessage.startsWith("/")) {
            return handlePendingInstallmentQuerySelection(telegramId, normalizedMessage, pendingQuery);
        }

        ParsedTelegramMessage parsedMessage = telegramIntentService.parse(normalizedMessage);
        parsedMessage = telegramQueryContextService.applyQueryContext(telegramId, normalizedMessage, parsedMessage);

        if (parsedMessage.intentType() != null && parsedMessage.intentType().name().startsWith("QUERY_")) {
            return handleNaturalLanguageQuery(parsedMessage, telegramId);
        }

        if (parsedMessage.intentType() == TelegramIntentType.CREATE_EXPENSE
                || parsedMessage.intentType() == TelegramIntentType.CREATE_INSTALLMENT_EXPENSE
                || parsedMessage.intentType() == TelegramIntentType.CREATE_INCOME) {
            return handleNaturalLanguageTransactionPreview(telegramId, parsedMessage);
        }

        return """
            Não reconheci sua mensagem.
            
            Você pode usar comandos:
            /start ou /iniciar
            /help ou /ajuda
            /connect ou /conectar CODIGO
            /me ou /perfil
            /status ou /resumo
            /analysis ou /analise
            /setincome ou /definirrenda VALOR
            /disconnect ou /desconectar
            
            Ou pode escrever naturalmente, por exemplo:
            - gastei 50 no mercado
            - recebi 1200 de salário
            - quanto gastei esse mês?
            - me dá a análise desse mês
            """;
    }

    private String handleNaturalLanguageTransactionPreview(Long telegramId, ParsedTelegramMessage parsedMessage) {
        if (parsedMessage.amount() == null && parsedMessage.totalAmount() == null) {
            return """
        Entendi a intenção, mas não consegui identificar o valor.
        
        Exemplos:
        - gastei 50 no mercado
        - paguei 120 de gasolina
        - recebi 1500 de salário
        """;
        }

        PendingTelegramTransaction pendingTransaction =
                pendingTelegramTransactionMapper.fromParsedMessage(parsedMessage);

        ResolvedPreviewAccount resolvedAccount = resolvePreviewAccount(pendingTransaction, telegramId);
        pendingTransaction = withResolvedAccountName(pendingTransaction, resolvedAccount.persistedName());

        if (pendingTransaction.intentType() == TelegramIntentType.CREATE_INSTALLMENT_EXPENSE) {
            if (pendingTransaction.totalInstallments() == null || pendingTransaction.totalInstallments() < 2) {
                return """
            Entendi a intenção de parcelamento, mas não consegui identificar uma quantidade válida de parcelas.
            
            Exemplos:
            - gastei 1200 parcelado em 10x
            - comprei um celular por 2400 em 12x
            - gastei 300 no inter parcelado em 3x
            """;
            }

            telegramPendingConfirmationService.savePending(telegramId, pendingTransaction);

            return telegramMessageFormatter.formatInstallmentTransactionPreview(
                    pendingTransaction,
                    resolvedAccount.displayName()
            );
        }

        telegramPendingConfirmationService.savePending(telegramId, pendingTransaction);

        return telegramMessageFormatter.formatTransactionPreview(
                pendingTransaction,
                resolvedAccount.displayName()
        );
    }

    private String handleNaturalLanguageQuery(ParsedTelegramMessage parsedMessage, Long telegramId) {
        try {
            String resultMessage = switch (parsedMessage.intentType()) {
                case QUERY_MONTH_EXPENSE_TOTAL -> {
                    MonthlyAmountSummaryResponse response = financeBotApiClient.getCurrentMonthExpenseSummary(telegramId);
                    yield telegramMessageFormatter.formatMonthExpenseSummary(response.totalAmount());
                }
                case QUERY_MONTH_INCOME_TOTAL -> {
                    MonthlyAmountSummaryResponse response = financeBotApiClient.getCurrentMonthIncomeSummary(telegramId);
                    yield telegramMessageFormatter.formatMonthIncomeSummary(response.totalAmount());
                }
                case QUERY_MONTH_ANALYSIS -> telegramBasicCommandHandler.handleAnalysis(telegramId);

                case QUERY_TRANSACTION_TOTAL -> {
                    String type = parsedMessage.originalMessage().toLowerCase().contains("recebi")
                            || parsedMessage.originalMessage().toLowerCase().contains("entrou")
                            ? "INCOME"
                            : "EXPENSE";

                    TelegramTransactionSummaryResponse response = financeBotApiClient.getTransactionSummary(
                            new TelegramTransactionSummaryRequest(
                                    telegramId,
                                    type,
                                    parsedMessage.categoryName(),
                                    parsedMessage.accountName(),
                                    parsedMessage.startDate(),
                                    parsedMessage.endDate()
                            )
                    );

                    String label = "EXPENSE".equals(type) ? "gasto" : "recebido";

                    StringBuilder complemento = new StringBuilder();
                    if (response.categoryName() != null) {
                        complemento.append(" em ").append(response.categoryName());
                    }
                    if (response.accountName() != null) {
                        complemento.append(" na conta ").append(response.accountName());
                    }

                    yield telegramMessageFormatter.formatTransactionSummary(
                            label,
                            complemento.toString(),
                            response.totalAmount()
                    );
                }

                case QUERY_INSTALLMENT_COUNT -> {
                    TelegramInstallmentCountResponse response = financeBotApiClient.getInstallmentCount(
                            new TelegramInstallmentCountRequest(
                                    telegramId,
                                    parsedMessage.startDate(),
                                    parsedMessage.endDate()
                            )
                    );

                    yield telegramMessageFormatter.formatInstallmentCountMessage(
                            response.installmentCount(),
                            response.startDate(),
                            response.endDate()
                    );
                }

                case QUERY_INSTALLMENT_PURCHASE_CAPACITY -> {
                    InstallmentPurchaseCapacityResponse response =
                            financeBotApiClient.getInstallmentPurchaseCapacity(
                                    new InstallmentPurchaseCapacityRequest(
                                            telegramId,
                                            parsedMessage.totalAmount(),
                                            parsedMessage.totalInstallments()
                                    )
                            );

                    yield telegramMessageFormatter.formatInstallmentPurchaseCapacityMessage(
                            response.totalAmount(),
                            response.totalInstallments(),
                            response.estimatedInstallmentAmount(),
                            response.analysisResult(),
                            response.observation()
                    );
                }

                case QUERY_ACTIVE_INSTALLMENTS -> {
                    TelegramActiveInstallmentsResponse response = financeBotApiClient.getActiveInstallments(telegramId);

                    yield telegramMessageFormatter.formatActiveInstallmentsMessage(
                            response.activeInstallmentGroupCount()
                    );
                }

                case QUERY_INSTALLMENT_REMAINING -> {
                    try {
                        TelegramActiveInstallmentSummaryResponse response =
                                financeBotApiClient.getActiveInstallmentSummary(
                                        telegramId,
                                        parsedMessage.installmentQueryTarget()
                                );

                        if (response == null || !response.hasActiveInstallment()) {
                            if (parsedMessage.installmentQueryTarget() != null
                                    && !parsedMessage.installmentQueryTarget().isBlank()) {
                                yield telegramMessageFormatter.formatInstallmentNotFoundMessage(
                                        parsedMessage.installmentQueryTarget()
                                );
                            }
                            yield telegramMessageFormatter.formatNoActiveInstallmentsMessage();
                        }

                        yield telegramMessageFormatter.formatRemainingInstallmentsMessage(
                                response.description(),
                                response.currentDueDate(),
                                response.currentInstallmentNumber(),
                                response.nextDueDate(),
                                response.remainingInstallments(),
                                response.nextInstallmentNumber(),
                                response.totalInstallments()
                        );
                    } catch (RestClientResponseException e) {
                        if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 403) {
                            telegramPendingQueryService.savePending(telegramId, parsedMessage);
                            yield telegramMessageFormatter.formatMultipleActiveInstallmentsMessage();
                        }
                        throw e;
                    }
                }

                case QUERY_INSTALLMENT_END_DATE -> {
                    try {
                        TelegramActiveInstallmentSummaryResponse response =
                                financeBotApiClient.getActiveInstallmentSummary(
                                        telegramId,
                                        parsedMessage.installmentQueryTarget()
                                );

                        if (response == null || !response.hasActiveInstallment()) {
                            if (parsedMessage.installmentQueryTarget() != null
                                    && !parsedMessage.installmentQueryTarget().isBlank()) {
                                yield telegramMessageFormatter.formatInstallmentNotFoundMessage(
                                        parsedMessage.installmentQueryTarget()
                                );
                            }
                            yield telegramMessageFormatter.formatNoActiveInstallmentsMessage();
                        }

                        yield telegramMessageFormatter.formatInstallmentEndDateMessage(
                                response.description(),
                                response.endDate()
                        );
                    } catch (RestClientResponseException e) {
                        if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 403) {
                            telegramPendingQueryService.savePending(telegramId, parsedMessage);
                            yield telegramMessageFormatter.formatMultipleActiveInstallmentsMessage();
                        }
                        throw e;
                    }
                }

                default -> "Não consegui interpretar sua consulta.";
            };

            telegramQueryContextService.saveQueryContext(telegramId, parsedMessage);
            return resultMessage;
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível consultar essas informações agora.";
        }
    }

    private PendingTelegramTransaction withResolvedAccountName(
            PendingTelegramTransaction pendingTransaction,
            String resolvedAccountName
    ) {
        if (pendingTransaction.accountName() != null && !pendingTransaction.accountName().isBlank()) {
            return pendingTransaction;
        }

        if (resolvedAccountName == null || resolvedAccountName.isBlank()) {
            return pendingTransaction;
        }

        return new PendingTelegramTransaction(
                pendingTransaction.intentType(),
                pendingTransaction.amount(),
                pendingTransaction.description(),
                pendingTransaction.date(),
                pendingTransaction.categoryName(),
                resolvedAccountName,
                pendingTransaction.totalInstallments(),
                pendingTransaction.originalMessage()
        );
    }

    private String handleConfirmation(Long telegramId) {
        PendingTelegramTransaction pending = telegramPendingConfirmationService.getPending(telegramId);

        if (pending == null) {
            return "Não há nenhuma operação pendente para confirmar.";
        }

        try {
            if (pending.isInstallment()) {
                CreateInstallmentTransactionFromTelegramRequest request =
                        new CreateInstallmentTransactionFromTelegramRequest(
                                telegramId,
                                pending.amount(),
                                pending.description(),
                                pending.date(),
                                pending.accountName(),
                                pending.categoryName(),
                                pending.totalInstallments()
                        );

                financeBotApiClient.createInstallmentTransaction(request);
            } else {
                CreateTransactionFromTelegramRequest request =
                        new CreateTransactionFromTelegramRequest(
                                telegramId,
                                mapIntentToTransactionType(pending.intentType()),
                                pending.amount(),
                                pending.description(),
                                pending.date(),
                                pending.categoryName(),
                                pending.accountName()
                        );

                financeBotApiClient.createTransaction(request);
            }

            telegramPendingConfirmationService.clearPending(telegramId);

            return telegramMessageFormatter.formatTransactionSuccess(pending.intentType());
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return """
        Não foi possível salvar sua transação agora.
        Você pode tentar confirmar novamente em instantes.
        """;
        }
    }

    private String handleCancellation(Long telegramId) {
        boolean hasPendingConfirmation = telegramPendingConfirmationService.hasPending(telegramId);
        boolean hasPendingQuery = telegramPendingQueryService.hasPending(telegramId);

        if (!hasPendingConfirmation && !hasPendingQuery) {
            return "Não há nenhuma operação pendente para cancelar.";
        }

        telegramPendingConfirmationService.clearPending(telegramId);
        telegramPendingQueryService.clearPending(telegramId);

        return "❌ Operação cancelada com sucesso.";
    }

    private String handlePendingEdit(Long telegramId, String messageText) {
        PendingTelegramTransaction pending = telegramPendingConfirmationService.getPending(telegramId);

        if (pending == null) {
            return "Não há nenhuma operação pendente para editar.";
        }

        String lower = telegramTextNormalizer.normalize(messageText);

        BigDecimal amount = pending.amount();
        String description = pending.description();
        LocalDate date = pending.date();
        String categoryName = pending.categoryName();
        String accountName = pending.accountName();

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

        if (!changed) {
            return "Entendi que você quer editar a operação, mas não consegui identificar alterações válidas.";
        }

        PendingTelegramTransaction updated = new PendingTelegramTransaction(
                pending.intentType(),
                amount,
                description,
                date,
                categoryName,
                accountName,
                pending.totalInstallments(),
                pending.originalMessage()
        );

        telegramPendingConfirmationService.savePending(telegramId, updated);

        return buildUpdatedPendingMessage(telegramId, updated);
    }

    private String handlePendingInstallmentQuerySelection(
            Long telegramId,
            String messageText,
            ParsedTelegramMessage pending
    ) {
        ParsedTelegramMessage reparsed = telegramIntentService.parse(messageText);
        String selectedTarget = reparsed.installmentQueryTarget() != null
                ? reparsed.installmentQueryTarget()
                : messageText.trim();

        ParsedTelegramMessage updated = new ParsedTelegramMessage(
                pending.intentType(),
                pending.amount(),
                pending.description(),
                pending.date(),
                pending.originalMessage(),
                pending.categoryName(),
                pending.accountName(),
                pending.startDate(),
                pending.endDate(),
                pending.totalInstallments(),
                selectedTarget,
                pending.totalAmount()
        );

        telegramPendingQueryService.clearPending(telegramId);
        return handleNaturalLanguageQuery(updated, telegramId);
    }

    private String buildUpdatedPendingMessage(Long telegramId, PendingTelegramTransaction pendingTransaction) {
        ResolvedPreviewAccount resolvedAccount = resolvePreviewAccount(pendingTransaction, telegramId);
        return telegramMessageFormatter.formatUpdatedPendingMessage(
                pendingTransaction,
                resolvedAccount.displayName()
        );
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

    private String mapIntentToTransactionType(TelegramIntentType intentType) {
        return switch (intentType) {
            case CREATE_EXPENSE, CREATE_INSTALLMENT_EXPENSE -> "EXPENSE";
            case CREATE_INCOME -> "INCOME";
            default -> throw new IllegalArgumentException("Intento inválido para criação de transação.");
        };
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

    private ResolvedPreviewAccount resolvePreviewAccount(PendingTelegramTransaction pendingTransaction, Long telegramId) {
        if (pendingTransaction.accountName() != null && !pendingTransaction.accountName().isBlank()) {
            return new ResolvedPreviewAccount(
                    pendingTransaction.accountName(),
                    pendingTransaction.accountName()
            );
        }

        try {
            TelegramDefaultAccountResponse response = financeBotApiClient.getDefaultAccount(telegramId);

            if (response != null && response.accountName() != null && !response.accountName().isBlank()) {
                return new ResolvedPreviewAccount(
                        response.accountName(),
                        response.accountName()
                );
            }
        } catch (RestClientResponseException e) {
            return new ResolvedPreviewAccount("conta padrão", null);
        } catch (Exception e) {
            return new ResolvedPreviewAccount("conta padrão", null);
        }

        return new ResolvedPreviewAccount("conta padrão", null);
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

    private record ResolvedPreviewAccount(
            String displayName,
            String persistedName
    ) {
    }
}