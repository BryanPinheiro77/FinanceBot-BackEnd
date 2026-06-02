package com.financebot.telegrambot.router;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.handler.TelegramBasicCommandHandler;
import com.financebot.telegrambot.handler.TelegramFinancialQueryHandler;
import com.financebot.telegrambot.handler.TelegramPendingEditHandler;
import com.financebot.telegrambot.handler.TelegramPendingOperationHandler;
import com.financebot.telegrambot.handler.TelegramTransactionPreviewHandler;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramIntentService;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.service.TelegramQueryContextService;
import com.financebot.telegrambot.support.TelegramCommandMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramCommandRouter {

    private final TelegramIntentService telegramIntentService;
    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramPendingQueryService telegramPendingQueryService;
    private final TelegramQueryContextService telegramQueryContextService;
    private final TelegramCommandMatcher telegramCommandMatcher;
    private final TelegramBasicCommandHandler telegramBasicCommandHandler;
    private final TelegramPendingOperationHandler telegramPendingOperationHandler;
    private final TelegramTransactionPreviewHandler telegramTransactionPreviewHandler;
    private final TelegramPendingEditHandler telegramPendingEditHandler;
    private final TelegramFinancialQueryHandler telegramFinancialQueryHandler;

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
            return telegramFinancialQueryHandler.handleQuery(parsedMessage, telegramId);
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
        return telegramFinancialQueryHandler.handleQuery(updated, telegramId);
    }

}