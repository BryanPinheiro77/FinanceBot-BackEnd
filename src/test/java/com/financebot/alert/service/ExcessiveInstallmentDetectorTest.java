package com.financebot.alert.service;

import com.financebot.alert.port.InstallmentRiskPort;
import com.financebot.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcessiveInstallmentDetectorTest {

    private final FakeInstallmentRiskPort port = new FakeInstallmentRiskPort();
    private ExcessiveInstallmentDetector detector;
    private User user;

    @BeforeEach
    void setUp() {
        detector = new ExcessiveInstallmentDetector(
                port,
                Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC)
        );
        user = new User();
        user.setId(1L);
    }

    @Test
    void shouldDetectFiveOrMoreActiveInstallmentGroups() {
        port.count = 5L;

        var alert = detector.detect(user);

        assertThat(alert).isNotNull();
        assertThat(alert.activeInstallmentGroups()).isEqualTo(5);
        assertThat(alert.explanation()).contains("5 grupos");
    }

    @Test
    void shouldIgnoreCountBelowThreshold() {
        port.count = 4L;

        assertThat(detector.detect(user)).isNull();
    }

    @Test
    void shouldRejectUserWithoutId() {
        assertThatThrownBy(() -> detector.detect(new User()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with id is required");
    }

    private static final class FakeInstallmentRiskPort implements InstallmentRiskPort {
        private Long count;

        @Override
        public Long countDistinctActiveInstallmentGroupsByUser(Long userId, LocalDate today) {
            return count;
        }
    }
}
