CREATE TABLE shared_services (
    id BIGINT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    category VARCHAR(24) NOT NULL,
    account_name VARCHAR(200),
    secret_encrypted TEXT,
    seat_total INT NOT NULL DEFAULT 1,
    monthly_price_cents INT NOT NULL DEFAULT 0,
    renew_at TIMESTAMP NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_service_category CHECK (category IN ('STREAMING','PROXY','SUBSCRIPTION','AI','OTHER')),
    CONSTRAINT ck_service_status CHECK (status IN ('ACTIVE','PAUSED','ARCHIVED')),
    CONSTRAINT ck_service_seats CHECK (seat_total > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE memberships (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    price_cents INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    access_revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_membership_service FOREIGN KEY (service_id) REFERENCES shared_services(id),
    CONSTRAINT ck_membership_status CHECK (status IN ('PENDING','ACTIVE','EXPIRING','EXPIRED','CANCELLED','REFUNDED')),
    INDEX idx_memberships_user (user_id),
    INDEX idx_memberships_service (service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE proxy_services (
    service_id BIGINT PRIMARY KEY,
    panel_url_encrypted TEXT,
    probe_url VARCHAR(1000),
    node_total INT NOT NULL DEFAULT 0,
    traffic_limit_gb INT,
    device_limit INT,
    CONSTRAINT fk_proxy_service FOREIGN KEY (service_id) REFERENCES shared_services(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE proxy_subscriptions (
    membership_id BIGINT PRIMARY KEY,
    subscription_url_encrypted TEXT NOT NULL,
    traffic_used_bytes BIGINT NOT NULL DEFAULT 0,
    access_revoked_at TIMESTAMP NULL,
    last_synced_at TIMESTAMP NULL,
    CONSTRAINT fk_proxy_membership FOREIGN KEY (membership_id) REFERENCES memberships(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
