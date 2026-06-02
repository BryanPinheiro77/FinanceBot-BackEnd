package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelegramPendingQueryService {

    private final Map<Long, ParsedTelegramMessage> pendingQueries = new ConcurrentHashMap<>();

    public void savePending(Long telegramId, ParsedTelegramMessage parsedMessage) {
        pendingQueries.put(telegramId, parsedMessage);
    }

    public ParsedTelegramMessage getPending(Long telegramId) {
        return pendingQueries.get(telegramId);
    }

    public void clearPending(Long telegramId) {
        pendingQueries.remove(telegramId);
    }

    public boolean hasPending(Long telegramId) {
        return pendingQueries.containsKey(telegramId);
    }
}