package com.financebot.transaction.adapter.in.web;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.dto.response.InstallmentTransactionCreationResponse;
import com.financebot.analysis.dto.response.TransactionCreationResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
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
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private FinancialAnalysisService financialAnalysisService;

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

        TransactionResponse transactionResponse = mock(TransactionResponse.class);
        FinancialCommitmentResponse financialAnalysis = mock(FinancialCommitmentResponse.class);

        when(createTransactionUseCase.execute(request, authentication)).thenReturn(transactionResponse);
        when(financialAnalysisService.getFinancialCommitment(authentication)).thenReturn(financialAnalysis);

        TransactionCreationResponse result = transactionController.create(request, authentication);

        assertThat(result.transaction()).isEqualTo(transactionResponse);
        assertThat(result.analysis()).isEqualTo(financialAnalysis);

        verify(createTransactionUseCase).execute(request, authentication);
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

        InstallmentTransactionResponse installmentResponse = mock(InstallmentTransactionResponse.class);
        FinancialCommitmentResponse financialAnalysis = mock(FinancialCommitmentResponse.class);

        when(createInstallmentTransactionUseCase.execute(request, authentication)).thenReturn(installmentResponse);
        when(financialAnalysisService.getFinancialCommitment(authentication)).thenReturn(financialAnalysis);

        InstallmentTransactionCreationResponse result =
                transactionController.createInstallment(request, authentication);

        assertThat(result.installment()).isEqualTo(installmentResponse);
        assertThat(result.analysis()).isEqualTo(financialAnalysis);

        verify(createInstallmentTransactionUseCase).execute(request, authentication);
        verify(financialAnalysisService).getFinancialCommitment(authentication);
    }

    @Test
    @DisplayName("deve listar transações usando filtros recebidos pela requisição")
    void shouldFindAllTransactionsWithFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        TransactionResponse transactionResponse = mock(TransactionResponse.class);
        Page<TransactionResponse> expectedPage = new PageImpl<>(List.of(transactionResponse), pageable, 1);

        when(listTransactionsUseCase.execute(
                org.mockito.ArgumentMatchers.any(TransactionFilter.class),
                org.mockito.ArgumentMatchers.eq(authentication),
                org.mockito.ArgumentMatchers.eq(pageable)
        )).thenReturn(expectedPage);

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

        assertThat(result).isEqualTo(expectedPage);

        ArgumentCaptor<TransactionFilter> filterCaptor = ArgumentCaptor.forClass(TransactionFilter.class);

        verify(listTransactionsUseCase).execute(
                filterCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(authentication),
                org.mockito.ArgumentMatchers.eq(pageable)
        );

        TransactionFilter capturedFilter = filterCaptor.getValue();

        assertThat(capturedFilter.type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(capturedFilter.categoryId()).isEqualTo(20L);
        assertThat(capturedFilter.accountId()).isEqualTo(10L);
        assertThat(capturedFilter.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(capturedFilter.endDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(capturedFilter.sourceType()).isEqualTo(SourceType.WEB);
        assertThat(capturedFilter.description()).isEqualTo("mercado");
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

        TransactionResponse response = mock(TransactionResponse.class);

        when(updateTransactionUseCase.execute(77L, request, authentication)).thenReturn(response);

        TransactionResponse result = transactionController.update(77L, request, authentication);

        assertThat(result).isEqualTo(response);

        verify(updateTransactionUseCase).execute(77L, request, authentication);
    }

    @Test
    @DisplayName("deve deletar transação")
    void shouldDeleteTransaction() {
        transactionController.delete(55L, authentication);

        verify(deleteTransactionUseCase).execute(55L, authentication);
    }
}