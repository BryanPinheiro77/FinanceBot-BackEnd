package com.financebot.transaction.adapter.in.web;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.dto.response.InstallmentTransactionCreationResponse;
import com.financebot.analysis.dto.response.TransactionCreationResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.transaction.application.command.CreateInstallmentTransactionCommand;
import com.financebot.transaction.application.command.CreateTransactionCommand;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.application.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.usecase.CreateInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateTransactionUseCase;
import com.financebot.transaction.application.usecase.DeleteTransactionUseCase;
import com.financebot.transaction.application.usecase.FindTransactionByIdUseCase;
import com.financebot.transaction.application.usecase.ListTransactionsUseCase;
import com.financebot.transaction.application.usecase.UpdateTransactionUseCase;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
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

    private final FinancialAnalysisService financialAnalysisService;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TransactionMapper transactionMapper;

    private final CreateTransactionUseCase createTransactionUseCase;
    private final CreateInstallmentTransactionUseCase createInstallmentTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;
    private final FindTransactionByIdUseCase findTransactionByIdUseCase;
    private final UpdateTransactionUseCase updateTransactionUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionCreationResponse create(
            @RequestBody @Valid CreateTransactionRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.resolve(authentication);
        CreateTransactionCommand command = transactionMapper.toCommand(request, user);

        TransactionResponse transaction = createTransactionUseCase.execute(command);
        FinancialCommitmentResponse analysis = financialAnalysisService.getFinancialCommitment(authentication);

        return new TransactionCreationResponse(transaction, analysis);
    }

    @PostMapping("/installments")
    @ResponseStatus(HttpStatus.CREATED)
    public InstallmentTransactionCreationResponse createInstallment(
            @RequestBody @Valid CreateInstallmentTransactionRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.resolve(authentication);
        CreateInstallmentTransactionCommand command = transactionMapper.toCommand(request, user);

        InstallmentTransactionResponse installment =
                createInstallmentTransactionUseCase.execute(command);

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

        return listTransactionsUseCase.execute(filter, authentication, pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return findTransactionByIdUseCase.execute(id, authentication);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateTransactionRequest request,
            Authentication authentication
    ) {
        return updateTransactionUseCase.execute(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        deleteTransactionUseCase.execute(id, authentication);
    }
}