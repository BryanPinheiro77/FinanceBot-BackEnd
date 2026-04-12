package com.financebot.telegrambot.service;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.*;
import com.financebot.telegrambot.dto.request.*;
import com.financebot.telegrambot.dto.response.*;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.intent.TelegramIntentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TelegramCommandService {

    private final FinanceBotApiClient financeBotApiClient;
    private final TelegramIntentService telegramIntentService;
    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramQueryContextService telegramQueryContextService;
    private final TelegramMessageFormatter telegramMessageFormatter;

    public String handleMessage(
            String messageText,
            Long telegramId,
            String telegramUsername,
            String telegramFirstName
    ) {
        if (messageText == null || messageText.isBlank()) {
            return "Não consegui entender sua mensagem. Tente /start, /iniciar, /help ou /ajuda.";
        }

        String normalizedMessage = messageText.trim();

        if (startsWithCommand(normalizedMessage, "/start", "/iniciar")) {
            return handleStart(telegramFirstName, telegramUsername);
        }

        if (startsWithCommand(normalizedMessage, "/help", "/ajuda")) {
            return handleHelp();
        }

        if (startsWithCommand(normalizedMessage, "/connect", "/conectar")) {
            return handleConnect(normalizedMessage, telegramId, telegramUsername);
        }

        if (startsWithCommand(normalizedMessage, "/me", "/perfil")) {
            return handleMe(telegramId);
        }

        if (startsWithCommand(normalizedMessage, "/disconnect", "/desconectar")) {
            return handleDisconnect(telegramId);
        }

        if (startsWithCommand(normalizedMessage, "/setincome", "/definirrenda")) {
            return handleSetIncome(normalizedMessage, telegramId);
        }

        if (startsWithCommand(normalizedMessage, "/analysis", "/analise")) {
            return handleAnalysis(telegramId);
        }

        if (startsWithCommand(normalizedMessage, "/status", "/resumo")) {
            return handleStatus(telegramId);
        }

        if (containsGreeting(normalizedMessage)) {
            return handleGreeting(telegramFirstName, telegramUsername);
        }

        if (looksLikeConnectionIntent(normalizedMessage)) {
            return telegramMessageFormatter.formatConnectInstructionsMessage();
        }

        if (isConfirmationMessage(normalizedMessage)) {
            return handleConfirmation(telegramId);
        }

        if (isCancellationMessage(normalizedMessage)) {
            return handleCancellation(telegramId);
        }

        if (telegramPendingConfirmationService.hasPending(telegramId) && looksLikeEditMessage(normalizedMessage)) {
            return handlePendingEdit(telegramId, normalizedMessage);
        }

        ParsedTelegramMessage pending = telegramPendingConfirmationService.getPending(telegramId);
        if (pending != null
                && pending.intentType() != null
                && pending.intentType().name().startsWith("QUERY_INSTALLMENT_")
                && !normalizedMessage.startsWith("/")) {
            return handlePendingInstallmentQuerySelection(telegramId, normalizedMessage, pending);
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

    private String handleStart(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);
        return telegramMessageFormatter.formatStartMessage(name);
    }

    private String handleHelp() {
        return telegramMessageFormatter.formatHelpMessage();
    }

    private String handleGreeting(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);
        return telegramMessageFormatter.formatGreetingMessage(name);
    }

    private String handleConnect(String messageText, Long telegramId, String telegramUsername) {
        String[] parts = messageText.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return telegramMessageFormatter.formatConnectCodeRequiredMessage();
        }

        String linkCode = parts[1].trim();

        try {
            TelegramLinkConfirmResponse response = financeBotApiClient.confirmTelegramLink(
                    new TelegramLinkConfirmRequest(linkCode, telegramId, telegramUsername)
            );

            return telegramMessageFormatter.formatConnectSuccessMessage(response.message());
        } catch (RestClientResponseException e) {
            return telegramMessageFormatter.formatConnectErrorMessage(e.getStatusCode().value());
        } catch (Exception e) {
            return telegramMessageFormatter.formatGenericConnectFailureMessage();
        }
    }

    private String handleDisconnect(Long telegramId) {
        try {
            financeBotApiClient.disconnectTelegram(telegramId);
            telegramPendingConfirmationService.clearPending(telegramId);

            return telegramMessageFormatter.formatDisconnectSuccessMessage();
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return telegramMessageFormatter.formatGenericDisconnectFailureMessage();
        }
    }

    private String handleMe(Long telegramId) {
        try {
            UserProfileResponse response = financeBotApiClient.getMe(telegramId);

            return telegramMessageFormatter.formatProfileMessage(
                    response.name(),
                    response.email(),
                    response.monthlyBaseIncome(),
                    response.telegramId() != null
            );
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return telegramMessageFormatter.formatGenericProfileFailureMessage();
        }
    }

    private String handleSetIncome(String messageText, Long telegramId) {
        String[] parts = messageText.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return telegramMessageFormatter.formatSetIncomeValueRequiredMessage();
        }

        try {
            BigDecimal income = parseBrazilianNumber(parts[1]);

            if (income.compareTo(BigDecimal.ZERO) <= 0) {
                return telegramMessageFormatter.formatSetIncomeNonPositiveMessage();
            }

            UserProfileResponse response = financeBotApiClient.updateMonthlyBaseIncome(
                    telegramId,
                    new UpdateMonthlyBaseIncomeRequest(income)
            );

            return telegramMessageFormatter.formatSetIncomeSuccessMessage(response.monthlyBaseIncome());
        } catch (NumberFormatException e) {
            return telegramMessageFormatter.formatSetIncomeInvalidValueMessage();
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return telegramMessageFormatter.formatGenericSetIncomeFailureMessage();
        }
    }

    private String handleAnalysis(Long telegramId) {
        try {
            FinancialCommitmentResponse response = financeBotApiClient.getFinancialAnalysis(telegramId);

            return telegramMessageFormatter.formatAnalysisMessage(
                    response.monthlyBaseIncome(),
                    response.monthlyIncomeReference(),
                    response.projectedRecurringIncomeNextMonth(),
                    response.projectedRecurringExpenseNextMonth(),
                    response.nextMonthProjectedIncome(),
                    response.nextMonthProjectedExpense(),
                    response.projectedNetNextMonth(),
                    response.commitmentPercentage(),
                    response.activeInstallmentCount(),
                    translateRiskLevel(response.riskLevel()),
                    response.message()
            );
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível gerar sua análise financeira agora.";
        }
    }

    private String handleStatus(Long telegramId) {
        try {
            UserProfileResponse profile = financeBotApiClient.getMe(telegramId);
            FinancialCommitmentResponse analysis = financeBotApiClient.getFinancialAnalysis(telegramId);

            return telegramMessageFormatter.formatStatusMessage(
                    profile.email(),
                    profile.monthlyBaseIncome(),
                    analysis.projectedNetNextMonth(),
                    translateRiskLevel(analysis.riskLevel())
            );
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível buscar o status da sua conta agora.";
        }
    }

    private String handleNaturalLanguageTransactionPreview(Long telegramId, ParsedTelegramMessage parsedMessage) {
        if (parsedMessage.amount() == null) {
            return """
            Entendi a intenção, mas não consegui identificar o valor.
            
            Exemplos:
            - gastei 50 no mercado
            - paguei 120 de gasolina
            - recebi 1500 de salário
            """;
        }

        String conta = resolvePreviewAccountName(parsedMessage, telegramId);

        if (parsedMessage.intentType() == TelegramIntentType.CREATE_INSTALLMENT_EXPENSE) {
            if (parsedMessage.totalInstallments() == null || parsedMessage.totalInstallments() < 2) {
                return """
                Entendi a intenção de parcelamento, mas não consegui identificar uma quantidade válida de parcelas.
                
                Exemplos:
                - gastei 1200 parcelado em 10x
                - comprei um celular por 2400 em 12x
                - gastei 300 no inter parcelado em 3x
                """;
            }

            telegramPendingConfirmationService.savePending(telegramId, parsedMessage);
            return telegramMessageFormatter.formatInstallmentTransactionPreview(parsedMessage, conta);
        }

        telegramPendingConfirmationService.savePending(telegramId, parsedMessage);
        return telegramMessageFormatter.formatTransactionPreview(parsedMessage, conta);
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
                case QUERY_MONTH_ANALYSIS -> handleAnalysis(telegramId);

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
                            telegramPendingConfirmationService.savePending(telegramId, parsedMessage);
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
                            telegramPendingConfirmationService.savePending(telegramId, parsedMessage);
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
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível consultar essas informações agora.";
        }
    }

    private String handleConfirmation(Long telegramId) {
        ParsedTelegramMessage pending = telegramPendingConfirmationService.getPending(telegramId);

        if (pending == null) {
            return "Não há nenhuma operação pendente para confirmar.";
        }

        if (pending.intentType() != null && pending.intentType().name().startsWith("QUERY_INSTALLMENT_")) {
            return "Me diga qual parcelamento deseja consultar, por exemplo: tv ou computador.";
        }

        try {
            if (pending.intentType() == TelegramIntentType.CREATE_INSTALLMENT_EXPENSE) {
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
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return """
                Não foi possível salvar sua transação agora.
                Você pode tentar confirmar novamente em instantes.
                """;
        }
    }

    private String handleCancellation(Long telegramId) {
        if (!telegramPendingConfirmationService.hasPending(telegramId)) {
            return "Não há nenhuma operação pendente para cancelar.";
        }

        telegramPendingConfirmationService.clearPending(telegramId);

        return "❌ Operação cancelada com sucesso.";
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

        telegramPendingConfirmationService.clearPending(telegramId);
        return handleNaturalLanguageQuery(updated, telegramId);
    }

    private String handlePendingEdit(Long telegramId, String messageText) {
        ParsedTelegramMessage pending = telegramPendingConfirmationService.getPending(telegramId);

        if (pending == null) {
            return "Não há nenhuma operação pendente para editar.";
        }

        String lower = normalizeText(messageText);

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

        ParsedTelegramMessage updated = new ParsedTelegramMessage(
                pending.intentType(),
                amount,
                description,
                date,
                pending.originalMessage(),
                categoryName,
                accountName,
                pending.startDate(),
                pending.endDate(),
                pending.totalInstallments(),
                pending.installmentQueryTarget(),
                pending.totalAmount()
        );

        telegramPendingConfirmationService.savePending(telegramId, updated);

        return buildUpdatedPendingMessage(telegramId, updated);
    }

    private String buildUpdatedPendingMessage(Long telegramId, ParsedTelegramMessage parsedMessage) {
        String conta = resolvePreviewAccountName(parsedMessage, telegramId);
        return telegramMessageFormatter.formatUpdatedPendingMessage(parsedMessage, conta);
    }

    private String mapDefaultBotErrors(RestClientResponseException e) {
        return telegramMessageFormatter.formatDefaultBotErrorMessage(e.getStatusCode().value());
    }

    private String translateRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return "Não informado";
        }

        return switch (riskLevel.toUpperCase()) {
            case "LOW" -> "Baixo";
            case "MEDIUM" -> "Médio";
            case "HIGH" -> "Alto";
            default -> riskLevel;
        };
    }

    private String resolveDisplayName(String telegramFirstName, String telegramUsername) {
        if (telegramFirstName != null && !telegramFirstName.isBlank()) {
            return telegramFirstName.trim();
        }

        if (telegramUsername != null && !telegramUsername.isBlank()) {
            return "@" + telegramUsername.trim();
        }

        return null;
    }

    private boolean containsGreeting(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.equals("oi")
                || lower.equals("olá")
                || lower.equals("ola")
                || lower.equals("bom dia")
                || lower.equals("boa tarde")
                || lower.equals("boa noite");
    }

    private boolean looksLikeConnectionIntent(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.contains("conectar")
                || lower.contains("vincular")
                || lower.contains("ligar conta")
                || lower.contains("linkar")
                || lower.contains("telegram");
    }

    private boolean isConfirmationMessage(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.equals("sim")
                || lower.equals("confirmar")
                || lower.equals("confirmado")
                || lower.equals("ok");
    }

    private boolean isCancellationMessage(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.equals("cancelar")
                || lower.equals("cancelado")
                || lower.equals("cancelar operação")
                || lower.equals("cancelar operacao")
                || lower.equals("não")
                || lower.equals("nao");
    }

    private boolean looksLikeEditMessage(String messageText) {
        String lower = normalizeText(messageText);

        return lower.contains("muda valor")
                || lower.contains("muda o valor")
                || lower.contains("mude valor")
                || lower.contains("mude o valor")
                || lower.contains("altera valor")
                || lower.contains("altera o valor")
                || lower.contains("altere valor")
                || lower.contains("altere o valor")
                || lower.contains("corrige valor")
                || lower.contains("corrige o valor")
                || lower.contains("corrija valor")
                || lower.contains("corrija o valor")
                || lower.contains("troca valor")
                || lower.contains("troque valor")
                || lower.contains("muda descricao")
                || lower.contains("muda a descricao")
                || lower.contains("altera descricao")
                || lower.contains("altera a descricao")
                || lower.contains("corrige descricao")
                || lower.contains("corrige a descricao")
                || lower.contains("muda categoria")
                || lower.contains("muda a categoria")
                || lower.contains("troca categoria")
                || lower.contains("troca a categoria")
                || lower.contains("altera categoria")
                || lower.contains("altera a categoria")
                || lower.contains("muda data")
                || lower.contains("muda a data")
                || lower.contains("troca data")
                || lower.contains("troca a data")
                || lower.contains("altera data")
                || lower.contains("altera a data")
                || lower.contains("muda conta")
                || lower.contains("muda a conta")
                || lower.contains("troca conta")
                || lower.contains("troca a conta")
                || lower.contains("altera conta")
                || lower.contains("altera a conta")
                || lower.contains("usa a conta")
                || lower.contains("coloca na conta")
                || lower.contains("coloca a conta")
                || lower.startsWith("data de ")
                || lower.startsWith("data para ")
                || lower.startsWith("data pra ")
                || lower.contains("pra ")
                || lower.contains("para ");
    }

    private String normalizeText(String text) {
        if(text == null){
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private boolean startsWithCommand(String messageText, String... commands) {
        for (String command : commands) {
            if (messageText.startsWith(command)) {
                return true;
            }
        }
        return false;
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
        String cleaned = normalizeText(text)
                .replaceFirst(".*?descricao\\s+para\\s+", "")
                .replaceFirst(".*?descricao\\s+pra\\s+", "")
                .replaceFirst(".*?descricao\\s+", "")
                .trim();

        cleaned = trimAtNextEditHint(cleaned);

        return cleaned.isBlank() ? null : cleaned;
    }

    private String extractCategoryFromEdit(String text) {
        String cleaned = normalizeText(text)
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
        String lower = normalizeText(text);

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

    private String extractAccountFromEdit(String text) {
        String cleaned = normalizeText(text)
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

    private String resolvePreviewAccountName(ParsedTelegramMessage parsedMessage, Long telegramId) {
        if (parsedMessage.accountName() != null && !parsedMessage.accountName().isBlank()) {
            return parsedMessage.accountName();
        }

        try {
            TelegramDefaultAccountResponse response = financeBotApiClient.getDefaultAccount(telegramId);

            if (response != null && response.accountName() != null && !response.accountName().isBlank()) {
                return response.accountName();
            }
        } catch (RestClientResponseException e) {
            return "conta padrão";
        } catch (Exception e) {
            return "conta padrão";
        }

        return "conta padrão";
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

        String normalized = normalizeText(text);

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
}
