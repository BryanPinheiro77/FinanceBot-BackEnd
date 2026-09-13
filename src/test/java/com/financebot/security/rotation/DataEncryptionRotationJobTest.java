package com.financebot.security.rotation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class DataEncryptionRotationJobTest {

    private final DataEncryptionRotationService service = mock(DataEncryptionRotationService.class);

    @Test
    void shouldRequireVerifiedBackup() {
        DataEncryptionRotationJob job = new DataEncryptionRotationJob(service, false, 100);

        assertThatThrownBy(job::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BACKUP_CONFIRMED=true");
    }

    @Test
    void shouldStopPollingAfterCompletingRotation() {
        DataEncryptionRotationJob job = new DataEncryptionRotationJob(service, true, 100);
        when(service.rotateNextBatch(100)).thenReturn(2, 0);

        job.validateConfiguration();
        job.rotateBatch();
        job.rotateBatch();
        job.rotateBatch();

        verify(service, times(2)).rotateNextBatch(100);
    }

    @Test
    void shouldRejectUnsafeBatchSize() {
        DataEncryptionRotationJob job = new DataEncryptionRotationJob(service, true, 1001);

        assertThatThrownBy(job::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("O lote de rotação deve estar entre 1 e 1000");
    }

    @Test
    void shouldStopSchedulingNewBatchesAfterFailure() {
        DataEncryptionRotationJob job = new DataEncryptionRotationJob(service, true, 100);
        doThrow(new IllegalStateException("falha simulada"))
                .when(service).rotateNextBatch(100);

        job.validateConfiguration();
        job.rotateBatch();
        job.rotateBatch();

        verify(service, times(1)).rotateNextBatch(100);
        org.assertj.core.api.Assertions.assertThat(job.health().getStatus()).isEqualTo(Status.DOWN);
    }
}
