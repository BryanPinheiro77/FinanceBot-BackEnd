package com.financebot.telegrambot.support;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.dto.response.TelegramDefaultAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class TelegramPreviewAccountResolver {

    private final FinanceBotApiClient financeBotApiClient;

    public ResolvedPreviewAccount resolve(PendingTelegramTransaction pendingTransaction, Long telegramId) {
        if (pendingTransaction.accountName() != null && !pendingTransaction.accountName().isBlank()) {
            return new ResolvedPreviewAccount(
                    pendingTransaction.accountName(),
                    pendingTransaction.accountName()
            );
        }

        try {
            TelegramDefaultAccountResponse response = financeBotApiClient.getDefaultAccount(telegramId);

            if (response != null && response.accountName() != null && !response.accountName().isBlank()) {
                return new ResolvedPreviewAccount(
                        response.accountName(),
                        response.accountName()
                );
            }
        } catch (RestClientResponseException e) {
            return new ResolvedPreviewAccount("conta padrão", null);
        } catch (Exception e) {
            return new ResolvedPreviewAccount("conta padrão", null);
        }

        return new ResolvedPreviewAccount("conta padrão", null);
    }

    public record ResolvedPreviewAccount(
            String displayName,
            String persistedName
    ) {
    }
}