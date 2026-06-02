package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.dto.request.CreateInstallmentTransactionFromTelegramRequest;
import com.financebot.telegrambot.dto.request.CreateTransactionFromTelegramRequest;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class TelegramPendingOperationHandler {

    private final FinanceBotApiClient financeBotApiClient;
    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramPendingQueryService telegramPendingQueryService;
    private final TelegramMessageFormatter telegramMessageFormatter;
    private final TelegramBotErrorMapper telegramBotErrorMapper;

    public String handleConfirmation(Long telegramId) {
        PendingTelegramTransaction pending = telegramPendingConfirmationService.getPending(telegramId);

        if (pending == null) {
            return "Não há nenhuma operação pendente para confirmar.";
        }

        try {
            if (pending.isInstallment()) {
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
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return """
                    Não foi possível salvar sua transação agora.
                    Você pode tentar confirmar novamente em instantes.
                    """;
        }
    }

    public String handleCancellation(Long telegramId) {
        boolean hasPendingConfirmation = telegramPendingConfirmationService.hasPending(telegramId);
        boolean hasPendingQuery = telegramPendingQueryService.hasPending(telegramId);

        if (!hasPendingConfirmation && !hasPendingQuery) {
            return "Não há nenhuma operação pendente para cancelar.";
        }

        telegramPendingConfirmationService.clearPending(telegramId);
        telegramPendingQueryService.clearPending(telegramId);

        return "❌ Operação cancelada com sucesso.";
    }

    private String mapIntentToTransactionType(TelegramIntentType intentType) {
        return switch (intentType) {
            case CREATE_EXPENSE, CREATE_INSTALLMENT_EXPENSE -> "EXPENSE";
            case CREATE_INCOME -> "INCOME";
            default -> throw new IllegalArgumentException("Intento inválido para criação de transação.");
        };
    }
}