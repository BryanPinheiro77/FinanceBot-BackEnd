package com.financebot.user.service;

import com.financebot.account.domain.Account;
import com.financebot.account.repository.AccountRepository;
import com.financebot.category.domain.Category;
import com.financebot.category.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserResourceResolver {

    private static final String ACCOUNT_NOT_FOUND_MESSAGE = "Account not found";
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Category not found";

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public Account resolveAccount(Long accountId, Long userId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND_MESSAGE));
    }

    public Category resolveCategory(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserIdAndActiveTrue(categoryId, userId)
                .orElseThrow(() -> new EntityNotFoundException(CATEGORY_NOT_FOUND_MESSAGE));
    }
}