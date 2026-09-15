package com.financebot.alert.adapter;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FinancialNotificationRepository extends JpaRepository<FinancialNotificationEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from FinancialNotificationEntity n where n.id = :id")
    Optional<FinancialNotificationEntity> lockById(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n from FinancialNotificationEntity n
            where n.status in (com.financebot.alert.domain.NotificationStatus.PENDING,
                               com.financebot.alert.domain.NotificationStatus.PUBLISHED,
                               com.financebot.alert.domain.NotificationStatus.SENDING)
              and n.nextAttemptAt <= :now
            order by n.createdAt, n.id
            """)
    List<FinancialNotificationEntity> findDue(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Query("""
            delete from FinancialNotificationEntity n
            where n.expiresAt < :cutoff
              and n.status in (com.financebot.alert.domain.NotificationStatus.SENT,
                               com.financebot.alert.domain.NotificationStatus.CANCELLED)
            """)
    int deleteExpiredTerminal(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("""
            update FinancialNotificationEntity n set n.title = 'Conteúdo expirado', n.body = 'Conteúdo expirado'
            where n.status = com.financebot.alert.domain.NotificationStatus.UNKNOWN and n.expiresAt < :cutoff
            """)
    int redactExpiredUncertain(@Param("cutoff") LocalDateTime cutoff);
}
