package com.financebot.transaction.idempotency;

public class IdempotencyUnavailableException extends RuntimeException {
    public IdempotencyUnavailableException(Throwable cause) {
        super("Não foi possível concluir a operação idempotente. Tente novamente com a mesma chave.", cause);
    }
}
