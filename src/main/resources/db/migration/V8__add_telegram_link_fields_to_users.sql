ALTER TABLE users
ADD COLUMN telegram_link_code VARCHAR(30);

ALTER TABLE users
ADD COLUMN telegram_link_code_expires_at TIMESTAMP;