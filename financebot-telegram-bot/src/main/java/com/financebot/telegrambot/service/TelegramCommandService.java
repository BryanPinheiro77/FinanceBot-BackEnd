package com.financebot.telegrambot.service;

import com.financebot.telegrambot.router.TelegramCommandRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramCommandService {

    private final TelegramCommandRouter telegramCommandRouter;

    public String handleMessage(
            String messageText,
            Long telegramId,
            String telegramUsername,
            String telegramFirstName
    ) {
        return telegramCommandRouter.route(
                messageText,
                telegramId,
                telegramUsername,
                telegramFirstName
        );
    }
}