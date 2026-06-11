package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.conversation.application.TelegramConversationContextService;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;
import com.financebot.telegrambot.conversation.domain.TelegramConversationContextType;
import com.financebot.telegrambot.conversation.domain.TelegramConversationMissingField;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.mapper.PendingTelegramTransactionMapper;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.support.TelegramPreviewAccountResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TelegramTransactionPreviewHandler {

    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramMessageFormatter telegramMessageFormatter;
    private final PendingTelegramTransactionMapper pendingTelegramTransactionMapper;
    private final TelegramPreviewAccountResolver telegramPreviewAccountResolver;
    private final TelegramConversationContextService telegramConversationContextService;

    public String handlePreview(Long telegramId, ParsedTelegramMessage parsedMessage) {
        return handlePreview(telegramId, parsedMessage, true);
    }

    public String handlePreview(
            Long telegramId,
            ParsedTelegramMessage parsedMessage,
            boolean allowConversationPrompt
    ) {

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

        if (pendingTransaction.isInstallment()) {
            return handleInstallmentPreview(
                    telegramId,
                    parsedMessage,
                    pendingTransaction,
                    resolvedAccount.displayName(),
                    allowConversationPrompt
            );
        }

        telegramPendingConfirmationService.savePending(telegramId, pendingTransaction);

        return telegramMessageFormatter.formatTransactionPreview(
                pendingTransaction,
                resolvedAccount.displayName()
        );

    }

    private String handleInstallmentPreview(
            Long telegramId,
            ParsedTelegramMessage parsedMessage,
            PendingTelegramTransaction pendingTransaction,
            String accountName,
            boolean allowConversationPrompt
    ) {
        String validationError = validateInstallment(pendingTransaction);

        if (validationError != null) {
            return validationError;
        }

        if (allowConversationPrompt) {
            saveInstallmentDueDayContext(telegramId, parsedMessage);
            return buildInstallmentDueDayQuestion(pendingTransaction);
        }

        telegramPendingConfirmationService.savePending(telegramId, pendingTransaction);

        return formatInstallmentPreview(pendingTransaction, accountName);
    }

    private String validateInstallment(PendingTelegramTransaction pendingTransaction) {
        if (pendingTransaction.totalInstallments() == null || pendingTransaction.totalInstallments() < 2) {
            return """
                    Entendi a intenção de parcelamento, mas não consegui identificar uma quantidade válida de parcelas.
                    
                    Exemplos:
                    - gastei 1200 parcelado em 10x
                    - comprei um celular por 2400 em 12x
                    - gastei 300 no inter parcelado em 3x
                    """;
        }

        if (!pendingTransaction.isExistingInstallment()) {
            return null;
        }

        if (pendingTransaction.firstRemainingInstallmentNumber() == null
                || pendingTransaction.firstRemainingInstallmentNumber() < 1
                || pendingTransaction.firstRemainingInstallmentNumber() > pendingTransaction.totalInstallments()) {
            return """
                    Entendi a intenção de parcelamento existente, mas não consegui identificar uma parcela atual válida.
                    
                    Exemplos:
                    - comprei um celular por 2400 em 12x e ja paguei 5 parcelas
                    - comprei um celular por 2400 em 12x e estou pagando a 6 parcela
                    - tenho um financiamento de 3000 em 10x e estou na 6 parcela
                    """;
        }

        return null;
    }

    private String buildInstallmentDueDayQuestion(PendingTelegramTransaction pendingTransaction) {
        String installmentLabel = pendingTransaction.isExistingInstallment()
                ? "próxima parcela"
                : "primeira parcela";

        return """
                Entendi o parcelamento.
                
                Qual o dia de vencimento da %s?
                
                Exemplo:
                - dia 15
                """.formatted(installmentLabel);
    }

    private String formatInstallmentPreview(
            PendingTelegramTransaction pendingTransaction,
            String accountName
    ) {
        if (pendingTransaction.isExistingInstallment()) {
            return telegramMessageFormatter.formatExistingInstallmentTransactionPreview(
                    pendingTransaction,
                    accountName
            );
        }

        return telegramMessageFormatter.formatInstallmentTransactionPreview(
                pendingTransaction,
                accountName
        );
    }

    private void saveInstallmentDueDayContext(
            Long telegramId,
            ParsedTelegramMessage parsedMessage
    ) {
        telegramConversationContextService.savePendingContext(
                telegramId,
                new TelegramConversationContext(
                        TelegramConversationContextType.PENDING_MISSING_INFORMATION,
                        parsedMessage.intentType(),
                        parsedMessage,
                        parsedMessage.originalMessage(),
                        Set.of(TelegramConversationMissingField.INSTALLMENT_DUE_DAY),
                        Instant.now()
                )
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
                pendingTransaction.firstRemainingInstallmentNumber(),
                pendingTransaction.originalMessage()
        );
    }
}
