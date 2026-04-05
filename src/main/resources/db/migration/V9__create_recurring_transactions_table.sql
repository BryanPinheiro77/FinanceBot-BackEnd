CREATE TABLE recurring_transactions (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    next_execution_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_executed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_recurring_transaction_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_recurring_transaction_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_recurring_transaction_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_recurring_transactions_user_id
    ON recurring_transactions(user_id);

CREATE INDEX idx_recurring_transactions_next_execution_date
    ON recurring_transactions(next_execution_date);

CREATE INDEX idx_recurring_transactions_active
    ON recurring_transactions(active);