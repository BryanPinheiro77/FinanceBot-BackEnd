package com.financebot.alert.service;

import com.financebot.alert.domain.TightBudgetAlert;
import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TightBudgetDetector {

    private static final BigDecimal ALERT_THRESHOLD = BigDecimal.valueOf(60);

    public TightBudgetAlert detect(FinancialCommitmentResponse analysis) {
        if (analysis == null) {
            throw new IllegalArgumentException("Financial analysis is required");
        }
        BigDecimal percentage = valueOrZero(analysis.commitmentPercentage());
        BigDecimal projectedNet = valueOrZero(analysis.projectedNetNextMonth());
        if (percentage.compareTo(ALERT_THRESHOLD) < 0 && projectedNet.signum() >= 0) {
            return null;
        }
        String reason = projectedNet.signum() < 0
                ? "a projeção do próximo mês ficou negativa"
                : "o comprometimento projetado atingiu " + percentage + "%";
        return new TightBudgetAlert(
                percentage,
                projectedNet,
                "Seu orçamento está apertado: " + reason + "."
        );
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
