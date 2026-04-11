package com.financebot.telegram.controller;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.telegram.dto.*;
import com.financebot.telegram.service.TelegramIntegrationService;
import com.financebot.user.dto.response.TelegramUserProfileResponse;
import com.financebot.user.dto.request.UpdateMonthlyBaseIncomeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public class TelegramIntegrationController {

    private final TelegramIntegrationService telegramIntegrationService;

    @GetMapping("/users/me")
    public TelegramUserProfileResponse getMe(@RequestParam Long telegramId) {
        return telegramIntegrationService.getMe(telegramId);
    }

    @PatchMapping("/users/me/monthly-base-income")
    public TelegramUserProfileResponse updateMonthlyBaseIncome(
            @RequestParam Long telegramId,
            @RequestBody @Valid UpdateMonthlyBaseIncomeRequest request
    ) {
        return telegramIntegrationService.updateMonthlyBaseIncome(telegramId, request);
    }

    @DeleteMapping("/users/me/link")
    public void disconnectTelegram(@RequestParam Long telegramId) {
        telegramIntegrationService.disconnectTelegram(telegramId);
    }

    @GetMapping("/financial-analysis")
    public FinancialCommitmentResponse getFinancialAnalysis(@RequestParam Long telegramId) {
        return telegramIntegrationService.getFinancialAnalysis(telegramId);
    }

    @GetMapping("/expenses/current-month")
    public MonthlyAmountSummaryResponse getCurrentMonthExpenseSummary(@RequestParam Long telegramId) {
        return telegramIntegrationService.getCurrentMonthExpenseSummary(telegramId);
    }

    @GetMapping("/income/current-month")
    public MonthlyAmountSummaryResponse getCurrentMonthIncomeSummary(@RequestParam Long telegramId) {
        return telegramIntegrationService.getCurrentMonthIncomeSummary(telegramId);
    }

    @PostMapping("/transactions")
    public void createTransaction(@RequestBody CreateTransactionFromTelegramRequest request) {
        telegramIntegrationService.createTransactionFromTelegram(request);
    }

    @PostMapping("/transactions/summary")
    public TelegramTransactionSummaryResponse getTransactionSummary(
            @RequestBody TelegramTransactionSummaryRequest request
    ) {
        return telegramIntegrationService.getTransactionSummary(request);
    }

    @GetMapping("/accounts/default")
    public TelegramDefaultAccountResponse getDefaultAccount(@RequestParam Long telegramId) {
        return telegramIntegrationService.getDefaultAccount(telegramId);
    }

    @PostMapping("/installments/count")
    public TelegramInstallmentCountResponse getInstallmentCount(
            @RequestBody TelegramInstallmentCountRequest request
    ) {
        return telegramIntegrationService.getInstallmentCount(request);
    }

    @GetMapping("/installments/active")
    public TelegramActiveInstallmentsResponse getActiveInstallments(
            @RequestParam Long telegramId
    ) {
        return telegramIntegrationService.getActiveInstallments(telegramId);
    }

    @GetMapping("/installments/summary")
    public TelegramActiveInstallmentSummaryResponse getActiveInstallmentSummary(
            @RequestParam Long telegramId
    ) {
        return telegramIntegrationService.getActiveInstallmentSummary(telegramId);
    }
}
