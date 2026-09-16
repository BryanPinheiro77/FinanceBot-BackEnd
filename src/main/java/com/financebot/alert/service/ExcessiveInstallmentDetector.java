package com.financebot.alert.service;

import com.financebot.alert.domain.ExcessiveInstallmentAlert;
import com.financebot.alert.port.InstallmentRiskPort;
import com.financebot.user.domain.User;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class ExcessiveInstallmentDetector {

    public static final int DEFAULT_THRESHOLD = 5;

    private final InstallmentRiskPort installmentRiskPort;
    private final Clock clock;

    public ExcessiveInstallmentDetector(InstallmentRiskPort installmentRiskPort, Clock clock) {
        this.installmentRiskPort = installmentRiskPort;
        this.clock = clock;
    }

    public ExcessiveInstallmentAlert detect(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User with id is required");
        }
        long activeGroups = valueOrZero(installmentRiskPort
                .countDistinctActiveInstallmentGroupsByUser(user.getId(), LocalDate.now(clock)));
        if (activeGroups < DEFAULT_THRESHOLD) {
            return null;
        }
        return new ExcessiveInstallmentAlert(
                activeGroups,
                DEFAULT_THRESHOLD,
                "Você tem " + activeGroups + " grupos de parcelas ativos; a partir de "
                        + DEFAULT_THRESHOLD + " o comprometimento merece revisão."
        );
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}
