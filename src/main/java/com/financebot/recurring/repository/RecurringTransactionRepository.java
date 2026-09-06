package com.financebot.recurring.repository;

import com.financebot.recurring.domain.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);

    List<RecurringTransaction> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<RecurringTransaction> findAllByUserIdAndActiveTrueOrderByNextExecutionDateAsc(Long userId);

    List<RecurringTransaction> findAllByActiveTrueAndNextExecutionDateLessThanEqual(LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecurringTransaction r where r.id = :id")
    Optional<RecurringTransaction> findByIdForUpdate(@Param("id") Long id);

    List<RecurringTransaction> findAllByUserIdAndActiveTrue(Long userId);
}
