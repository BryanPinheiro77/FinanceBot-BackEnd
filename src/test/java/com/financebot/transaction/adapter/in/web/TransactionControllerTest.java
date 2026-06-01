package com.financebot.transaction.adapter.in.web;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.dto.response.InstallmentTransactionCreationResponse;
import com.financebot.analysis.dto.response.TransactionCreationResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.common.pagination.PageQuery;
import com.financebot.common.pagination.PageResult;
import com.financebot.common.pagination.SortDirection;
import com.financebot.transaction.application.command.CreateInstallmentTransactionCommand;
import com.financebot.transaction.application.command.CreateTransactionCommand;
import com.financebot.transaction.application.command.UpdateTransactionCommand;
import com.financebot.transaction.application.dto.request.CreateInstallmentTransactionRequest;
import com.financebot.transaction.application.dto.request.CreateTransactionRequest;
import com.financebot.transaction.application.dto.request.UpdateTransactionRequest;
import com.financebot.transaction.application.dto.response.InstallmentTransactionResponse;
import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.usecase.CreateInstallmentTransactionUseCase;
import com.financebot.transaction.application.usecase.CreateTransactionUseCase;
import com.financebot.transaction.application.usecase.DeleteTransactionUseCase;
import com.financebot.transaction.application.usecase.FindTransactionByIdUseCase;
import com.financebot.transaction.application.usecase.ListTransactionsUseCase;
import com.financebot.transaction.application.usecase.UpdateTransactionUseCase;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private FinancialAnalysisService financialAnalysisService;

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private CreateTransactionUseCase createTransactionUseCase;

    @Mock
    private CreateInstallmentTransactionUseCase createInstallmentTransactionUseCase;

    @Mock
    private ListTransactionsUseCase listTransactionsUseCase;

    @Mock
    private FindTransactionByIdUseCase findTransactionByIdUseCase;

    @Mock
    private UpdateTransactionUseCase updateTransactionUseCase;

    @Mock
    private DeleteTransactionUseCase deleteTransactionUseCase;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionController transactionController;

    @Test
    @DisplayName("deve criar transação e retornar análise financeira atualizada")
    void shouldCreateTransactionAndReturnFinancialAnalysis() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("150.00"),
                "Mercado",
                LocalDate.of(2026, 4, 4),
                TransactionType.EXPENSE,
                SourceType.WEB,
                10L,
                20L
        );

        User user = new User();
        user.setId(1L);

        CreateTransactionCommand command = new CreateTransactionCommand(
                request.amount(),
                request.description(),
                request.date(),
                request.type(),
                request.sourceType(),
                request.accountId(),
                request.categoryId(),
                user
        );

        TransactionResponse transactionResponse = mock(TransactionResponse.class);
        FinancialCommitmentResponse financialAnalysis = mock(FinancialCommitmentResponse.class);

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(transactionMapper.toCommand(request, user)).thenReturn(command);
        when(createTransactionUseCase.execute(command)).thenReturn(transactionResponse);
        when(financialAnalysisService.getFinancialCommitment(authentication)).thenReturn(financialAnalysis);

        TransactionCreationResponse result = transactionController.create(request, authentication);

        assertThat(result.transaction()).isEqualTo(transactionResponse);
        assertThat(result.analysis()).isEqualTo(financialAnalysis);

        verify(authenticatedUserResolver).resolve(authentication);
        verify(transactionMapper).toCommand(request, user);
        verify(createTransactionUseCase).execute(command);
        verify(financialAnalysisService).getFinancialCommitment(authentication);
    }

    @Test
    @DisplayName("deve criar transação parcelada e retornar análise financeira atualizada")
    void shouldCreateInstallmentTransactionAndReturnFinancialAnalysis() {
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

        User user = new User();
        user.setId(1L);

        CreateInstallmentTransactionCommand command = new CreateInstallmentTransactionCommand(
                request.totalAmount(),
                request.description(),
                request.firstInstallmentDate(),
                request.type(),
                request.sourceType(),
                request.accountId(),
                request.categoryId(),
                request.totalInstallments(),
                user
        );

        InstallmentTransactionResponse installmentResponse = mock(InstallmentTransactionResponse.class);
        FinancialCommitmentResponse financialAnalysis = mock(FinancialCommitmentResponse.class);

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(transactionMapper.toCommand(request, user)).thenReturn(command);
        when(createInstallmentTransactionUseCase.execute(command)).thenReturn(installmentResponse);
        when(financialAnalysisService.getFinancialCommitment(authentication)).thenReturn(financialAnalysis);

        InstallmentTransactionCreationResponse result =
                transactionController.createInstallment(request, authentication);

        assertThat(result.installment()).isEqualTo(installmentResponse);
        assertThat(result.analysis()).isEqualTo(financialAnalysis);

        verify(authenticatedUserResolver).resolve(authentication);
        verify(transactionMapper).toCommand(request, user);
        verify(createInstallmentTransactionUseCase).execute(command);
        verify(financialAnalysisService).getFinancialCommitment(authentication);
    }

    @Test
    @DisplayName("deve listar transações usando filtros recebidos pela requisição")
    void shouldFindAllTransactionsWithFilters() {
        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Order.desc("date"), Sort.Order.asc("amount"))
        );

        TransactionResponse transactionResponse = mock(TransactionResponse.class);

        PageResult<TransactionResponse> pageResult = new PageResult<>(
                List.of(transactionResponse),
                0,
                10,
                1,
                1,
                true,
                true
        );

        when(listTransactionsUseCase.execute(
                any(TransactionFilter.class),
                eq(authentication),
                any(PageQuery.class)
        )).thenReturn(pageResult);

        Page<TransactionResponse> result = transactionController.findAll(
                TransactionType.EXPENSE,
                20L,
                10L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                SourceType.WEB,
                "mercado",
                authentication,
                pageable
        );

        assertThat(result.getContent()).containsExactly(transactionResponse);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getSort()).isEqualTo(pageable.getSort());

        ArgumentCaptor<TransactionFilter> filterCaptor = ArgumentCaptor.forClass(TransactionFilter.class);
        ArgumentCaptor<PageQuery> pageQueryCaptor = ArgumentCaptor.forClass(PageQuery.class);

        verify(listTransactionsUseCase).execute(
                filterCaptor.capture(),
                eq(authentication),
                pageQueryCaptor.capture()
        );

        TransactionFilter capturedFilter = filterCaptor.getValue();

        assertThat(capturedFilter.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(capturedFilter.categoryId()).isEqualTo(20L);
        assertThat(capturedFilter.accountId()).isEqualTo(10L);
        assertThat(capturedFilter.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(capturedFilter.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(capturedFilter.sourceType()).isEqualTo(SourceType.WEB);
        assertThat(capturedFilter.description()).isEqualTo("mercado");

        PageQuery capturedPageQuery = pageQueryCaptor.getValue();

        assertThat(capturedPageQuery.page()).isZero();
        assertThat(capturedPageQuery.size()).isEqualTo(10);
        assertThat(capturedPageQuery.sorts()).hasSize(2);

        assertThat(capturedPageQuery.sorts().get(0).property()).isEqualTo("date");
        assertThat(capturedPageQuery.sorts().get(0).direction()).isEqualTo(SortDirection.DESC);

        assertThat(capturedPageQuery.sorts().get(1).property()).isEqualTo("amount");
        assertThat(capturedPageQuery.sorts().get(1).direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    @DisplayName("deve buscar transação por id")
    void shouldFindTransactionById() {
        TransactionResponse response = mock(TransactionResponse.class);

        when(findTransactionByIdUseCase.execute(99L, authentication)).thenReturn(response);

        TransactionResponse result = transactionController.findById(99L, authentication);

        assertThat(result).isEqualTo(response);

        verify(findTransactionByIdUseCase).execute(99L, authentication);
    }

    @Test
    @DisplayName("deve atualizar transação")
    void shouldUpdateTransaction() {
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                new BigDecimal("500.00"),
                "Salário",
                LocalDate.of(2026, 4, 5),
                TransactionType.INCOME,
                SourceType.WEB,
                10L,
                20L
        );

        User user = new User();
        user.setId(1L);

        UpdateTransactionCommand command = new UpdateTransactionCommand(
                77L,
                request.amount(),
                request.description(),
                request.date(),
                request.type(),
                request.sourceType(),
                request.accountId(),
                request.categoryId(),
                user
        );

        TransactionResponse response = mock(TransactionResponse.class);

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(transactionMapper.toCommand(77L, request, user)).thenReturn(command);
        when(updateTransactionUseCase.execute(command)).thenReturn(response);

        TransactionResponse result = transactionController.update(77L, request, authentication);

        assertThat(result).isEqualTo(response);

        verify(authenticatedUserResolver).resolve(authentication);
        verify(transactionMapper).toCommand(77L, request, user);
        verify(updateTransactionUseCase).execute(command);
    }

    @Test
    @DisplayName("deve deletar transação")
    void shouldDeleteTransaction() {
        transactionController.delete(55L, authentication);

        verify(deleteTransactionUseCase).execute(55L, authentication);
    }
}