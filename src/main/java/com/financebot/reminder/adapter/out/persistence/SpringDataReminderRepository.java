package com.financebot.reminder.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface SpringDataReminderRepository extends JpaRepository<ReminderJpaEntity, Long> {
    Optional<ReminderJpaEntity> findByIdAndUserId(Long id, Long userId);
    List<ReminderJpaEntity> findAllByUserIdOrderByReminderDateAsc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReminderJpaEntity r where r.id = :id")
    Optional<ReminderJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select r from ReminderJpaEntity r join fetch r.user u
           where r.active = true and r.sentAt is null and r.reminderDate <= :date
             and (r.claimedAt is null or r.claimedAt < :staleBefore)
             and u.telegramId is not null
           order by r.reminderDate asc, r.id asc
           """)
    List<ReminderJpaEntity> findPendingForClaim(@Param("date") LocalDate date,
                                                @Param("staleBefore") LocalDateTime staleBefore,
                                                Pageable pageable);
}
