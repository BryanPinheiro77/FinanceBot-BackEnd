package com.financebot.transaction.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(TransactionIdempotencyProperties.class)
public class IdempotencyCleanupScheduler {
    private final TransactionIdempotencyService idempotency;

    @Scheduled(fixedDelayString = "${app.transaction-idempotency.cleanup-interval:600000}",
            initialDelayString = "${app.transaction-idempotency.cleanup-interval:600000}")
    public void purgeExpired() {
        idempotency.purgeExpired();
    }
}
