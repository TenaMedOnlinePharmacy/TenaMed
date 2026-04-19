CREATE TABLE IF NOT EXISTS order_metrics (
    order_id UUID PRIMARY KEY,
    pharmacy_id UUID,
    customer_id UUID,
    status VARCHAR(60),
    payment_status VARCHAR(60),
    amount NUMERIC(19, 2),
    created_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    rejected_at TIMESTAMP,
    paid_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    correlation_id VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_order_metrics_pharmacy_id ON order_metrics (pharmacy_id);
CREATE INDEX IF NOT EXISTS idx_order_metrics_customer_id ON order_metrics (customer_id);
CREATE INDEX IF NOT EXISTS idx_order_metrics_status ON order_metrics (status);
CREATE INDEX IF NOT EXISTS idx_order_metrics_payment_status ON order_metrics (payment_status);
CREATE INDEX IF NOT EXISTS idx_order_metrics_paid_at ON order_metrics (paid_at);

CREATE TABLE IF NOT EXISTS prescription_metrics (
    prescription_id UUID PRIMARY KEY,
    status VARCHAR(60),
    verified_at TIMESTAMP,
    rejected_at TIMESTAMP,
    verified_by UUID,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    correlation_id VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_prescription_metrics_status ON prescription_metrics (status);
CREATE INDEX IF NOT EXISTS idx_prescription_metrics_verified_at ON prescription_metrics (verified_at);
CREATE INDEX IF NOT EXISTS idx_prescription_metrics_rejected_at ON prescription_metrics (rejected_at);

CREATE TABLE IF NOT EXISTS actor_activity_metrics (
    id UUID PRIMARY KEY,
    actor_type VARCHAR(60) NOT NULL,
    actor_id UUID NOT NULL,
    activity_date DATE NOT NULL,
    total_events BIGINT NOT NULL,
    order_events BIGINT NOT NULL,
    prescription_events BIGINT NOT NULL,
    payment_events BIGINT NOT NULL,
    verification_events BIGINT NOT NULL,
    last_event_type VARCHAR(120),
    last_event_at TIMESTAMP,
    last_event_correlation_id VARCHAR(100),
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_actor_activity_day UNIQUE (actor_type, actor_id, activity_date)
);

CREATE INDEX IF NOT EXISTS idx_actor_activity_date ON actor_activity_metrics (activity_date);
CREATE INDEX IF NOT EXISTS idx_actor_activity_actor ON actor_activity_metrics (actor_type, actor_id);
