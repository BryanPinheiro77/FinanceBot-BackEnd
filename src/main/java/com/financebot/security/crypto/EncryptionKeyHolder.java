package com.financebot.security.crypto;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EncryptionKeyHolder {

    private static volatile RotatingFieldEncryption encryption;

    private EncryptionKeyHolder() {
    }

    public static void configure(String base64Key) {
        configure("primary", base64Key, "primary", "");
    }

    public static void configure(
            String activeKeyId,
            String base64ActiveKey,
            String legacyKeyId,
            String previousKeys
    ) {
        if (base64ActiveKey == null || base64ActiveKey.isBlank()) {
            encryption = null;
            return;
        }

        try {
            encryption = new RotatingFieldEncryption(
                    activeKeyId,
                    decodeKey(base64ActiveKey),
                    legacyKeyId,
                    parsePreviousKeys(previousKeys)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Configuração de criptografia de dados inválida", exception);
        }
    }

    public static RotatingFieldEncryption get() {
        RotatingFieldEncryption configured = encryption;
        if (configured == null) {
            throw new FieldEncryptionException("Chave de criptografia de dados não configurada");
        }
        return configured;
    }

    private static Map<String, byte[]> parsePreviousKeys(String previousKeys) {
        Map<String, byte[]> parsed = new LinkedHashMap<>();
        if (previousKeys == null || previousKeys.isBlank()) {
            return parsed;
        }

        for (String entry : previousKeys.split(",")) {
            String[] parts = entry.trim().split("=", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("FINANCEBOT_DATA_ENCRYPTION_PREVIOUS_KEYS possui entrada inválida");
            }
            if (parsed.putIfAbsent(parts[0].trim(), decodeKey(parts[1].trim())) != null) {
                throw new IllegalArgumentException("FINANCEBOT_DATA_ENCRYPTION_PREVIOUS_KEYS possui ID duplicado");
            }
        }
        return parsed;
    }

    private static byte[] decodeKey(String base64Key) {
        return Base64.getDecoder().decode(base64Key);
    }
}
