package com.financebot.telegrambot.service;

import com.financebot.telegrambot.confirmation.PendingConfirmationLockedException;
import com.financebot.telegrambot.confirmation.PendingConfirmation;
import com.financebot.telegrambot.confirmation.PendingConfirmationStore;
import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramPendingConfirmationService {
    // Menor que a retenção mínima de uma hora para chaves Telegram na API.
    private static final Duration TTL = Duration.ofMinutes(15);
    private final PendingConfirmationStore store;

    public void savePending(Long telegramId, PendingTelegramTransaction pendingTransaction) {
        if (!store.save(telegramId, new PendingConfirmation(UUID.randomUUID().toString(), pendingTransaction,
                Instant.now().plus(TTL), false))) {
            throw new PendingConfirmationLockedException();
        }
    }
    public PendingConfirmation beginConfirmation(Long telegramId) { return store.begin(telegramId); }
    public PendingConfirmation getConfirmation(Long telegramId) { return store.find(telegramId); }
    public PendingTelegramTransaction getPending(Long telegramId) {
        PendingConfirmation value = getConfirmation(telegramId);
        return value == null ? null : value.transaction();
    }
    public void clearPending(Long telegramId) { store.delete(telegramId); }
    public void clearConfirmed(Long telegramId, String key) { store.deleteIfKey(telegramId, key); }
    public boolean hasPending(Long telegramId) { return getConfirmation(telegramId) != null; }
}
