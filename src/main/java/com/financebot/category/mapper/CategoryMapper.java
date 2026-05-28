package com.financebot.category.mapper;

import com.financebot.category.domain.Category;
import com.financebot.category.dto.response.CategoryResponse;
import com.financebot.category.dto.request.CreateCategoryRequest;
import com.financebot.category.dto.request.UpdateCategoryRequest;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CreateCategoryRequest dto) {
        Category category = new Category();
        category.setName(dto.name().trim());
        category.setType(dto.type());
        return category;
    }

    public void updateEntity(UpdateCategoryRequest dto, Category category) {
        category.setName(dto.name().trim());
        category.setType(dto.type());
    }

    public CategoryResponse toResponse(Category category) {
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