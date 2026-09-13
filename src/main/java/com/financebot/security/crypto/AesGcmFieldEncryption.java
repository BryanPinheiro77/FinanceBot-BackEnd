package com.financebot.security.crypto;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia autenticada para valores de campos que não precisam ser
 * pesquisados diretamente pelo banco.
 */
public final class AesGcmFieldEncryption {

    private static final String VERSION = "v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom;

    public AesGcmFieldEncryption(byte[] keyBytes) {
        this(keyBytes, new SecureRandom());
    }

    AesGcmFieldEncryption(byte[] keyBytes, SecureRandom secureRandom) {
        if (keyBytes == null || keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException("A chave de criptografia deve ter 32 bytes");
        }
        if (secureRandom == null) {
            throw new IllegalArgumentException("O gerador seguro de números aleatórios é obrigatório");
        }

        this.key = new SecretKeySpec(keyBytes.clone(), "AES");
        this.secureRandom = secureRandom;
    }

    public String encrypt(String plaintext) {
        return encrypt(plaintext, null);
    }

    public String encrypt(String plaintext, byte[] associatedData) {
        if (plaintext == null) {
            return null;
        }

        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        secureRandom.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            if (associatedData != null) {
                cipher.updateAAD(associatedData);
            }
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(ciphertext, 0, payload, nonce.length, ciphertext.length);
            return VERSION + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new FieldEncryptionException("Não foi possível criptografar o campo", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        return decrypt(encryptedValue, null);
    }

    public String decrypt(String encryptedValue, byte[] associatedData) {
        if (encryptedValue == null) {
            return null;
        }

        String[] parts = encryptedValue.split("\\.", 2);
        if (parts.length != 2 || !VERSION.equals(parts[0])) {
            throw new FieldEncryptionException("Versão de criptografia inválida");
        }

        byte[] payload;
        try {
            payload = Base64.getUrlDecoder().decode(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new FieldEncryptionException("Valor criptografado inválido", exception);
        }

        if (payload.length <= NONCE_LENGTH_BYTES) {
            throw new FieldEncryptionException("Valor criptografado incompleto");
        }

        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        byte[] ciphertext = new byte[payload.length - NONCE_LENGTH_BYTES];
        System.arraycopy(payload, 0, nonce, 0, nonce.length);
        System.arraycopy(payload, nonce.length, ciphertext, 0, ciphertext.length);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            if (associatedData != null) {
                cipher.updateAAD(associatedData);
            }
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (AEADBadTagException exception) {
            throw new FieldEncryptionException("Autenticação do valor criptografado falhou", exception);
        } catch (GeneralSecurityException exception) {
            throw new FieldEncryptionException("Não foi possível descriptografar o campo", exception);
        }
    }
}
