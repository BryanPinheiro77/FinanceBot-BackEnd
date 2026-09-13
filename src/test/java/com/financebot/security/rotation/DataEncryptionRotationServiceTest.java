package com.financebot.security.rotation;

import com.financebot.security.crypto.AesGcmFieldEncryption;
import com.financebot.security.crypto.EncryptionKeyHolder;
import com.financebot.security.crypto.FieldEncryptionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataEncryptionRotationServiceTest {

    private static final byte[] OLD_KEY_BYTES = "abcdef0123456789abcdef0123456789".getBytes();
    private static final String NEW_KEY = encode("0123456789abcdef0123456789abcdef");
    private static final String OLD_KEY = Base64.getEncoder().encodeToString(OLD_KEY_BYTES);

    private final DataEncryptionRotationRepository repository = mock(DataEncryptionRotationRepository.class);
    private final DataEncryptionRotationService service = new DataEncryptionRotationService(repository);

    @BeforeEach
    void configureKeys() {
        EncryptionKeyHolder.configure("new", NEW_KEY, "old", "old=" + OLD_KEY);
    }

    @AfterEach
    void clearKeys() {
        EncryptionKeyHolder.configure("");
    }

    @Test
    void shouldRotateEncryptedAndLegacyValuesInSameBatch() {
        String oldEncrypted = new AesGcmFieldEncryption(OLD_KEY_BYTES).encrypt("3200.00");
        when(repository.findNextBatch("v2.new.", 100)).thenReturn(List.of(
                new EncryptedFieldRotationCandidate(1L, oldEncrypted, null),
                new EncryptedFieldRotationCandidate(2L, null, new BigDecimal("4100.00"))
        ));
        when(repository.replaceEncryptedValue(eq(1L), eq(oldEncrypted), anyString())).thenReturn(1);
        when(repository.migrateLegacyValue(eq(2L), anyString())).thenReturn(1);

        int rotated = service.rotateNextBatch(100);

        assertThat(rotated).isEqualTo(2);
        ArgumentCaptor<String> encryptedCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> legacyCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).replaceEncryptedValue(eq(1L), eq(oldEncrypted), encryptedCaptor.capture());
        verify(repository).migrateLegacyValue(eq(2L), legacyCaptor.capture());
        assertThat(encryptedCaptor.getValue()).startsWith("v2.new.");
        assertThat(EncryptionKeyHolder.get().decrypt(encryptedCaptor.getValue())).isEqualTo("3200.00");
        assertThat(legacyCaptor.getValue()).startsWith("v2.new.");
        assertThat(EncryptionKeyHolder.get().decrypt(legacyCaptor.getValue())).isEqualTo("4100.00");
    }

    @Test
    void shouldReturnZeroWhenRotationIsComplete() {
        when(repository.findNextBatch("v2.new.", 100)).thenReturn(List.of());

        assertThat(service.rotateNextBatch(100)).isZero();
    }

    @Test
    void shouldStopOnConcurrentModification() {
        String oldEncrypted = new AesGcmFieldEncryption(OLD_KEY_BYTES).encrypt("3200.00");
        when(repository.findNextBatch("v2.new.", 100)).thenReturn(List.of(
                new EncryptedFieldRotationCandidate(1L, oldEncrypted, null)
        ));
        when(repository.replaceEncryptedValue(eq(1L), eq(oldEncrypted), anyString())).thenReturn(0);

        assertThatThrownBy(() -> service.rotateNextBatch(100))
                .isInstanceOf(FieldEncryptionException.class)
                .hasMessage("A rotação foi interrompida por alteração concorrente");
    }

    @Test
    void shouldRejectUnsafeBatchSize() {
        assertThatThrownBy(() -> service.rotateNextBatch(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.rotateNextBatch(1001))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String encode(String key) {
        return Base64.getEncoder().encodeToString(key.getBytes());
    }
}
