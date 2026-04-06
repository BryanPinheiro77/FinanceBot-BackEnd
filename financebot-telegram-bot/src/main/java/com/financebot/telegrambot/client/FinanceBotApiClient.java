package com.financebot.telegrambot.client;

import com.financebot.telegrambot.dto.FinancialCommitmentResponse;
import com.financebot.telegrambot.dto.TelegramLinkConfirmRequest;
import com.financebot.telegrambot.dto.TelegramLinkConfirmResponse;
import com.financebot.telegrambot.dto.UpdateMonthlyBaseIncomeRequest;
import com.financebot.telegrambot.dto.UserProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FinanceBotApiClient {

    private final RestClient restClient;

    public FinanceBotApiClient(@Value("${financebot.api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public TelegramLinkConfirmResponse confirmTelegramLink(TelegramLinkConfirmRequest request) {
        return restClient.post()
                .uri("/users/telegram/confirm-link")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelegramLinkConfirmResponse.class);
    }

    public UserProfileResponse getMe(Long telegramId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/telegram/users/me")
                        .queryParam("telegramId", telegramId)
                        .build())
                .retrieve()
                .body(UserProfileResponse.class);
    }

    public UserProfileResponse updateMonthlyBaseIncome(Long telegramId, UpdateMonthlyBaseIncomeRequest request) {
        return restClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/telegram/users/me/monthly-base-income")
                        .queryParam("telegramId", telegramId)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(UserProfileResponse.class);
    }

    public FinancialCommitmentResponse getFinancialAnalysis(Long telegramId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/telegram/financial-analysis")
                        .queryParam("telegramId", telegramId)
                        .build())
                .retrieve()
                .body(FinancialCommitmentResponse.class);
    }

    public void disconnectTelegram(Long telegramId) {
        restClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/telegram/users/me/link")
                        .queryParam("telegramId", telegramId)
                        .build())
                .retrieve()
                .toBodilessEntity();
    }
}