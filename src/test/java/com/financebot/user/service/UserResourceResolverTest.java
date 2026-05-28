package com.financebot.user.service;

import com.financebot.account.domain.Account;
import com.financebot.account.repository.AccountRepository;
import com.financebot.category.domain.Category;
import com.financebot.category.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserResourceResolverTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private UserResourceResolver userResourceResolver;

    @Nested
    @DisplayName("resolveAccount")
    class ResolveAccountTests {

        @Test
        @DisplayName("deve retornar conta quando ela pertencer ao usuario")
        void shouldResolveAccount() {
            Account account = new Account();
            account.setId(10L);

            when(accountRepository.findByIdAndUserId(10L, 1L))
                    .thenReturn(Optional.of(account));

            Account result = userResourceResolver.resolveAccount(10L, 1L);

            assertThat(result).isEqualTo(account);

            verify(accountRepository).findByIdAndUserId(10L, 1L);
        }

        @Test
        @DisplayName("deve lancar erro quando conta nao pertencer ao usuario")
        void shouldThrowWhenAccountDoesNotBelongToUser() {
            when(accountRepository.findByIdAndUserId(10L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userResourceResolver.resolveAccount(10L, 1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verify(accountRepository).findByIdAndUserId(10L, 1L);
        }
    }

    @Nested
    @DisplayName("resolveCategory")
    class ResolveCategoryTests {

        @Test
        @DisplayName("deve retornar categoria ativa quando ela pertencer ao usuario")
        void shouldResolveActiveCategory() {
            Category category = new Category();
            category.setId(20L);
            category.setActive(true);

            when(categoryRepository.findByIdAndUserIdAndActiveTrue(20L, 1L))
                    .thenReturn(Optional.of(category));

            Category result = userResourceResolver.resolveCategory(20L, 1L);

            assertThat(result).isEqualTo(category);

            verify(categoryRepository).findByIdAndUserIdAndActiveTrue(20L, 1L);
        }

        @Test
        @DisplayName("deve lancar erro quando categoria nao pertencer ao usuario ou estiver inativa")
        void shouldThrowWhenCategoryDoesNotBelongToUserOrIsInactive() {
            when(categoryRepository.findByIdAndUserIdAndActiveTrue(20L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userResourceResolver.resolveCategory(20L, 1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verify(categoryRepository).findByIdAndUserIdAndActiveTrue(20L, 1L);
        }
    }
}