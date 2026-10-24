package com.financebot.transaction.idempotency;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.transaction-idempotency")
public class TransactionIdempotencyProperties {
    @Min(60) @Max(604800)
    private long ttlSeconds = 86400;
    @Min(1) @Max(10000)
    private int cleanupBatchSize = 1000;
}
