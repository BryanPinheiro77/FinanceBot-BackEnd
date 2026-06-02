package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.support.TelegramPendingEditParser;
import com.financebot.telegrambot.support.TelegramPreviewAccountResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramPendingEditHandler {

    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramMessageFormatter telegramMessageFormatter;
    private final TelegramPendingEditParser telegramPendingEditParser;
    private final TelegramPreviewAccountResolver telegramPreviewAccountResolver;

    public String handleEdit(Long telegramId, String messageText) {
        PendingTelegramTransaction pending = telegramPendingConfirmationService.getPending(telegramId);

        if (pending == null) {
            return "Não há nenhuma operação pendente para editar.";
        }

        TelegramPendingEditParser.PendingEditResult editResult =
                telegramPendingEditParser.parse(messageText);

        if (!editResult.changed()) {
            return "Entendi que você quer editar a operação, mas não consegui identificar alterações válidas.";
        }

        PendingTelegramTransaction updated = new PendingTelegramTransaction(
                pending.intentType(),
                editResult.amount() != null ? editResult.amount() : pending.amount(),
                editResult.description() != null ? editResult.description() : pending.description(),
                editResult.date() != null ? editResult.date() : pending.date(),
                editResult.categoryName() != null ? editResult.categoryName() : pending.categoryName(),
                editResult.accountName() != null ? editResult.accountName() : pending.accountName(),
                pending.totalInstallments(),
                pending.originalMessage()
        );

        telegramPendingConfirmationService.savePending(telegramId, updated);

        return buildUpdatedPendingMessage(telegramId, updated);
    }

    private String buildUpdatedPendingMessage(Long telegramId, PendingTelegramTransaction pendingTransaction) {
        TelegramPreviewAccountResolver.ResolvedPreviewAccount resolvedAccount =
                telegramPreviewAccountResolver.resolve(pendingTransaction, telegramId);

        return telegramMessageFormatter.formatUpdatedPendingMessage(
                pendingTransaction,
                resolvedAccount.displayName()
        );
    }
}