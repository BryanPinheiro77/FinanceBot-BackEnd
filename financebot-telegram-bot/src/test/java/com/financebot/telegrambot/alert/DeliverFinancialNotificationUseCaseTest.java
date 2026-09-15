package com.financebot.telegrambot.alert;

import org.junit.jupiter.api.Test;
import com.financebot.telegrambot.alert.FinancialNotificationSender.DeliveryOutcome;
import static org.assertj.core.api.Assertions.*;

class DeliverFinancialNotificationUseCaseTest {
    private final FakeGateway gateway = new FakeGateway();
    private int sends;

    @Test
    void sendsAndAcknowledgesWithReservationToken() {
        execute(DeliveryOutcome.SENT);
        assertThat(sends).isEqualTo(1);
        assertThat(gateway.outcome).isEqualTo("SENT");
        assertThat(gateway.ackToken).isEqualTo("token");
    }

    @Test
    void duplicateOrCancelledNotificationIsSkipped() {
        gateway.claim = null;
        execute(DeliveryOutcome.SENT);
        assertThat(sends).isZero();
        assertThat(gateway.outcome).isNull();
    }

    @Test
    void explicitRejectionAllowsOutboxRetry() {
        execute(DeliveryOutcome.REJECTED);
        assertThat(gateway.outcome).isEqualTo("PENDING");
    }

    @Test
    void timeoutDoesNotCauseAutomaticResend() {
        execute(DeliveryOutcome.UNKNOWN);
        assertThat(sends).isEqualTo(1);
        assertThat(gateway.outcome).isEqualTo("UNKNOWN");
    }

    @Test
    void ackFailuresRetryOnlyAckAndNeverSendAgain() {
        gateway.ackFailures = 2;
        execute(DeliveryOutcome.SENT);
        assertThat(sends).isEqualTo(1);
        assertThat(gateway.acks).isEqualTo(3);
        assertThat(gateway.outcome).isEqualTo("SENT");
    }

    @Test
    void permanentAckFailureLeavesLeaseForReconciliation() {
        gateway.ackFailures = 3;
        assertThatThrownBy(() -> execute(DeliveryOutcome.SENT)).isInstanceOf(IllegalStateException.class);
        assertThat(sends).isEqualTo(1);
    }

    @Test
    void senderExceptionIsUncertain() {
        new DeliverFinancialNotificationUseCase(gateway, claim -> { throw new IllegalStateException(); }).execute("id");
        assertThat(gateway.outcome).isEqualTo("UNKNOWN");
    }

    @Test
    void backendUnavailableDoesNotSend() {
        gateway.claimFailure = true;
        assertThatThrownBy(() -> execute(DeliveryOutcome.SENT)).isInstanceOf(IllegalStateException.class);
        assertThat(sends).isZero();
    }

    private void execute(DeliveryOutcome outcome) {
        new DeliverFinancialNotificationUseCase(gateway, claim -> { sends++; return outcome; }).execute("id");
    }

    static class FakeGateway implements FinancialNotificationGateway {
        NotificationDeliveryClaim claim = new NotificationDeliveryClaim("token", 123L, "Título", "Corpo");
        String outcome;
        String ackToken;
        int acks;
        int ackFailures;
        boolean claimFailure;
        public NotificationDeliveryClaim claim(String id) {
            if (claimFailure) throw new IllegalStateException("API unavailable");
            return claim;
        }
        public void complete(String id, String token, String outcome) {
            acks++;
            if (ackFailures-- > 0) throw new IllegalStateException("API unavailable");
            this.outcome = outcome;
            this.ackToken = token;
        }
    }
}
