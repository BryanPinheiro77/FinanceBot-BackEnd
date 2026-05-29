package com.financebot.transaction.adapter.out.persistence;

import com.financebot.transaction.application.port.out.DeleteTransactionPort;
import com.financebot.transaction.application.port.out.FindTransactionPort;
import com.financebot.transaction.application.port.out.SaveTransactionPort;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.dto.TransactionFilter;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.transaction.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<Transaction> findAllByFilter(Long userId, TransactionFilter filter, Pageable pageable) {
        return transactionRepository.findAll(
                TransactionSpecification.withFilters(userId, filter),
                pageable
        );
    }

    @Override
    public void delete(Transaction transaction) {
        transactionRepository.delete(transaction);
    }
}