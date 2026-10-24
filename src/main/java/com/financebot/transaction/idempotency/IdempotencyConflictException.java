package com.financebot.transaction.idempotency;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("Esta chave de idempotência já foi usada com outro conteúdo.");
    }
}
