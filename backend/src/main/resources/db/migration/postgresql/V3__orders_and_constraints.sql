CREATE UNIQUE INDEX uk_memberships_active_user_service
    ON memberships(user_id, service_id)
    WHERE status IN ('PENDING', 'ACTIVE', 'EXPIRING');

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    service_id BIGINT NOT NULL REFERENCES shared_services(id),
    membership_id BIGINT REFERENCES memberships(id),
    type VARCHAR(16) NOT NULL,
    amount_cents INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PAID',
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    note VARCHAR(500),
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_order_type CHECK (type IN ('NEW', 'RENEWAL', 'REFUND')),
    CONSTRAINT ck_order_status CHECK (status IN ('PENDING', 'PAID', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT ck_order_amount CHECK (amount_cents >= 0),
    CONSTRAINT ck_order_period CHECK (period_end > period_start)
);
CREATE INDEX idx_orders_user ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_service ON orders(service_id, created_at DESC);
