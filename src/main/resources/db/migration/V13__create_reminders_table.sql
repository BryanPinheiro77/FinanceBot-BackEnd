CREATE TABLE reminders (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    reminder_date DATE NOT NULL,
    days_before INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    recurring_transaction_id BIGINT,
    CONSTRAINT fk_reminder_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_reminder_recurring_transaction FOREIGN KEY (recurring_transaction_id)
        REFERENCES recurring_transactions(id)
);

CREATE INDEX idx_reminders_user_id ON reminders(user_id);
CREATE INDEX idx_reminders_due ON reminders(active, sent_at, reminder_date);
