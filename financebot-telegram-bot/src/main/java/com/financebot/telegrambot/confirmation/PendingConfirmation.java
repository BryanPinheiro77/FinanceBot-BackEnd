package com.financebot.telegrambot.confirmation;

import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import java.time.Instant;

public record PendingConfirmation(String key, PendingTelegramTransaction transaction, Instant expiresAt, boolean attempted) { }
