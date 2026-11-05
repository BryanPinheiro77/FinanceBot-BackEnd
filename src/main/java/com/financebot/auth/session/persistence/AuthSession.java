package com.financebot.auth.session.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
@Getter
@Setter
public class AuthSession {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "last_activity_at", nullable = false) private Instant lastActivityAt;
    @Column(name = "revoked_at") private Instant revokedAt;
}
