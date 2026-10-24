package com.financebot.telegram.service;

import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.transaction.idempotency.TransactionIdempotencyService;
import com.financebot.transaction.idempotency.TransactionIdempotencyService.Operation;
import com.financebot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramTransactionIdempotencyService {
    private final UserRepository users;
    private final TransactionIdempotencyService idempotency;

    public void execute(Long telegramId, Operation operation, String key, Object request, Runnable action) {
        if (key == null) {
            action.run();
            return;
        }
        var user = users.findByTelegramId(telegramId).orElseThrow(TelegramUserNotFoundException::new);
        idempotency.execute(user.getId(), operation, key, request, Boolean.class, () -> {
            action.run();
            return Boolean.TRUE;
        });
    }
}
