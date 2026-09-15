package com.financebot.alert.port;

import java.time.LocalDate;

public interface InstallmentRiskPort {

    Long countDistinctActiveInstallmentGroupsByUser(Long userId, LocalDate today);
}
