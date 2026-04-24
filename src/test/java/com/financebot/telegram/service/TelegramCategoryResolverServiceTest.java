package com.financebot.telegram.service;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramCategoryResolverServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TelegramCategoryResolverService telegramCategoryResolverService;

    @Nested
    @DisplayName("resolveCategory")
    class ResolveCategoryTests {

        @ParameterizedTest
        @CsvSource(value = {
                "Mercado|compra no supermercado",
                "Moradia|pagamento do aluguel",
                "Transporte|corrida de uber",
                "Saúde|remedio na farmacia"
        }, delimiter = '|')
        @DisplayName("deve sugerir categoria correta para despesas")
        void shouldSuggestCorrectExpenseCategory(String expectedCategoryName, String description) {
            User user = buildUser(1L);
            Category expectedCategory = buildCategory(expectedCategoryName, CategoryType.EXPENSE);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    expectedCategoryName
            )).thenReturn(Optional.of(expectedCategory));

            Category result = telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    description
            );

            assertThat(result).isEqualTo(expectedCategory);
        }

        @ParameterizedTest
        @CsvSource(value = {
                "Salário|pagamento da empresa",
                "Freelance|freela de sistema",
                "Transferências|pix recebido"
        }, delimiter = '|')
        @DisplayName("deve sugerir categoria correta para receitas")
        void shouldSuggestCorrectIncomeCategory(String expectedCategoryName, String description) {
            User user = buildUser(1L);
            Category expectedCategory = buildCategory(expectedCategoryName, CategoryType.INCOME);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.INCOME,
                    expectedCategoryName
            )).thenReturn(Optional.of(expectedCategory));

            Category result = telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.INCOME,
                    description
            );

            assertThat(result).isEqualTo(expectedCategory);
        }

        @Test
        @DisplayName("deve retornar categoria fallback existente quando sugestao nao existir")
        void shouldReturnExistingFallbackExpenseCategoryWhenSuggestedCategoryDoesNotExist() {
            User user = buildUser(1L);
            Category fallbackCategory = buildCategory("Outros", CategoryType.EXPENSE);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Combustível"
            )).thenReturn(Optional.empty());

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            )).thenReturn(Optional.of(fallbackCategory));

            Category result = telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    "gasolina no posto"
            );

            assertThat(result).isEqualTo(fallbackCategory);

            verify(categoryRepository, never()).save(org.mockito.ArgumentMatchers.any(Category.class));
        }

        @Test
        @DisplayName("deve criar categoria fallback de despesa quando sugestao e fallback nao existirem")
        void shouldCreateFallbackExpenseCategoryWhenSuggestedAndFallbackDoNotExist() {
            User user = buildUser(1L);
            Category savedCategory = buildCategory("Outros", CategoryType.EXPENSE);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Alimentação"
            )).thenReturn(Optional.empty());

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            )).thenReturn(Optional.empty());

            when(categoryRepository.save(org.mockito.ArgumentMatchers.any(Category.class)))
                    .thenReturn(savedCategory);

            Category result = telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    "lanche no ifood"
            );

            assertThat(result).isEqualTo(savedCategory);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            Category categoryToSave = captor.getValue();

            assertThat(categoryToSave.getUser()).isEqualTo(user);
            assertThat(categoryToSave.getType()).isEqualTo(CategoryType.EXPENSE);
            assertThat(categoryToSave.getName()).isEqualTo("Outros");
        }

        @Test
        @DisplayName("deve criar categoria fallback de receita quando nenhuma categoria existir")
        void shouldCreateFallbackIncomeCategoryWhenNoCategoryExists() {
            User user = buildUser(1L);
            Category savedCategory = buildCategory("Receitas Gerais", CategoryType.INCOME);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.INCOME,
                    "Receitas Gerais"
            )).thenReturn(Optional.empty(), Optional.empty());

            when(categoryRepository.save(org.mockito.ArgumentMatchers.any(Category.class)))
                    .thenReturn(savedCategory);

            Category result = telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.INCOME,
                    "valor recebido"
            );

            assertThat(result).isEqualTo(savedCategory);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            Category categoryToSave = captor.getValue();

            assertThat(categoryToSave.getUser()).isEqualTo(user);
            assertThat(categoryToSave.getType()).isEqualTo(CategoryType.INCOME);
            assertThat(categoryToSave.getName()).isEqualTo("Receitas Gerais");
        }

        @Test
        @DisplayName("deve normalizar descricao removendo acentos e ignorando maiusculas")
        void shouldNormalizeDescriptionRemovingAccentsAndIgnoringCase() {
            User user = buildUser(1L);
            Category expectedCategory = buildCategory("Combustível", CategoryType.EXPENSE);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Combustível"
            )).thenReturn(Optional.of(expectedCategory));

            Category result = telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    "COMBUSTÍVEL no posto"
            );

            assertThat(result).isEqualTo(expectedCategory);
        }

        @Test
        @DisplayName("deve usar fallback quando descricao for nula")
        void shouldUseFallbackWhenDescriptionIsNull() {
            User user = buildUser(1L);
            Category fallbackCategory = buildCategory("Outros", CategoryType.EXPENSE);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            )).thenReturn(Optional.of(fallbackCategory));

            Category result = telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    null
            );

            assertThat(result).isEqualTo(fallbackCategory);
        }
    }

    @Nested
    @DisplayName("resolveExplicitCategory")
    class ResolveExplicitCategoryTests {

        @Test
        @DisplayName("deve retornar categoria explicita existente")
        void shouldReturnExistingExplicitCategory() {
            User user = buildUser(1L);
            Category existingCategory = buildCategory("Cartão", CategoryType.EXPENSE);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Cartão"
            )).thenReturn(Optional.of(existingCategory));

            Category result = telegramCategoryResolverService.resolveExplicitCategory(
                    user,
                    TransactionType.EXPENSE,
                    " Cartão "
            );

            assertThat(result).isEqualTo(existingCategory);

            verify(categoryRepository, never()).save(org.mockito.ArgumentMatchers.any(Category.class));
        }

        @Test
        @DisplayName("deve criar categoria explicita quando nao existir")
        void shouldCreateExplicitCategoryWhenItDoesNotExist() {
            User user = buildUser(1L);
            Category savedCategory = buildCategory("Viagem", CategoryType.EXPENSE);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Viagem"
            )).thenReturn(Optional.empty());

            when(categoryRepository.save(org.mockito.ArgumentMatchers.any(Category.class)))
                    .thenReturn(savedCategory);

            Category result = telegramCategoryResolverService.resolveExplicitCategory(
                    user,
                    TransactionType.EXPENSE,
                    " Viagem "
            );

            assertThat(result).isEqualTo(savedCategory);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());

            Category categoryToSave = captor.getValue();

            assertThat(categoryToSave.getUser()).isEqualTo(user);
            assertThat(categoryToSave.getType()).isEqualTo(CategoryType.EXPENSE);
            assertThat(categoryToSave.getName()).isEqualTo("Viagem");
        }

        @Test
        @DisplayName("deve usar categoria sugerida quando categoria explicita for nula")
        void shouldUseSuggestedCategoryWhenExplicitCategoryIsNull() {
            User user = buildUser(1L);
            Category fallbackCategory = buildCategory("Outros", CategoryType.EXPENSE);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            )).thenReturn(Optional.of(fallbackCategory));

            Category result = telegramCategoryResolverService.resolveExplicitCategory(
                    user,
                    TransactionType.EXPENSE,
                    null
            );

            assertThat(result).isEqualTo(fallbackCategory);
        }

        @Test
        @DisplayName("deve usar categoria sugerida quando categoria explicita estiver em branco")
        void shouldUseSuggestedCategoryWhenExplicitCategoryIsBlank() {
            User user = buildUser(1L);
            Category fallbackCategory = buildCategory("Receitas Gerais", CategoryType.INCOME);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.INCOME,
                    "Receitas Gerais"
            )).thenReturn(Optional.of(fallbackCategory));

            Category result = telegramCategoryResolverService.resolveExplicitCategory(
                    user,
                    TransactionType.INCOME,
                    "   "
            );

            assertThat(result).isEqualTo(fallbackCategory);
        }
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Category buildCategory(String name, CategoryType type) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        return category;
    }
}