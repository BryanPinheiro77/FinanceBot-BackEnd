package com.financebot.alert.adapter;

import com.financebot.alert.domain.NotificationKind;
import com.financebot.alert.domain.NotificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_notifications")
@Getter
@Setter
public class FinancialNotificationEntity {
    @Id
    @Column(length = 64)
    private String id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationKind kind;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String body;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;
    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;
    @Column(name = "delivery_token", length = 36)
    private String deliveryToken;
    @Column(nullable = false)
    private int attempts;
}
