package com.financebot.transaction.adapter.out.persistence;

import com.financebot.common.pagination.PageQuery;
import com.financebot.common.pagination.PageResult;
import com.financebot.common.pagination.SortDirection;
import com.financebot.transaction.application.port.out.DeleteTransactionPort;
import com.financebot.transaction.application.port.out.FindTransactionPort;
import com.financebot.transaction.application.port.out.SaveTransactionPort;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements SaveTransactionPort, FindTransactionPort, DeleteTransactionPort {

    private final TransactionRepository transactionRepository;

    @Override
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    public List<Transaction> saveAll(List<Transaction> transactions) {
        return transactionRepository.saveAll(transactions);
    }

    @Override
    public Optional<Transaction> findByIdAndUserId(Long transactionId, Long userId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId);
    }

    @Override
    public PageResult<Transaction> findAllByFilter(
            Long userId,
            TransactionFilter filter,
            PageQuery pageQuery
    ) {
        PageRequest pageRequest = toPageRequest(pageQuery);

        Page<Transaction> page = transactionRepository.findAll(
                TransactionSpecification.withFilters(userId, filter),
                pageRequest
        );

        return toPageResult(page);
    }

    @Override
    public void delete(Transaction transaction) {
        transactionRepository.delete(transaction);
    }

    private PageRequest toPageRequest(PageQuery pageQuery) {
        Sort.Direction direction = pageQuery.direction() == SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(direction, pageQuery.sortBy())
        );
    }

    private PageResult<Transaction> toPageResult(Page<Transaction> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}