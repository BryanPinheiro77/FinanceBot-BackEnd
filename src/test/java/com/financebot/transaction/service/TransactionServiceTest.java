package com.financebot.transaction.service;

import com.financebot.account.domain.Account;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.domain.installment.InstallmentPlan;
import com.financebot.transaction.domain.installment.InstallmentPlanFactory;
import com.financebot.transaction.domain.installment.InstallmentPlanItem;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.dto.request.CreateTransactionRequest;
import com.financebot.transaction.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.dto.response.TransactionResponse;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.validation.TransactionCategoryValidator;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import com.financebot.user.service.UserResourceResolver;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String INSTALLMENT_GROUP_ID = "installment-group-123";

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Mock
    private UserResourceResolver userResourceResolver;

    @Mock
    private TransactionCategoryValidator transactionCategoryValidator;

    @Mock
    private InstallmentPlanFactory installmentPlanFactory;

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

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
            when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);
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

            verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
            verify(transactionMapper).toEntity(request);
            verify(transactionRepository).save(transaction);
            verify(transactionMapper).toResponse(savedTransaction);
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

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(10L, 1L))
                    .thenThrow(new EntityNotFoundException("Account not found"));

            assertThatThrownBy(() -> transactionService.create(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verify(userResourceResolver, never()).resolveCategory(any(), any());
            verifyNoInteractions(transactionCategoryValidator, transactionRepository, transactionMapper);
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

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
            when(userResourceResolver.resolveCategory(20L, 1L))
                    .thenThrow(new EntityNotFoundException("Category not found"));

            assertThatThrownBy(() -> transactionService.create(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verifyNoInteractions(transactionCategoryValidator, transactionRepository, transactionMapper);
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

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
            when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);

            doThrow(new IllegalArgumentException("Category type does not match transaction type"))
                    .when(transactionCategoryValidator)
                    .validate(category, TransactionType.EXPENSE);

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
        @DisplayName("deve criar parcelamento com sucesso a partir do plano de dominio")
        void shouldCreateInstallmentSuccessfullyFromDomainPlan() {
            User user = buildUser(1L, "bryan@email.com");
            Account account = buildAccount(10L);
            Category category = buildCategory(20L, CategoryType.EXPENSE);

            CreateInstallmentTransactionRequest request = buildInstallmentRequest();

            TransactionResponse response1 = mock(TransactionResponse.class);
            TransactionResponse response2 = mock(TransactionResponse.class);
            TransactionResponse response3 = mock(TransactionResponse.class);

            mockAuthenticatedUser(user);
            mockInstallmentCreationDependencies(user, account, category, request, response1, response2, response3);

            InstallmentTransactionResponse result =
                    transactionService.createInstallment(request, authentication);

            List<Transaction> savedTransactions = captureSavedInstallments();

            assertInstallmentAmounts(savedTransactions);
            assertInstallmentDescriptions(savedTransactions);
            assertInstallmentDates(savedTransactions);
            assertInstallmentCommonData(savedTransactions, user, account, category);
            assertInstallmentMetadata(savedTransactions, result);
            assertInstallmentResponse(result, response1, response2, response3);

            verify(installmentPlanFactory).create(
                    request.totalAmount(),
                    request.description(),
                    request.firstInstallmentDate(),
                    request.type(),
                    request.totalInstallments()
            );
            verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("deve lançar erro quando plano de parcelamento rejeitar receita")
        void shouldThrowWhenInstallmentPlanRejectsIncomeTransaction() {
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

            mockAuthenticatedUser(user);
            when(installmentPlanFactory.create(
                    request.totalAmount(),
                    request.description(),
                    request.firstInstallmentDate(),
                    request.type(),
                    request.totalInstallments()
            )).thenThrow(new IllegalArgumentException("Installment transactions are allowed only for expenses"));

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Installment transactions are allowed only for expenses");

            verifyNoInteractions(userResourceResolver, transactionCategoryValidator, transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("deve lançar erro quando plano de parcelamento rejeitar total menor que dois")
        void shouldThrowWhenInstallmentPlanRejectsTotalInstallmentsLessThanTwo() {
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

            mockAuthenticatedUser(user);
            when(installmentPlanFactory.create(
                    request.totalAmount(),
                    request.description(),
                    request.firstInstallmentDate(),
                    request.type(),
                    request.totalInstallments()
            )).thenThrow(new IllegalArgumentException("Total installments must be at least 2"));

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Total installments must be at least 2");

            verifyNoInteractions(userResourceResolver, transactionCategoryValidator, transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("deve lançar erro quando conta não pertencer ao usuário no parcelamento")
        void shouldThrowWhenAccountIsNotFoundForUserOnCreateInstallment() {
            User user = buildUser(1L, "bryan@email.com");

            CreateInstallmentTransactionRequest request = buildInstallmentRequest();
            InstallmentPlan plan = buildInstallmentPlan();

            mockAuthenticatedUser(user);
            when(installmentPlanFactory.create(
                    request.totalAmount(),
                    request.description(),
                    request.firstInstallmentDate(),
                    request.type(),
                    request.totalInstallments()
            )).thenReturn(plan);
            when(userResourceResolver.resolveAccount(10L, 1L))
                    .thenThrow(new EntityNotFoundException("Account not found"));

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verify(userResourceResolver, never()).resolveCategory(any(), any());
            verifyNoInteractions(transactionCategoryValidator, transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não pertencer ao usuário no parcelamento")
        void shouldThrowWhenCategoryIsNotFoundForUserOnCreateInstallment() {
            User user = buildUser(1L, "bryan@email.com");
            Account account = buildAccount(10L);

            CreateInstallmentTransactionRequest request = buildInstallmentRequest();
            InstallmentPlan plan = buildInstallmentPlan();

            mockAuthenticatedUser(user);
            when(installmentPlanFactory.create(
                    request.totalAmount(),
                    request.description(),
                    request.firstInstallmentDate(),
                    request.type(),
                    request.totalInstallments()
            )).thenReturn(plan);
            when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
            when(userResourceResolver.resolveCategory(20L, 1L))
                    .thenThrow(new EntityNotFoundException("Category not found"));

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verifyNoInteractions(transactionCategoryValidator, transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("deve lançar erro quando categoria não combinar com o tipo no parcelamento")
        void shouldThrowWhenCategoryTypeDoesNotMatchTransactionTypeOnCreateInstallment() {
            User user = buildUser(1L, "bryan@email.com");
            Account account = buildAccount(10L);
            Category category = buildCategory(20L, CategoryType.INCOME);

            CreateInstallmentTransactionRequest request = buildInstallmentRequest();
            InstallmentPlan plan = buildInstallmentPlan();

            mockAuthenticatedUser(user);
            when(installmentPlanFactory.create(
                    request.totalAmount(),
                    request.description(),
                    request.firstInstallmentDate(),
                    request.type(),
                    request.totalInstallments()
            )).thenReturn(plan);
            when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
            when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);

            doThrow(new IllegalArgumentException("Category type does not match transaction type"))
                    .when(transactionCategoryValidator)
                    .validate(category, TransactionType.EXPENSE);

            assertThatThrownBy(() -> transactionService.createInstallment(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category type does not match transaction type");

            verifyNoInteractions(transactionMapper);
            verify(transactionRepository, never()).saveAll(anyList());
        }
    }

    @Test
    @DisplayName("deve criar parcelamento para usuario informado diretamente")
    void shouldCreateInstallmentForUserSuccessfully() {
        User user = buildUser(1L, "bryan@email.com");
        Account account = buildAccount(10L);
        Category category = buildCategory(20L, CategoryType.EXPENSE);

        CreateInstallmentTransactionRequest request = buildInstallmentRequest();

        TransactionResponse response1 = mock(TransactionResponse.class);
        TransactionResponse response2 = mock(TransactionResponse.class);
        TransactionResponse response3 = mock(TransactionResponse.class);

        mockInstallmentCreationDependencies(user, account, category, request, response1, response2, response3);

        InstallmentTransactionResponse result =
                transactionService.createInstallmentForUser(request, user);

        List<Transaction> savedTransactions = captureSavedInstallments();

        assertThat(savedTransactions).hasSize(3);
        assertThat(result.totalInstallments()).isEqualTo(3);
        assertThat(result.installmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(result.transactions()).containsExactly(response1, response2, response3);

        verify(installmentPlanFactory).create(
                request.totalAmount(),
                request.description(),
                request.firstInstallmentDate(),
                request.type(),
                request.totalInstallments()
        );
        verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
        verify(authenticatedUserResolver, never()).resolve(any());
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

            mockAuthenticatedUser(user);
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

            mockAuthenticatedUser(user);

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

            mockAuthenticatedUser(user);
            when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(java.util.Optional.of(transaction));
            when(transactionMapper.toResponse(transaction)).thenReturn(response);

            TransactionResponse result = transactionService.findById(99L, authentication);

            assertThat(result).isEqualTo(response);
            verify(transactionMapper).toResponse(transaction);
        }

        @Test
        @DisplayName("deve lançar erro quando transação não for encontrada para o usuário")
        void shouldThrowWhenTransactionIsNotFound() {
            User user = buildUser(1L, "bryan@email.com");

            mockAuthenticatedUser(user);
            when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(java.util.Optional.empty());

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

            mockAuthenticatedUser(user);
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(java.util.Optional.of(transaction));
            when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
            when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);
            when(transactionRepository.save(transaction)).thenReturn(transaction);
            when(transactionMapper.toResponse(transaction)).thenReturn(response);

            TransactionResponse result = transactionService.update(77L, request, authentication);

            assertThat(result).isEqualTo(response);
            assertThat(transaction.getAccount()).isEqualTo(account);
            assertThat(transaction.getCategory()).isEqualTo(category);

            verify(transactionCategoryValidator).validate(category, TransactionType.INCOME);
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

            mockAuthenticatedUser(user);
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> transactionService.update(77L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Transaction not found");

            verifyNoInteractions(userResourceResolver, transactionCategoryValidator, transactionMapper);
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

            mockAuthenticatedUser(user);
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(java.util.Optional.of(transaction));
            when(userResourceResolver.resolveAccount(10L, 1L))
                    .thenThrow(new EntityNotFoundException("Account not found"));

            assertThatThrownBy(() -> transactionService.update(77L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verify(userResourceResolver, never()).resolveCategory(any(), any());
            verifyNoInteractions(transactionCategoryValidator);
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

            mockAuthenticatedUser(user);
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(java.util.Optional.of(transaction));
            when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
            when(userResourceResolver.resolveCategory(20L, 1L))
                    .thenThrow(new EntityNotFoundException("Category not found"));

            assertThatThrownBy(() -> transactionService.update(77L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verifyNoInteractions(transactionCategoryValidator);
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

            mockAuthenticatedUser(user);
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(java.util.Optional.of(transaction));
            when(userResourceResolver.resolveAccount(10L, 1L)).thenReturn(account);
            when(userResourceResolver.resolveCategory(20L, 1L)).thenReturn(category);

            doThrow(new IllegalArgumentException("Category type does not match transaction type"))
                    .when(transactionCategoryValidator)
                    .validate(category, TransactionType.INCOME);

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

            mockAuthenticatedUser(user);
            when(transactionRepository.findByIdAndUserId(55L, 1L)).thenReturn(java.util.Optional.of(transaction));

            transactionService.delete(55L, authentication);

            verify(transactionRepository).delete(transaction);
        }

        @Test
        @DisplayName("deve lançar erro quando transação a deletar não existir para o usuário")
        void shouldThrowWhenTransactionToDeleteDoesNotExist() {
            User user = buildUser(1L, "bryan@email.com");

            mockAuthenticatedUser(user);
            when(transactionRepository.findByIdAndUserId(55L, 1L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> transactionService.delete(55L, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Transaction not found");

            verify(transactionRepository, never()).delete(any(Transaction.class));
        }
    }

    private void mockAuthenticatedUser(User user) {
        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
    }

    private CreateInstallmentTransactionRequest buildInstallmentRequest() {
        return new CreateInstallmentTransactionRequest(
                new BigDecimal("1000.00"),
                "Notebook",
                LocalDate.of(2026, 4, 10),
                TransactionType.EXPENSE,
                SourceType.WEB,
                10L,
                20L,
                3
        );
    }

    private void mockInstallmentCreationDependencies(
            User user,
            Account account,
            Category category,
            CreateInstallmentTransactionRequest request,
            TransactionResponse response1,
            TransactionResponse response2,
            TransactionResponse response3
    ) {
        InstallmentPlan plan = buildInstallmentPlan();

        when(installmentPlanFactory.create(
                request.totalAmount(),
                request.description(),
                request.firstInstallmentDate(),
                request.type(),
                request.totalInstallments()
        )).thenReturn(plan);

        when(userResourceResolver.resolveAccount(request.accountId(), user.getId())).thenReturn(account);
        when(userResourceResolver.resolveCategory(request.categoryId(), user.getId())).thenReturn(category);

        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(response1, response2, response3);
    }

    private InstallmentPlan buildInstallmentPlan() {
        List<InstallmentPlanItem> items = List.of(
                new InstallmentPlanItem(
                        new BigDecimal("333.33"),
                        "Notebook - 1/3",
                        LocalDate.of(2026, 4, 10),
                        1,
                        3,
                        INSTALLMENT_GROUP_ID
                ),
                new InstallmentPlanItem(
                        new BigDecimal("333.33"),
                        "Notebook - 2/3",
                        LocalDate.of(2026, 5, 10),
                        2,
                        3,
                        INSTALLMENT_GROUP_ID
                ),
                new InstallmentPlanItem(
                        new BigDecimal("333.34"),
                        "Notebook - 3/3",
                        LocalDate.of(2026, 6, 10),
                        3,
                        3,
                        INSTALLMENT_GROUP_ID
                )
        );

        return new InstallmentPlan(
                INSTALLMENT_GROUP_ID,
                3,
                items
        );
    }

    private List<Transaction> captureSavedInstallments() {
        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);

        verify(transactionRepository).saveAll(captor.capture());

        return captor.getValue();
    }

    private void assertInstallmentAmounts(List<Transaction> savedTransactions) {
        assertThat(savedTransactions).hasSize(3);
        assertThat(savedTransactions.get(0).getAmount()).isEqualByComparingTo("333.33");
        assertThat(savedTransactions.get(1).getAmount()).isEqualByComparingTo("333.33");
        assertThat(savedTransactions.get(2).getAmount()).isEqualByComparingTo("333.34");
    }

    private void assertInstallmentDescriptions(List<Transaction> savedTransactions) {
        assertThat(savedTransactions.get(0).getDescription()).isEqualTo("Notebook - 1/3");
        assertThat(savedTransactions.get(1).getDescription()).isEqualTo("Notebook - 2/3");
        assertThat(savedTransactions.get(2).getDescription()).isEqualTo("Notebook - 3/3");
    }

    private void assertInstallmentDates(List<Transaction> savedTransactions) {
        assertThat(savedTransactions.get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(savedTransactions.get(1).getDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(savedTransactions.get(2).getDate()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    private void assertInstallmentCommonData(
            List<Transaction> savedTransactions,
            User user,
            Account account,
            Category category
    ) {
        Transaction first = savedTransactions.get(0);

        assertThat(first.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(first.getSourceType()).isEqualTo(SourceType.WEB);
        assertThat(first.getUser()).isEqualTo(user);
        assertThat(first.getAccount()).isEqualTo(account);
        assertThat(first.getCategory()).isEqualTo(category);
    }

    private void assertInstallmentMetadata(
            List<Transaction> savedTransactions,
            InstallmentTransactionResponse result
    ) {
        Transaction first = savedTransactions.get(0);
        Transaction second = savedTransactions.get(1);
        Transaction third = savedTransactions.get(2);

        assertThat(first.getInstallment()).isTrue();
        assertThat(second.getInstallment()).isTrue();
        assertThat(third.getInstallment()).isTrue();

        assertThat(first.getInstallmentNumber()).isEqualTo(1);
        assertThat(second.getInstallmentNumber()).isEqualTo(2);
        assertThat(third.getInstallmentNumber()).isEqualTo(3);

        assertThat(first.getTotalInstallments()).isEqualTo(3);
        assertThat(second.getTotalInstallments()).isEqualTo(3);
        assertThat(third.getTotalInstallments()).isEqualTo(3);

        assertThat(first.getInstallmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(second.getInstallmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(third.getInstallmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);

        assertThat(result.installmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
    }

    private void assertInstallmentResponse(
            InstallmentTransactionResponse result,
            TransactionResponse response1,
            TransactionResponse response2,
            TransactionResponse response3
    ) {
        assertThat(result.installmentGroupId()).isEqualTo(INSTALLMENT_GROUP_ID);
        assertThat(result.totalInstallments()).isEqualTo(3);
        assertThat(result.transactions()).containsExactly(response1, response2, response3);
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