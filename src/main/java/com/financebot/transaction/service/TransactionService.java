package com.financebot.transaction.service;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.dto.request.CreateTransactionRequest;
import com.financebot.transaction.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.dto.response.TransactionResponse;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.specification.TransactionSpecification;
import com.financebot.user.domain.User;
import com.financebot.user.service.UserResourceResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.financebot.user.service.AuthenticatedUserResolver;
import com.financebot.transaction.validation.TransactionCategoryValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found";
    private static final String INSTALLMENT_ONLY_EXPENSE_MESSAGE =
            "Installment transactions are allowed only for expenses";
    private static final String TOTAL_INSTALLMENTS_MINIMUM_MESSAGE = "Total installments must be at least 2";
    private static final String INVALID_PERIOD_MESSAGE = "Start date cannot be after end date";

    private final TransactionRepository transactionRepository;
    private final UserResourceResolver userResourceResolver;
    private final TransactionMapper transactionMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TransactionCategoryValidator transactionCategoryValidator;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);

        Account account = userResourceResolver.resolveAccount(request.accountId(), user.getId());
        Category category = userResourceResolver.resolveCategory(request.categoryId(), user.getId());

        transactionCategoryValidator.validate(category, request.type());

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setInstallment(false);
        transaction.setInstallmentNumber(null);
        transaction.setTotalInstallments(null);
        transaction.setInstallmentGroupId(null);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toResponse(savedTransaction);
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
        validateInstallmentRequest(request);

        Account account = userResourceResolver.resolveAccount(request.accountId(), user.getId());
        Category category = userResourceResolver.resolveCategory(request.categoryId(), user.getId());

        transactionCategoryValidator.validate(category, request.type());

        String installmentGroupId = UUID.randomUUID().toString();
        int totalInstallments = request.totalInstallments();
        BigDecimal totalAmount = request.totalAmount();

        BigDecimal installmentAmount = totalAmount.divide(
                BigDecimal.valueOf(totalInstallments),
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal accumulated = BigDecimal.ZERO;
        List<Transaction> transactions = new ArrayList<>();

        InstallmentTransactionContext context = new InstallmentTransactionContext(
                request,
                user,
                account,
                category,
                installmentGroupId,
                totalInstallments
        );

        for (int i = 1; i <= totalInstallments; i++) {
            BigDecimal currentAmount = calculateCurrentInstallmentAmount(
                    i,
                    totalInstallments,
                    installmentAmount,
                    totalAmount,
                    accumulated
            );

            if (i < totalInstallments) {
                accumulated = accumulated.add(currentAmount);
            }

            transactions.add(buildInstallmentTransaction(
                    context,
                    i,
                    currentAmount
            ));
        }

        List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);

        List<TransactionResponse> responses = savedTransactions.stream()
                .map(transactionMapper::toResponse)
                .toList();

        return new InstallmentTransactionResponse(
                installmentGroupId,
                totalInstallments,
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

    private void validateInstallmentRequest(CreateInstallmentTransactionRequest request) {
        if (request.type() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException(INSTALLMENT_ONLY_EXPENSE_MESSAGE);
        }

        if (request.totalInstallments() == null || request.totalInstallments() < 2) {
            throw new IllegalArgumentException(TOTAL_INSTALLMENTS_MINIMUM_MESSAGE);
        }
    }

    private BigDecimal calculateCurrentInstallmentAmount(
            int currentInstallment,
            int totalInstallments,
            BigDecimal installmentAmount,
            BigDecimal totalAmount,
            BigDecimal accumulated
    ) {
        if (currentInstallment < totalInstallments) {
            return installmentAmount;
        }

        return totalAmount.subtract(accumulated);
    }

    private Transaction buildInstallmentTransaction(
            InstallmentTransactionContext context,
            int installmentNumber,
            BigDecimal currentAmount
    ) {
        Transaction transaction = new Transaction();
        transaction.setAmount(currentAmount);
        transaction.setDescription(
                context.request().description().trim()
                        + " - "
                        + installmentNumber
                        + "/"
                        + context.totalInstallments()
        );
        transaction.setDate(context.request().firstInstallmentDate().plusMonths((long) installmentNumber - 1));
        transaction.setType(context.request().type());
        transaction.setSourceType(context.request().sourceType());
        transaction.setUser(context.user());
        transaction.setAccount(context.account());
        transaction.setCategory(context.category());
        transaction.setInstallment(true);
        transaction.setInstallmentNumber(installmentNumber);
        transaction.setTotalInstallments(context.totalInstallments());
        transaction.setInstallmentGroupId(context.installmentGroupId());

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

    private record InstallmentTransactionContext(
            CreateInstallmentTransactionRequest request,
            User user,
            Account account,
            Category category,
            String installmentGroupId,
            int totalInstallments
    ) {
    }
}