package com.financebot.telegrambot.confirmation;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "telegram.state.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryPendingConfirmationStore implements PendingConfirmationStore {
    private final ConcurrentHashMap<Long, PendingConfirmation> pending = new ConcurrentHashMap<>();

    public boolean save(Long id, PendingConfirmation value) {
        return pending.compute(id, (ignored, old) -> old != null && old.attempted()
                && old.expiresAt().isAfter(Instant.now()) ? old : value) == value;
    }
    public PendingConfirmation begin(Long id) {
        return pending.computeIfPresent(id, (ignored, value) -> value.expiresAt().isAfter(Instant.now())
                ? new PendingConfirmation(value.key(), value.transaction(), value.expiresAt(), true) : null);
    }
    public PendingConfirmation find(Long id) {
        return pending.computeIfPresent(id, (ignored, value) ->
                value.expiresAt().isAfter(Instant.now()) ? value : null);
    }
    public void delete(Long id) { pending.remove(id); }
    public void deleteIfKey(Long id, String key) {
        pending.computeIfPresent(id, (ignored, value) -> value.key().equals(key) ? null : value);
    }
}
