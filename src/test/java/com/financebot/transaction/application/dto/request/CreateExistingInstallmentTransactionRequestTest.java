package com.financebot.transaction.application.dto.request;

import com.financebot.transaction.domain.SourceType;
import com.financebot.transaction.domain.TransactionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateExistingInstallmentTransactionRequestTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("deve aceitar parcelamento existente com somente valor total")
    void shouldAcceptOnlyTotalAmount() {
        CreateExistingInstallmentTransactionRequest request = validRequest(
                new BigDecimal("6000.00"),
                null
        );

        Set<ConstraintViolation<CreateExistingInstallmentTransactionRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("deve aceitar parcelamento existente com somente valor mensal")
    void shouldAcceptOnlyMonthlyAmount() {
        CreateExistingInstallmentTransactionRequest request = validRequest(
                null,
                new BigDecimal("600.00")
        );

        Set<ConstraintViolation<CreateExistingInstallmentTransactionRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("deve rejeitar parcelamento existente com valor total e mensal juntos")
    void shouldRejectTotalAmountAndMonthlyAmountTogether() {
        CreateExistingInstallmentTransactionRequest request = validRequest(
                new BigDecimal("6000.00"),
                new BigDecimal("600.00")
        );

        Set<ConstraintViolation<CreateExistingInstallmentTransactionRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Exactly one of total amount or monthly amount must be provided");
    }

    @Test
    @DisplayName("deve rejeitar parcelamento existente sem valor total e sem valor mensal")
    void shouldRejectMissingTotalAmountAndMonthlyAmount() {
        CreateExistingInstallmentTransactionRequest request = validRequest(null, null);

        Set<ConstraintViolation<CreateExistingInstallmentTransactionRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Exactly one of total amount or monthly amount must be provided");
    }

    private CreateExistingInstallmentTransactionRequest validRequest(
            BigDecimal totalAmount,
            BigDecimal monthlyAmount
    ) {
        return new CreateExistingInstallmentTransactionRequest(
                totalAmount,
                monthlyAmount,
                "iPhone",
                LocalDate.of(2026, 6, 15),
                TransactionType.EXPENSE,
                SourceType.WEB,
                10L,
                20L,
                10,
                6
        );
    }
}
