package com.financebot.alert.adapter;

import com.financebot.alert.application.AlertPreferences;
import com.financebot.alert.service.FinancialNotificationService;
import com.financebot.alert.application.NotificationDeliveryClaim;
import com.financebot.alert.domain.NotificationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telegram/financial-notifications")
@RequiredArgsConstructor
public class TelegramFinancialNotificationController {
    private final FinancialNotificationService service;

    @PostMapping("/{id}/claim")
    public ResponseEntity<NotificationDeliveryClaim> claim(@PathVariable String id) {
        NotificationDeliveryClaim claim = service.claim(id);
        return claim == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(claim);
    }

    @PatchMapping("/{id}/delivery")
    public void complete(@PathVariable String id, @RequestBody @Valid DeliveryResult result) {
        service.complete(id, result.token(), result.outcome());
    }

    @GetMapping("/preferences")
    public AlertPreferences preferences(@RequestParam Long telegramId) {
        return service.preferences(telegramId);
    }

    @PatchMapping("/preferences")
    public AlertPreferences updatePreferences(@RequestParam Long telegramId, @RequestBody @Valid PreferencesRequest request) {
        return service.updatePreferences(telegramId,
                new AlertPreferences(request.alerts(), request.weeklySummary(), request.monthlySummary()));
    }

    public record DeliveryResult(@NotBlank String token, @NotNull NotificationStatus outcome) { }
    public record PreferencesRequest(@NotNull Boolean alerts, @NotNull Boolean weeklySummary, @NotNull Boolean monthlySummary) { }
}
