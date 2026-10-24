package com.financebot.security.crypto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionKeyHolderTest {

    private static final String ACTIVE_KEY = encode("0123456789abcdef0123456789abcdef");
    private static final String PREVIOUS_KEY = encode("abcdef0123456789abcdef0123456789");

    private RotatingFieldEncryption previousEncryption;

    @BeforeEach
    void preserveHolder() {
        previousEncryption = (RotatingFieldEncryption) ReflectionTestUtils.getField(
                EncryptionKeyHolder.class, "encryption");
    }

    @AfterEach
    void restoreHolder() {
        // Contextos Spring em cache continuam usando o chaveiro anterior ao teste.
        ReflectionTestUtils.setField(EncryptionKeyHolder.class, "encryption", previousEncryption);
    }

    @Test
    void shouldConfigureActiveAndPreviousKeys() {
        EncryptionKeyHolder.configure(
                "current",
                ACTIVE_KEY,
                "previous",
                "previous=" + PREVIOUS_KEY
        );

        String encrypted = EncryptionKeyHolder.get().encrypt("3500.00");

        assertThat(encrypted).startsWith("v2.current.");
        assertThat(EncryptionKeyHolder.get().decrypt(encrypted)).isEqualTo("3500.00");
    }

    @Test
    void shouldRejectMalformedPreviousKeyEntry() {
        assertThatThrownBy(() -> EncryptionKeyHolder.configure(
                "current",
                ACTIVE_KEY,
                "current",
                "invalid-entry"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("Configuração de criptografia de dados inválida");
    }

    @Test
    void shouldFailClosedWhenKeyIsMissing() {
        EncryptionKeyHolder.configure("");

        assertThatThrownBy(EncryptionKeyHolder::get)
                .isInstanceOf(FieldEncryptionException.class)
                .hasMessage("Chave de criptografia de dados não configurada");
    }

    private static String encode(String key) {
        return Base64.getEncoder().encodeToString(key.getBytes());
    }
}
