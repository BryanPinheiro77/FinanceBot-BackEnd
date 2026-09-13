package com.financebot.security.crypto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RotatingFieldEncryptionTest {

    private static final byte[] ACTIVE_KEY = "0123456789abcdef0123456789abcdef".getBytes();
    private static final byte[] PREVIOUS_KEY = "abcdef0123456789abcdef0123456789".getBytes();

    @Test
    void shouldEncryptWithActiveKeyAndReadCurrentValue() {
        RotatingFieldEncryption encryption = encryption();

        String encrypted = encryption.encrypt("3500.00");

        assertThat(encrypted).startsWith("v2.current.");
        assertThat(encryption.decrypt(encrypted)).isEqualTo("3500.00");
        assertThat(encryption.needsRotation(encrypted)).isFalse();
    }

    @Test
    void shouldReadPreviousKeyAndMarkValueForRotation() {
        AesGcmFieldEncryption previous = new AesGcmFieldEncryption(PREVIOUS_KEY);
        String encrypted = "v2.previous." + previous.encryptPayload("4200.00", null);

        RotatingFieldEncryption encryption = encryption();

        assertThat(encryption.decrypt(encrypted)).isEqualTo("4200.00");
        assertThat(encryption.needsRotation(encrypted)).isTrue();
    }

    @Test
    void shouldReadLegacyV1ValueWithConfiguredLegacyKey() {
        String legacy = new AesGcmFieldEncryption(PREVIOUS_KEY).encrypt("2800.00");

        RotatingFieldEncryption encryption = encryption();

        assertThat(encryption.decrypt(legacy)).isEqualTo("2800.00");
        assertThat(encryption.needsRotation(legacy)).isTrue();
    }

    @Test
    void shouldRejectValueWhoseKeyIsNotConfigured() {
        String unknown = "v2.revoked." + new AesGcmFieldEncryption(PREVIOUS_KEY)
                .encryptPayload("100.00", null);

        assertThatThrownBy(() -> encryption().decrypt(unknown))
                .isInstanceOf(FieldEncryptionException.class)
                .hasMessage("Chave de criptografia não configurada para o valor");
    }

    @Test
    void shouldRejectInvalidKeyIdentifier() {
        assertThatThrownBy(() -> new RotatingFieldEncryption(
                "invalid.key",
                ACTIVE_KEY,
                "invalid.key",
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID da chave ativa");
    }

    @Test
    void shouldRequireLegacyKeyInKeyring() {
        assertThatThrownBy(() -> new RotatingFieldEncryption(
                "current",
                ACTIVE_KEY,
                "previous",
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A chave legada deve estar presente no keyring");
    }

    @Test
    void shouldAllowLegacyKeyRevocationAfterRotation() {
        RotatingFieldEncryption encryption = new RotatingFieldEncryption(
                "current",
                ACTIVE_KEY,
                "",
                Map.of()
        );
        String legacy = new AesGcmFieldEncryption(PREVIOUS_KEY).encrypt("2800.00");

        assertThatThrownBy(() -> encryption.decrypt(legacy))
                .isInstanceOf(FieldEncryptionException.class)
                .hasMessage("Chave legada não configurada");
    }

    private RotatingFieldEncryption encryption() {
        return new RotatingFieldEncryption(
                "current",
                ACTIVE_KEY,
                "previous",
                Map.of("previous", PREVIOUS_KEY)
        );
    }
}
