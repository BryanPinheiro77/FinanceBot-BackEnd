package com.financebot.category.controller;

import com.financebot.category.domain.CategoryType;
import com.financebot.category.dto.CategoryResponse;
import com.financebot.category.dto.CreateCategoryRequest;
import com.financebot.category.dto.UpdateCategoryRequest;
import com.financebot.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(
            @RequestBody @Valid CreateCategoryRequest request,
            Authentication authentication
    ) {
        return categoryService.create(request, authentication);
    }

    @GetMapping
    public List<CategoryResponse> findAll(
            @RequestParam(required = false) CategoryType type,
            Authentication authentication
    ) {
        if (type != null) {
            return categoryService.findByType(type, authentication);
        }

        return categoryService.findAll(authentication);
    }

    @GetMapping("/{id}")
    public CategoryResponse findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return categoryService.findById(id, authentication);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCategoryRequest request,
            Authentication authentication
    ) {
        return categoryService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        categoryService.delete(id, authentication);
    }
}