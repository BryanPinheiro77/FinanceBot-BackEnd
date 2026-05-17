package com.financebot.transaction.domain.installment;

import com.financebot.transaction.domain.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallmentPlanFactoryTest {

    private final InstallmentPlanFactory installmentPlanFactory = new InstallmentPlanFactory();

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("deve criar plano de parcelamento com valores, datas e descricoes corretas")
        void shouldCreateInstallmentPlanSuccessfully() {
            InstallmentPlan plan = installmentPlanFactory.create(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    3
            );

            assertThat(plan.installmentGroupId()).isNotBlank();
            assertThat(plan.totalInstallments()).isEqualTo(3);
            assertThat(plan.items()).hasSize(3);

            assertThat(plan.items().get(0).amount()).isEqualByComparingTo("333.33");
            assertThat(plan.items().get(1).amount()).isEqualByComparingTo("333.33");
            assertThat(plan.items().get(2).amount()).isEqualByComparingTo("333.34");

            assertThat(plan.items().get(0).description()).isEqualTo("Notebook - 1/3");
            assertThat(plan.items().get(1).description()).isEqualTo("Notebook - 2/3");
            assertThat(plan.items().get(2).description()).isEqualTo("Notebook - 3/3");

            assertThat(plan.items().get(0).date()).isEqualTo(LocalDate.of(2026, 4, 10));
            assertThat(plan.items().get(1).date()).isEqualTo(LocalDate.of(2026, 5, 10));
            assertThat(plan.items().get(2).date()).isEqualTo(LocalDate.of(2026, 6, 10));

            assertThat(plan.items().get(0).installmentNumber()).isEqualTo(1);
            assertThat(plan.items().get(1).installmentNumber()).isEqualTo(2);
            assertThat(plan.items().get(2).installmentNumber()).isEqualTo(3);

            assertThat(plan.items().get(0).totalInstallments()).isEqualTo(3);
            assertThat(plan.items().get(1).totalInstallments()).isEqualTo(3);
            assertThat(plan.items().get(2).totalInstallments()).isEqualTo(3);

            assertThat(plan.items().get(0).installmentGroupId()).isEqualTo(plan.installmentGroupId());
            assertThat(plan.items().get(1).installmentGroupId()).isEqualTo(plan.installmentGroupId());
            assertThat(plan.items().get(2).installmentGroupId()).isEqualTo(plan.installmentGroupId());
        }

        @Test
        @DisplayName("deve lancar erro quando tipo da transacao nao for despesa")
        void shouldThrowWhenTransactionTypeIsNotExpense() {
            assertThatThrownBy(() -> installmentPlanFactory.create(
                    new BigDecimal("1000.00"),
                    "Salário parcelado",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.INCOME,
                    3
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Installment transactions are allowed only for expenses");
        }

        @Test
        @DisplayName("deve lancar erro quando total de parcelas for menor que dois")
        void shouldThrowWhenTotalInstallmentsIsLessThanTwo() {
            assertThatThrownBy(() -> installmentPlanFactory.create(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    1
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Total installments must be at least 2");
        }

        @Test
        @DisplayName("deve lancar erro quando total de parcelas for nulo")
        void shouldThrowWhenTotalInstallmentsIsNull() {
            assertThatThrownBy(() -> installmentPlanFactory.create(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Total installments must be at least 2");
        }
    }
}