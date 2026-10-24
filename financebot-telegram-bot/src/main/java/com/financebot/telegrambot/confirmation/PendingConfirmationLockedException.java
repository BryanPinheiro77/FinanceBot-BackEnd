package com.financebot.telegrambot.confirmation;

public class PendingConfirmationLockedException extends RuntimeException {
    public static final String MESSAGE = "Já tentei salvar a operação anterior. Confirme novamente para resolver o resultado antes de editar ou criar outra operação. Se preferir cancelar, confira seus lançamentos antes de registrar novamente.";
}
