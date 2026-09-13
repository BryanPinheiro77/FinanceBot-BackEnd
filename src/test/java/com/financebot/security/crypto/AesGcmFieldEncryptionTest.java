package com.financebot.security.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmFieldEncryptionTest {

    private static final byte[] KEY = Base64.getDecoder().decode(
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    );

    private final AesGcmFieldEncryption encryption = new AesGcmFieldEncryption(KEY);

    @Test
    void shouldEncryptAndDecryptValue() {
        String encrypted = encryption.encrypt("Renda mensal confidencial");

        assertThat(encrypted).startsWith("v1.");
        assertThat(encrypted).doesNotContain("Renda mensal confidencial");
        assertThat(encryption.decrypt(encrypted)).isEqualTo("Renda mensal confidencial");
    }

    @Test
    void shouldGenerateDifferentCiphertextForSameValue() {
        String first = encryption.encrypt("3500.00");
        String second = encryption.encrypt("3500.00");

        assertThat(first).isNotEqualTo(second);
        assertThat(encryption.decrypt(first)).isEqualTo("3500.00");
        assertThat(encryption.decrypt(second)).isEqualTo("3500.00");
    }

    @Test
    void shouldRejectTamperedCiphertext() {
        String encrypted = encryption.encrypt("3500.00");
        byte[] payload = Base64.getUrlDecoder().decode(encrypted.substring(3));
        payload[payload.length - 1] ^= 1;
        String tampered = "v1." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);

        assertThatThrownBy(() -> encryption.decrypt(tampered))
                .isInstanceOf(FieldEncryptionException.class)
                .hasMessage("Autenticação do valor criptografado falhou");
    }

    @Test
    void shouldAuthenticateAssociatedData() {
        byte[] context = "users.monthly_base_income".getBytes(StandardCharsets.UTF_8);
        String encrypted = encryption.encrypt("3500.00", context);

        assertThat(encryption.decrypt(encrypted, context)).isEqualTo("3500.00");
        assertThatThrownBy(() -> encryption.decrypt(encrypted, "other.field".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(FieldEncryptionException.class);
    }

    @Test
    void shouldAllowNullValues() {
        assertThat(encryption.encrypt(null)).isNull();
        assertThat(encryption.decrypt(null)).isNull();
    }

    @Test
    void shouldRejectInvalidKeyLength() {
        assertThatThrownBy(() -> new AesGcmFieldEncryption(new byte[16]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A chave de criptografia deve ter 32 bytes");
    }
}
