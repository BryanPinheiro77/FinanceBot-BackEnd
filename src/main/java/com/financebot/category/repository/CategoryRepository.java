package com.financebot.category.repository;

import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByUserIdOrderByNameAsc(Long userId);

    List<Category> findAllByUserIdAndTypeOrderByNameAsc(Long userId, CategoryType type);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    boolean existsByNameIgnoreCaseAndTypeAndUserId(String name, CategoryType type, Long userId);

    Optional<Category> findByUserIdAndTypeAndNameIgnoreCase(Long userId, CategoryType type, String name);

    Optional<Category> findFirstByUserIdAndTypeOrderByNameAsc(Long userId, CategoryType type);
}