package com.financebot.telegrambot.client;

import com.financebot.telegrambot.dto.request.*;
import com.financebot.telegrambot.dto.response.*;
import com.financebot.telegrambot.observability.CorrelationIds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.financebot.telegrambot.alert.AlertPreferences;
import com.financebot.telegrambot.alert.NotificationDeliveryClaim;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.time.Duration;

@Component
public class FinanceBotApiClient implements com.financebot.telegrambot.reminder.application.port.out.ReminderGateway,
        com.financebot.telegrambot.alert.FinancialNotificationGateway {

    private final RestClient restClient;

    public FinanceBotApiClient(
            @Value("${financebot.api.base-url}") String baseUrl,
            @Value("${financebot.api.internal-token:}") String internalToken
    ) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        requests.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .requestFactory(requests)
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Service-Token", internalToken)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set(CorrelationIds.HEADER_NAME, CorrelationIds.currentOrCreate());
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public NotificationDeliveryClaim claim(String id) {
        return restClient.post().uri("/telegram/financial-notifications/{id}/claim", id)
                .retrieve().body(NotificationDeliveryClaim.class);
    }

    @Override
    public void complete(String id, String token, String outcome) {
        restClient.patch().uri("/telegram/financial-notifications/{id}/delivery", id)
                .body(new DeliveryResult(token, outcome)).retrieve().toBodilessEntity();
    }

    public AlertPreferences getAlertPreferences(Long telegramId) {
        return restClient.get().uri("/telegram/financial-notifications/preferences?telegramId={telegramId}", telegramId)
                .retrieve().body(AlertPreferences.class);
    }

    public AlertPreferences updateAlertPreferences(Long telegramId, AlertPreferences preferences) {
        return restClient.patch().uri("/telegram/financial-notifications/preferences?telegramId={telegramId}", telegramId)
                .body(preferences).retrieve().body(AlertPreferences.class);
    }

    private record DeliveryResult(String token, String outcome) { }

    public void createTransaction(CreateTransactionFromTelegramRequest request) {
        restClient.post()
                .uri("/telegram/transactions")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void createInstallmentTransaction(CreateInstallmentTransactionFromTelegramRequest request) {
        restClient.post()
                .uri("/telegram/transactions/installments")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void createExistingInstallmentTransaction(CreateExistingInstallmentTransactionFromTelegramRequest request) {
        restClient.post()
                .uri("/telegram/transactions/installments/existing")
                .body(request)
                .retrieve()
                .toBodilessEntity();
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

    public MonthlyAmountSummaryResponse getCurrentMonthExpenseSummary(Long telegramId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/telegram/expenses/current-month")
                        .queryParam("telegramId", telegramId)
                        .build())
                .retrieve()
                .body(MonthlyAmountSummaryResponse.class);
    }

    public MonthlyAmountSummaryResponse getCurrentMonthIncomeSummary(Long telegramId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/telegram/income/current-month")
                        .queryParam("telegramId", telegramId)
                        .build())
                .retrieve()
                .body(MonthlyAmountSummaryResponse.class);
    }

    public TelegramTransactionSummaryResponse getTransactionSummary(TelegramTransactionSummaryRequest request) {
        return restClient.post()
                .uri("/telegram/transactions/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelegramTransactionSummaryResponse.class);
    }

    public TelegramDefaultAccountResponse getDefaultAccount(Long telegramId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/telegram/accounts/default")
                        .queryParam("telegramId", telegramId)
                        .build())
                .retrieve()
                .body(TelegramDefaultAccountResponse.class);
    }

    public TelegramInstallmentCountResponse getInstallmentCount(TelegramInstallmentCountRequest request) {
        return restClient.post()
                .uri("/telegram/installments/count")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TelegramInstallmentCountResponse.class);
    }

    public InstallmentPurchaseCapacityResponse getInstallmentPurchaseCapacity(
            InstallmentPurchaseCapacityRequest request
    ) {
        return restClient.post()
                .uri("/telegram/installments/purchase-capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(InstallmentPurchaseCapacityResponse.class);
    }

    public TelegramActiveInstallmentsResponse getActiveInstallments(Long telegramId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/telegram/installments/active")
                        .queryParam("telegramId", telegramId)
                        .build())
                .retrieve()
                .body(TelegramActiveInstallmentsResponse.class);
    }

    public TelegramActiveInstallmentSummaryResponse getActiveInstallmentSummary(Long telegramId, String query) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/telegram/installments/summary")
                            .queryParam("telegramId", telegramId);

                    if (query != null && !query.isBlank()) {
                        builder.queryParam("query", query);
                    }

                    return builder.build();
                })
                .retrieve()
                .body(TelegramActiveInstallmentSummaryResponse.class);
    }

    public void markReminderSent(Long reminderId) {
        restClient.patch()
                .uri("/telegram/reminders/{id}/sent", reminderId)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public boolean isDeliverable(Long reminderId, java.time.LocalDate reminderDate) {
        Boolean deliverable = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/telegram/reminders/{id}/deliverable")
                        .queryParam("reminderDate", reminderDate)
                        .build(reminderId))
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(deliverable);
    }

    public void releaseReminder(Long reminderId) {
        restClient.patch().uri("/telegram/reminders/{id}/release", reminderId).retrieve().toBodilessEntity();
    }

    public void createReminder(Long telegramId, String description, java.time.LocalDate reminderDate, Integer daysBefore) {
        createReminder(telegramId, description, reminderDate, daysBefore, null);
    }

    public void createReminder(Long telegramId, String description, java.time.LocalDate reminderDate,
                               Integer daysBefore, String recurringDescription) {
        restClient.post()
                .uri("/telegram/reminders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateReminderPayload(telegramId, description, reminderDate, daysBefore, recurringDescription))
                .retrieve()
                .toBodilessEntity();
    }

    private record CreateReminderPayload(Long telegramId, String description, java.time.LocalDate reminderDate,
                                         Integer daysBefore, String recurringDescription) {
    }
}
