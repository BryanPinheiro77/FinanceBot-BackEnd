package com.financebot.telegram.service;

import com.financebot.account.domain.Account;
import com.financebot.account.domain.AccountType;
import com.financebot.account.repository.AccountRepository;
import com.financebot.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramAccountResolverService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account resolveDefaultAccount(User user) {
        Optional<Account> defaultAccount = accountRepository.findByUserIdAndDefaultAccountTrue(user.getId());

        if (defaultAccount.isPresent()) {
            return defaultAccount.get();
        }

        List<Account> accounts = accountRepository.findAllByUserId(user.getId());

        if (accounts.size() == 1) {
            Account account = accounts.get(0);
            account.setDefaultAccount(true);
            return accountRepository.save(account);
        }

        if (accounts.isEmpty()) {
            Account newAccount = new Account();
            newAccount.setName("Banco Principal");
            newAccount.setType(AccountType.CHECKING_ACCOUNT);
            newAccount.setInitialBalance(BigDecimal.ZERO);
            newAccount.setDefaultAccount(true);
            newAccount.setUser(user);

            return accountRepository.save(newAccount);
        }

        Account firstAccount = accounts.get(0);
        firstAccount.setDefaultAccount(true);
        return accountRepository.save(firstAccount);
    }
}