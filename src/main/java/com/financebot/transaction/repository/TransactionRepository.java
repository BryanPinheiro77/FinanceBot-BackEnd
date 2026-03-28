package com.financebot.transaction.repository;

import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
           select coalesce(sum(t.amount), 0)
           from Transaction t
           where t.account.id = :accountId
             and t.user.id = :userId
             and t.type = :type
           """)
    BigDecimal sumAmountByAccountAndUserAndType(
            @Param("accountId") Long accountId,
            @Param("userId") Long userId,
            @Param("type") TransactionType type
    );
}