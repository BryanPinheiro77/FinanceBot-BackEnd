package com.financebot.alert.service;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TightBudgetDetectorTest {

    private final TightBudgetDetector detector = new TightBudgetDetector();

    @Test
    void shouldDetectCommitmentAtSixtyPercent() {
        var alert = detector.detect(analysis("60", "100"));

        assertThat(alert).isNotNull();
        assertThat(alert.commitmentPercentage()).isEqualByComparingTo("60");
        assertThat(alert.explanation()).contains("60");
    }

    @Test
    void shouldDetectNegativeProjectedNetEvenBelowPercentageThreshold() {
        var alert = detector.detect(analysis("20", "-1"));

        assertThat(alert).isNotNull();
        assertThat(alert.explanation()).contains("negativa");
    }

    @Test
    void shouldIgnoreHealthyBudget() {
        assertThat(detector.detect(analysis("59.99", "100"))).isNull();
    }

    @Test
    void shouldRejectNullAnalysis() {
        assertThatThrownBy(() -> detector.detect(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Financial analysis is required");
    }

    private FinancialCommitmentResponse analysis(String percentage, String projectedNet) {
        return new FinancialCommitmentResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal(projectedNet), new BigDecimal(percentage), 0L,
                false, false, false, "LOW", ""
        );
    }
}
