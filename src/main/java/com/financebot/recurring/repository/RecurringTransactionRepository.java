package com.financebot.recurring.repository;

import com.financebot.recurring.domain.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);

    List<RecurringTransaction> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<RecurringTransaction> findAllByUserIdAndActiveTrueOrderByNextExecutionDateAsc(Long userId);

    List<RecurringTransaction> findAllByActiveTrueAndNextExecutionDateLessThanEqual(LocalDate date);

    List<RecurringTransaction> findAllByUserIdAndActiveTrue(Long userId);
}