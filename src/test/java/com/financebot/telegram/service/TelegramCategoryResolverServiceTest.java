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
import static org.mockito.ArgumentMatchers.any;
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
        @DisplayName("deve sugerir categoria ativa correta para despesas")
        void shouldSuggestCorrectActiveExpenseCategory(String expectedCategoryName, String description) {
            User user = buildUser(1L);
            Category expectedCategory = buildCategory(expectedCategoryName, CategoryType.EXPENSE, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
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

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.EXPENSE,
                    expectedCategoryName
            );
            verify(categoryRepository, never()).save(any(Category.class));
        }

        @ParameterizedTest
        @CsvSource(value = {
                "Salário|pagamento da empresa",
                "Freelance|freela de sistema",
                "Transferências|pix recebido"
        }, delimiter = '|')
        @DisplayName("deve sugerir categoria ativa correta para receitas")
        void shouldSuggestCorrectActiveIncomeCategory(String expectedCategoryName, String description) {
            User user = buildUser(1L);
            Category expectedCategory = buildCategory(expectedCategoryName, CategoryType.INCOME, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
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

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.INCOME,
                    expectedCategoryName
            );
            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("deve retornar categoria fallback existente quando sugestao ativa nao existir")
        void shouldReturnExistingFallbackExpenseCategoryWhenActiveSuggestedCategoryDoesNotExist() {
            User user = buildUser(1L);
            Category fallbackCategory = buildCategory("Outros", CategoryType.EXPENSE, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
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

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Combustível"
            );
            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            );
            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("deve reativar categoria fallback inativa quando sugestao ativa nao existir")
        void shouldReactivateInactiveFallbackCategoryWhenActiveSuggestedCategoryDoesNotExist() {
            User user = buildUser(1L);
            Category inactiveFallbackCategory = buildCategory("Outros", CategoryType.EXPENSE, false, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Combustível"
            )).thenReturn(Optional.empty());

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            )).thenReturn(Optional.of(inactiveFallbackCategory));

            when(categoryRepository.save(inactiveFallbackCategory)).thenReturn(inactiveFallbackCategory);

            Category result = telegramCategoryResolverService.resolveCategory(
                    user,
                    TransactionType.EXPENSE,
                    "gasolina no posto"
            );

            assertThat(result).isEqualTo(inactiveFallbackCategory);
            assertThat(inactiveFallbackCategory.getActive()).isTrue();

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Combustível"
            );
            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            );
            verify(categoryRepository).save(inactiveFallbackCategory);
        }

        @Test
        @DisplayName("deve criar categoria fallback de despesa quando sugestao ativa e fallback nao existirem")
        void shouldCreateFallbackExpenseCategoryWhenActiveSuggestedAndFallbackDoNotExist() {
            User user = buildUser(1L);
            Category savedCategory = buildCategory("Outros", CategoryType.EXPENSE, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Alimentação"
            )).thenReturn(Optional.empty());

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            )).thenReturn(Optional.empty());

            when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

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
            assertThat(categoryToSave.getActive()).isTrue();
            assertThat(categoryToSave.getDefaultCategory()).isFalse();
        }

        @Test
        @DisplayName("deve criar categoria fallback de receita quando nenhuma categoria existir")
        void shouldCreateFallbackIncomeCategoryWhenNoCategoryExists() {
            User user = buildUser(1L);
            Category savedCategory = buildCategory("Receitas Gerais", CategoryType.INCOME, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.INCOME,
                    "Receitas Gerais"
            )).thenReturn(Optional.empty());

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.INCOME,
                    "Receitas Gerais"
            )).thenReturn(Optional.empty());

            when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

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
            assertThat(categoryToSave.getActive()).isTrue();
            assertThat(categoryToSave.getDefaultCategory()).isFalse();
        }

        @Test
        @DisplayName("deve normalizar descricao removendo acentos e ignorando maiusculas")
        void shouldNormalizeDescriptionRemovingAccentsAndIgnoringCase() {
            User user = buildUser(1L);
            Category expectedCategory = buildCategory("Combustível", CategoryType.EXPENSE, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
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

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Combustível"
            );
            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("deve usar fallback ativo quando descricao for nula")
        void shouldUseActiveFallbackWhenDescriptionIsNull() {
            User user = buildUser(1L);
            Category fallbackCategory = buildCategory("Outros", CategoryType.EXPENSE, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
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

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            );
            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("resolveExplicitCategory")
    class ResolveExplicitCategoryTests {

        @Test
        @DisplayName("deve retornar categoria explicita existente e ativa")
        void shouldReturnExistingActiveExplicitCategory() {
            User user = buildUser(1L);
            Category existingCategory = buildCategory("Cartão", CategoryType.EXPENSE, true, false);

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

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Cartão"
            );
            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("deve reativar categoria explicita existente e inativa")
        void shouldReactivateExistingInactiveExplicitCategory() {
            User user = buildUser(1L);
            Category inactiveCategory = buildCategory("Cartão", CategoryType.EXPENSE, false, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Cartão"
            )).thenReturn(Optional.of(inactiveCategory));

            when(categoryRepository.save(inactiveCategory)).thenReturn(inactiveCategory);

            Category result = telegramCategoryResolverService.resolveExplicitCategory(
                    user,
                    TransactionType.EXPENSE,
                    " Cartão "
            );

            assertThat(result).isEqualTo(inactiveCategory);
            assertThat(inactiveCategory.getActive()).isTrue();

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Cartão"
            );
            verify(categoryRepository).save(inactiveCategory);
        }

        @Test
        @DisplayName("deve criar categoria explicita quando nao existir")
        void shouldCreateExplicitCategoryWhenItDoesNotExist() {
            User user = buildUser(1L);
            Category savedCategory = buildCategory("Viagem", CategoryType.EXPENSE, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Viagem"
            )).thenReturn(Optional.empty());

            when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

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
            assertThat(categoryToSave.getActive()).isTrue();
            assertThat(categoryToSave.getDefaultCategory()).isFalse();
        }

        @Test
        @DisplayName("deve usar categoria sugerida quando categoria explicita for nula")
        void shouldUseSuggestedCategoryWhenExplicitCategoryIsNull() {
            User user = buildUser(1L);
            Category fallbackCategory = buildCategory("Outros", CategoryType.EXPENSE, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
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

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Outros"
            );
            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("deve usar categoria sugerida quando categoria explicita estiver em branco")
        void shouldUseSuggestedCategoryWhenExplicitCategoryIsBlank() {
            User user = buildUser(1L);
            Category fallbackCategory = buildCategory("Receitas Gerais", CategoryType.INCOME, true, false);

            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
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

            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCaseAndActiveTrue(
                    user.getId(),
                    CategoryType.INCOME,
                    "Receitas Gerais"
            );
            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Category buildCategory(String name, CategoryType type, Boolean active, Boolean defaultCategory) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setActive(active);
        category.setDefaultCategory(defaultCategory);
        return category;
    }
}