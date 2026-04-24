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

    private static final String DEFAULT_ACCOUNT_NAME = "Banco Principal";

    private final AccountRepository accountRepository;

    @Transactional
    public Account resolve(User user, String explicitAccountName) {
        if (explicitAccountName != null && !explicitAccountName.isBlank()) {
            Optional<Account> explicitAccount = accountRepository
                    .findByUserIdAndNameIgnoreCase(user.getId(), explicitAccountName.trim());

            if (explicitAccount.isPresent()) {
                return explicitAccount.get();
            }

            Account newAccount = new Account();
            newAccount.setName(capitalizeWords(explicitAccountName.trim()));
            newAccount.setType(AccountType.CHECKING_ACCOUNT);
            newAccount.setInitialBalance(BigDecimal.ZERO);
            newAccount.setDefaultAccount(false);
            newAccount.setUser(user);

            return accountRepository.save(newAccount);
        }

        return resolveDefaultAccount(user);
    }

    @Transactional
    public Account getOrCreateDefaultAccount(User user) {
        return resolveDefaultAccount(user);
    }

    private Account resolveDefaultAccount(User user) {
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
            newAccount.setName(DEFAULT_ACCOUNT_NAME);
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

    private String capitalizeWords(String text) {
        String[] parts = text.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(" ");
            }

            result.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase());
        }

        return result.toString();
    }
}