package com.financebot.security.crypto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class RotatingFieldEncryption {

    private static final String CURRENT_FORMAT = "v2";
    private static final String LEGACY_FORMAT = "v1";
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final String activeKeyId;
    private final String legacyKeyId;
    private final Map<String, AesGcmFieldEncryption> keys;

    public RotatingFieldEncryption(
            String activeKeyId,
            byte[] activeKey,
            String legacyKeyId,
            Map<String, byte[]> previousKeys
    ) {
        this.activeKeyId = validateKeyId(activeKeyId, "ID da chave ativa");
        this.legacyKeyId = legacyKeyId == null || legacyKeyId.isBlank()
                ? null
                : validateKeyId(legacyKeyId, "ID da chave legada");

        Map<String, AesGcmFieldEncryption> configuredKeys = new LinkedHashMap<>();
        configuredKeys.put(this.activeKeyId, new AesGcmFieldEncryption(activeKey));
        if (previousKeys != null) {
            previousKeys.forEach((keyId, keyBytes) -> {
                String validatedKeyId = validateKeyId(keyId, "ID de chave anterior");
                if (configuredKeys.putIfAbsent(validatedKeyId, new AesGcmFieldEncryption(keyBytes)) != null) {
                    throw new IllegalArgumentException("ID de chave duplicado: " + validatedKeyId);
                }
            });
        }

        if (this.legacyKeyId != null && !configuredKeys.containsKey(this.legacyKeyId)) {
            throw new IllegalArgumentException("A chave legada deve estar presente no keyring");
        }
        this.keys = Map.copyOf(configuredKeys);
    }

    public String encrypt(String plaintext) {
        return encrypt(plaintext, null);
    }

    public String encrypt(String plaintext, byte[] associatedData) {
        if (plaintext == null) {
            return null;
        }
        return CURRENT_FORMAT + "." + activeKeyId + "."
                + keys.get(activeKeyId).encryptPayload(plaintext, associatedData);
    }

    public String decrypt(String encryptedValue) {
        return decrypt(encryptedValue, null);
    }

    public String decrypt(String encryptedValue, byte[] associatedData) {
        if (encryptedValue == null) {
            return null;
        }

        String[] parts = encryptedValue.split("\\.", 3);
        if (parts.length == 2 && LEGACY_FORMAT.equals(parts[0])) {
            if (legacyKeyId == null) {
                throw new FieldEncryptionException("Chave legada não configurada");
            }
            return keys.get(legacyKeyId).decrypt(encryptedValue, associatedData);
        }
        if (parts.length != 3 || !CURRENT_FORMAT.equals(parts[0])) {
            throw new FieldEncryptionException("Versão de criptografia inválida");
        }

        AesGcmFieldEncryption encryption = keys.get(parts[1]);
        if (encryption == null) {
            throw new FieldEncryptionException("Chave de criptografia não configurada para o valor");
        }
        return encryption.decryptPayload(parts[2], associatedData);
    }

    public boolean needsRotation(String encryptedValue) {
        return encryptedValue != null && !encryptedValue.startsWith(activePrefix());
    }

    public String activePrefix() {
        return CURRENT_FORMAT + "." + activeKeyId + ".";
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    private static String validateKeyId(String keyId, String label) {
        String value = Objects.requireNonNull(keyId, label + " é obrigatório").trim();
        if (!KEY_ID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " deve conter apenas letras, números, '_' ou '-'");
        }
        return value;
    }
}
