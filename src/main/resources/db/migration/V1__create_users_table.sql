CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    telegram_id BIGINT UNIQUE,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email);