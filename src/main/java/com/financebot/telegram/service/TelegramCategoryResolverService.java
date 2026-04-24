package com.financebot.telegram.service;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramCategoryResolverService {

    private static final String OTHER_EXPENSES_CATEGORY = "Outros";
    private static final String GENERAL_INCOME_CATEGORY = "Receitas Gerais";

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category resolveCategory(User user, TransactionType transactionType, String description) {
        return resolveSuggestedCategory(user, transactionType, description);
    }

    @Transactional
    public Category resolveExplicitCategory(User user, TransactionType transactionType, String categoryName) {
        CategoryType categoryType = mapTransactionTypeToCategoryType(transactionType);
        String normalizedCategoryName = categoryName != null ? categoryName.trim() : null;

        if (normalizedCategoryName == null || normalizedCategoryName.isBlank()) {
            return resolveSuggestedCategory(user, transactionType, null);
        }

        Optional<Category> existingCategory =
                categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                        user.getId(),
                        categoryType,
                        normalizedCategoryName
                );

        if (existingCategory.isPresent()) {
            return existingCategory.get();
        }

        return createCategory(user, categoryType, normalizedCategoryName);
    }

    private Category resolveSuggestedCategory(User user, TransactionType transactionType, String description) {
        CategoryType categoryType = mapTransactionTypeToCategoryType(transactionType);
        String suggestedCategoryName = suggestCategoryName(transactionType, description);

        Optional<Category> existingSuggestedCategory =
                categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                        user.getId(),
                        categoryType,
                        suggestedCategoryName
                );

        if (existingSuggestedCategory.isPresent()) {
            return existingSuggestedCategory.get();
        }

        String fallbackCategoryName = getFallbackCategoryName(transactionType);

        Optional<Category> existingFallbackCategory =
                categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                        user.getId(),
                        categoryType,
                        fallbackCategoryName
                );

        if (existingFallbackCategory.isPresent()) {
            return existingFallbackCategory.get();
        }

        return createCategory(user, categoryType, fallbackCategoryName);
    }

    private CategoryType mapTransactionTypeToCategoryType(TransactionType transactionType) {
        return switch (transactionType) {
            case EXPENSE -> CategoryType.EXPENSE;
            case INCOME -> CategoryType.INCOME;
        };
    }

    private String suggestCategoryName(TransactionType transactionType, String description) {
        String normalized = normalize(description);

        return switch (transactionType) {
            case EXPENSE -> suggestExpenseCategoryName(normalized);
            case INCOME -> suggestIncomeCategoryName(normalized);
        };
    }

    private String suggestExpenseCategoryName(String normalizedDescription) {
        if (isMarketExpense(normalizedDescription)) {
            return "Mercado";
        }

        if (isFuelExpense(normalizedDescription)) {
            return "Combustível";
        }

        if (isFoodExpense(normalizedDescription)) {
            return "Alimentação";
        }

        if (isHousingExpense(normalizedDescription)) {
            return "Moradia";
        }

        if (isTransportExpense(normalizedDescription)) {
            return "Transporte";
        }

        if (isHealthExpense(normalizedDescription)) {
            return "Saúde";
        }

        return OTHER_EXPENSES_CATEGORY;
    }

    private String suggestIncomeCategoryName(String normalizedDescription) {
        if (isSalaryIncome(normalizedDescription)) {
            return "Salário";
        }

        if (isFreelanceIncome(normalizedDescription)) {
            return "Freelance";
        }

        if (isTransferIncome(normalizedDescription)) {
            return "Transferências";
        }

        return GENERAL_INCOME_CATEGORY;
    }

    private boolean isMarketExpense(String normalizedDescription) {
        return containsAny(normalizedDescription, "mercado", "supermercado", "atacadao", "atacado");
    }

    private boolean isFuelExpense(String normalizedDescription) {
        return containsAny(normalizedDescription, "gasolina", "combustivel", "posto", "etanol", "diesel");
    }

    private boolean isFoodExpense(String normalizedDescription) {
        return containsAny(normalizedDescription, "ifood", "restaurante", "lanche", "lanchonete", "pizza", "comida");
    }

    private boolean isHousingExpense(String normalizedDescription) {
        return containsAny(normalizedDescription, "aluguel", "condominio", "moradia");
    }

    private boolean isTransportExpense(String normalizedDescription) {
        return containsAny(normalizedDescription, "uber", "99", "taxi", "onibus", "metro", "transporte");
    }

    private boolean isHealthExpense(String normalizedDescription) {
        return containsAny(normalizedDescription, "farmacia", "remedio", "medico", "consulta");
    }

    private boolean isSalaryIncome(String normalizedDescription) {
        return containsAny(normalizedDescription, "salario", "pagamento", "empresa");
    }

    private boolean isFreelanceIncome(String normalizedDescription) {
        return containsAny(normalizedDescription, "freela", "freelance", "bico", "servico");
    }

    private boolean isTransferIncome(String normalizedDescription) {
        return containsAny(normalizedDescription, "pix", "transferencia", "deposito");
    }

    private String getFallbackCategoryName(TransactionType transactionType) {
        return transactionType == TransactionType.INCOME
                ? GENERAL_INCOME_CATEGORY
                : OTHER_EXPENSES_CATEGORY;
    }

    private Category createCategory(User user, CategoryType categoryType, String name) {
        Category category = new Category();
        category.setUser(user);
        category.setType(categoryType);
        category.setName(name);
        return categoryRepository.save(category);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT).trim();
    }
}