-- ============================================================
-- V90: Audit log table — records who changed what, and when.
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id           BIGSERIAL PRIMARY KEY,
    actor        VARCHAR(100)  NOT NULL,
    action       VARCHAR(50)   NOT NULL,
    entity_type  VARCHAR(100),
    entity_id    VARCHAR(100),
    detail       TEXT,
    occurred_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_actor       ON audit_log (actor);
CREATE INDEX idx_audit_log_entity      ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at DESC);

