CREATE TABLE shared_services (
    id BIGINT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    category VARCHAR(24) NOT NULL,
    account_name VARCHAR(200),
    secret_encrypted TEXT,
    seat_total INTEGER NOT NULL DEFAULT 1,
    monthly_price_cents INTEGER NOT NULL DEFAULT 0,
    renew_at TIMESTAMPTZ,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_service_category CHECK (category IN ('STREAMING','PROXY','SUBSCRIPTION','AI','OTHER')),
    CONSTRAINT ck_service_status CHECK (status IN ('ACTIVE','PAUSED','ARCHIVED')),
    CONSTRAINT ck_service_seats CHECK (seat_total > 0)
);

CREATE TABLE memberships (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    service_id BIGINT NOT NULL REFERENCES shared_services(id),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    price_cents INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    access_revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_membership_status CHECK (status IN ('PENDING','ACTIVE','EXPIRING','EXPIRED','CANCELLED','REFUNDED'))
);
CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_memberships_service ON memberships(service_id);

CREATE TABLE proxy_services (
    service_id BIGINT PRIMARY KEY REFERENCES shared_services(id) ON DELETE CASCADE,
    panel_url_encrypted TEXT,
    probe_url VARCHAR(1000),
    node_total INTEGER NOT NULL DEFAULT 0,
    traffic_limit_gb INTEGER,
    device_limit INTEGER
);

CREATE TABLE proxy_subscriptions (
    membership_id BIGINT PRIMARY KEY REFERENCES memberships(id) ON DELETE CASCADE,
    subscription_url_encrypted TEXT NOT NULL,
    traffic_used_bytes BIGINT NOT NULL DEFAULT 0,
    access_revoked_at TIMESTAMPTZ,
    last_synced_at TIMESTAMPTZ
);
