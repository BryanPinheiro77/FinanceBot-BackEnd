package com.financebot.transaction.application.port.out;

import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.dto.TransactionFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface FindTransactionPort {

    Optional<Transaction> findByIdAndUserId(Long transactionId, Long userId);

    Page<Transaction> findAllByFilter(Long userId, TransactionFilter filter, Pageable pageable);
}