package com.financebot.telegrambot.conversation.application.port.out;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;

import java.util.Optional;

public interface TelegramQueryContextStore {

    void save(Long telegramId, ParsedTelegramMessage parsedMessage);

    Optional<ParsedTelegramMessage> findByTelegramId(Long telegramId);

    void deleteByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);
}
