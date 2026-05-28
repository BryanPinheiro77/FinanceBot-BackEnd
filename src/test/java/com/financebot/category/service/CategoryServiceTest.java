package com.financebot.category.service;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.dto.request.CreateCategoryRequest;
import com.financebot.category.dto.request.UpdateCategoryRequest;
import com.financebot.category.dto.response.CategoryResponse;
import com.financebot.category.mapper.CategoryMapper;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CategoryService categoryService;

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("deve criar categoria nova quando nao existir categoria com mesmo nome e tipo")
        void shouldCreateNewCategoryWhenCategoryDoesNotExist() {
            User user = buildUser();

            CreateCategoryRequest request = new CreateCategoryRequest(
                    " Alimentação ",
                    CategoryType.EXPENSE
            );

            Category categoryToSave = buildCategory(10L, "Alimentação", CategoryType.EXPENSE, true, false);
            Category savedCategory = buildCategory(10L, "Alimentação", CategoryType.EXPENSE, true, false);
            CategoryResponse response = buildResponse(savedCategory);

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Alimentação"
            )).thenReturn(Optional.empty());

            when(categoryMapper.toEntity(request)).thenReturn(categoryToSave);
            when(categoryRepository.save(categoryToSave)).thenReturn(savedCategory);
            when(categoryMapper.toResponse(savedCategory)).thenReturn(response);

            CategoryResponse result = categoryService.create(request, authentication);

            assertThat(result).isEqualTo(response);

            assertThat(categoryToSave.getName()).isEqualTo("Alimentação");
            assertThat(categoryToSave.getUser()).isEqualTo(user);
            assertThat(categoryToSave.getActive()).isTrue();
            assertThat(categoryToSave.getDefaultCategory()).isFalse();

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Alimentação"
            );
            verify(categoryRepository).save(categoryToSave);
            verify(categoryMapper).toResponse(savedCategory);
        }

        @Test
        @DisplayName("deve lançar erro quando categoria com mesmo nome e tipo ja estiver ativa")
        void shouldThrowWhenCategoryAlreadyExistsAndIsActive() {
            User user = buildUser();
            Category existingCategory = buildCategory(10L, "Alimentação", CategoryType.EXPENSE, true, false);

            CreateCategoryRequest request = new CreateCategoryRequest(
                    "Alimentação",
                    CategoryType.EXPENSE
            );

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Alimentação"
            )).thenReturn(Optional.of(existingCategory));

            assertThatThrownBy(() -> categoryService.create(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category already exists for this user and type");

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Alimentação"
            );
            verify(categoryRepository, never()).save(any());
            verifyNoInteractions(categoryMapper);
        }

        @Test
        @DisplayName("deve reativar categoria inativa quando criar com mesmo nome e tipo")
        void shouldReactivateInactiveCategoryWhenCreatingWithSameNameAndType() {
            User user = buildUser();
            Category inactiveCategory = buildCategory(10L, "Alimentação", CategoryType.EXPENSE, false, true);
            CategoryResponse response = buildResponse(inactiveCategory);

            CreateCategoryRequest request = new CreateCategoryRequest(
                    " Alimentação ",
                    CategoryType.EXPENSE
            );

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Alimentação"
            )).thenReturn(Optional.of(inactiveCategory));

            when(categoryRepository.save(inactiveCategory)).thenReturn(inactiveCategory);
            when(categoryMapper.toResponse(inactiveCategory)).thenReturn(response);

            CategoryResponse result = categoryService.create(request, authentication);

            assertThat(result).isEqualTo(response);
            assertThat(inactiveCategory.getActive()).isTrue();

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByUserIdAndTypeAndNameIgnoreCase(
                    user.getId(),
                    CategoryType.EXPENSE,
                    "Alimentação"
            );
            verify(categoryRepository).save(inactiveCategory);
            verify(categoryMapper).toResponse(inactiveCategory);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("deve listar apenas categorias ativas do usuario")
        void shouldFindOnlyActiveCategories() {
            User user = buildUser();

            Category category = buildCategory(10L, "Alimentação", CategoryType.EXPENSE, true, true);
            CategoryResponse response = buildResponse(category);

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(user.getId()))
                    .thenReturn(List.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(response);

            List<CategoryResponse> result = categoryService.findAll(authentication);

            assertThat(result).containsExactly(response);

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findAllByUserIdAndActiveTrueOrderByNameAsc(user.getId());
            verify(categoryMapper).toResponse(category);
        }
    }

    @Nested
    @DisplayName("findByType")
    class FindByTypeTests {

        @Test
        @DisplayName("deve listar apenas categorias ativas do usuario por tipo")
        void shouldFindOnlyActiveCategoriesByType() {
            User user = buildUser();

            Category category = buildCategory(10L, "Salário", CategoryType.INCOME, true, true);
            CategoryResponse response = buildResponse(category);

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findAllByUserIdAndTypeAndActiveTrueOrderByNameAsc(
                    user.getId(),
                    CategoryType.INCOME
            )).thenReturn(List.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(response);

            List<CategoryResponse> result = categoryService.findByType(CategoryType.INCOME, authentication);

            assertThat(result).containsExactly(response);

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findAllByUserIdAndTypeAndActiveTrueOrderByNameAsc(
                    user.getId(),
                    CategoryType.INCOME
            );
            verify(categoryMapper).toResponse(category);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("deve retornar categoria ativa quando encontrada")
        void shouldReturnActiveCategoryWhenFound() {
            User user = buildUser();

            Category category = buildCategory(20L, "Alimentação", CategoryType.EXPENSE, true, true);
            CategoryResponse response = buildResponse(category);

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserIdAndActiveTrue(20L, user.getId()))
                    .thenReturn(Optional.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(response);

            CategoryResponse result = categoryService.findById(20L, authentication);

            assertThat(result).isEqualTo(response);

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserIdAndActiveTrue(20L, user.getId());
            verify(categoryMapper).toResponse(category);
        }

        @Test
        @DisplayName("deve lançar erro quando categoria ativa não for encontrada ao buscar por id")
        void shouldThrowWhenActiveCategoryIsNotFoundOnFindById() {
            User user = buildUser();

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserIdAndActiveTrue(20L, user.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findById(20L, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserIdAndActiveTrue(20L, user.getId());
            verifyNoInteractions(categoryMapper);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("deve atualizar categoria ativa quando encontrada")
        void shouldUpdateActiveCategoryWhenFound() {
            User user = buildUser();

            Category category = buildCategory(20L, "Alimentação", CategoryType.EXPENSE, true, true);
            Category updatedCategory = buildCategory(20L, "Mercado", CategoryType.EXPENSE, true, true);
            CategoryResponse response = buildResponse(updatedCategory);

            UpdateCategoryRequest request = new UpdateCategoryRequest(
                    "Mercado",
                    CategoryType.EXPENSE
            );

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserIdAndActiveTrue(20L, user.getId()))
                    .thenReturn(Optional.of(category));
            when(categoryRepository.existsByNameIgnoreCaseAndTypeAndUserId(
                    "Mercado",
                    CategoryType.EXPENSE,
                    user.getId()
            )).thenReturn(false);
            when(categoryRepository.save(category)).thenReturn(updatedCategory);
            when(categoryMapper.toResponse(updatedCategory)).thenReturn(response);

            CategoryResponse result = categoryService.update(20L, request, authentication);

            assertThat(result).isEqualTo(response);

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserIdAndActiveTrue(20L, user.getId());
            verify(categoryRepository).existsByNameIgnoreCaseAndTypeAndUserId(
                    "Mercado",
                    CategoryType.EXPENSE,
                    user.getId()
            );
            verify(categoryMapper).updateEntity(request, category);
            verify(categoryRepository).save(category);
            verify(categoryMapper).toResponse(updatedCategory);
        }

        @Test
        @DisplayName("deve lançar erro quando categoria ativa não for encontrada ao atualizar")
        void shouldThrowWhenActiveCategoryIsNotFoundOnUpdate() {
            User user = buildUser();

            UpdateCategoryRequest request = new UpdateCategoryRequest(
                    "Alimentação",
                    CategoryType.EXPENSE
            );

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserIdAndActiveTrue(20L, user.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(20L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserIdAndActiveTrue(20L, user.getId());
            verify(categoryRepository, never()).save(any());
            verifyNoInteractions(categoryMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando atualização gerar categoria duplicada")
        void shouldThrowWhenUpdateCreatesDuplicateCategory() {
            User user = buildUser();

            Category category = buildCategory(20L, "Alimentação", CategoryType.EXPENSE, true, true);

            UpdateCategoryRequest request = new UpdateCategoryRequest(
                    "Mercado",
                    CategoryType.EXPENSE
            );

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserIdAndActiveTrue(20L, user.getId()))
                    .thenReturn(Optional.of(category));
            when(categoryRepository.existsByNameIgnoreCaseAndTypeAndUserId(
                    "Mercado",
                    CategoryType.EXPENSE,
                    user.getId()
            )).thenReturn(true);

            assertThatThrownBy(() -> categoryService.update(20L, request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category already exists for this user and type");

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserIdAndActiveTrue(20L, user.getId());
            verify(categoryRepository).existsByNameIgnoreCaseAndTypeAndUserId(
                    "Mercado",
                    CategoryType.EXPENSE,
                    user.getId()
            );
            verify(categoryRepository, never()).save(any());
            verify(categoryMapper, never()).updateEntity(any(), any());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deve desativar categoria ativa ao deletar")
        void shouldDisableActiveCategoryOnDelete() {
            User user = buildUser();
            Category category = buildCategory(10L, "Alimentação", CategoryType.EXPENSE, true, true);

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserId(10L, user.getId()))
                    .thenReturn(Optional.of(category));

            categoryService.delete(10L, authentication);

            ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserId(10L, user.getId());
            verify(categoryRepository).save(categoryCaptor.capture());
            verify(categoryRepository, never()).delete(any());

            Category savedCategory = categoryCaptor.getValue();

            assertThat(savedCategory.getActive()).isFalse();
        }

        @Test
        @DisplayName("deve ser idempotente ao deletar categoria já inativa")
        void shouldBeIdempotentWhenDeletingInactiveCategory() {
            User user = buildUser();
            Category category = buildCategory(10L, "Alimentação", CategoryType.EXPENSE, false, true);

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserId(10L, user.getId()))
                    .thenReturn(Optional.of(category));

            categoryService.delete(10L, authentication);

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserId(10L, user.getId());
            verify(categoryRepository, never()).save(any());
            verify(categoryRepository, never()).delete(any());
            verifyNoInteractions(categoryMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não for encontrada ao deletar")
        void shouldThrowWhenCategoryIsNotFoundOnDelete() {
            User user = buildUser();

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserId(20L, user.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.delete(20L, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserId(20L, user.getId());
            verify(categoryRepository, never()).save(any());
            verify(categoryRepository, never()).delete(any());
            verifyNoInteractions(categoryMapper);
        }
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("bryan@email.com");
        return user;
    }

    private Category buildCategory(
            Long id,
            String name,
            CategoryType type,
            Boolean active,
            Boolean defaultCategory
    ) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setType(type);
        category.setUser(buildUser());
        category.setActive(active);
        category.setDefaultCategory(defaultCategory);
        category.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return category;
    }

    private CategoryResponse buildResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getActive(),
                category.getDefaultCategory(),
                category.getCreatedAt()
        );
    }
}