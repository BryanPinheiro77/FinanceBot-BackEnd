package com.financebot.transaction.validation;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.transaction.domain.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TransactionCategoryValidatorTest {

    @InjectMocks
    private TransactionCategoryValidator transactionCategoryValidator;

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("deve aceitar categoria de despesa para transacao de despesa")
        void shouldAcceptExpenseCategoryForExpenseTransaction() {
            Category category = buildCategory(CategoryType.EXPENSE);

            assertThatCode(() -> transactionCategoryValidator.validate(category, TransactionType.EXPENSE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve aceitar categoria de receita para transacao de receita")
        void shouldAcceptIncomeCategoryForIncomeTransaction() {
            Category category = buildCategory(CategoryType.INCOME);

            assertThatCode(() -> transactionCategoryValidator.validate(category, TransactionType.INCOME))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deve lancar erro quando categoria de receita for usada em despesa")
        void shouldThrowWhenIncomeCategoryIsUsedForExpenseTransaction() {
            Category category = buildCategory(CategoryType.INCOME);

            assertThatThrownBy(() -> transactionCategoryValidator.validate(category, TransactionType.EXPENSE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category type does not match transaction type");
        }

        @Test
        @DisplayName("deve lancar erro quando categoria de despesa for usada em receita")
        void shouldThrowWhenExpenseCategoryIsUsedForIncomeTransaction() {
            Category category = buildCategory(CategoryType.EXPENSE);

            assertThatThrownBy(() -> transactionCategoryValidator.validate(category, TransactionType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category type does not match transaction type");
        }
    }

    private Category buildCategory(CategoryType type) {
        Category category = new Category();
        category.setType(type);
        return category;
    }
}