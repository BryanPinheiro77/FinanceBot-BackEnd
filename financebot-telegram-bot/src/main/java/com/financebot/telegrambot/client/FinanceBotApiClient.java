package com.financebot.telegrambot.client;

import com.financebot.telegrambot.dto.TelegramLinkConfirmRequest;
import com.financebot.telegrambot.dto.TelegramLinkConfirmResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TelegramLinkConfirmRequest> entity = new HttpEntity<>(request, headers);

        return restClient.post()
                .uri("/users/telegram/confirm-link")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelegramLinkConfirmResponse.class);
    }
}