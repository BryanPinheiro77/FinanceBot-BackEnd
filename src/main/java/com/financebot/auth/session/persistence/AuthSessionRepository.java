package com.financebot.auth.session.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSession s where s.id = :id")
    Optional<AuthSession> findLocked(@Param("id") UUID id);

    // Projeção: a autenticação não deixa uma entidade antiga no cache da requisição
    // antes do lock usado por activity/logout/refresh.
    @Query("select count(s) from AuthSession s where s.id = :id and s.userId = :userId "
            + "and s.revokedAt is null and s.expiresAt > :now and s.lastActivityAt > :cutoff")
    long countActive(@Param("id") UUID id, @Param("userId") Long userId,
                     @Param("now") Instant now, @Param("cutoff") Instant cutoff);

    @Modifying
    @Query(value = "delete from auth_sessions where expires_at < :now", nativeQuery = true)
    int deleteExpired(@Param("now") Instant now);
}
