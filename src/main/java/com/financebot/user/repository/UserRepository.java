package com.financebot.user.repository;

import com.financebot.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByTelegramLinkCode(String telegramLinkCode);

    boolean existsByTelegramId(Long telegramId);

    Optional<User> findByTelegramId(Long telegramId);
}