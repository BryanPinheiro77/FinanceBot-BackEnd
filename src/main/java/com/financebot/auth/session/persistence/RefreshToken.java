package com.financebot.auth.session.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_refresh_tokens")
@Getter
@Setter
public class RefreshToken {
    @Id @Column(name = "token_hash", length = 64) private String tokenHash;
    @Column(name = "session_id", nullable = false) private UUID sessionId;
    @Column(name = "consumed_at") private Instant consumedAt;
}
