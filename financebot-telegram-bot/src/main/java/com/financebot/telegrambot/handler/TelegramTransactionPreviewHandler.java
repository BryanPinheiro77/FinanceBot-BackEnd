package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.mapper.PendingTelegramTransactionMapper;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.support.TelegramPreviewAccountResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramTransactionPreviewHandler {

    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramMessageFormatter telegramMessageFormatter;
    private final PendingTelegramTransactionMapper pendingTelegramTransactionMapper;
    private final TelegramPreviewAccountResolver telegramPreviewAccountResolver;

    public String handlePreview(Long telegramId, ParsedTelegramMessage parsedMessage) {
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

        TelegramPreviewAccountResolver.ResolvedPreviewAccount resolvedAccount =
                telegramPreviewAccountResolver.resolve(pendingTransaction, telegramId);

        pendingTransaction = withResolvedAccountName(
                pendingTransaction,
                resolvedAccount.persistedName()
        );

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
}
