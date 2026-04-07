package com.financebot.category.dto;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        LocalDateTime createdAt
) {
    public static CategoryResponse fromEntity(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getCreatedAt()
        );
    }
}