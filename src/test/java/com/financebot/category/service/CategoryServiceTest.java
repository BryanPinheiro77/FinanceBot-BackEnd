package com.financebot.category.service;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.dto.request.UpdateCategoryRequest;
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
    @DisplayName("not found")
    class NotFoundTests {

        @Test
        @DisplayName("deve lançar erro quando categoria não for encontrada ao buscar por id")
        void shouldThrowWhenCategoryIsNotFoundOnFindById() {
            User user = buildUser();

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findById(20L, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserId(20L, 1L);
            verifyNoInteractions(categoryMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não for encontrada ao atualizar")
        void shouldThrowWhenCategoryIsNotFoundOnUpdate() {
            User user = buildUser();

            UpdateCategoryRequest request = new UpdateCategoryRequest(
                    "Alimentação",
                    CategoryType.EXPENSE
            );

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(20L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserId(20L, 1L);
            verify(categoryRepository, never()).save(any());
            verifyNoInteractions(categoryMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não for encontrada ao deletar")
        void shouldThrowWhenCategoryIsNotFoundOnDelete() {
            User user = buildUser();

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.delete(20L, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserId(20L, 1L);
            verify(categoryRepository, never()).save(any());
            verify(categoryRepository, never()).delete(any());
            verifyNoInteractions(categoryMapper);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deve desativar categoria ao deletar")
        void shouldDisableCategoryOnDelete() {
            User user = buildUser();
            Category category = buildCategory();

            when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
            when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));

            categoryService.delete(10L, authentication);

            ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

            verify(authenticatedUserResolver).resolve(authentication);
            verify(categoryRepository).findByIdAndUserId(10L, 1L);
            verify(categoryRepository).save(categoryCaptor.capture());
            verify(categoryRepository, never()).delete(any());

            Category savedCategory = categoryCaptor.getValue();

            assertThat(savedCategory.getActive()).isFalse();
        }
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("bryan@email.com");
        return user;
    }

    private Category buildCategory() {
        Category category = new Category();
        category.setId(10L);
        category.setName("Alimentação");
        category.setType(CategoryType.EXPENSE);
        category.setUser(buildUser());
        category.setActive(true);
        category.setDefaultCategory(true);
        return category;
    }
}