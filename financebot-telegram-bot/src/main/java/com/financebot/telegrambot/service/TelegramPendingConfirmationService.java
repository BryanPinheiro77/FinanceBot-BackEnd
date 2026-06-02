package com.financebot.telegrambot.service;

import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelegramPendingConfirmationService {

    private final Map<Long, PendingTelegramTransaction> pendingMessages = new ConcurrentHashMap<>();

    public void savePending(Long telegramId, PendingTelegramTransaction pendingTransaction) {
        pendingMessages.put(telegramId, pendingTransaction);
    }

    public PendingTelegramTransaction getPending(Long telegramId) {
        return pendingMessages.get(telegramId);
    }

    public void clearPending(Long telegramId) {
        pendingMessages.remove(telegramId);
    }

    public boolean hasPending(Long telegramId) {
        return pendingMessages.containsKey(telegramId);
    }
}