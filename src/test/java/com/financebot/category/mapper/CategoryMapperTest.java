package com.financebot.category.mapper;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.dto.request.CreateCategoryRequest;
import com.financebot.category.dto.request.UpdateCategoryRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {
    private final CategoryMapper mapper = new CategoryMapper();

    @Test
    void shouldMapCreateAndUpdateRequestsWithTrimmedName() {
        Category category = mapper.toEntity(new CreateCategoryRequest("  Casa  ", CategoryType.EXPENSE));
        mapper.updateEntity(new UpdateCategoryRequest("  Moradia  ", CategoryType.EXPENSE), category);

        assertThat(category.getName()).isEqualTo("Moradia");
        assertThat(category.getType()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void shouldMapCategoryToResponse() {
        Category category = new Category();
        category.setId(2L);
        category.setName("Casa");
        category.setType(CategoryType.EXPENSE);
        category.setActive(true);
        category.setDefaultCategory(false);
        category.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

        var response = mapper.toResponse(category);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.name()).isEqualTo("Casa");
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(category.getCreatedAt());
    }
}
