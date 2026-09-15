ALTER TABLE users ADD COLUMN financial_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN weekly_summary_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN monthly_summary_enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE financial_notifications (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    telegram_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    next_attempt_at TIMESTAMP NOT NULL,
    claimed_at TIMESTAMP,
    delivery_token VARCHAR(36),
    attempts INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_financial_notifications_pending ON financial_notifications(status, next_attempt_at);
CREATE INDEX idx_financial_notifications_user ON financial_notifications(user_id);
