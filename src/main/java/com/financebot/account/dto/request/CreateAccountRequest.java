package com.financebot.account.dto.request;

import com.financebot.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must have at most 100 characters")
        String name,

        @NotNull(message = "Type is required")
        AccountType type,

        @NotNull(message = "Initial balance is required")
        BigDecimal initialBalance
) {
}