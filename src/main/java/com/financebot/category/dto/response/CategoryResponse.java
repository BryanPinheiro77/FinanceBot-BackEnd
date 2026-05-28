package com.financebot.category.dto.response;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        Boolean active,
        Boolean defaultCategory,
        LocalDateTime createdAt
) {
    public static CategoryResponse fromEntity(Category category) {
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