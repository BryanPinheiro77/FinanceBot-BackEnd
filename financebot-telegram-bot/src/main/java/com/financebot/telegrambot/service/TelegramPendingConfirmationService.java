package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelegramPendingConfirmationService {

    private final Map<Long, ParsedTelegramMessage> pendingMessages = new ConcurrentHashMap<>();

    public void savePending(Long telegramId, ParsedTelegramMessage parsedMessage) {
        pendingMessages.put(telegramId, parsedMessage);
    }

    public ParsedTelegramMessage getPending(Long telegramId) {
        return pendingMessages.get(telegramId);
    }

    public void clearPending(Long telegramId) {
        pendingMessages.remove(telegramId);
    }

    public boolean hasPending(Long telegramId) {
        return pendingMessages.containsKey(telegramId);
    }
}