package com.financebot.account.mapper;

import com.financebot.account.domain.Account;
import com.financebot.account.domain.AccountType;
import com.financebot.account.dto.request.CreateAccountRequest;
import com.financebot.account.dto.request.UpdateAccountRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {
    private final AccountMapper mapper = new AccountMapper();

    @Test
    void shouldMapCreateAndUpdateRequestsWithTrimmedName() {
        Account account = mapper.toEntity(new CreateAccountRequest(
                "  Carteira  ", AccountType.CASH, new BigDecimal("100")));
        mapper.updateEntity(new UpdateAccountRequest(
                "  Conta principal  ", AccountType.CHECKING_ACCOUNT, new BigDecimal("250")), account);

        assertThat(account.getName()).isEqualTo("Conta principal");
        assertThat(account.getType()).isEqualTo(AccountType.CHECKING_ACCOUNT);
        assertThat(account.getInitialBalance()).isEqualByComparingTo("250");
    }

    @Test
    void shouldMapAccountToResponseWithCurrentBalance() {
        Account account = new Account();
        account.setId(3L);
        account.setName("Conta");
        account.setType(AccountType.CASH);
        account.setInitialBalance(new BigDecimal("100"));
        account.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

        var response = mapper.toResponse(account, new BigDecimal("125.50"));

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.currentBalance()).isEqualByComparingTo("125.50");
        assertThat(response.createdAt()).isEqualTo(account.getCreatedAt());
    }
}
