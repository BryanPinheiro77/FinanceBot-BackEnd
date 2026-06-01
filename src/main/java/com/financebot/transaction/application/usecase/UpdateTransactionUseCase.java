package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.application.command.UpdateTransactionCommand;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.FindTransactionPort;
import com.financebot.transaction.application.port.out.SaveTransactionPort;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.service.UserResourceResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateTransactionUseCase {

    private static final String TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found";

    private final FindTransactionPort findTransactionPort;
    private final SaveTransactionPort saveTransactionPort;
    private final UserResourceResolver userResourceResolver;
    private final TransactionMapper transactionMapper;
    private final TransactionCategoryValidator transactionCategoryValidator;

    @Transactional
    public TransactionResponse execute(UpdateTransactionCommand command) {
        Transaction transaction = findTransactionPort.findByIdAndUserId(
                        command.transactionId(),
                        command.user().getId()
                )
                .orElseThrow(() -> new EntityNotFoundException(TRANSACTION_NOT_FOUND_MESSAGE));

        Account account = userResourceResolver.resolveAccount(
                command.accountId(),
                command.user().getId()
        );

        Category category = userResourceResolver.resolveCategory(
                command.categoryId(),
                command.user().getId()
        );

        transactionCategoryValidator.validate(category, command.type());

        transactionMapper.updateEntity(command, transaction);
        transaction.setAccount(account);
        transaction.setCategory(category);

        Transaction updatedTransaction = saveTransactionPort.save(transaction);

        return transactionMapper.toResponse(updatedTransaction);
    }
}