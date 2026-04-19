CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    action VARCHAR(120) NOT NULL,
    action_details JSONB NOT NULL,
    changes JSONB NOT NULL,
    actor_type VARCHAR(60) NOT NULL,
    actor_id UUID,
    context_type VARCHAR(60) NOT NULL,
    context_id UUID,
    correlation_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs (actor_type, actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_context ON audit_logs (context_type, context_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation_id ON audit_logs (correlation_id);
