package com.financebot.account.service;

import com.financebot.account.domain.Account;
import com.financebot.account.dto.AccountResponse;
import com.financebot.account.dto.CreateAccountRequest;
import com.financebot.account.dto.UpdateAccountRequest;
import com.financebot.account.mapper.AccountMapper;
import com.financebot.account.repository.AccountRepository;
import com.financebot.transaction.domain.TransactionType;
import com.financebot.transaction.repository.TransactionRepository;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String ACCOUNT_NOT_FOUND_MESSAGE = "Account not found";
    private static final String AUTHENTICATED_USER_NOT_FOUND_MESSAGE = "Authenticated user not found";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse create(CreateAccountRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        validateDuplicateAccountName(request.name(), user.getId());
        validateInitialBalance(request.initialBalance());

        Account account = accountMapper.toEntity(request);
        account.setUser(user);

        Account saved = accountRepository.save(account);

        BigDecimal currentBalance = calculateCurrentBalance(saved);

        return accountMapper.toResponse(saved, currentBalance);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        return accountRepository.findAllByUserIdOrderByNameAsc(user.getId())
                .stream()
                .map(account -> accountMapper.toResponse(account, calculateCurrentBalance(account)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Account account = accountRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND_MESSAGE));

        BigDecimal currentBalance = calculateCurrentBalance(account);

        return accountMapper.toResponse(account, currentBalance);
    }

    @Transactional
    public AccountResponse update(Long id, UpdateAccountRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Account account = accountRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND_MESSAGE));

        boolean changedName = !account.getName().equalsIgnoreCase(request.name().trim());

        if (changedName) {
            validateDuplicateAccountName(request.name(), user.getId());
        }

        validateInitialBalance(request.initialBalance());

        accountMapper.updateEntity(request, account);

        Account updated = accountRepository.save(account);

        BigDecimal currentBalance = calculateCurrentBalance(updated);

        return accountMapper.toResponse(updated, currentBalance);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Account account = accountRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(ACCOUNT_NOT_FOUND_MESSAGE));

        accountRepository.delete(account);
    }

    private BigDecimal calculateCurrentBalance(Account account) {
        BigDecimal totalIncome = transactionRepository.sumAmountByAccountAndUserAndType(
                account.getId(),
                account.getUser().getId(),
                TransactionType.INCOME
        );

        BigDecimal totalExpense = transactionRepository.sumAmountByAccountAndUserAndType(
                account.getId(),
                account.getUser().getId(),
                TransactionType.EXPENSE
        );

        return account.getInitialBalance()
                .add(totalIncome)
                .subtract(totalExpense);
    }

    private void validateDuplicateAccountName(String name, Long userId) {
        boolean alreadyExists = accountRepository.existsByNameIgnoreCaseAndUserId(
                name.trim(),
                userId
        );

        if (alreadyExists) {
            throw new IllegalArgumentException("Account already exists for this user");
        }
    }

    private void validateInitialBalance(BigDecimal initialBalance) {
        if (initialBalance == null) {
            throw new IllegalArgumentException("Initial balance is required");
        }

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(AUTHENTICATED_USER_NOT_FOUND_MESSAGE));
    }
}