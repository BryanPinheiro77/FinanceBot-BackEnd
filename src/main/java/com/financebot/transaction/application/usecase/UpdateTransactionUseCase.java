package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.application.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import com.financebot.user.service.UserResourceResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateTransactionUseCase {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found";

    private final TransactionRepository transactionRepository;
    private final UserResourceResolver userResourceResolver;
    private final TransactionMapper transactionMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TransactionCategoryValidator transactionCategoryValidator;

    @Transactional
    public TransactionResponse execute(
            Long id,
            UpdateTransactionRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.resolve(authentication);

        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE));

        Account account = userResourceResolver.resolveAccount(request.accountId(), user.getId());
        Category category = userResourceResolver.resolveCategory(request.categoryId(), user.getId());

        transactionCategoryValidator.validate(category, request.type());

        transactionMapper.updateEntity(request, transaction);
        transaction.setAccount(account);
        transaction.setCategory(category);

        Transaction updatedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toResponse(updatedTransaction);
    }
}