package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.service.TelegramIntentService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramPendingQueryHandler {

    private final TelegramIntentService telegramIntentService;
    private final TelegramPendingQueryService telegramPendingQueryService;
    private final TelegramFinancialQueryHandler telegramFinancialQueryHandler;

    public boolean hasPendingInstallmentQuery(Long telegramId, String messageText) {
        ParsedTelegramMessage pendingQuery = telegramPendingQueryService.getPending(telegramId);

        return pendingQuery != null
                && pendingQuery.intentType() != null
                && pendingQuery.intentType().name().startsWith("QUERY_INSTALLMENT_")
                && !messageText.startsWith("/");
    }

    public String handlePendingInstallmentQuerySelection(
            Long telegramId,
            String messageText
    ) {
        ParsedTelegramMessage pending = telegramPendingQueryService.getPending(telegramId);

        if (pending == null) {
            return "Não há nenhuma consulta pendente para continuar.";
        }

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
                pending.firstRemainingInstallmentNumber(),
                selectedTarget,
                pending.totalAmount(),
                pending.monthlyAmount()
        );

        telegramPendingQueryService.clearPending(telegramId);

        return telegramFinancialQueryHandler.handleQuery(updated, telegramId);
    }
}