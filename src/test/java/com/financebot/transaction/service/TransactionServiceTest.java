package com.financebot.transaction.service;

import com.financebot.account.domain.Account;
import com.financebot.account.repository.AccountRepository;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.category.repository.CategoryRepository;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.CreateInstallmentTransactionRequest;
import com.financebot.transaction.dto.CreateTransactionRequest;
import com.financebot.transaction.dto.InstallmentTransactionResponse;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.dto.TransactionResponse;
import com.financebot.transaction.dto.UpdateTransactionRequest;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionService transactionService;

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("deve criar transação com sucesso quando dados forem válidos")
        void shouldCreateTransactionSuccessfully() {
            User user = buildUser(1L, "bryan@email.com");
            Account account = buildAccount(10L);
            Category category = buildCategory(20L, CategoryType.EXPENSE);

            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    "Mercado",
                    LocalDate.of(2026, 4, 4),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L
            );

            Transaction transaction = new Transaction();
            Transaction savedTransaction = new Transaction();
            TransactionResponse response = mock(TransactionResponse.class);

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(category));
            when(transactionMapper.toEntity(request)).thenReturn(transaction);
            when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
            when(transactionMapper.toResponse(savedTransaction)).thenReturn(response);

            TransactionResponse result = transactionService.create(request, authentication);

            assertThat(result).isEqualTo(response);
            assertThat(transaction.getUser()).isEqualTo(user);
            assertThat(transaction.getAccount()).isEqualTo(account);
            assertThat(transaction.getCategory()).isEqualTo(category);
            assertThat(transaction.getInstallment()).isFalse();
            assertThat(transaction.getInstallmentNumber()).isNull();
            assertThat(transaction.getTotalInstallments()).isNull();
            assertThat(transaction.getInstallmentGroupId()).isNull();

            verify(transactionMapper).toEntity(request);
            verify(transactionRepository).save(transaction);
            verify(transactionMapper).toResponse(savedTransaction);
        }

        @Test
        @DisplayName("deve lançar erro quando authentication for nulo")
        void shouldThrowWhenAuthenticationIsNull() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    "Mercado",
                    LocalDate.of(2026, 4, 4),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L
            );

            assertThatThrownBy(() -> transactionService.create(request, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Authenticated user is invalid");

            verifyNoInteractions(userRepository, accountRepository, categoryRepository, transactionRepository, transactionMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando authentication não possuir nome")
        void shouldThrowWhenAuthenticationNameIsNull() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    "Mercado",
                    LocalDate.of(2026, 4, 4),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn(null);

            assertThatThrownBy(() -> transactionService.create(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Authenticated user is invalid");

            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("deve lançar erro quando usuário autenticado não for encontrado")
        void shouldThrowWhenAuthenticatedUserIsNotFound() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    "Mercado",
                    LocalDate.of(2026, 4, 4),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.create(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Authenticated user not found");

            verifyNoInteractions(accountRepository, categoryRepository, transactionRepository, transactionMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando conta não pertencer ao usuário")
        void shouldThrowWhenAccountIsNotFoundForUser() {
            User user = buildUser(1L, "bryan@email.com");

            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    "Mercado",
                    LocalDate.of(2026, 4, 4),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.create(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verifyNoInteractions(categoryRepository, transactionRepository, transactionMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não pertencer ao usuário")
        void shouldThrowWhenCategoryIsNotFoundForUser() {
            User user = buildUser(1L, "bryan@email.com");
            Account account = buildAccount(10L);

            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    "Mercado",
                    LocalDate.of(2026, 4, 4),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.create(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verifyNoInteractions(transactionRepository, transactionMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não combinar com o tipo da transação")
        void shouldThrowWhenCategoryTypeDoesNotMatchTransactionType() {
            User user = buildUser(1L, "bryan@email.com");
            Account account = buildAccount(10L);
            Category category = buildCategory(20L, CategoryType.INCOME);

            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("150.00"),
                    "Mercado",
                    LocalDate.of(2026, 4, 4),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(category));

            assertThatThrownBy(() -> transactionService.create(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category type does not match transaction type");

            verifyNoInteractions(transactionRepository, transactionMapper);
        }
    }

    @Nested
    @DisplayName("createInstallment")
    class CreateInstallmentTests {

        @Test
        @DisplayName("deve criar parcelamento com sucesso e ajustar a última parcela")
        void shouldCreateInstallmentSuccessfullyWithRoundingAdjustment() {
            User user = buildUser(1L, "bryan@email.com");
            Account account = buildAccount(10L);
            Category category = buildCategory(20L, CategoryType.EXPENSE);

            CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L,
                    3
            );

            TransactionResponse response1 = mock(TransactionResponse.class);
            TransactionResponse response2 = mock(TransactionResponse.class);
            TransactionResponse response3 = mock(TransactionResponse.class);

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(category));
            when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            when(transactionMapper.toResponse(any(Transaction.class)))
                    .thenReturn(response1, response2, response3);

            InstallmentTransactionResponse result =
                    transactionService.createInstallment(request, authentication);

            ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
            verify(transactionRepository).saveAll(captor.capture());

            List<Transaction> savedTransactions = captor.getValue();

            assertThat(savedTransactions).hasSize(3);

            Transaction first = savedTransactions.get(0);
            Transaction second = savedTransactions.get(1);
            Transaction third = savedTransactions.get(2);

            assertThat(first.getAmount()).isEqualByComparingTo("333.33");
            assertThat(second.getAmount()).isEqualByComparingTo("333.33");
            assertThat(third.getAmount()).isEqualByComparingTo("333.34");

            assertThat(first.getDescription()).isEqualTo("Notebook - 1/3");
            assertThat(second.getDescription()).isEqualTo("Notebook - 2/3");
            assertThat(third.getDescription()).isEqualTo("Notebook - 3/3");

            assertThat(first.getDate()).isEqualTo(LocalDate.of(2026, 4, 10));
            assertThat(second.getDate()).isEqualTo(LocalDate.of(2026, 5, 10));
            assertThat(third.getDate()).isEqualTo(LocalDate.of(2026, 6, 10));

            assertThat(first.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(first.getSourceType()).isEqualTo(SourceType.WEB);
            assertThat(first.getUser()).isEqualTo(user);
            assertThat(first.getAccount()).isEqualTo(account);
            assertThat(first.getCategory()).isEqualTo(category);

            assertThat(first.getInstallment()).isTrue();
            assertThat(second.getInstallment()).isTrue();
            assertThat(third.getInstallment()).isTrue();

            assertThat(first.getInstallmentNumber()).isEqualTo(1);
            assertThat(second.getInstallmentNumber()).isEqualTo(2);
            assertThat(third.getInstallmentNumber()).isEqualTo(3);

            assertThat(first.getTotalInstallments()).isEqualTo(3);
            assertThat(second.getTotalInstallments()).isEqualTo(3);
            assertThat(third.getTotalInstallments()).isEqualTo(3);

            assertThat(first.getInstallmentGroupId()).isNotBlank();
            assertThat(second.getInstallmentGroupId()).isEqualTo(first.getInstallmentGroupId());
            assertThat(third.getInstallmentGroupId()).isEqualTo(first.getInstallmentGroupId());

            assertThat(result.installmentGroupId()).isEqualTo(first.getInstallmentGroupId());
            assertThat(result.totalInstallments()).isEqualTo(3);
            assertThat(result.transactions()).containsExactly(response1, response2, response3);
        }

        @Test
        @DisplayName("deve lançar erro quando tentar parcelar uma receita")
        void shouldThrowWhenTryingToCreateInstallmentForIncome() {
            User user = buildUser(1L, "bryan@email.com");

            CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                    new BigDecimal("1000.00"),
                    "Salário parcelado",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.INCOME,
                    SourceType.WEB,
                    10L,
                    20L,
                    3
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Installment transactions are allowed only for expenses");

            verifyNoInteractions(accountRepository, categoryRepository, transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("deve lançar erro quando total de parcelas for menor que 2")
        void shouldThrowWhenTotalInstallmentsIsLessThanTwo() {
            User user = buildUser(1L, "bryan@email.com");

            CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L,
                    1
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Total installments must be at least 2");

            verifyNoInteractions(accountRepository, categoryRepository, transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("deve lançar erro quando authentication for nulo no parcelamento")
        void shouldThrowWhenAuthenticationIsNullOnCreateInstallment() {
            CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L,
                    3
            );

            assertThatThrownBy(() -> transactionService.createInstallment(request, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Authenticated user is invalid");

            verifyNoInteractions(userRepository, accountRepository, categoryRepository, transactionRepository, transactionMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando usuário autenticado não for encontrado no parcelamento")
        void shouldThrowWhenAuthenticatedUserIsNotFoundOnCreateInstallment() {
            CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L,
                    3
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Authenticated user not found");

            verifyNoInteractions(accountRepository, categoryRepository, transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("deve lançar erro quando conta não pertencer ao usuário no parcelamento")
        void shouldThrowWhenAccountIsNotFoundForUserOnCreateInstallment() {
            User user = buildUser(1L, "bryan@email.com");

            CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L,
                    3
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verifyNoInteractions(categoryRepository, transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não pertencer ao usuário no parcelamento")
        void shouldThrowWhenCategoryIsNotFoundForUserOnCreateInstallment() {
            User user = buildUser(1L, "bryan@email.com");
            Account account = buildAccount(10L);

            CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L,
                    3
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verifyNoInteractions(transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não combinar com o tipo no parcelamento")
        void shouldThrowWhenCategoryTypeDoesNotMatchTransactionTypeOnCreateInstallment() {
            User user = buildUser(1L, "bryan@email.com");
            Account account = buildAccount(10L);
            Category category = buildCategory(20L, CategoryType.INCOME);

            CreateInstallmentTransactionRequest request = new CreateInstallmentTransactionRequest(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    10L,
                    20L,
                    3
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(category));

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category type does not match transaction type");

            verifyNoInteractions(transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("deve listar transações com sucesso quando período for válido")
        void shouldFindAllSuccessfully() {
            User user = buildUser(1L, "bryan@email.com");

            TransactionFilter filter = new TransactionFilter(
                    TransactionType.EXPENSE,
                    20L,
                    10L,
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 30),
                    SourceType.WEB,
                    "mercado"
            );

            Pageable pageable = PageRequest.of(0, 10, Sort.by("date").descending());
            Transaction transaction = new Transaction();
            TransactionResponse response = mock(TransactionResponse.class);

            Page<Transaction> page = new PageImpl<>(List.of(transaction), pageable, 1);

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(transactionMapper.toResponse(transaction)).thenReturn(response);

            Page<TransactionResponse> result = transactionService.findAll(filter, authentication, pageable);

            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
            verify(transactionMapper).toResponse(transaction);
        }

        @Test
        @DisplayName("deve lançar erro quando startDate for maior que endDate")
        void shouldThrowWhenStartDateIsAfterEndDate() {
            User user = buildUser(1L, "bryan@email.com");

            TransactionFilter filter = new TransactionFilter(
                    null,
                    null,
                    null,
                    LocalDate.of(2026, 4, 30),
                    LocalDate.of(2026, 4, 1),
                    null,
                    null
            );

            Pageable pageable = PageRequest.of(0, 10);

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> transactionService.findAll(filter, authentication, pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Start date cannot be after end date");

            verify(transactionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
            verifyNoInteractions(transactionMapper);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("deve buscar transação por id com sucesso")
        void shouldFindByIdSuccessfully() {
            User user = buildUser(1L, "bryan@email.com");
            Transaction transaction = new Transaction();
            TransactionResponse response = mock(TransactionResponse.class);

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(transaction));
            when(transactionMapper.toResponse(transaction)).thenReturn(response);

            TransactionResponse result = transactionService.findById(99L, authentication);

            assertThat(result).isEqualTo(response);
            verify(transactionMapper).toResponse(transaction);
        }

        @Test
        @DisplayName("deve lançar erro quando transação não for encontrada para o usuário")
        void shouldThrowWhenTransactionIsNotFound() {
            User user = buildUser(1L, "bryan@email.com");

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.findById(99L, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Transaction not found");

            verifyNoInteractions(transactionMapper);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("deve atualizar transação com sucesso")
        void shouldUpdateSuccessfully() {
            User user = buildUser(1L, "bryan@email.com");
            Transaction transaction = new Transaction();
            Account account = buildAccount(10L);
            Category category = buildCategory(20L, CategoryType.INCOME);
            TransactionResponse response = mock(TransactionResponse.class);

            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("500.00"),
                    "Salário",
                    LocalDate.of(2026, 4, 5),
                    TransactionType.INCOME,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(transaction));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(category));
            when(transactionRepository.save(transaction)).thenReturn(transaction);
            when(transactionMapper.toResponse(transaction)).thenReturn(response);

            TransactionResponse result = transactionService.update(77L, request, authentication);

            assertThat(result).isEqualTo(response);
            assertThat(transaction.getAccount()).isEqualTo(account);
            assertThat(transaction.getCategory()).isEqualTo(category);

            verify(transactionMapper).updateEntity(request, transaction);
            verify(transactionRepository).save(transaction);
            verify(transactionMapper).toResponse(transaction);
        }

        @Test
        @DisplayName("deve lançar erro quando transação a atualizar não existir")
        void shouldThrowWhenTransactionToUpdateDoesNotExist() {
            User user = buildUser(1L, "bryan@email.com");

            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("500.00"),
                    "Salário",
                    LocalDate.of(2026, 4, 5),
                    TransactionType.INCOME,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.update(77L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Transaction not found");

            verifyNoInteractions(accountRepository, categoryRepository, transactionMapper);
        }

        @Test
        @DisplayName("deve lançar erro quando conta informada no update não existir para o usuário")
        void shouldThrowWhenAccountForUpdateIsNotFound() {
            User user = buildUser(1L, "bryan@email.com");
            Transaction transaction = new Transaction();

            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("500.00"),
                    "Salário",
                    LocalDate.of(2026, 4, 5),
                    TransactionType.INCOME,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(transaction));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.update(77L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verifyNoInteractions(categoryRepository);
            verify(transactionMapper, never()).updateEntity(any(), any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar erro quando categoria informada no update não existir para o usuário")
        void shouldThrowWhenCategoryForUpdateIsNotFound() {
            User user = buildUser(1L, "bryan@email.com");
            Transaction transaction = new Transaction();
            Account account = buildAccount(10L);

            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("500.00"),
                    "Salário",
                    LocalDate.of(2026, 4, 5),
                    TransactionType.INCOME,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(transaction));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.update(77L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verify(transactionMapper, never()).updateEntity(any(), any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não combinar com o tipo no update")
        void shouldThrowWhenCategoryTypeDoesNotMatchOnUpdate() {
            User user = buildUser(1L, "bryan@email.com");
            Transaction transaction = new Transaction();
            Account account = buildAccount(10L);
            Category category = buildCategory(20L, CategoryType.EXPENSE);

            UpdateTransactionRequest request = new UpdateTransactionRequest(
                    new BigDecimal("500.00"),
                    "Salário",
                    LocalDate.of(2026, 4, 5),
                    TransactionType.INCOME,
                    SourceType.WEB,
                    10L,
                    20L
            );

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(transaction));
            when(accountRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
            when(categoryRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(category));

            assertThatThrownBy(() -> transactionService.update(77L, request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category type does not match transaction type");

            verify(transactionMapper, never()).updateEntity(any(), any());
            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deve deletar transação com sucesso")
        void shouldDeleteSuccessfully() {
            User user = buildUser(1L, "bryan@email.com");
            Transaction transaction = new Transaction();

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findByIdAndUserId(55L, 1L)).thenReturn(Optional.of(transaction));

            transactionService.delete(55L, authentication);

            verify(transactionRepository).delete(transaction);
        }

        @Test
        @DisplayName("deve lançar erro quando transação a deletar não existir para o usuário")
        void shouldThrowWhenTransactionToDeleteDoesNotExist() {
            User user = buildUser(1L, "bryan@email.com");

            when(authentication.getName()).thenReturn("bryan@email.com");
            when(userRepository.findByEmail("bryan@email.com")).thenReturn(Optional.of(user));
            when(transactionRepository.findByIdAndUserId(55L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.delete(55L, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Transaction not found");

            verify(transactionRepository, never()).delete(any(Transaction.class));
        }
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private Account buildAccount(Long id) {
        Account account = new Account();
        account.setId(id);
        account.setName("Conta teste");
        return account;
    }

    private Category buildCategory(Long id, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setName("Categoria teste");
        category.setType(type);
        return category;
    }
}