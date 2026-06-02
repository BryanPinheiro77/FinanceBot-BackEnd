package com.financebot.telegrambot.router;

import com.financebot.telegrambot.handler.TelegramBasicCommandHandler;
import com.financebot.telegrambot.handler.TelegramPendingEditHandler;
import com.financebot.telegrambot.handler.TelegramPendingOperationHandler;
import com.financebot.telegrambot.handler.TelegramPendingQueryHandler;
import com.financebot.telegrambot.handler.TelegramNaturalLanguageHandler;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.support.TelegramCommandMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramCommandRouter {

    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramCommandMatcher telegramCommandMatcher;
    private final TelegramBasicCommandHandler telegramBasicCommandHandler;
    private final TelegramPendingOperationHandler telegramPendingOperationHandler;
    private final TelegramPendingEditHandler telegramPendingEditHandler;
    private final TelegramPendingQueryHandler telegramPendingQueryHandler;
    private final TelegramNaturalLanguageHandler telegramNaturalLanguageHandler;

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

        if (telegramPendingQueryHandler.hasPendingInstallmentQuery(telegramId, normalizedMessage)) {
            return telegramPendingQueryHandler.handlePendingInstallmentQuerySelection(
                    telegramId,
                    normalizedMessage
            );
        }

        return telegramNaturalLanguageHandler.handle(normalizedMessage, telegramId);
    }

}