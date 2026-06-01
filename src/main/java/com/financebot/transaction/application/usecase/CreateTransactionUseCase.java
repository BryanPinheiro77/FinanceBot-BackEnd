package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.application.command.CreateTransactionCommand;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.SaveTransactionPort;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.service.UserResourceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateTransactionUseCase {

    private final SaveTransactionPort saveTransactionPort;
    private final UserResourceResolver userResourceResolver;
    private final TransactionMapper transactionMapper;
    private final TransactionCategoryValidator transactionCategoryValidator;

    @Transactional
    public TransactionResponse execute(CreateTransactionCommand command) {
        Account account = userResourceResolver.resolveAccount(
                command.accountId(),
                command.user().getId()
        );

        Category category = userResourceResolver.resolveCategory(
                command.categoryId(),
                command.user().getId()
        );

        transactionCategoryValidator.validate(category, command.type());

        Transaction transaction = transactionMapper.toEntity(command);
        transaction.setUser(command.user());
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