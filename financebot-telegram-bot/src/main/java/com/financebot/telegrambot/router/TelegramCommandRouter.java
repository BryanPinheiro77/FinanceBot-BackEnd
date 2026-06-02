package com.financebot.telegrambot.router;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.dto.request.InstallmentPurchaseCapacityRequest;
import com.financebot.telegrambot.dto.request.TelegramInstallmentCountRequest;
import com.financebot.telegrambot.dto.request.TelegramTransactionSummaryRequest;
import com.financebot.telegrambot.dto.response.InstallmentPurchaseCapacityResponse;
import com.financebot.telegrambot.dto.response.MonthlyAmountSummaryResponse;
import com.financebot.telegrambot.dto.response.TelegramActiveInstallmentSummaryResponse;
import com.financebot.telegrambot.dto.response.TelegramActiveInstallmentsResponse;
import com.financebot.telegrambot.dto.response.TelegramInstallmentCountResponse;
import com.financebot.telegrambot.dto.response.TelegramTransactionSummaryResponse;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.handler.TelegramPendingEditHandler;
import com.financebot.telegrambot.handler.TelegramPendingOperationHandler;
import com.financebot.telegrambot.handler.TelegramBasicCommandHandler;
import com.financebot.telegrambot.handler.TelegramTransactionPreviewHandler;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramIntentService;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.service.TelegramQueryContextService;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import com.financebot.telegrambot.support.TelegramCommandMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class TelegramCommandRouter {

    private final FinanceBotApiClient financeBotApiClient;
    private final TelegramIntentService telegramIntentService;
    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramPendingQueryService telegramPendingQueryService;
    private final TelegramQueryContextService telegramQueryContextService;
    private final TelegramMessageFormatter telegramMessageFormatter;
    private final TelegramCommandMatcher telegramCommandMatcher;
    private final TelegramBotErrorMapper telegramBotErrorMapper;
    private final TelegramBasicCommandHandler telegramBasicCommandHandler;
    private final TelegramPendingOperationHandler telegramPendingOperationHandler;
    private final TelegramTransactionPreviewHandler telegramTransactionPreviewHandler;
    private final TelegramPendingEditHandler telegramPendingEditHandler;

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
            return telegramPendingOperationHandler.handleConfirmation(telegramId);
        }

        if (telegramCommandMatcher.isCancellationMessage(normalizedMessage)) {
            return telegramPendingOperationHandler.handleCancellation(telegramId);
        }

        if (telegramPendingConfirmationService.hasPending(telegramId)
                && telegramCommandMatcher.looksLikeEditMessage(normalizedMessage)) {
            return telegramPendingEditHandler.handleEdit(telegramId, normalizedMessage);
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
            return telegramTransactionPreviewHandler.handlePreview(telegramId, parsedMessage);
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

}