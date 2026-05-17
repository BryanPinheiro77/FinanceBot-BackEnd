package com.financebot.recurring.service;

import com.financebot.account.domain.Account;
import com.financebot.account.domain.AccountType;
import com.financebot.category.domain.Category;
import com.financebot.category.domain.CategoryType;
import com.financebot.recurring.domain.RecurrenceFrequency;
import com.financebot.recurring.domain.RecurringTransaction;
import com.financebot.recurring.dto.request.CreateRecurringTransactionRequest;
import com.financebot.recurring.dto.request.UpdateRecurringTransactionRequest;
import com.financebot.recurring.dto.response.RecurringTransactionResponse;
import com.financebot.recurring.mapper.RecurringTransactionMapper;
import com.financebot.recurring.repository.RecurringTransactionRepository;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long CATEGORY_ID = 20L;
    private static final Long RECURRING_TRANSACTION_ID = 100L;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Spy
    private RecurringTransactionMapper recurringTransactionMapper = new RecurringTransactionMapper();

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Mock
    private UserResourceResolver userResourceResolver;

    @Mock
    private TransactionCategoryValidator transactionCategoryValidator;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("deve criar transacao recorrente com sucesso")
        void shouldCreateRecurringTransactionSuccessfully() {
            User user = buildUser();
            Account account = buildAccount();
            Category category = buildCategory(CategoryType.EXPENSE);

            CreateRecurringTransactionRequest request = buildCreateRequest();

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID)).thenReturn(category);
            when(recurringTransactionRepository.save(any(RecurringTransaction.class)))
                    .thenAnswer(invocation -> {
                        RecurringTransaction recurringTransaction = invocation.getArgument(0);
                        recurringTransaction.setId(RECURRING_TRANSACTION_ID);
                        return recurringTransaction;
                    });

            RecurringTransactionResponse response = recurringTransactionService.create(request, authentication);

            assertThat(response.id()).isEqualTo(RECURRING_TRANSACTION_ID);
            assertThat(response.description()).isEqualTo("Aluguel");
            assertThat(response.amount()).isEqualByComparingTo("1200.00");
            assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(response.sourceType()).isEqualTo(SourceType.WEB);
            assertThat(response.frequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
            assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 12, 1));
            assertThat(response.nextExecutionDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(response.active()).isTrue();
            assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
            assertThat(response.categoryId()).isEqualTo(CATEGORY_ID);

            ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);
            verify(recurringTransactionRepository).save(captor.capture());

            RecurringTransaction saved = captor.getValue();

            assertThat(saved.getDescription()).isEqualTo("Aluguel");
            assertThat(saved.getAmount()).isEqualByComparingTo("1200.00");
            assertThat(saved.getType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(saved.getSourceType()).isEqualTo(SourceType.WEB);
            assertThat(saved.getFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);
            assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 1));
            assertThat(saved.getNextExecutionDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(saved.isActive()).isTrue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getAccount()).isEqualTo(account);
            assertThat(saved.getCategory()).isEqualTo(category);

            verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("deve remover espacos da descricao ao criar")
        void shouldTrimDescriptionWhenCreating() {
            User user = buildUser();
            Account account = buildAccount();
            Category category = buildCategory(CategoryType.EXPENSE);

            CreateRecurringTransactionRequest request = new CreateRecurringTransactionRequest(
                    "  Academia  ",
                    new BigDecimal("99.90"),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    RecurrenceFrequency.MONTHLY,
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 12, 1),
                    ACCOUNT_ID,
                    CATEGORY_ID
            );

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID)).thenReturn(category);
            when(recurringTransactionRepository.save(any(RecurringTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RecurringTransactionResponse response = recurringTransactionService.create(request, authentication);

            assertThat(response.description()).isEqualTo("Academia");

            verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("deve lancar erro quando categoria nao combinar com tipo da transacao")
        void shouldThrowWhenCategoryDoesNotMatchTransactionTypeOnCreate() {
            User user = buildUser();
            Account account = buildAccount();
            Category category = buildCategory(CategoryType.INCOME);

            CreateRecurringTransactionRequest request = buildCreateRequest();

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID)).thenReturn(category);

            doThrow(new IllegalArgumentException("Category type does not match transaction type"))
                    .when(transactionCategoryValidator)
                    .validate(category, TransactionType.EXPENSE);

            assertThatThrownBy(() -> recurringTransactionService.create(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category type does not match transaction type");

            verify(recurringTransactionRepository, never()).save(any(RecurringTransaction.class));
        }

        @Test
        @DisplayName("deve lancar erro quando conta nao existir")
        void shouldThrowWhenAccountDoesNotExistOnCreate() {
            User user = buildUser();

            CreateRecurringTransactionRequest request = buildCreateRequest();

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID))
                    .thenThrow(new EntityNotFoundException("Account not found"));

            assertThatThrownBy(() -> recurringTransactionService.create(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Account not found");

            verify(userResourceResolver, never()).resolveCategory(any(), any());
            verifyNoInteractions(transactionCategoryValidator);
            verify(recurringTransactionRepository, never()).save(any(RecurringTransaction.class));
        }

        @Test
        @DisplayName("deve lancar erro quando categoria nao existir")
        void shouldThrowWhenCategoryDoesNotExistOnCreate() {
            User user = buildUser();
            Account account = buildAccount();

            CreateRecurringTransactionRequest request = buildCreateRequest();

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID))
                    .thenThrow(new EntityNotFoundException("Category not found"));

            assertThatThrownBy(() -> recurringTransactionService.create(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Category not found");

            verifyNoInteractions(transactionCategoryValidator);
            verify(recurringTransactionRepository, never()).save(any(RecurringTransaction.class));
        }

        @Test
        @DisplayName("deve lancar erro quando data final for anterior a data inicial ao criar")
        void shouldThrowWhenEndDateIsBeforeStartDateOnCreate() {
            User user = buildUser();
            Account account = buildAccount();
            Category category = buildCategory(CategoryType.EXPENSE);

            CreateRecurringTransactionRequest request = new CreateRecurringTransactionRequest(
                    "Aluguel",
                    new BigDecimal("1200.00"),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    RecurrenceFrequency.MONTHLY,
                    LocalDate.of(2026, 4, 10),
                    LocalDate.of(2026, 4, 1),
                    ACCOUNT_ID,
                    CATEGORY_ID
            );

            mockAuthenticatedUser(user);
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID)).thenReturn(category);

            assertThatThrownBy(() -> recurringTransactionService.create(request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("End date cannot be before start date");

            verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
            verify(recurringTransactionRepository, never()).save(any(RecurringTransaction.class));
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("deve listar transacoes recorrentes do usuario autenticado")
        void shouldFindAllRecurringTransactions() {
            User user = buildUser();

            RecurringTransaction first = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );

            RecurringTransaction second = buildRecurringTransaction(
                    101L,
                    "Salario",
                    TransactionType.INCOME,
                    CategoryType.INCOME
            );

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(first, second));

            List<RecurringTransactionResponse> responses = recurringTransactionService.findAll(authentication);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).id()).isEqualTo(RECURRING_TRANSACTION_ID);
            assertThat(responses.get(0).description()).isEqualTo("Aluguel");
            assertThat(responses.get(1).id()).isEqualTo(101L);
            assertThat(responses.get(1).description()).isEqualTo("Salario");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("deve buscar transacao recorrente por id")
        void shouldFindRecurringTransactionById() {
            User user = buildUser();
            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));

            RecurringTransactionResponse response = recurringTransactionService.findById(
                    RECURRING_TRANSACTION_ID,
                    authentication
            );

            assertThat(response.id()).isEqualTo(RECURRING_TRANSACTION_ID);
            assertThat(response.description()).isEqualTo("Aluguel");
            assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
            assertThat(response.categoryId()).isEqualTo(CATEGORY_ID);
        }

        @Test
        @DisplayName("deve lancar erro quando transacao recorrente nao existir")
        void shouldThrowWhenRecurringTransactionDoesNotExist() {
            User user = buildUser();

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> recurringTransactionService.findById(RECURRING_TRANSACTION_ID, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Recurring transaction not found");
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("deve atualizar transacao recorrente com sucesso")
        void shouldUpdateRecurringTransactionSuccessfully() {
            User user = buildUser();
            Account account = buildAccount();
            Category category = buildCategory(CategoryType.EXPENSE);

            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );
            recurringTransaction.setNextExecutionDate(LocalDate.of(2026, 4, 1));

            UpdateRecurringTransactionRequest request = buildUpdateRequest(
                    "  Internet  ",
                    LocalDate.of(2026, 5, 1)
            );

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID)).thenReturn(category);
            when(recurringTransactionRepository.save(recurringTransaction))
                    .thenReturn(recurringTransaction);

            RecurringTransactionResponse response = recurringTransactionService.update(
                    RECURRING_TRANSACTION_ID,
                    request,
                    authentication
            );

            assertThat(response.description()).isEqualTo("Internet");
            assertThat(response.amount()).isEqualByComparingTo("150.00");
            assertThat(response.active()).isTrue();
            assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 5, 1));
            assertThat(response.nextExecutionDate()).isEqualTo(LocalDate.of(2026, 5, 1));

            verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
            verify(recurringTransactionRepository).save(recurringTransaction);
        }

        @Test
        @DisplayName("deve manter proxima execucao quando ela for posterior a nova data inicial")
        void shouldKeepNextExecutionDateWhenItIsAfterStartDateOnUpdate() {
            User user = buildUser();
            Account account = buildAccount();
            Category category = buildCategory(CategoryType.EXPENSE);

            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );
            recurringTransaction.setNextExecutionDate(LocalDate.of(2026, 6, 1));

            UpdateRecurringTransactionRequest request = buildUpdateRequest(
                    "Internet",
                    LocalDate.of(2026, 5, 1)
            );

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID)).thenReturn(category);
            when(recurringTransactionRepository.save(recurringTransaction))
                    .thenReturn(recurringTransaction);

            RecurringTransactionResponse response = recurringTransactionService.update(
                    RECURRING_TRANSACTION_ID,
                    request,
                    authentication
            );

            assertThat(response.nextExecutionDate()).isEqualTo(LocalDate.of(2026, 6, 1));

            verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("deve lancar erro quando categoria nao combinar com tipo da transacao ao atualizar")
        void shouldThrowWhenCategoryDoesNotMatchTransactionTypeOnUpdate() {
            User user = buildUser();
            Account account = buildAccount();
            Category category = buildCategory(CategoryType.INCOME);

            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );

            UpdateRecurringTransactionRequest request = buildUpdateRequest(
                    "Internet",
                    LocalDate.of(2026, 5, 1)
            );

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID)).thenReturn(category);

            doThrow(new IllegalArgumentException("Category type does not match transaction type"))
                    .when(transactionCategoryValidator)
                    .validate(category, TransactionType.EXPENSE);

            assertThatThrownBy(() -> recurringTransactionService.update(RECURRING_TRANSACTION_ID, request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Category type does not match transaction type");

            verify(recurringTransactionRepository, never()).save(any(RecurringTransaction.class));
        }

        @Test
        @DisplayName("deve lancar erro quando data final for anterior a data inicial ao atualizar")
        void shouldThrowWhenEndDateIsBeforeStartDateOnUpdate() {
            User user = buildUser();
            Account account = buildAccount();
            Category category = buildCategory(CategoryType.EXPENSE);

            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );

            UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                    "Internet",
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    RecurrenceFrequency.MONTHLY,
                    LocalDate.of(2026, 5, 10),
                    LocalDate.of(2026, 5, 1),
                    ACCOUNT_ID,
                    CATEGORY_ID,
                    true
            );

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID)).thenReturn(category);

            assertThatThrownBy(() -> recurringTransactionService.update(RECURRING_TRANSACTION_ID, request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("End date cannot be before start date");

            verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
            verify(recurringTransactionRepository, never()).save(any(RecurringTransaction.class));
        }

        @Test
        @DisplayName("deve desativar transacao recorrente ao atualizar active como falso")
        void shouldDeactivateRecurringTransactionWhenUpdatingActiveToFalse() {
            User user = buildUser();
            Account account = buildAccount();
            Category category = buildCategory(CategoryType.EXPENSE);

            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );
            recurringTransaction.setActive(true);

            UpdateRecurringTransactionRequest request = new UpdateRecurringTransactionRequest(
                    "Internet",
                    new BigDecimal("150.00"),
                    TransactionType.EXPENSE,
                    SourceType.WEB,
                    RecurrenceFrequency.MONTHLY,
                    LocalDate.of(2026, 5, 1),
                    LocalDate.of(2026, 12, 1),
                    ACCOUNT_ID,
                    CATEGORY_ID,
                    false
            );

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));
            when(userResourceResolver.resolveAccount(ACCOUNT_ID, USER_ID)).thenReturn(account);
            when(userResourceResolver.resolveCategory(CATEGORY_ID, USER_ID)).thenReturn(category);
            when(recurringTransactionRepository.save(recurringTransaction))
                    .thenReturn(recurringTransaction);

            RecurringTransactionResponse response = recurringTransactionService.update(
                    RECURRING_TRANSACTION_ID,
                    request,
                    authentication
            );

            assertThat(response.active()).isFalse();
            assertThat(recurringTransaction.isActive()).isFalse();

            verify(transactionCategoryValidator).validate(category, TransactionType.EXPENSE);
            verify(recurringTransactionRepository).save(recurringTransaction);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deve deletar transacao recorrente")
        void shouldDeleteRecurringTransaction() {
            User user = buildUser();
            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));

            recurringTransactionService.delete(RECURRING_TRANSACTION_ID, authentication);

            verify(recurringTransactionRepository).delete(recurringTransaction);
        }
    }

    @Nested
    @DisplayName("activate")
    class ActivateTests {

        @Test
        @DisplayName("deve ativar transacao recorrente e definir proxima execucao quando estiver nula")
        void shouldActivateAndSetNextExecutionDateWhenNull() {
            User user = buildUser();
            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );
            recurringTransaction.setActive(false);
            recurringTransaction.setNextExecutionDate(null);

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));
            when(recurringTransactionRepository.save(recurringTransaction))
                    .thenReturn(recurringTransaction);

            RecurringTransactionResponse response = recurringTransactionService.activate(
                    RECURRING_TRANSACTION_ID,
                    authentication
            );

            assertThat(response.active()).isTrue();
            assertThat(response.nextExecutionDate()).isEqualTo(recurringTransaction.getStartDate());
        }

        @Test
        @DisplayName("deve ativar transacao recorrente mantendo proxima execucao existente")
        void shouldActivateAndKeepExistingNextExecutionDate() {
            User user = buildUser();
            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );
            recurringTransaction.setActive(false);
            recurringTransaction.setNextExecutionDate(LocalDate.of(2026, 6, 1));

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));
            when(recurringTransactionRepository.save(recurringTransaction))
                    .thenReturn(recurringTransaction);

            RecurringTransactionResponse response = recurringTransactionService.activate(
                    RECURRING_TRANSACTION_ID,
                    authentication
            );

            assertThat(response.active()).isTrue();
            assertThat(response.nextExecutionDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        }
    }

    @Nested
    @DisplayName("deactivate")
    class DeactivateTests {

        @Test
        @DisplayName("deve desativar transacao recorrente")
        void shouldDeactivateRecurringTransaction() {
            User user = buildUser();
            RecurringTransaction recurringTransaction = buildRecurringTransaction(
                    RECURRING_TRANSACTION_ID,
                    "Aluguel",
                    TransactionType.EXPENSE,
                    CategoryType.EXPENSE
            );
            recurringTransaction.setActive(true);

            mockAuthenticatedUser(user);
            when(recurringTransactionRepository.findByIdAndUserId(RECURRING_TRANSACTION_ID, USER_ID))
                    .thenReturn(java.util.Optional.of(recurringTransaction));
            when(recurringTransactionRepository.save(recurringTransaction))
                    .thenReturn(recurringTransaction);

            RecurringTransactionResponse response = recurringTransactionService.deactivate(
                    RECURRING_TRANSACTION_ID,
                    authentication
            );

            assertThat(response.active()).isFalse();
        }
    }

    private void mockAuthenticatedUser(User user) {
        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
    }

    private CreateRecurringTransactionRequest buildCreateRequest() {
        return new CreateRecurringTransactionRequest(
                "Aluguel",
                new BigDecimal("1200.00"),
                TransactionType.EXPENSE,
                SourceType.WEB,
                RecurrenceFrequency.MONTHLY,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 12, 1),
                ACCOUNT_ID,
                CATEGORY_ID
        );
    }

    private UpdateRecurringTransactionRequest buildUpdateRequest(
            String description,
            LocalDate startDate
    ) {
        return new UpdateRecurringTransactionRequest(
                description,
                new BigDecimal("150.00"),
                TransactionType.EXPENSE,
                SourceType.WEB,
                RecurrenceFrequency.MONTHLY,
                startDate,
                LocalDate.of(2026, 12, 1),
                ACCOUNT_ID,
                CATEGORY_ID,
                true
        );
    }

    private RecurringTransaction buildRecurringTransaction(
            Long id,
            String description,
            TransactionType transactionType,
            CategoryType categoryType
    ) {
        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setId(id);
        recurringTransaction.setDescription(description);
        recurringTransaction.setAmount(new BigDecimal("1200.00"));
        recurringTransaction.setType(transactionType);
        recurringTransaction.setSourceType(SourceType.WEB);
        recurringTransaction.setFrequency(RecurrenceFrequency.MONTHLY);
        recurringTransaction.setStartDate(LocalDate.of(2026, 4, 1));
        recurringTransaction.setEndDate(LocalDate.of(2026, 12, 1));
        recurringTransaction.setNextExecutionDate(LocalDate.of(2026, 4, 1));
        recurringTransaction.setActive(true);
        recurringTransaction.setUser(buildUser());
        recurringTransaction.setAccount(buildAccount());
        recurringTransaction.setCategory(buildCategory(categoryType));
        return recurringTransaction;
    }

    private User buildUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setName("Bryan");
        user.setEmail("bryan@email.com");
        return user;
    }

    private Account buildAccount() {
        Account account = new Account();
        account.setId(ACCOUNT_ID);
        account.setName("Banco Principal");
        account.setType(AccountType.CHECKING_ACCOUNT);
        account.setUser(buildUser());
        return account;
    }

    private Category buildCategory(CategoryType type) {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setName(type == CategoryType.EXPENSE ? "Moradia" : "Salário");
        category.setType(type);
        category.setUser(buildUser());
        return category;
    }
}