package com.financebot.transaction.domain.installment;

import com.financebot.transaction.domain.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
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
                RoundingMode.DOWN
        );

        BigDecimal distributedAmount = installmentAmount.multiply(BigDecimal.valueOf(totalInstallments));
        BigDecimal adjustment = totalAmount.subtract(distributedAmount);

        List<InstallmentPlanItem> items = new ArrayList<>();

        for (int installmentNumber = 1; installmentNumber <= totalInstallments; installmentNumber++) {
            BigDecimal amount = installmentAmount;

            if (installmentNumber == totalInstallments) {
                amount = amount.add(adjustment);
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

    private void validate(TransactionType type, Integer totalInstallments) {
        if (type != TransactionType.EXPENSE) {
            throw new IllegalArgumentException(INSTALLMENT_ONLY_EXPENSE_MESSAGE);
        }

        if (totalInstallments == null || totalInstallments < 2) {
            throw new IllegalArgumentException(MINIMUM_INSTALLMENTS_MESSAGE);
        }
    }

    private String buildInstallmentDescription(
            String description,
            int installmentNumber,
            int totalInstallments
    ) {
        return "%s - %d/%d".formatted(description, installmentNumber, totalInstallments);
    }
}