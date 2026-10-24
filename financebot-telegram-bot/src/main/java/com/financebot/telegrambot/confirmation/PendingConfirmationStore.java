package com.financebot.telegrambot.confirmation;

public interface PendingConfirmationStore {
    boolean save(Long telegramId, PendingConfirmation confirmation);
    PendingConfirmation begin(Long telegramId);
    PendingConfirmation find(Long telegramId);
    void delete(Long telegramId);
    void deleteIfKey(Long telegramId, String key);
}
