package com.financebot.transaction.service;

import com.financebot.transaction.application.usecase.CreateInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateTransactionUseCase;
import com.financebot.transaction.application.usecase.DeleteTransactionUseCase;
import com.financebot.transaction.application.usecase.FindTransactionByIdUseCase;
import com.financebot.transaction.application.usecase.ListTransactionsUseCase;
import com.financebot.transaction.application.usecase.UpdateTransactionUseCase;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.application.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final CreateInstallmentTransactionUseCase createInstallmentTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;
    private final FindTransactionByIdUseCase findTransactionByIdUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;
    private final UpdateTransactionUseCase updateTransactionUseCase;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request, Authentication authentication) {
        return createTransactionUseCase.execute(request, authentication);
    }

    @Transactional
    public InstallmentTransactionResponse createInstallment(
            CreateInstallmentTransactionRequest request,
            Authentication authentication
    ) {
        return createInstallmentTransactionUseCase.execute(request, authentication);
    }

    @Transactional
    public InstallmentTransactionResponse createInstallmentForUser(
            CreateInstallmentTransactionRequest request,
            User user
    ) {
        return createInstallmentTransactionUseCase.executeForUser(request, user);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findAll(
            TransactionFilter filter,
            Authentication authentication,
            Pageable pageable
    ) {
        return listTransactionsUseCase.execute(filter, authentication, pageable);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id, Authentication authentication) {
        return findTransactionByIdUseCase.execute(id, authentication);
    }

    @Transactional
    public TransactionResponse update(Long id, UpdateTransactionRequest request, Authentication authentication) {
        return updateTransactionUseCase.execute(id, request, authentication);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        deleteTransactionUseCase.execute(id, authentication);
    }
}