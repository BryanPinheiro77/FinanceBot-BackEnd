package com.financebot.telegram.controller;

import com.financebot.telegram.dto.request.*;
import com.financebot.telegram.dto.response.*;
import com.financebot.telegram.service.TelegramIntegrationService;
import com.financebot.user.dto.request.UpdateMonthlyBaseIncomeRequest;
import com.financebot.user.dto.response.TelegramUserProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramIntegrationControllerTest {
    @Mock TelegramIntegrationService service;
    @InjectMocks TelegramIntegrationController controller;

    @Test
    void shouldDelegateAllTelegramEndpoints() {
        TelegramUserProfileResponse profile = mock(TelegramUserProfileResponse.class);
        when(service.getMe(1L)).thenReturn(profile);
        when(service.updateMonthlyBaseIncome(eq(1L), any())).thenReturn(profile);
        when(service.getFinancialAnalysis(1L)).thenReturn(mock(com.financebot.analysis.dto.response.FinancialCommitmentResponse.class));
        when(service.getCurrentMonthExpenseSummary(1L)).thenReturn(mock(MonthlyAmountSummaryResponse.class));
        when(service.getCurrentMonthIncomeSummary(1L)).thenReturn(mock(MonthlyAmountSummaryResponse.class));
        when(service.getTransactionSummary(any())).thenReturn(mock(TelegramTransactionSummaryResponse.class));
        when(service.getDefaultAccount(1L)).thenReturn(mock(TelegramDefaultAccountResponse.class));
        when(service.getInstallmentCount(any())).thenReturn(mock(TelegramInstallmentCountResponse.class));
        when(service.analyzeInstallmentPurchaseCapacity(any())).thenReturn(mock(com.financebot.analysis.dto.response.InstallmentPurchaseCapacityResponse.class));
        when(service.getActiveInstallments(1L)).thenReturn(mock(TelegramActiveInstallmentsResponse.class));
        when(service.getActiveInstallmentSummary(eq(1L), eq("cafe"))).thenReturn(mock(TelegramActiveInstallmentSummaryResponse.class));

        controller.getMe(1L);
        controller.updateMonthlyBaseIncome(1L, mock(UpdateMonthlyBaseIncomeRequest.class));
        controller.disconnectTelegram(1L);
        controller.getFinancialAnalysis(1L);
        controller.getCurrentMonthExpenseSummary(1L);
        controller.getCurrentMonthIncomeSummary(1L);
        controller.createTransaction(mock(CreateTransactionFromTelegramRequest.class));
        controller.createInstallmentTransaction(mock(CreateInstallmentTransactionFromTelegramRequest.class));
        controller.createExistingInstallmentTransaction(mock(CreateExistingInstallmentTransactionFromTelegramRequest.class));
        controller.getTransactionSummary(mock(TelegramTransactionSummaryRequest.class));
        controller.getDefaultAccount(1L);
        controller.getInstallmentCount(mock(TelegramInstallmentCountRequest.class));
        controller.analyzeInstallmentPurchaseCapacity(mock(InstallmentPurchaseCapacityRequest.class));
        controller.getActiveInstallments(1L);
        controller.getActiveInstallmentSummary(1L, "cafe");

        verify(service).getMe(1L);
        verify(service).disconnectTelegram(1L);
        verify(service).getFinancialAnalysis(1L);
        verify(service).getCurrentMonthExpenseSummary(1L);
        verify(service).getCurrentMonthIncomeSummary(1L);
        verify(service).getDefaultAccount(1L);
        verify(service).getActiveInstallments(1L);
        verify(service).getActiveInstallmentSummary(1L, "cafe");
        verify(service).createTransactionFromTelegram(any());
        verify(service).createInstallmentTransactionFromTelegram(any());
        verify(service).createExistingInstallmentTransactionFromTelegram(any());
        verify(service).getTransactionSummary(any());
        verify(service).getInstallmentCount(any());
        verify(service).analyzeInstallmentPurchaseCapacity(any());
    }
}
