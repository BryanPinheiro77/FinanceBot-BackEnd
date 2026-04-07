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

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category resolveCategory(User user, TransactionType transactionType, String description) {
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

        if (transactionType == TransactionType.EXPENSE) {
            if (containsAny(normalized, "mercado", "supermercado", "atacadao", "atacado")) {
                return "Mercado";
            }
            if (containsAny(normalized, "gasolina", "combustivel", "posto", "etanol", "diesel")) {
                return "Combustível";
            }
            if (containsAny(normalized, "ifood", "restaurante", "lanche", "lanchonete", "pizza", "comida")) {
                return "Alimentação";
            }
            if (containsAny(normalized, "aluguel", "condominio", "moradia")) {
                return "Moradia";
            }
            if (containsAny(normalized, "uber", "99", "taxi", "onibus", "metro", "transporte")) {
                return "Transporte";
            }
            if (containsAny(normalized, "farmacia", "remedio", "medico", "consulta")) {
                return "Saúde";
            }

            return "Outros";
        }

        if (transactionType == TransactionType.INCOME) {
            if (containsAny(normalized, "salario", "pagamento", "empresa")) {
                return "Salário";
            }
            if (containsAny(normalized, "freela", "freelance", "bico", "servico")) {
                return "Freelance";
            }
            if (containsAny(normalized, "pix", "transferencia", "deposito")) {
                return "Transferências";
            }

            return "Receitas Gerais";
        }

        return "Outros";
    }

    private String getFallbackCategoryName(TransactionType transactionType) {
        return transactionType == TransactionType.INCOME
                ? "Receitas Gerais"
                : "Outros";
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