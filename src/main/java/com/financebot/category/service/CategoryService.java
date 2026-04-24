package com.financebot.category.service;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.dto.CategoryResponse;
import com.financebot.category.dto.CreateCategoryRequest;
import com.financebot.category.dto.UpdateCategoryRequest;
import com.financebot.category.mapper.CategoryMapper;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Category not found";
    private static final String AUTHENTICATED_USER_NOT_FOUND_MESSAGE = "Authenticated user not found";

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        validateDuplicateCategory(request.name(), request.type(), user.getId());

        Category category = categoryMapper.toEntity(request);
        category.setUser(user);

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        return categoryRepository.findAllByUserIdOrderByNameAsc(user.getId())
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findByType(CategoryType type, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        return categoryRepository.findAllByUserIdAndTypeOrderByNameAsc(user.getId(), type)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(CATEGORY_NOT_FOUND_MESSAGE));

        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(CATEGORY_NOT_FOUND_MESSAGE));

        boolean changedName = !category.getName().equalsIgnoreCase(request.name().trim());
        boolean changedType = category.getType() != request.type();

        if (changedName || changedType) {
            validateDuplicateCategory(request.name(), request.type(), user.getId());
        }

        categoryMapper.updateEntity(request, category);

        Category updated = categoryRepository.save(category);
        return categoryMapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(CATEGORY_NOT_FOUND_MESSAGE));

        categoryRepository.delete(category);
    }

    @Transactional
    public void createDefaultCategoriesForUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be persisted before creating default categories");
        }

        createCategoryIfNotExists("Salário", CategoryType.INCOME, user);
        createCategoryIfNotExists("Freelance", CategoryType.INCOME, user);
        createCategoryIfNotExists("Investimentos", CategoryType.INCOME, user);

        createCategoryIfNotExists("Alimentação", CategoryType.EXPENSE, user);
        createCategoryIfNotExists("Transporte", CategoryType.EXPENSE, user);
        createCategoryIfNotExists("Moradia", CategoryType.EXPENSE, user);
        createCategoryIfNotExists("Lazer", CategoryType.EXPENSE, user);
        createCategoryIfNotExists("Saúde", CategoryType.EXPENSE, user);
    }

    private void createCategoryIfNotExists(String name, CategoryType type, User user) {
        boolean exists = categoryRepository.existsByNameIgnoreCaseAndTypeAndUserId(
                name,
                type,
                user.getId()
        );

        if (!exists) {
            Category category = new Category();
            category.setName(name);
            category.setType(type);
            category.setUser(user);
            categoryRepository.save(category);
        }
    }

    private void validateDuplicateCategory(String name, CategoryType type, Long userId) {
        boolean alreadyExists = categoryRepository.existsByNameIgnoreCaseAndTypeAndUserId(
                name.trim(),
                type,
                userId
        );

        if (alreadyExists) {
            throw new IllegalArgumentException("Category already exists for this user and type");
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(AUTHENTICATED_USER_NOT_FOUND_MESSAGE));
    }
}