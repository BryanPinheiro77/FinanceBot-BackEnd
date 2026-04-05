package com.financebot.user.service;

import com.financebot.user.domain.User;
import com.financebot.user.dto.request.TelegramLinkConfirmRequest;
import com.financebot.user.dto.request.UpdateMonthlyBaseIncomeRequest;
import com.financebot.user.dto.response.CurrentUserResponse;
import com.financebot.user.dto.response.TelegramLinkCodeResponse;
import com.financebot.user.dto.response.TelegramLinkConfirmResponse;
import com.financebot.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TELEGRAM_CODE_PREFIX = "FIN-";
    private static final String TELEGRAM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TELEGRAM_CODE_SIZE = 6;
    private static final int TELEGRAM_CODE_EXPIRATION_MINUTES = 10;

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public CurrentUserResponse getMe(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        return new CurrentUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMonthlyBaseIncome(),
                user.getTelegramId(),
                user.getTelegramId() != null,
                user.getTelegramLinkCode(),
                user.getTelegramLinkCodeExpiresAt()
        );
    }

    @Transactional
    public void updateMonthlyBaseIncome(
            UpdateMonthlyBaseIncomeRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        user.setMonthlyBaseIncome(request.monthlyBaseIncome());
        userRepository.save(user);
    }

    @Transactional
    public TelegramLinkCodeResponse generateTelegramLinkCode(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        String code = TELEGRAM_CODE_PREFIX + generateRandomCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TELEGRAM_CODE_EXPIRATION_MINUTES);

        user.setTelegramLinkCode(code);
        user.setTelegramLinkCodeExpiresAt(expiresAt);

        userRepository.save(user);

        return new TelegramLinkCodeResponse(
                code,
                expiresAt,
                "Send this code to the Telegram bot to link your account."
        );
    }

    @Transactional
    public TelegramLinkConfirmResponse confirmTelegramLink(TelegramLinkConfirmRequest request) {
        User user = userRepository.findByTelegramLinkCode(request.linkCode())
                .orElseThrow(() -> new EntityNotFoundException("Código de vínculo inválido"));

        if (user.getTelegramLinkCodeExpiresAt() == null || user.getTelegramLinkCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código de vínculo expirado");
        }

        if (user.getTelegramId() != null && user.getTelegramId().equals(request.telegramId())) {
            user.setTelegramLinkCode(null);
            user.setTelegramLinkCodeExpiresAt(null);
            userRepository.save(user);

            return new TelegramLinkConfirmResponse(
                    true,
                    "Sua conta já estava conectada a este Telegram."
            );
        }

        boolean telegramAlreadyLinked = userRepository.existsByTelegramId(request.telegramId());

        if (telegramAlreadyLinked) {
            throw new IllegalArgumentException("Este Telegram já está vinculado a outra conta");
        }

        user.setTelegramId(request.telegramId());
        user.setTelegramLinkCode(null);
        user.setTelegramLinkCodeExpiresAt(null);

        userRepository.save(user);

        return new TelegramLinkConfirmResponse(
                true,
                "Conta conectada com sucesso ao Telegram."
        );
    }

    @Transactional
    public void disconnectTelegram(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        user.setTelegramId(null);
        user.setTelegramLinkCode(null);
        user.setTelegramLinkCodeExpiresAt(null);

        userRepository.save(user);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is invalid");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    private String generateRandomCode() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < TELEGRAM_CODE_SIZE; i++) {
            int index = secureRandom.nextInt(TELEGRAM_CODE_CHARS.length());
            builder.append(TELEGRAM_CODE_CHARS.charAt(index));
        }

        return builder.toString();
    }
}