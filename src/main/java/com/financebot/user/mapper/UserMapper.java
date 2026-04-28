package com.financebot.user.mapper;

import com.financebot.user.domain.User;
import com.financebot.user.dto.response.CurrentUserResponse;
import com.financebot.user.dto.response.TelegramLinkCodeResponse;
import com.financebot.user.dto.response.TelegramLinkConfirmResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserMapper {

    public CurrentUserResponse toCurrentUserResponse(User user) {
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

    public TelegramLinkCodeResponse toTelegramLinkCodeResponse(
            String telegramLinkCode,
            LocalDateTime expiresAt
    ) {
        return new TelegramLinkCodeResponse(
                telegramLinkCode,
                expiresAt,
                "Send this code to the Telegram bot to link your account."
        );
    }

    public TelegramLinkConfirmResponse toTelegramLinkAlreadyConnectedResponse() {
        return new TelegramLinkConfirmResponse(
                true,
                "Sua conta já estava conectada a este Telegram."
        );
    }

    public TelegramLinkConfirmResponse toTelegramLinkConnectedResponse() {
        return new TelegramLinkConfirmResponse(
                true,
                "Conta conectada com sucesso ao Telegram."
        );
    }
}