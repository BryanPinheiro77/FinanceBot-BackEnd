package com.financebot.alert.adapter;

import com.financebot.alert.application.*;
import com.financebot.alert.service.FinancialNotificationService;
import com.financebot.alert.domain.*;
import com.financebot.security.config.DataEncryptionConfig;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({DataEncryptionConfig.class, FinancialNotificationServiceTest.Config.class})
class FinancialNotificationServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 15, 9, 0);
    @Autowired FinancialNotificationService service;
    @Autowired FinancialNotificationRepository notifications;
    @Autowired UserRepository users;
    @Autowired FakePublisher publisher;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void enqueueIsIdempotentAndStoresWorkBeforePublication() {
        User user = user();
        enqueue(user, NotificationKind.ALERT, "alert:2026-09");
        enqueue(user, NotificationKind.ALERT, "alert:2026-09");
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(notifications.findAll().getFirst().getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void brokerFailurePreservesPendingWorkForRetry() {
        enqueue(user(), NotificationKind.ALERT, "alert:2026-09");
        publisher.fail = true;
        try {
            assertThat(service.publishDue()).isZero();
            assertThat(notifications.findAll().getFirst().getStatus()).isEqualTo(NotificationStatus.PENDING);
        } finally {
            publisher.fail = false;
        }
        makeDue();
        assertThat(service.publishDue()).isEqualTo(1);
    }

    @Test
    void duplicateMessageCannotAcquireASecondDelivery() {
        var notification = published();
        var claim = service.claim(notification.getId());
        assertThat(claim).isNotNull();
        assertThat(service.claim(notification.getId())).isNull();
        service.complete(notification.getId(), claim.token(), NotificationStatus.SENT);
        service.complete(notification.getId(), claim.token(), NotificationStatus.SENT);
        assertThat(service.claim(notification.getId())).isNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void expiredSendingLeaseIsUncertainRatherThanRetried() {
        var notification = published();
        var claim = service.claim(notification.getId());
        makeDue();
        service.publishDue();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNKNOWN);
        assertThat(service.claim(notification.getId())).isNull();
        // Uma confirmação atrasada com o mesmo token pode resolver o estado incerto.
        service.complete(notification.getId(), claim.token(), NotificationStatus.SENT);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void rejectedDeliveryBecomesPendingAndCanBeRepublished() {
        var notification = published();
        var claim = service.claim(notification.getId());
        service.complete(notification.getId(), claim.token(), NotificationStatus.PENDING);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        makeDue();
        assertThat(service.publishDue()).isEqualTo(1);
        assertThat(service.claim(notification.getId()).token()).isNotEqualTo(claim.token());
    }

    @Test
    void fiveExplicitRejectionsStopAutomaticRetry() {
        var notification = published();
        for (int attempt = 0; attempt < 5; attempt++) {
            var claim = service.claim(notification.getId());
            service.complete(notification.getId(), claim.token(), NotificationStatus.PENDING);
            makeDue();
            service.publishDue();
        }
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
    }

    @Test
    void disconnectOrOptOutCancelsQueuedDelivery() {
        var notification = published();
        User user = users.findById(notification.getUserId()).orElseThrow();
        service.updatePreferences(user.getTelegramId(), new AlertPreferences(false, true, true));
        assertThat(service.claim(notification.getId())).isNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
    }

    @Test
    void changedTelegramLinkCannotReceiveOldNotification() {
        var notification = published();
        users.findById(notification.getUserId()).orElseThrow().setTelegramId(456L);
        assertThat(service.claim(notification.getId())).isNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
    }

    @Test
    void weeklyAndMonthlyPreferencesAreIndependent() {
        User user = user();
        service.updatePreferences(123L, new AlertPreferences(false, true, false));
        enqueue(user, NotificationKind.ALERT, "alert:2026-09");
        enqueue(user, NotificationKind.WEEKLY, "weekly:2026-09-13");
        enqueue(user, NotificationKind.MONTHLY, "monthly:2026-08");
        assertThat(notifications.findAll()).singleElement()
                .extracting(FinancialNotificationEntity::getKind).isEqualTo(NotificationKind.WEEKLY);
        assertThat(service.preferences(123L)).isEqualTo(new AlertPreferences(false, true, false));
    }

    @Test
    void expiredNotificationIsCancelledAndNotSent() {
        var notification = published();
        notification.setExpiresAt(NOW.minusSeconds(1));
        assertThat(service.claim(notification.getId())).isNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
    }

    @Test
    void publishedNotificationIsRepublishedWhenNoConsumerClaimsIt() {
        var notification = published();
        makeDue();
        assertThat(service.publishDue()).isEqualTo(1);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PUBLISHED);
    }

    @Test
    void unrelatedTokenCannotAcknowledgeDelivery() {
        var notification = published();
        service.claim(notification.getId());
        assertThatThrownBy(() -> service.complete(notification.getId(), "wrong", NotificationStatus.SENT))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid delivery token");
    }

    @Test
    void globalDisablePreventsQueuedDelivery() {
        var notification = published();
        var disabled = new FinancialNotificationService(notifications, users, publisher,
                Clock.fixed(Instant.parse("2026-09-15T09:00:00Z"), ZoneOffset.UTC), false);
        assertThat(disabled.claim(notification.getId())).isNull();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
    }

    @Test
    void expiredCompletedContentIsPurged() {
        var notification = published();
        var claim = service.claim(notification.getId());
        service.complete(notification.getId(), claim.token(), NotificationStatus.SENT);
        notification.setExpiresAt(NOW.minusDays(8));
        notifications.flush();
        service.publishDue();
        entityManager.clear();
        assertThat(notifications.count()).isZero();
    }

    @Test
    void expiredUncertainContentIsRedactedWhileMetadataRemains() {
        var notification = published();
        var claim = service.claim(notification.getId());
        service.complete(notification.getId(), claim.token(), NotificationStatus.UNKNOWN);
        notification.setExpiresAt(NOW.minusDays(8));
        notifications.flush();
        String id = notification.getId();
        service.publishDue();
        entityManager.clear();
        var stored = notifications.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(NotificationStatus.UNKNOWN);
        assertThat(stored.getBody()).isEqualTo("Conteúdo expirado");
    }

    @Test
    void expiredPeriodCannotBeRecreated() {
        User user = user();
        service.enqueue(user, NotificationKind.ALERT, "Título", "Corpo", "expired", NOW.minusSeconds(1));
        assertThat(notifications.count()).isZero();
    }

    private FinancialNotificationEntity published() {
        enqueue(user(), NotificationKind.ALERT, "alert:2026-09");
        service.publishDue();
        return notifications.findAll().getFirst();
    }

    private void makeDue() {
        notifications.findAll().forEach(n -> n.setNextAttemptAt(NOW.minusSeconds(1)));
        notifications.flush();
    }

    private void enqueue(User user, NotificationKind kind, String key) {
        service.enqueue(user, kind, "Título", "Mensagem financeira", key, NOW.plusDays(30));
    }

    private User user() {
        User user = new User();
        user.setName("Test");
        user.setEmail("test@example.com");
        user.setPassword("test-password");
        user.setTelegramId(123L);
        return users.saveAndFlush(user);
    }

    static class FakePublisher implements FinancialAlertNotificationPublisher {
        boolean fail;
        List<FinancialAlertNotificationEvent> events = new ArrayList<>();
        public void publish(FinancialAlertNotificationEvent event) {
            if (fail) throw new IllegalStateException("broker unavailable");
            events.add(event);
        }
    }

    @TestConfiguration
    static class Config {
        @Bean FakePublisher publisher() { return new FakePublisher(); }
        @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-09-15T09:00:00Z"), ZoneOffset.UTC); }
        @Bean FinancialNotificationService service(FinancialNotificationRepository notifications,
                UserRepository users, FakePublisher publisher, Clock clock) {
            return new FinancialNotificationService(notifications, users, publisher, clock, true);
        }
    }
}
