package com.financebot.transaction.validation;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.transaction.domain.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryValidator {

    private static final String CATEGORY_TYPE_MISMATCH_MESSAGE =
            "Category type does not match transaction type";

    public void validate(Category category, TransactionType transactionType) {
        boolean isIncomeMatch =
                category.getType() == CategoryType.INCOME && transactionType == TransactionType.INCOME;

        boolean isExpenseMatch =
                category.getType() == CategoryType.EXPENSE && transactionType == TransactionType.EXPENSE;

        if (!isIncomeMatch && !isExpenseMatch) {
            throw new IllegalArgumentException(CATEGORY_TYPE_MISMATCH_MESSAGE);
        }
    }
}