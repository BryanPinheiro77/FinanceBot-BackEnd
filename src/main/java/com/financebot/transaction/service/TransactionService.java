package com.financebot.transaction.service;

import com.financebot.account.domain.Account;
import com.financebot.account.repository.AccountRepository;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.dto.request.CreateTransactionRequest;
import com.financebot.transaction.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.dto.response.TransactionResponse;
import com.financebot.transaction.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.specification.TransactionSpecification;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Account account = getUserAccount(request.accountId(), user.getId());
        Category category = getUserCategory(request.categoryId(), user.getId());

        validateCategoryMatchesTransactionType(category, request.type());

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
        User user = getAuthenticatedUser(authentication);

        if (request.type() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Installment transactions are allowed only for expenses");
        }

        if (request.totalInstallments() == null || request.totalInstallments() < 2) {
            throw new IllegalArgumentException("Total installments must be at least 2");
        }

        Account account = getUserAccount(request.accountId(), user.getId());
        Category category = getUserCategory(request.categoryId(), user.getId());

        validateCategoryMatchesTransactionType(category, request.type());

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

        for (int i = 1; i <= totalInstallments; i++) {
            BigDecimal currentAmount = installmentAmount;

            if (i < totalInstallments) {
                accumulated = accumulated.add(currentAmount);
            } else {
                currentAmount = totalAmount.subtract(accumulated);
            }

            Transaction transaction = new Transaction();
            transaction.setAmount(currentAmount);
            transaction.setDescription(request.description().trim() + " - " + i + "/" + totalInstallments);
            transaction.setDate(request.firstInstallmentDate().plusMonths(i - 1));
            transaction.setType(request.type());
            transaction.setSourceType(request.sourceType());
            transaction.setUser(user);
            transaction.setAccount(account);
            transaction.setCategory(category);
            transaction.setInstallment(true);
            transaction.setInstallmentNumber(i);
            transaction.setTotalInstallments(totalInstallments);
            transaction.setInstallmentGroupId(installmentGroupId);

            transactions.add(transaction);
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
        User user = getAuthenticatedUser(authentication);

        validatePeriod(filter.startDate(), filter.endDate());

        return transactionRepository.findAll(
                        TransactionSpecification.withFilters(user.getId(), filter),
                        pageable
                )
                .map(transactionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Transaction transaction = getUserTransaction(id, user.getId());
        return transactionMapper.toResponse(transaction);
    }

    @Transactional
    public TransactionResponse update(Long id, UpdateTransactionRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Transaction transaction = getUserTransaction(id, user.getId());
        Account account = getUserAccount(request.accountId(), user.getId());
        Category category = getUserCategory(request.categoryId(), user.getId());

        validateCategoryMatchesTransactionType(category, request.type());

        transactionMapper.updateEntity(request, transaction);
        transaction.setAccount(account);
        transaction.setCategory(category);

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toResponse(updatedTransaction);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Transaction transaction = getUserTransaction(id, user.getId());
        transactionRepository.delete(transaction);
    }

    private void validateCategoryMatchesTransactionType(Category category, TransactionType transactionType) {
        boolean isIncomeMatch =
                category.getType() == CategoryType.INCOME && transactionType == TransactionType.INCOME;

        boolean isExpenseMatch =
                category.getType() == CategoryType.EXPENSE && transactionType == TransactionType.EXPENSE;

        if (!isIncomeMatch && !isExpenseMatch) {
            throw new IllegalArgumentException("Category type does not match transaction type");
        }
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    private Transaction getUserTransaction(Long transactionId, Long userId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
    }

    private Account getUserAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    private Category getUserCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is invalid");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }
}