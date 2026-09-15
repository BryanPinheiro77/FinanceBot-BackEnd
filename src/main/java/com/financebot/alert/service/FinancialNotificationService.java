package com.financebot.alert.service;

import com.financebot.alert.application.AlertPreferences;
import com.financebot.alert.application.FinancialAlertNotificationEvent;
import com.financebot.alert.application.FinancialAlertNotificationPublisher;
import com.financebot.alert.application.NotificationDeliveryClaim;

import com.financebot.alert.adapter.FinancialNotificationEntity;
import com.financebot.alert.adapter.FinancialNotificationRepository;
import com.financebot.alert.domain.NotificationKind;
import com.financebot.alert.domain.NotificationStatus;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Service
public class FinancialNotificationService {
    private final FinancialNotificationRepository notifications;
    private final UserRepository users;
    private final FinancialAlertNotificationPublisher publisher;
    private final Clock clock;
    private final boolean enabled;

    public FinancialNotificationService(FinancialNotificationRepository notifications, UserRepository users,
            FinancialAlertNotificationPublisher publisher, Clock clock,
            @Value("${financebot.alerts.enabled:true}") boolean enabled) {
        this.notifications = notifications;
        this.users = users;
        this.publisher = publisher;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Transactional
    public void enqueue(User snapshot, NotificationKind kind, String title, String body,
            String periodKey, LocalDateTime expiresAt) {
        User user = users.lockById(snapshot.getId()).orElseThrow(EntityNotFoundException::new);
        if (!enabled || user.getTelegramId() == null || !allows(user, kind) || !expiresAt.isAfter(now())) {
            return;
        }
        String id = notificationId(user.getId(), periodKey);
        if (notifications.existsById(id)) {
            return;
        }
        FinancialNotificationEntity notification = new FinancialNotificationEntity();
        notification.setId(id);
        notification.setUserId(user.getId());
        notification.setTelegramId(user.getTelegramId());
        notification.setKind(kind);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(now());
        notification.setExpiresAt(expiresAt);
        notification.setNextAttemptAt(now());
        notifications.save(notification);
    }

    @Transactional
    public int publishDue() {
        notifications.deleteExpiredTerminal(now().minusDays(7));
        notifications.redactExpiredUncertain(now().minusDays(7));
        int published = 0;
        for (FinancialNotificationEntity notification : notifications.findDue(now(), PageRequest.of(0, 100))) {
            if (notification.getStatus() == NotificationStatus.SENDING) {
                // A reserva expirada pode ter enviado ao Telegram: não repetir automaticamente.
                notification.setStatus(NotificationStatus.UNKNOWN);
                continue;
            }
            if (!isEligible(notification)) {
                notification.setStatus(NotificationStatus.CANCELLED);
                continue;
            }
            try {
                publisher.publish(new FinancialAlertNotificationEvent(notification.getId()));
                notification.setStatus(NotificationStatus.PUBLISHED);
                notification.setNextAttemptAt(now().plusMinutes(5));
                published++;
            } catch (RuntimeException exception) {
                notification.setStatus(NotificationStatus.PENDING);
                notification.setNextAttemptAt(now().plusMinutes(1));
            }
        }
        return published;
    }

    @Transactional
    public NotificationDeliveryClaim claim(String id) {
        FinancialNotificationEntity notification = notifications.lockById(id).orElse(null);
        if (notification == null || notification.getStatus() == NotificationStatus.SENT
                || notification.getStatus() == NotificationStatus.UNKNOWN
                || notification.getStatus() == NotificationStatus.CANCELLED) {
            return null;
        }
        if (notification.getStatus() == NotificationStatus.SENDING) {
            if (!notification.getNextAttemptAt().isAfter(now())) {
                notification.setStatus(NotificationStatus.UNKNOWN);
            }
            return null;
        }
        if (notification.getStatus() != NotificationStatus.PUBLISHED || !isEligible(notification)) {
            if (!isEligible(notification)) {
                notification.setStatus(NotificationStatus.CANCELLED);
            }
            return null;
        }
        notification.setStatus(NotificationStatus.SENDING);
        notification.setDeliveryToken(UUID.randomUUID().toString());
        notification.setClaimedAt(now());
        notification.setNextAttemptAt(now().plusMinutes(2));
        notification.setAttempts(notification.getAttempts() + 1);
        return new NotificationDeliveryClaim(notification.getDeliveryToken(), notification.getTelegramId(),
                notification.getTitle(), notification.getBody());
    }

    @Transactional
    public void complete(String id, String token, NotificationStatus outcome) {
        if (outcome != NotificationStatus.SENT && outcome != NotificationStatus.PENDING
                && outcome != NotificationStatus.UNKNOWN) {
            throw new IllegalArgumentException("Invalid delivery outcome");
        }
        FinancialNotificationEntity notification = notifications.lockById(id).orElseThrow(EntityNotFoundException::new);
        if (!Objects.equals(token, notification.getDeliveryToken()) || token == null) {
            throw new IllegalArgumentException("Invalid delivery token");
        }
        if (notification.getStatus() == NotificationStatus.SENT) {
            return; // ACK repetido é idempotente.
        }
        if (notification.getStatus() != NotificationStatus.SENDING && notification.getStatus() != NotificationStatus.UNKNOWN) {
            return;
        }
        notification.setStatus(outcome == NotificationStatus.PENDING && notification.getAttempts() >= 5
                ? NotificationStatus.CANCELLED : outcome);
        notification.setNextAttemptAt(now().plusMinutes(5));
    }

    @Transactional(readOnly = true)
    public AlertPreferences preferences(Long telegramId) {
        User user = findUser(telegramId);
        return new AlertPreferences(user.isFinancialAlertsEnabled(), user.isWeeklySummaryEnabled(), user.isMonthlySummaryEnabled());
    }

    @Transactional
    public AlertPreferences updatePreferences(Long telegramId, AlertPreferences preferences) {
        User user = users.lockById(findUser(telegramId).getId()).orElseThrow(EntityNotFoundException::new);
        user.setFinancialAlertsEnabled(preferences.alerts());
        user.setWeeklySummaryEnabled(preferences.weeklySummary());
        user.setMonthlySummaryEnabled(preferences.monthlySummary());
        return preferences;
    }

    private User findUser(Long telegramId) {
        return users.findByTelegramId(telegramId).orElseThrow(() -> new EntityNotFoundException("Telegram user not found"));
    }

    private boolean isEligible(FinancialNotificationEntity notification) {
        if (!enabled || !notification.getExpiresAt().isAfter(now())) {
            return false;
        }
        return users.findById(notification.getUserId())
                .filter(user -> Objects.equals(user.getTelegramId(), notification.getTelegramId()))
                .filter(user -> allows(user, notification.getKind())).isPresent();
    }

    private boolean allows(User user, NotificationKind kind) {
        return switch (kind) {
            case ALERT -> user.isFinancialAlertsEnabled();
            case WEEKLY -> user.isWeeklySummaryEnabled();
            case MONTHLY -> user.isMonthlySummaryEnabled();
        };
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public static String notificationId(Long userId, String periodKey) {
        try {
            byte[] value = (userId + ":" + periodKey).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
