package com.financebot.transaction.service;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.application.usecase.CreateTransactionUseCase;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.installment.InstallmentPlan;
import com.financebot.transaction.domain.installment.InstallmentPlanFactory;
import com.financebot.transaction.domain.installment.InstallmentPlanItem;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.application.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.specification.TransactionSpecification;
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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found";
    private static final String INVALID_PERIOD_MESSAGE = "Start date cannot be after end date";

    private final TransactionRepository transactionRepository;
    private final UserResourceResolver userResourceResolver;
    private final TransactionMapper transactionMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TransactionCategoryValidator transactionCategoryValidator;
    private final InstallmentPlanFactory installmentPlanFactory;

    private final CreateTransactionUseCase createTransactionUseCase;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request, Authentication authentication) {
        return createTransactionUseCase.execute(request, authentication);
    }

    @Transactional
    public InstallmentTransactionResponse createInstallment(
            CreateInstallmentTransactionRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.resolve(authentication);

        return createInstallmentInternal(request, user);
    }

    @Transactional
    public InstallmentTransactionResponse createInstallmentForUser(
            CreateInstallmentTransactionRequest request,
            User user
    ) {
        return createInstallmentInternal(request, user);
    }

    private InstallmentTransactionResponse createInstallmentInternal(
            CreateInstallmentTransactionRequest request,
            User user
    ) {
        InstallmentPlan plan = installmentPlanFactory.create(
                request.totalAmount(),
                request.description(),
                request.firstInstallmentDate(),
                request.type(),
                request.totalInstallments()
        );

        Account account = userResourceResolver.resolveAccount(request.accountId(), user.getId());
        Category category = userResourceResolver.resolveCategory(request.categoryId(), user.getId());

        transactionCategoryValidator.validate(category, request.type());

        List<Transaction> transactions = plan.items().stream()
                .map(item -> buildInstallmentTransaction(
                        item,
                        request,
                        user,
                        account,
                        category
                ))
                .toList();

        List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);

        List<TransactionResponse> responses = savedTransactions.stream()
                .map(transactionMapper::toResponse)
                .toList();

        return new InstallmentTransactionResponse(
                plan.installmentGroupId(),
                plan.totalInstallments(),
                responses
        );
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findAll(
            TransactionFilter filter,
            Authentication authentication,
            Pageable pageable
    ) {
        User user = authenticatedUserResolver.resolve(authentication);

        validatePeriod(filter.startDate(), filter.endDate());

        return transactionRepository.findAll(
                        TransactionSpecification.withFilters(user.getId(), filter),
                        pageable
                )
                .map(transactionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);

        Transaction transaction = getUserTransaction(id, user.getId());

        return transactionMapper.toResponse(transaction);
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

    private Transaction buildInstallmentTransaction(
            InstallmentPlanItem item,
            CreateInstallmentTransactionRequest request,
            User user,
            Account account,
            Category category
    ) {
        Transaction transaction = new Transaction();

        transaction.setAmount(item.amount());
        transaction.setDescription(item.description());
        transaction.setDate(item.date());
        transaction.setType(request.type());
        transaction.setSourceType(request.sourceType());
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setInstallment(true);
        transaction.setInstallmentNumber(item.installmentNumber());
        transaction.setTotalInstallments(item.totalInstallments());
        transaction.setInstallmentGroupId(item.installmentGroupId());

        return transaction;
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(INVALID_PERIOD_MESSAGE);
        }
    }

    private Transaction getUserTransaction(Long transactionId, Long userId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new EntityNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE));
    }
}