package com.financebot.transaction.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TransactionIdempotencyStore {
    private final JdbcTemplate jdbc;

    public boolean claim(Long userId, String operation, String keyHash, String requestHash, long ttlSeconds) {
        return !jdbc.queryForList("""
                INSERT INTO transaction_idempotency_requests AS existing
                    (user_id, operation, key_hash, request_hash, expires_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP + (? * INTERVAL '1 second'))
                ON CONFLICT (user_id, operation, key_hash) DO UPDATE
                SET request_hash = EXCLUDED.request_hash, response_encrypted = NULL,
                    created_at = EXCLUDED.created_at, expires_at = EXCLUDED.expires_at
                WHERE existing.expires_at <= CURRENT_TIMESTAMP
                RETURNING user_id
                """, Long.class, userId, operation, keyHash, requestHash, ttlSeconds).isEmpty();
    }

    public StoredResponse find(Long userId, String operation, String keyHash) {
        // O UPSERT mantém a trava até o fim da transação, inclusive quando o WHERE impede a atualização.
        return jdbc.queryForObject("""
                SELECT request_hash, response_encrypted FROM transaction_idempotency_requests
                WHERE user_id = ? AND operation = ? AND key_hash = ?
                """, (rs, row) -> new StoredResponse(rs.getString("request_hash"), rs.getString("response_encrypted")),
                userId, operation, keyHash);
    }

    public void complete(Long userId, String operation, String keyHash, String responseEncrypted) {
        int updated = jdbc.update("""
                UPDATE transaction_idempotency_requests SET response_encrypted = ?
                WHERE user_id = ? AND operation = ? AND key_hash = ?
                """, responseEncrypted, userId, operation, keyHash);
        if (updated != 1) throw new IllegalStateException("Reserva de idempotência ausente");
    }

    public int deleteExpired(int batchSize) {
        return jdbc.update("""
                DELETE FROM transaction_idempotency_requests
                WHERE (user_id, operation, key_hash) IN (
                    SELECT user_id, operation, key_hash FROM transaction_idempotency_requests
                    WHERE expires_at <= CURRENT_TIMESTAMP ORDER BY expires_at
                    LIMIT ? FOR UPDATE SKIP LOCKED
                )
                """, batchSize);
    }

    public record StoredResponse(String requestHash, String encryptedResponse) { }
}
