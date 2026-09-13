package com.financebot.security.crypto;

public class FieldEncryptionException extends RuntimeException {

    public FieldEncryptionException(String message) {
        super(message);
    }

    public FieldEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
