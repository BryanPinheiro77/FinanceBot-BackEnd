package com.financebot.transaction.application.usecase;

import com.financebot.transaction.application.dto.response.TransactionResponse;
import com.financebot.transaction.application.port.out.FindTransactionPort;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.mapper.TransactionMapper;
import com.financebot.user.domain.User;
import com.financebot.user.service.AuthenticatedUserResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTransactionUseCaseTest {

    @Mock
    private FindTransactionPort findTransactionPort;

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ListTransactionsUseCase listTransactionsUseCase;

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

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(findTransactionPort.findAllByFilter(1L, filter, pageable)).thenReturn(page);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        Page<TransactionResponse> result = listTransactionsUseCase.execute(filter, authentication, pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(findTransactionPort).findAllByFilter(1L, filter, pageable);
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

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);

        assertThatThrownBy(() -> listTransactionsUseCase.execute(filter, authentication, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date cannot be after end date");

        verify(findTransactionPort, never()).findAllByFilter(1L, filter, pageable);
        verifyNoInteractions(transactionMapper);
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}