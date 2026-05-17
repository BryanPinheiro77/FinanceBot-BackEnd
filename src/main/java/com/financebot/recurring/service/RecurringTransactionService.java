package com.financebot.recurring.service;

import com.financebot.account.domain.Account;
import com.financebot.account.repository.AccountRepository;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.dto.request.CreateRecurringTransactionRequest;
import com.financebot.recurring.dto.request.UpdateRecurringTransactionRequest;
import com.financebot.recurring.dto.response.RecurringTransactionResponse;
import com.financebot.recurring.mapper.RecurringTransactionMapper;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.financebot.user.service.AuthenticatedUserResolver;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final RecurringTransactionMapper recurringTransactionMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Transactional
    public RecurringTransactionResponse create(
            CreateRecurringTransactionRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.resolve(authentication);
        Account account = getUserAccount(request.accountId(), user.getId());
        Category category = getUserCategory(request.categoryId(), user.getId());

        validateCategoryMatchesTransactionType(category, request.type());

        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setDescription(request.description().trim());
        recurringTransaction.setAmount(request.amount());
        recurringTransaction.setType(request.type());
        recurringTransaction.setSourceType(request.sourceType());
        recurringTransaction.setFrequency(request.frequency());
        recurringTransaction.setStartDate(request.startDate());
        recurringTransaction.setEndDate(request.endDate());
        recurringTransaction.setNextExecutionDate(request.startDate());
        recurringTransaction.setActive(true);
        recurringTransaction.setUser(user);
        recurringTransaction.setAccount(account);
        recurringTransaction.setCategory(category);

        recurringTransaction.validateDates();

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return recurringTransactionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> findAll(Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);

        return recurringTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(recurringTransactionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse findById(Long id, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);
        RecurringTransaction recurringTransaction = getUserRecurringTransaction(id, user.getId());

        return recurringTransactionMapper.toResponse(recurringTransaction);
    }

    @Transactional
    public RecurringTransactionResponse update(
            Long id,
            UpdateRecurringTransactionRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.resolve(authentication);
        RecurringTransaction recurringTransaction = getUserRecurringTransaction(id, user.getId());

        Account account = getUserAccount(request.accountId(), user.getId());
        Category category = getUserCategory(request.categoryId(), user.getId());

        validateCategoryMatchesTransactionType(category, request.type());

        recurringTransaction.setDescription(request.description().trim());
        recurringTransaction.setAmount(request.amount());
        recurringTransaction.setType(request.type());
        recurringTransaction.setSourceType(request.sourceType());
        recurringTransaction.setFrequency(request.frequency());
        recurringTransaction.setStartDate(request.startDate());
        recurringTransaction.setEndDate(request.endDate());
        recurringTransaction.setAccount(account);
        recurringTransaction.setCategory(category);
        recurringTransaction.setActive(request.active());

        if (recurringTransaction.getNextExecutionDate() == null
                || recurringTransaction.getNextExecutionDate().isBefore(request.startDate())) {
            recurringTransaction.setNextExecutionDate(request.startDate());
        }

        recurringTransaction.validateDates();

        RecurringTransaction updated = recurringTransactionRepository.save(recurringTransaction);
        return recurringTransactionMapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);
        RecurringTransaction recurringTransaction = getUserRecurringTransaction(id, user.getId());

        recurringTransactionRepository.delete(recurringTransaction);
    }

    @Transactional
    public RecurringTransactionResponse activate(Long id, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);
        RecurringTransaction recurringTransaction = getUserRecurringTransaction(id, user.getId());

        recurringTransaction.setActive(true);

        if (recurringTransaction.getNextExecutionDate() == null) {
            recurringTransaction.setNextExecutionDate(recurringTransaction.getStartDate());
        }

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return recurringTransactionMapper.toResponse(saved);
    }

    @Transactional
    public RecurringTransactionResponse deactivate(Long id, Authentication authentication) {
        User user = authenticatedUserResolver.resolve(authentication);
        RecurringTransaction recurringTransaction = getUserRecurringTransaction(id, user.getId());

        recurringTransaction.setActive(false);

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return recurringTransactionMapper.toResponse(saved);
    }

    private RecurringTransaction getUserRecurringTransaction(Long id, Long userId) {
        return recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Recurring transaction not found"));
    }

    private Account getUserAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    private Category getUserCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
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
}