package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.application.command.CreateExistingInstallmentTransactionCommand;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.SaveTransactionPort;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.installment.InstallmentPlan;
import com.financebot.transaction.domain.installment.InstallmentPlanFactory;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.service.UserResourceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CreateExistingInstallmentTransactionUseCase {

    private final SaveTransactionPort saveTransactionPort;
    private final UserResourceResolver userResourceResolver;
    private final TransactionMapper transactionMapper;
    private final TransactionCategoryValidator transactionCategoryValidator;
    private final InstallmentPlanFactory installmentPlanFactory;
    private final InstallmentTransactionBuilder installmentTransactionBuilder;

    @Transactional
    public InstallmentTransactionResponse execute(CreateExistingInstallmentTransactionCommand command) {
        BigDecimal effectiveTotalAmount = resolveEffectiveTotalAmount(command);

        InstallmentPlan plan = installmentPlanFactory.createRemaining(
                effectiveTotalAmount,
                command.description(),
                command.firstRemainingInstallmentDate(),
                command.type(),
                command.totalInstallments(),
                command.firstRemainingInstallmentNumber()
        );

        Account account = userResourceResolver.resolveAccount(command.accountId(), command.user().getId());
        Category category = userResourceResolver.resolveCategory(command.categoryId(), command.user().getId());

        transactionCategoryValidator.validate(category, command.type());

        List<Transaction> transactions = plan.items().stream()
                .map(item -> installmentTransactionBuilder.build(
                        item,
                        command.type(),
                        command.sourceType(),
                        command.user(),
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

    private BigDecimal resolveEffectiveTotalAmount(CreateExistingInstallmentTransactionCommand command) {
        boolean hasTotalAmount = command.totalAmount() != null;
        boolean hasMonthlyAmount = command.monthlyAmount() != null;

        if (hasTotalAmount == hasMonthlyAmount) {
            throw new IllegalArgumentException("Exactly one of total amount or monthly amount must be provided");
        }

        if (hasTotalAmount) {
            validatePositiveAmount(command.totalAmount(), "Total amount must be greater than zero");
            return command.totalAmount();
        }

        validatePositiveAmount(command.monthlyAmount(), "Monthly amount must be greater than zero");

        return command.monthlyAmount()
                .multiply(BigDecimal.valueOf(command.totalInstallments()));
    }

    private void validatePositiveAmount(BigDecimal amount, String message) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
