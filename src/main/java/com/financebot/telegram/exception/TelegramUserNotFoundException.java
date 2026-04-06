package com.financebot.telegram.exception;

public class TelegramUserNotFoundException extends RuntimeException {

    public TelegramUserNotFoundException() {
        super("Usuário não encontrado para este Telegram.");
    }
}