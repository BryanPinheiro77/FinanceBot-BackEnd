package com.financebot.telegrambot.conversation.application;

import com.financebot.telegrambot.conversation.domain.TelegramConversationContext;
import com.financebot.telegrambot.conversation.domain.TelegramConversationMissingField;
import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.handler.TelegramTransactionPreviewHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramConversationContinuationService {

    private final TelegramConversationContextService telegramConversationContextService;
    private final TelegramInstallmentDueDayResolver telegramInstallmentDueDayResolver;
    private final TelegramTransactionPreviewHandler telegramTransactionPreviewHandler;

    public boolean hasPendingContext(Long telegramId) {
        return telegramConversationContextService.hasPendingContext(telegramId);
    }

    public String handle(Long telegramId, String messageText) {
        Optional<TelegramConversationContext> pendingContext =
                telegramConversationContextService.findPendingContext(telegramId);

        if (pendingContext.isEmpty()) {
            return "Não encontrei uma conversa pendente. Envie sua mensagem novamente.";
        }

        TelegramConversationContext context = pendingContext.get();

        if (context.missingFields() == null || context.missingFields().isEmpty()) {
            telegramConversationContextService.clearPendingContext(telegramId);
            return "Não consegui continuar essa conversa. Envie sua mensagem novamente.";
        }

        if (context.missingFields().contains(TelegramConversationMissingField.INSTALLMENT_DUE_DAY)) {
            return handleInstallmentDueDay(telegramId, messageText, context);
        }

        return """
                Ainda não consigo completar essa informação pendente.

                Você pode cancelar e enviar a mensagem novamente.
                """;
    }

    private String handleInstallmentDueDay(
            Long telegramId,
            String messageText,
            TelegramConversationContext context
    ) {
        Optional<LocalDate> dueDate = telegramInstallmentDueDayResolver.resolve(messageText);

        if (dueDate.isEmpty()) {
            return """
                    Não consegui identificar o dia de vencimento.

                    Me diga algo como:
                    - dia 15
                    - vencimento dia 10
                    """;
        }

        ParsedTelegramMessage completedMessage = withDate(context.parsedMessage(), dueDate.get());
        telegramConversationContextService.clearPendingContext(telegramId);

        return telegramTransactionPreviewHandler.handlePreview(telegramId, completedMessage, false);
    }

    private ParsedTelegramMessage withDate(
            ParsedTelegramMessage parsedMessage,
            LocalDate date
    ) {
        return new ParsedTelegramMessage(
                parsedMessage.intentType(),
                parsedMessage.amount(),
                parsedMessage.description(),
                date,
                parsedMessage.originalMessage(),
                parsedMessage.categoryName(),
                parsedMessage.accountName(),
                parsedMessage.startDate(),
                parsedMessage.endDate(),
                parsedMessage.totalInstallments(),
                parsedMessage.firstRemainingInstallmentNumber(),
                parsedMessage.installmentQueryTarget(),
                parsedMessage.totalAmount()
        );
    }
}
