package com.financebot.transaction.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionIdempotencyPropertiesTest {
    @Test
    void invalidRetentionAndBatchSizeFailAtStartup() {
        new ApplicationContextRunner().withUserConfiguration(Config.class)
                .withPropertyValues("app.transaction-idempotency.ttl-seconds=0",
                        "app.transaction-idempotency.cleanup-batch-size=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TransactionIdempotencyProperties.class)
    static class Config { }
}
