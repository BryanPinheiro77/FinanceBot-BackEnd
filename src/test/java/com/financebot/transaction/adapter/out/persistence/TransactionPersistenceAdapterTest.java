package com.financebot.transaction.adapter.out.persistence;

import com.financebot.common.pagination.PageQuery;
import com.financebot.common.pagination.PageResult;
import com.financebot.common.pagination.SortDirection;
import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionPersistenceAdapterTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionPersistenceAdapter transactionPersistenceAdapter;

    @Test
    @DisplayName("deve salvar uma transação")
    void shouldSaveTransaction() {
        Transaction transaction = new Transaction();
        Transaction savedTransaction = new Transaction();

        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);

        Transaction result = transactionPersistenceAdapter.save(transaction);

        assertThat(result).isEqualTo(savedTransaction);
        verify(transactionRepository).save(transaction);
    }

    @Test
    @DisplayName("deve salvar uma lista de transações")
    void shouldSaveAllTransactions() {
        Transaction transaction1 = new Transaction();
        Transaction transaction2 = new Transaction();

        List<Transaction> transactions = List.of(transaction1, transaction2);

        when(transactionRepository.saveAll(transactions)).thenReturn(transactions);

        List<Transaction> result = transactionPersistenceAdapter.saveAll(transactions);

        assertThat(result).containsExactly(transaction1, transaction2);
        verify(transactionRepository).saveAll(transactions);
    }

    @Test
    @DisplayName("deve buscar transação por id e usuário")
    void shouldFindByIdAndUserId() {
        Transaction transaction = new Transaction();

        when(transactionRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.of(transaction));

        Optional<Transaction> result = transactionPersistenceAdapter.findByIdAndUserId(99L, 1L);

        assertThat(result).contains(transaction);

        verify(transactionRepository).findByIdAndUserId(99L, 1L);
    }

    @Test
    @DisplayName("deve retornar optional vazio quando transação não for encontrada por id e usuário")
    void shouldReturnEmptyWhenTransactionIsNotFoundByIdAndUserId() {
        when(transactionRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        Optional<Transaction> result = transactionPersistenceAdapter.findByIdAndUserId(99L, 1L);

        assertThat(result).isEmpty();

        verify(transactionRepository).findByIdAndUserId(99L, 1L);
    }

    @Test
    @DisplayName("deve listar transações usando filtros")
    void shouldFindAllByFilter() {
        Long userId = 1L;

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
                "date",
                SortDirection.DESC
        );

        PageRequest expectedPageRequest = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "date")
        );

        Transaction transaction = new Transaction();
        Page<Transaction> page = new PageImpl<>(List.of(transaction), expectedPageRequest, 1);

        when(transactionRepository.findAll(any(Specification.class), eq(expectedPageRequest)))
                .thenReturn(page);

        PageResult<Transaction> result = transactionPersistenceAdapter.findAllByFilter(
                userId,
                filter,
                pageQuery
        );

        assertThat(result.content()).containsExactly(transaction);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();

        verify(transactionRepository).findAll(any(Specification.class), eq(expectedPageRequest));
    }

    @Test
    @DisplayName("deve deletar transação")
    void shouldDeleteTransaction() {
        Transaction transaction = new Transaction();

        transactionPersistenceAdapter.delete(transaction);

        verify(transactionRepository).delete(transaction);
    }
}