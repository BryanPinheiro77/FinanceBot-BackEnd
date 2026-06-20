package com.financebot.transaction.domain.installment;

import com.financebot.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstallmentPlanFactory {

    private static final int MONEY_SCALE = 2;
    private static final String INSTALLMENT_ONLY_EXPENSE_MESSAGE =
            "Installment transactions are allowed only for expenses";
    private static final String MINIMUM_INSTALLMENTS_MESSAGE =
            "Total installments must be at least 2";

    public InstallmentPlan create(
            BigDecimal totalAmount,
            String description,
            LocalDate firstDate,
            TransactionType type,
            Integer totalInstallments
    ) {
        validate(type, totalInstallments);

        String installmentGroupId = UUID.randomUUID().toString();

        BigDecimal installmentAmount = totalAmount.divide(
                BigDecimal.valueOf(totalInstallments),
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );

        BigDecimal accumulated = BigDecimal.ZERO;
        List<InstallmentPlanItem> items = new ArrayList<>();

        for (int installmentNumber = 1; installmentNumber <= totalInstallments; installmentNumber++) {
            BigDecimal amount = calculateCurrentInstallmentAmount(
                    installmentNumber,
                    totalInstallments,
                    installmentAmount,
                    totalAmount,
                    accumulated
            );

            if (installmentNumber < totalInstallments) {
                accumulated = accumulated.add(amount);
            }

            items.add(new InstallmentPlanItem(
                    amount,
                    buildInstallmentDescription(description, installmentNumber, totalInstallments),
                    firstDate.plusMonths(installmentNumber - 1L),
                    installmentNumber,
                    totalInstallments,
                    installmentGroupId
            ));
        }

        return new InstallmentPlan(
                installmentGroupId,
                totalInstallments,
                items
        );
    }

    public InstallmentPlan createRemaining(
            BigDecimal totalAmount,
            String description,
            LocalDate firstRemainingInstallmentDate,
            TransactionType type,
            Integer totalInstallments,
            Integer firstRemainingInstallmentNumber
    ) {
        validate(type, totalInstallments, firstRemainingInstallmentNumber);

        String installmentGroupId = UUID.randomUUID().toString();

        BigDecimal installmentAmount = totalAmount.divide(
                BigDecimal.valueOf(totalInstallments),
                MONEY_SCALE,
                RoundingMode.HALF_UP
        );

        BigDecimal accumulated = BigDecimal.ZERO;
        List<InstallmentPlanItem> items = new ArrayList<>();

        for (int installmentNumber = 1; installmentNumber <= totalInstallments; installmentNumber++) {
            BigDecimal amount = calculateCurrentInstallmentAmount(
                    installmentNumber,
                    totalInstallments,
                    installmentAmount,
                    totalAmount,
                    accumulated
            );

            if (installmentNumber < totalInstallments) {
                accumulated = accumulated.add(amount);
            }

            if (installmentNumber < firstRemainingInstallmentNumber) {
                continue;
            }

            items.add(new InstallmentPlanItem(
                    amount,
                    buildInstallmentDescription(description, installmentNumber, totalInstallments),
                    firstRemainingInstallmentDate.plusMonths((long) installmentNumber - firstRemainingInstallmentNumber),
                    installmentNumber,
                    totalInstallments,
                    installmentGroupId
            ));
        }

        return new InstallmentPlan(
                installmentGroupId,
                totalInstallments,
                items
        );
    }

    private void validate(TransactionType type, Integer totalInstallments) {
        if (type != TransactionType.EXPENSE) {
            throw new IllegalArgumentException(INSTALLMENT_ONLY_EXPENSE_MESSAGE);
        }

        if (totalInstallments == null || totalInstallments < 2) {
            throw new IllegalArgumentException(MINIMUM_INSTALLMENTS_MESSAGE);
        }
    }

    private void validate(
            TransactionType type,
            Integer totalInstallments,
            Integer firstRemainingInstallmentNumber
    ) {
        validate(type, totalInstallments);

        if (firstRemainingInstallmentNumber == null || firstRemainingInstallmentNumber < 1) {
            throw new IllegalArgumentException("First remaining installment number must be at least 1");
        }

        if (firstRemainingInstallmentNumber > totalInstallments) {
            throw new IllegalArgumentException(
                    "First remaining installment number cannot be greater than total installments"
            );
        }
    }

    private BigDecimal calculateCurrentInstallmentAmount(
            int currentInstallment,
            int totalInstallments,
            BigDecimal installmentAmount,
            BigDecimal totalAmount,
            BigDecimal accumulated
    ) {
        if (currentInstallment < totalInstallments) {
            return installmentAmount;
        }

        return totalAmount.subtract(accumulated);
    }

    private String buildInstallmentDescription(
            String description,
            int installmentNumber,
            int totalInstallments
    ) {
        return "%s - %d/%d".formatted(description.trim(), installmentNumber, totalInstallments);
    }
}
