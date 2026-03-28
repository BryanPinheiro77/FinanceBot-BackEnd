CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    initial_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_accounts_user_name
        UNIQUE (user_id, name)
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);