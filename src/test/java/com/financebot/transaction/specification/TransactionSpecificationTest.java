package com.financebot.transaction.specification;

import com.financebot.account.domain.Account;
import com.financebot.account.domain.AccountType;
import com.financebot.account.repository.AccountRepository;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionSpecificationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("deve filtrar transacoes por todos os filtros informados")
    void shouldFilterTransactionsByAllFilters() {
        User user = saveUser("bryan@email.com");
        User otherUser = saveUser("other@email.com");

        Account account = saveAccount(user, "Conta Principal");
        Account otherAccount = saveAccount(user, "Outra Conta");

        Category category = saveCategory(user, "Mercado", CategoryType.EXPENSE);
        Category incomeCategory = saveCategory(user, "Salario", CategoryType.INCOME);

        saveTransaction(
                user,
                account,
                category,
                new BigDecimal("100.00"),
                "Compra no mercado",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        saveTransaction(
                user,
                otherAccount,
                category,
                new BigDecimal("200.00"),
                "Compra no mercado outra conta",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        saveTransaction(
                user,
                account,
                incomeCategory,
                new BigDecimal("3000.00"),
                "Salario mensal",
                LocalDate.of(2026, 4, 10),
                TransactionType.INCOME,
                SourceType.WEB
        );

        saveTransaction(
                otherUser,
                saveAccount(otherUser, "Conta Outro Usuario"),
                saveCategory(otherUser, "Mercado", CategoryType.EXPENSE),
                new BigDecimal("150.00"),
                "Compra no mercado",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        TransactionFilter filter = new TransactionFilter(
                TransactionType.EXPENSE,
                category.getId(),
                account.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                SourceType.WEB,
                "mercado"
        );

        Specification<Transaction> specification = TransactionSpecification.withFilters(user.getId(), filter);

        List<Transaction> result = transactionRepository.findAll(specification);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("Compra no mercado");
        assertThat(result.get(0).getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get(0).getAccount().getId()).isEqualTo(account.getId());
        assertThat(result.get(0).getCategory().getId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("deve filtrar somente por usuario quando filtro for nulo")
    void shouldFilterOnlyByUserWhenFilterIsNull() {
        User user = saveUser("bryan@email.com");
        User otherUser = saveUser("other@email.com");

        Account account = saveAccount(user, "Conta Principal");
        Category category = saveCategory(user, "Mercado", CategoryType.EXPENSE);

        saveTransaction(
                user,
                account,
                category,
                new BigDecimal("100.00"),
                "Compra usuario",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        saveTransaction(
                otherUser,
                saveAccount(otherUser, "Conta Outro Usuario"),
                saveCategory(otherUser, "Mercado", CategoryType.EXPENSE),
                new BigDecimal("150.00"),
                "Compra outro usuario",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        Specification<Transaction> specification = TransactionSpecification.withFilters(user.getId(), null);

        List<Transaction> result = transactionRepository.findAll(specification);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("Compra usuario");
    }

    @Test
    @DisplayName("deve filtrar usando apenas data inicial")
    void shouldFilterByStartDateOnly() {
        User user = saveUser("bryan@email.com");
        Account account = saveAccount(user, "Conta Principal");
        Category category = saveCategory(user, "Mercado", CategoryType.EXPENSE);

        saveTransaction(
                user,
                account,
                category,
                new BigDecimal("100.00"),
                "Compra antiga",
                LocalDate.of(2026, 3, 20),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        saveTransaction(
                user,
                account,
                category,
                new BigDecimal("200.00"),
                "Compra recente",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        TransactionFilter filter = new TransactionFilter(
                null,
                null,
                null,
                LocalDate.of(2026, 4, 1),
                null,
                null,
                null
        );

        Specification<Transaction> specification = TransactionSpecification.withFilters(user.getId(), filter);

        List<Transaction> result = transactionRepository.findAll(specification);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("Compra recente");
    }

    @Test
    @DisplayName("deve filtrar usando apenas data final")
    void shouldFilterByEndDateOnly() {
        User user = saveUser("bryan@email.com");
        Account account = saveAccount(user, "Conta Principal");
        Category category = saveCategory(user, "Mercado", CategoryType.EXPENSE);

        saveTransaction(
                user,
                account,
                category,
                new BigDecimal("100.00"),
                "Compra antiga",
                LocalDate.of(2026, 3, 20),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        saveTransaction(
                user,
                account,
                category,
                new BigDecimal("200.00"),
                "Compra recente",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        TransactionFilter filter = new TransactionFilter(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 3, 31),
                null,
                null
        );

        Specification<Transaction> specification = TransactionSpecification.withFilters(user.getId(), filter);

        List<Transaction> result = transactionRepository.findAll(specification);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("Compra antiga");
    }

    @Test
    @DisplayName("deve ignorar filtro de descricao em branco")
    void shouldIgnoreBlankDescriptionFilter() {
        User user = saveUser("bryan@email.com");
        Account account = saveAccount(user, "Conta Principal");
        Category category = saveCategory(user, "Mercado", CategoryType.EXPENSE);

        saveTransaction(
                user,
                account,
                category,
                new BigDecimal("100.00"),
                "Compra mercado",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB
        );

        TransactionFilter filter = new TransactionFilter(
                null,
                null,
                null,
                null,
                null,
                null,
                "   "
        );

        Specification<Transaction> specification = TransactionSpecification.withFilters(user.getId(), filter);

        List<Transaction> result = transactionRepository.findAll(specification);

        assertThat(result).hasSize(1);
    }

    private User saveUser(String email) {
        User user = new User();
        user.setName("Bryan");
        user.setEmail(email);
        user.setPassword("123456");
        user.setMonthlyBaseIncome(new BigDecimal("3000.00"));

        return userRepository.save(user);
    }

    private Account saveAccount(User user, String name) {
        Account account = new Account();
        account.setName(name);
        account.setType(AccountType.CHECKING_ACCOUNT);
        account.setInitialBalance(BigDecimal.ZERO);
        account.setDefaultAccount(false);
        account.setUser(user);

        return accountRepository.save(account);
    }

    private Category saveCategory(User user, String name, CategoryType type) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        category.setUser(user);

        return categoryRepository.save(category);
    }

    private Transaction saveTransaction(
            User user,
            Account account,
            Category category,
            BigDecimal amount,
            String description,
            LocalDate date,
            TransactionType type,
            SourceType sourceType
    ) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setDate(date);
        transaction.setType(type);
        transaction.setSourceType(sourceType);
        transaction.setInstallment(false);

        return transactionRepository.save(transaction);
    }
}