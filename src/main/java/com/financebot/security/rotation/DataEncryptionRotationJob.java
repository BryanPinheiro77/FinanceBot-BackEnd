package com.financebot.security.rotation;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "app.data-encryption.rotation.enabled", havingValue = "true")
class DataEncryptionRotationJob implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataEncryptionRotationJob.class);

    private final DataEncryptionRotationService rotationService;
    private final boolean backupConfirmed;
    private final int batchSize;
    private final AtomicBoolean completed = new AtomicBoolean();
    private final AtomicBoolean failed = new AtomicBoolean();
    private final AtomicLong rotatedRecords = new AtomicLong();
    private volatile String failureType;

    DataEncryptionRotationJob(
            DataEncryptionRotationService rotationService,
            @Value("${app.data-encryption.rotation.backup-confirmed:false}") boolean backupConfirmed,
            @Value("${app.data-encryption.rotation.batch-size:100}") int batchSize
    ) {
        this.rotationService = rotationService;
        this.backupConfirmed = backupConfirmed;
        this.batchSize = batchSize;
    }

    @PostConstruct
    void validateConfiguration() {
        if (!backupConfirmed) {
            throw new IllegalStateException(
                    "A rotação exige FINANCEBOT_DATA_ENCRYPTION_ROTATION_BACKUP_CONFIRMED=true"
            );
        }
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalStateException("O lote de rotação deve estar entre 1 e 1000");
        }
    }

    @Scheduled(fixedDelayString = "${app.data-encryption.rotation.interval-ms:1000}")
    void rotateBatch() {
        if (completed.get() || failed.get()) {
            return;
        }

        int rotated;
        try {
            rotated = rotationService.rotateNextBatch(batchSize);
        } catch (RuntimeException exception) {
            failureType = exception.getClass().getSimpleName();
            failed.set(true);
            LOGGER.error(
                    "Rotação de dados interrompida; nenhum novo lote será iniciado; tipo={}",
                    exception.getClass().getSimpleName()
            );
            return;
        }
        long total = rotatedRecords.addAndGet(rotated);
        if (rotated == 0 && completed.compareAndSet(false, true)) {
            LOGGER.info("Rotação de dados concluída; registros processados={}", total);
        } else if (rotated > 0) {
            LOGGER.info("Lote de rotação concluído; registros processados no lote={}", rotated);
        }
    }

    @Override
    public Health health() {
        if (failed.get()) {
            return Health.down()
                    .withDetail("state", "failed")
                    .withDetail("errorType", failureType)
                    .withDetail("rotatedRecords", rotatedRecords.get())
                    .build();
        }
        return Health.up()
                .withDetail("state", completed.get() ? "completed" : "running")
                .withDetail("rotatedRecords", rotatedRecords.get())
                .build();
    }
}
