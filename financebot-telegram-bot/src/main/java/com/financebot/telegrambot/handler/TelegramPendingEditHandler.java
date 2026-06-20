package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.support.TelegramPendingEditParser;
import com.financebot.telegrambot.support.TelegramPreviewAccountResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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

        Integer firstRemainingInstallmentNumber = resolveFirstRemainingInstallmentNumber(pending, editResult);
        if (firstRemainingInstallmentNumber == null && editResult.firstRemainingInstallmentNumber() != null) {
            return """
                    Entendi que você quer alterar a parcela atual, mas o valor informado não é válido para este parcelamento.

                    Exemplo:
                    - ja paguei 5 parcelas
                    - estou pagando a 6 parcela
                    """;
        }

        PendingTelegramTransaction updated = new PendingTelegramTransaction(
                pending.intentType(),
                resolveAmount(pending, editResult),
                resolveMonthlyAmount(pending, editResult),
                editResult.description() != null ? editResult.description() : pending.description(),
                editResult.date() != null ? editResult.date() : pending.date(),
                editResult.categoryName() != null ? editResult.categoryName() : pending.categoryName(),
                editResult.accountName() != null ? editResult.accountName() : pending.accountName(),
                pending.totalInstallments(),
                firstRemainingInstallmentNumber,
                pending.originalMessage()
        );

        telegramPendingConfirmationService.savePending(telegramId, updated);

        return buildUpdatedPendingMessage(telegramId, updated);
    }

    private BigDecimal resolveAmount(
            PendingTelegramTransaction pending,
            TelegramPendingEditParser.PendingEditResult editResult
    ) {
        if (editResult.amount() == null) {
            return pending.amount();
        }

        if (!pending.isExistingInstallment()) {
            return editResult.amount();
        }

        return switch (editResult.amountKind()) {
            case TOTAL -> editResult.amount();
            case MONTHLY -> null;
            case UNSPECIFIED -> pending.monthlyAmount() != null && pending.amount() == null
                    ? null
                    : editResult.amount();
        };
    }

    private BigDecimal resolveMonthlyAmount(
            PendingTelegramTransaction pending,
            TelegramPendingEditParser.PendingEditResult editResult
    ) {
        if (editResult.amount() == null) {
            return pending.monthlyAmount();
        }

        if (!pending.isExistingInstallment()) {
            return pending.monthlyAmount();
        }

        return switch (editResult.amountKind()) {
            case TOTAL -> null;
            case MONTHLY -> editResult.amount();
            case UNSPECIFIED -> pending.monthlyAmount() != null && pending.amount() == null
                    ? editResult.amount()
                    : null;
        };
    }

    private String buildUpdatedPendingMessage(Long telegramId, PendingTelegramTransaction pendingTransaction) {
        TelegramPreviewAccountResolver.ResolvedPreviewAccount resolvedAccount =
                telegramPreviewAccountResolver.resolve(pendingTransaction, telegramId);

        return telegramMessageFormatter.formatUpdatedPendingMessage(
                pendingTransaction,
                resolvedAccount.displayName()
        );
    }

    private Integer resolveFirstRemainingInstallmentNumber(
            PendingTelegramTransaction pending,
            TelegramPendingEditParser.PendingEditResult editResult
    ) {
        Integer editedFirstRemainingInstallmentNumber = editResult.firstRemainingInstallmentNumber();

        if (editedFirstRemainingInstallmentNumber == null) {
            return pending.firstRemainingInstallmentNumber();
        }

        if (!pending.isExistingInstallment()) {
            return null;
        }

        if (editedFirstRemainingInstallmentNumber < 1) {
            return null;
        }

        if (pending.totalInstallments() != null
                && editedFirstRemainingInstallmentNumber > pending.totalInstallments()) {
            return null;
        }

        return editedFirstRemainingInstallmentNumber;
    }
}
