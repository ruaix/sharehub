CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT REFERENCES users(id),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN','MEMBER')),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING','ACTIVE','REJECTED','DISABLED'))
);
CREATE INDEX idx_users_status ON users(status);

CREATE TABLE system_settings (
    setting_key VARCHAR(80) PRIMARY KEY,
    setting_value VARCHAR(500) NOT NULL,
    updated_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO system_settings(setting_key,setting_value) VALUES ('registration_enabled','false');

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id),
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(40),
    target_id BIGINT,
    detail_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
