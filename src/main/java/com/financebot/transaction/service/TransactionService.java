package com.financebot.transaction.service;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.application.usecase.CreateInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateTransactionUseCase;
import com.financebot.transaction.application.usecase.FindTransactionByIdUseCase;
import com.financebot.transaction.application.usecase.ListTransactionsUseCase;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.application.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import com.financebot.user.service.UserResourceResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found";

    private final TransactionRepository transactionRepository;
    private final UserResourceResolver userResourceResolver;
    private final TransactionMapper transactionMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TransactionCategoryValidator transactionCategoryValidator;

    private final CreateTransactionUseCase createTransactionUseCase;
    private final CreateInstallmentTransactionUseCase createInstallmentTransactionUseCase;
    private final ListTransactionsUseCase listTransactionsUseCase;
    private final FindTransactionByIdUseCase findTransactionByIdUseCase;

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
        User user = authenticatedUserResolver.resolve(authentication);

        Transaction transaction = getUserTransaction(id, user.getId());
        Account account = userResourceResolver.resolveAccount(request.accountId(), user.getId());
        Category category = userResourceResolver.resolveCategory(request.categoryId(), user.getId());

        transactionCategoryValidator.validate(category, request.type());

        transactionMapper.updateEntity(request, transaction);
        transaction.setAccount(account);
        transaction.setCategory(category);

        Transaction updatedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toResponse(updatedTransaction);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);

        Transaction transaction = getUserTransaction(id, user.getId());

        transactionRepository.delete(transaction);
    }

    private Transaction getUserTransaction(Long transactionId, Long userId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new EntityNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE));
    }
}