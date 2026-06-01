package com.financebot.transaction.application.port.out;

import com.financebot.common.pagination.PageQuery;
import com.financebot.common.pagination.PageResult;
import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.dto.TransactionFilter;

import java.util.Optional;

public interface FindTransactionPort {

    Optional<Transaction> findByIdAndUserId(Long transactionId, Long userId);

    PageResult<Transaction> findAllByFilter(
            Long userId,
            TransactionFilter filter,
            PageQuery pageQuery
    );
}