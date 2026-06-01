package com.financebot.transaction.application.usecase;

import com.financebot.common.pagination.PageQuery;
import com.financebot.common.pagination.PageResult;
import com.financebot.common.pagination.PageSort;
import com.financebot.common.pagination.SortDirection;
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

        PageQuery pageQuery = new PageQuery(
                0,
                10,
                List.of(new PageSort("date", SortDirection.DESC))
        );

        Transaction transaction = new Transaction();
        TransactionResponse response = mock(TransactionResponse.class);

        PageResult<Transaction> pageResult = new PageResult<>(
                List.of(transaction),
                0,
                10,
                1,
                1,
                true,
                true
        );

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);
        when(findTransactionPort.findAllByFilter(1L, filter, pageQuery)).thenReturn(pageResult);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        PageResult<TransactionResponse> result =
                listTransactionsUseCase.execute(filter, authentication, pageQuery);

        assertThat(result.content()).containsExactly(response);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();

        verify(findTransactionPort).findAllByFilter(1L, filter, pageQuery);
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

        PageQuery pageQuery = new PageQuery(
                0,
                10,
                List.of()
        );

        when(authenticatedUserResolver.resolve(authentication)).thenReturn(user);

        assertThatThrownBy(() -> listTransactionsUseCase.execute(filter, authentication, pageQuery))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date cannot be after end date");

        verify(findTransactionPort, never()).findAllByFilter(1L, filter, pageQuery);
        verifyNoInteractions(transactionMapper);
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}