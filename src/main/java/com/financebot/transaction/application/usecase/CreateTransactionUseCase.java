package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.SaveTransactionPort;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import com.financebot.user.service.UserResourceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateTransactionUseCase {

    private final SaveTransactionPort saveTransactionPort;
    private final UserResourceResolver userResourceResolver;
    private final TransactionMapper transactionMapper;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TransactionCategoryValidator transactionCategoryValidator;

    @Transactional
    public TransactionResponse execute(
            CreateTransactionRequest request,
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.resolve(authentication);

        return createTransaction(request, user);
    }

    @Transactional
    public TransactionResponse executeForUser(
            CreateTransactionRequest request,
            User user
    ) {
        return createTransaction(request, user);
    }

    private TransactionResponse createTransaction(
            CreateTransactionRequest request,
            User user
    ) {

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

        Transaction savedTransaction = saveTransactionPort.save(transaction);

        return transactionMapper.toResponse(savedTransaction);
    }
}