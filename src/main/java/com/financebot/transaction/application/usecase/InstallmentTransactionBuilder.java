package com.financebot.transaction.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.domain.installment.InstallmentPlanItem;
import com.financebot.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class InstallmentTransactionBuilder {

    public Transaction build(
            InstallmentPlanItem item,
            TransactionType type,
            SourceType sourceType,
            User user,
            Account account,
            Category category
    ) {
        Transaction transaction = new Transaction();

        transaction.setAmount(item.amount());
        transaction.setDescription(item.description());
        transaction.setDate(item.date());
        transaction.setType(type);
        transaction.setSourceType(sourceType);
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setInstallment(true);
        transaction.setInstallmentNumber(item.installmentNumber());
        transaction.setTotalInstallments(item.totalInstallments());
        transaction.setInstallmentGroupId(item.installmentGroupId());

        return transaction;
    }
}
