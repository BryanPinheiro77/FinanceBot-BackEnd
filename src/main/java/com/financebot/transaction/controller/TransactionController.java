package com.financebot.transaction.controller;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.dto.response.InstallmentTransactionCreationResponse;
import com.financebot.analysis.dto.response.TransactionCreationResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final FinancialAnalysisService financialAnalysisService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionCreationResponse create(
            @RequestBody @Valid CreateTransactionRequest request,
            Authentication authentication
    ) {
        TransactionResponse transaction = transactionService.create(request, authentication);
        FinancialCommitmentResponse analysis = financialAnalysisService.getFinancialCommitment(authentication);

        return new TransactionCreationResponse(transaction, analysis);
    }

    @PostMapping("/installments")
    @ResponseStatus(HttpStatus.CREATED)
    public InstallmentTransactionCreationResponse createInstallment(
            @RequestBody @Valid CreateInstallmentTransactionRequest request,
            Authentication authentication
    ) {
        InstallmentTransactionResponse installment =
                transactionService.createInstallment(request, authentication);

        FinancialCommitmentResponse analysis = financialAnalysisService.getFinancialCommitment(authentication);

        return new InstallmentTransactionCreationResponse(installment, analysis);
    }

    @GetMapping
    public Page<TransactionResponse> findAll(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(required = false) String description,
            Authentication authentication,
            Pageable pageable
    ) {
        TransactionFilter filter = new TransactionFilter(
                type,
                categoryId,
                accountId,
                startDate,
                endDate,
                sourceType,
                description
        );

        return transactionService.findAll(filter, authentication, pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return transactionService.findById(id, authentication);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateTransactionRequest request,
            Authentication authentication
    ) {
        return transactionService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        transactionService.delete(id, authentication);
    }
}