-- ============================================================
-- AUTH SERVICE — полная схема БД
-- V1: начальное состояние на момент внедрения Flyway
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    last_login_at TIMESTAMP,
    last_logout_at TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    lock_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id),
    role_id BIGINT NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    token VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);

INSERT INTO roles (name, description) VALUES 
('ADMIN', 'Администратор системы'),
('USER', 'Обычный пользователь');

INSERT INTO users (username, password_hash, first_name, last_name, enabled, failed_attempts, account_locked) VALUES 
('admin', '$2a$12$3DPI.S0ayaszKgSWc/lwDeFS7/eLoKpL6rFs1DuMmbbCs.nF5Ka6W', 'Админ', 'Админов', true, 0, false);

INSERT INTO user_roles (user_id, role_id) VALUES 
(1, 1), 
(1, 2);