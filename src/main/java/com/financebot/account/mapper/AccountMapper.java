package com.financebot.account.mapper;

import com.financebot.account.domain.Account;
import com.financebot.account.dto.AccountResponse;
import com.financebot.account.dto.CreateAccountRequest;
import com.financebot.account.dto.UpdateAccountRequest;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toEntity(CreateAccountRequest dto) {
        Account account = new Account();
        account.setName(dto.name().trim());
        account.setType(dto.type());
        account.setInitialBalance(dto.initialBalance());
        return account;
    }

    public void updateEntity(UpdateAccountRequest dto, Account account) {
        account.setName(dto.name().trim());
        account.setType(dto.type());
        account.setInitialBalance(dto.initialBalance());
    }

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getInitialBalance(),
                account.getCreatedAt()
        );
    }
}