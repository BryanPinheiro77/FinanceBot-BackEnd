package com.financebot.alert.domain;

public record ExcessiveInstallmentAlert(
        long activeInstallmentGroups,
        int threshold,
        String explanation
) {
}
