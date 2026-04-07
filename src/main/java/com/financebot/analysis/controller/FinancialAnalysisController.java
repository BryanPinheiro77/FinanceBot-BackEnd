package com.financebot.analysis.controller;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.transaction.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.dto.request.CreateTransactionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class FinancialAnalysisController {

    private final FinancialAnalysisService financialAnalysisService;

    @GetMapping("/financial-commitment")
    public FinancialCommitmentResponse getFinancialCommitment(Authentication authentication) {
        return financialAnalysisService.getFinancialCommitment(authentication);
    }

    @PostMapping("/financial-commitment/precheck/transaction")
    public FinancialCommitmentResponse previewTransactionAlert(
            @RequestBody @Valid CreateTransactionRequest request,
            Authentication authentication
    ) {
        return financialAnalysisService.previewTransactionAlert(request, authentication);
    }

    @PostMapping("/financial-commitment/precheck/installment")
    public FinancialCommitmentResponse previewInstallmentAlert(
            @RequestBody @Valid CreateInstallmentTransactionRequest request,
            Authentication authentication
    ) {
        return financialAnalysisService.previewInstallmentAlert(request, authentication);
    }
}