package com.financebot.security.crypto;

import java.util.Base64;

public final class EncryptionKeyHolder {

    private static volatile AesGcmFieldEncryption encryption;

    private EncryptionKeyHolder() {
    }

    public static void configure(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            encryption = null;
            return;
        }

        try {
            encryption = new AesGcmFieldEncryption(Base64.getDecoder().decode(base64Key));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("FINANCEBOT_DATA_ENCRYPTION_KEY deve ser Base64 de 32 bytes", exception);
        }
    }

    public static AesGcmFieldEncryption get() {
        AesGcmFieldEncryption configured = encryption;
        if (configured == null) {
            throw new FieldEncryptionException("Chave de criptografia de dados não configurada");
        }
        return configured;
    }
}
