package com.financebot.account.repository;

import com.financebot.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByUserIdOrderByNameAsc(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    boolean existsByNameIgnoreCaseAndUserId(String name, Long userId);

    Optional<Account> findByUserIdAndDefaultAccountTrue(Long userId);

    List<Account> findAllByUserId(Long userId);

    Optional<Account> findByUserIdAndNameIgnoreCase(Long userId, String name);
}