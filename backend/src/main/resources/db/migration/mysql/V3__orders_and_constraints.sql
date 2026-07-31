ALTER TABLE memberships
    ADD COLUMN active_key TINYINT
        GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING', 'ACTIVE', 'EXPIRING') THEN 1 ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_memberships_active_user_service (user_id, service_id, active_key);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    membership_id BIGINT NULL,
    type VARCHAR(16) NOT NULL,
    amount_cents INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PAID',
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    note VARCHAR(500),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_order_service FOREIGN KEY (service_id) REFERENCES shared_services(id),
    CONSTRAINT fk_order_membership FOREIGN KEY (membership_id) REFERENCES memberships(id),
    CONSTRAINT fk_order_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_order_type CHECK (type IN ('NEW', 'RENEWAL', 'REFUND')),
    CONSTRAINT ck_order_status CHECK (status IN ('PENDING', 'PAID', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT ck_order_amount CHECK (amount_cents >= 0),
    CONSTRAINT ck_order_period CHECK (period_end > period_start),
    INDEX idx_orders_user (user_id, created_at),
    INDEX idx_orders_service (service_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
