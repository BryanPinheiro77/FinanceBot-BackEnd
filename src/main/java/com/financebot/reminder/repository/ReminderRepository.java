package com.financebot.reminder.repository;

import com.financebot.reminder.domain.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    List<Reminder> findAllByUserIdOrderByReminderDateAsc(Long userId);

    @Query("""
           select r from Reminder r
           join fetch r.user u
           where r.active = true
             and r.sentAt is null
             and r.reminderDate <= :date
             and u.telegramId is not null
           order by r.reminderDate asc, r.id asc
           """)
    List<Reminder> findPendingForTelegram(@Param("date") LocalDate date);
}
