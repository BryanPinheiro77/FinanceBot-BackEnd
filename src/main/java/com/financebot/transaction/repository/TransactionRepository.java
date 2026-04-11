package com.financebot.transaction.repository;

import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Query("""
           select coalesce(sum(t.amount), 0)
           from Transaction t
           where t.user.id = :userId
             and t.type = com.financebot.transaction.domain.TransactionType.EXPENSE
             and t.installment = true
             and t.date > :today
           """)
    BigDecimal sumFutureInstallmentsByUser(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    @Query("""
           select coalesce(sum(t.amount), 0)
           from Transaction t
           where t.user.id = :userId
             and t.type = com.financebot.transaction.domain.TransactionType.EXPENSE
             and t.date between :startDate and :endDate
           """)
    BigDecimal sumProjectedExpensesBetweenDatesByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
           select coalesce(sum(t.amount), 0)
           from Transaction t
           where t.user.id = :userId
             and t.type = com.financebot.transaction.domain.TransactionType.INCOME
             and t.date between :startDate and :endDate
           """)
    BigDecimal sumIncomeBetweenDatesByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
           select count(distinct t.installmentGroupId)
           from Transaction t
           where t.user.id = :userId
             and t.type = com.financebot.transaction.domain.TransactionType.EXPENSE
             and t.installment = true
             and t.installmentGroupId is not null
             and t.date >= :today
           """)
    Long countDistinctActiveInstallmentGroupsByUser(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    @Query("""
           select t
           from Transaction t
           where t.user.id = :userId
             and t.type = com.financebot.transaction.domain.TransactionType.EXPENSE
             and t.installment = true
             and t.installmentGroupId is not null
             and t.date >= :today
           order by t.installmentGroupId asc, t.date asc
           """)
    List<Transaction> findActiveInstallmentTransactionsByUser(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    @Query("""
       select coalesce(sum(t.amount), 0)
       from Transaction t
       where t.user.id = :userId
         and t.type = :type
         and t.date between :startDate and :endDate
       """)
    BigDecimal sumAmountByUserAndTypeBetweenDates(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
       select coalesce(sum(t.amount), 0)
       from Transaction t
       where t.user.id = :userId
         and t.type = :type
         and t.date between :startDate and :endDate
       """)
    BigDecimal sumAmountByUserAndTypeAndDateBetween(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
       select coalesce(sum(t.amount), 0)
       from Transaction t
       where t.user.id = :userId
         and t.type = :type
         and t.date between :startDate and :endDate
         and lower(t.category.name) = lower(:categoryName)
       """)
    BigDecimal sumAmountByUserAndTypeAndDateBetweenAndCategory(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryName") String categoryName
    );

    @Query("""
       select coalesce(sum(t.amount), 0)
       from Transaction t
       where t.user.id = :userId
         and t.type = :type
         and t.date between :startDate and :endDate
         and lower(t.account.name) = lower(:accountName)
       """)
    BigDecimal sumAmountByUserAndTypeAndDateBetweenAndAccount(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountName") String accountName
    );

    @Query("""
       select coalesce(sum(t.amount), 0)
       from Transaction t
       where t.user.id = :userId
         and t.type = :type
         and t.date between :startDate and :endDate
         and lower(t.category.name) = lower(:categoryName)
         and lower(t.account.name) = lower(:accountName)
       """)
    BigDecimal sumAmountByUserAndTypeAndDateBetweenAndCategoryAndAccount(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryName") String categoryName,
            @Param("accountName") String accountName
    );

    @Query("""
       select count(t)
       from Transaction t
       where t.user.id = :userId
         and t.type = com.financebot.transaction.domain.TransactionType.EXPENSE
         and t.installment = true
         and t.date between :startDate and :endDate
       """)
    Long countInstallmentsByUserBetweenDates(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
