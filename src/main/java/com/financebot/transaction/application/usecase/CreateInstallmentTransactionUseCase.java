package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.application.command.CreateInstallmentTransactionCommand;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.SaveTransactionPort;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.installment.InstallmentPlan;
import com.financebot.transaction.domain.installment.InstallmentPlanFactory;
import com.financebot.transaction.domain.installment.InstallmentPlanItem;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.service.UserResourceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CreateInstallmentTransactionUseCase {

    private final SaveTransactionPort saveTransactionPort;
    private final UserResourceResolver userResourceResolver;
    private final TransactionMapper transactionMapper;
    private final TransactionCategoryValidator transactionCategoryValidator;
    private final InstallmentPlanFactory installmentPlanFactory;

    @Transactional
    public InstallmentTransactionResponse execute(CreateInstallmentTransactionCommand command) {
        InstallmentPlan plan = installmentPlanFactory.create(
                command.totalAmount(),
                command.description(),
                command.firstInstallmentDate(),
                command.type(),
                command.totalInstallments()
        );

        Account account = userResourceResolver.resolveAccount(command.accountId(), command.user().getId());
        Category category = userResourceResolver.resolveCategory(command.categoryId(), command.user().getId());

        transactionCategoryValidator.validate(category, command.type());

        List<Transaction> transactions = plan.items().stream()
                .map(item -> buildInstallmentTransaction(
                        item,
                        command,
                        account,
                        category
                ))
                .toList();

        List<Transaction> savedTransactions = saveTransactionPort.saveAll(transactions);

        List<TransactionResponse> responses = savedTransactions.stream()
                .map(transactionMapper::toResponse)
                .toList();

        return new InstallmentTransactionResponse(
                plan.installmentGroupId(),
                plan.totalInstallments(),
                responses
        );
    }

    private Transaction buildInstallmentTransaction(
            InstallmentPlanItem item,
            CreateInstallmentTransactionCommand command,
            Account account,
            Category category
    ) {
        Transaction transaction = new Transaction();

        transaction.setAmount(item.amount());
        transaction.setDescription(item.description());
        transaction.setDate(item.date());
        transaction.setType(command.type());
        transaction.setSourceType(command.sourceType());
        transaction.setUser(command.user());
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setInstallment(true);
        transaction.setInstallmentNumber(item.installmentNumber());
        transaction.setTotalInstallments(item.totalInstallments());
        transaction.setInstallmentGroupId(item.installmentGroupId());

        return transaction;
    }
}