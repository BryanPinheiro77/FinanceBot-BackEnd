package com.financebot.transaction.domain.installment;

import com.financebot.transaction.domain.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

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
            BigDecimal totalAmount = new BigDecimal("1000.00");
            String description = "Salário parcelado";
            LocalDate firstInstallmentDate = LocalDate.of(2026, 4, 10);
            TransactionType transactionType = TransactionType.INCOME;
            Integer totalInstallments = 3;

            assertThatThrownBy(() -> installmentPlanFactory.create(
                    totalAmount,
                    description,
                    firstInstallmentDate,
                    transactionType,
                    totalInstallments
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Installment transactions are allowed only for expenses");
        }

        @Test
        @DisplayName("deve lancar erro quando total de parcelas for menor que dois")
        void shouldThrowWhenTotalInstallmentsIsLessThanTwo() {
            BigDecimal totalAmount = new BigDecimal("1000.00");
            String description = "Notebook";
            LocalDate firstInstallmentDate = LocalDate.of(2026, 4, 10);
            TransactionType transactionType = TransactionType.EXPENSE;
            Integer totalInstallments = 1;

            assertThatThrownBy(() -> installmentPlanFactory.create(
                    totalAmount,
                    description,
                    firstInstallmentDate,
                    transactionType,
                    totalInstallments
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Total installments must be at least 2");
        }

        @Test
        @DisplayName("deve lancar erro quando total de parcelas for nulo")
        void shouldThrowWhenTotalInstallmentsIsNull() {
            BigDecimal totalAmount = new BigDecimal("1000.00");
            String description = "Notebook";
            LocalDate firstInstallmentDate = LocalDate.of(2026, 4, 10);
            TransactionType transactionType = TransactionType.EXPENSE;
            Integer totalInstallments = null;

            assertThatThrownBy(() -> installmentPlanFactory.create(
                    totalAmount,
                    description,
                    firstInstallmentDate,
                    transactionType,
                    totalInstallments
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Total installments must be at least 2");
        }

        @Test
        @DisplayName("deve preservar arredondamento half up e ajustar ultima parcela")
        void shouldPreserveHalfUpRoundingAndAdjustLastInstallment() {
            InstallmentPlan plan = installmentPlanFactory.create(
                    new BigDecimal("10.01"),
                    "Compra teste",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    2
            );

            assertThat(plan.items()).hasSize(2);

            assertThat(plan.items().get(0).amount()).isEqualByComparingTo("5.01");
            assertThat(plan.items().get(1).amount()).isEqualByComparingTo("5.00");
        }
    }

    @Nested
    @DisplayName("createRemaining")
    class CreateRemainingTests {

        @Test
        @DisplayName("deve criar apenas parcelas restantes a partir da primeira parcela em aberto")
        void shouldCreateOnlyRemainingInstallmentsFromFirstOpenInstallment() {
            InstallmentPlan plan = installmentPlanFactory.createRemaining(
                    new BigDecimal("6000.00"),
                    "iPhone",
                    LocalDate.of(2026, 6, 15),
                    TransactionType.EXPENSE,
                    10,
                    6
            );

            assertThat(plan.installmentGroupId()).isNotBlank();
            assertThat(plan.totalInstallments()).isEqualTo(10);
            assertThat(plan.items()).hasSize(5);

            assertThat(plan.items())
                    .extracting(
                            InstallmentPlanItem::amount,
                            InstallmentPlanItem::description,
                            InstallmentPlanItem::date,
                            InstallmentPlanItem::installmentNumber,
                            InstallmentPlanItem::totalInstallments,
                            InstallmentPlanItem::installmentGroupId
                    )
                    .containsExactly(
                            tuple(
                                    new BigDecimal("600.00"),
                                    "iPhone - 6/10",
                                    LocalDate.of(2026, 6, 15),
                                    6,
                                    10,
                                    plan.installmentGroupId()
                            ),
                            tuple(
                                    new BigDecimal("600.00"),
                                    "iPhone - 7/10",
                                    LocalDate.of(2026, 7, 15),
                                    7,
                                    10,
                                    plan.installmentGroupId()
                            ),
                            tuple(
                                    new BigDecimal("600.00"),
                                    "iPhone - 8/10",
                                    LocalDate.of(2026, 8, 15),
                                    8,
                                    10,
                                    plan.installmentGroupId()
                            ),
                            tuple(
                                    new BigDecimal("600.00"),
                                    "iPhone - 9/10",
                                    LocalDate.of(2026, 9, 15),
                                    9,
                                    10,
                                    plan.installmentGroupId()
                            ),
                            tuple(
                                    new BigDecimal("600.00"),
                                    "iPhone - 10/10",
                                    LocalDate.of(2026, 10, 15),
                                    10,
                                    10,
                                    plan.installmentGroupId()
                            )
                    );
        }

        @Test
        @DisplayName("deve preservar arredondamento do total original ao gerar parcelas restantes")
        void shouldPreserveOriginalTotalRoundingWhenCreatingRemainingInstallments() {
            InstallmentPlan plan = installmentPlanFactory.createRemaining(
                    new BigDecimal("10.01"),
                    "Compra teste",
                    LocalDate.of(2026, 5, 10),
                    TransactionType.EXPENSE,
                    2,
                    2
            );

            assertThat(plan.items()).hasSize(1);
            assertThat(plan.items().get(0).amount()).isEqualByComparingTo("5.00");
            assertThat(plan.items().get(0).description()).isEqualTo("Compra teste - 2/2");
            assertThat(plan.items().get(0).date()).isEqualTo(LocalDate.of(2026, 5, 10));
            assertThat(plan.items().get(0).installmentNumber()).isEqualTo(2);
            assertThat(plan.items().get(0).totalInstallments()).isEqualTo(2);
        }

        @Test
        @DisplayName("deve permitir primeira parcela restante igual a um")
        void shouldAllowFirstRemainingInstallmentNumberEqualToOne() {
            InstallmentPlan plan = installmentPlanFactory.createRemaining(
                    new BigDecimal("1000.00"),
                    "Notebook",
                    LocalDate.of(2026, 4, 10),
                    TransactionType.EXPENSE,
                    3,
                    1
            );

            assertThat(plan.items()).hasSize(3);
            assertThat(plan.items().get(0).description()).isEqualTo("Notebook - 1/3");
            assertThat(plan.items().get(0).date()).isEqualTo(LocalDate.of(2026, 4, 10));
            assertThat(plan.items().get(1).date()).isEqualTo(LocalDate.of(2026, 5, 10));
            assertThat(plan.items().get(2).date()).isEqualTo(LocalDate.of(2026, 6, 10));
        }

        @Test
        @DisplayName("deve lancar erro quando primeira parcela restante for nula")
        void shouldThrowWhenFirstRemainingInstallmentNumberIsNull() {
            BigDecimal totalAmount = new BigDecimal("1000.00");
            LocalDate firstRemainingInstallmentDate = LocalDate.of(2026, 4, 10);

            assertThatThrownBy(() -> installmentPlanFactory.createRemaining(
                    totalAmount,
                    "Notebook",
                    firstRemainingInstallmentDate,
                    TransactionType.EXPENSE,
                    3,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("First remaining installment number must be at least 1");
        }

        @Test
        @DisplayName("deve lancar erro quando primeira parcela restante for menor que um")
        void shouldThrowWhenFirstRemainingInstallmentNumberIsLessThanOne() {
            BigDecimal totalAmount = new BigDecimal("1000.00");
            LocalDate firstRemainingInstallmentDate = LocalDate.of(2026, 4, 10);

            assertThatThrownBy(() -> installmentPlanFactory.createRemaining(
                    totalAmount,
                    "Notebook",
                    firstRemainingInstallmentDate,
                    TransactionType.EXPENSE,
                    3,
                    0
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("First remaining installment number must be at least 1");
        }

        @Test
        @DisplayName("deve lancar erro quando primeira parcela restante for maior que o total")
        void shouldThrowWhenFirstRemainingInstallmentNumberIsGreaterThanTotalInstallments() {
            BigDecimal totalAmount = new BigDecimal("1000.00");
            LocalDate firstRemainingInstallmentDate = LocalDate.of(2026, 4, 10);

            assertThatThrownBy(() -> installmentPlanFactory.createRemaining(
                    totalAmount,
                    "Notebook",
                    firstRemainingInstallmentDate,
                    TransactionType.EXPENSE,
                    3,
                    4
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("First remaining installment number cannot be greater than total installments");
        }

        @Test
        @DisplayName("deve lancar erro quando tipo da transacao restante nao for despesa")
        void shouldThrowWhenRemainingTransactionTypeIsNotExpense() {
            BigDecimal totalAmount = new BigDecimal("1000.00");
            LocalDate firstRemainingInstallmentDate = LocalDate.of(2026, 4, 10);

            assertThatThrownBy(() -> installmentPlanFactory.createRemaining(
                    totalAmount,
                    "Salário parcelado",
                    firstRemainingInstallmentDate,
                    TransactionType.INCOME,
                    3,
                    2
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Installment transactions are allowed only for expenses");
        }
    }
}
