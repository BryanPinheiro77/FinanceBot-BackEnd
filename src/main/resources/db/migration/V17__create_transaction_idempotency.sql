CREATE TABLE transaction_idempotency_requests (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation VARCHAR(40) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_encrypted TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, operation, key_hash),
    CONSTRAINT ck_transaction_idempotency_hashes
        CHECK (length(key_hash) = 64 AND length(request_hash) = 64),
    CONSTRAINT ck_transaction_idempotency_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_transaction_idempotency_expiry ON transaction_idempotency_requests(expires_at);
