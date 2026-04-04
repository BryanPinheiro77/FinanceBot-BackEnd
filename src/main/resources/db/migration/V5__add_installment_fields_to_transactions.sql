ALTER TABLE transactions
ADD COLUMN is_installment BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE transactions
ADD COLUMN installment_number INT NULL;

ALTER TABLE transactions
ADD COLUMN total_installments INT NULL;

ALTER TABLE transactions
ADD COLUMN installment_group_id VARCHAR(100) NULL;

CREATE INDEX idx_transactions_installment_group
    ON transactions(installment_group_id);