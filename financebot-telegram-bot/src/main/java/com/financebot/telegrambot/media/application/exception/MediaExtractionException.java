package com.financebot.telegrambot.media.application.exception;

public class MediaExtractionException extends RuntimeException {

    public MediaExtractionException(String message) {
        super(message);
    }

    public MediaExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
