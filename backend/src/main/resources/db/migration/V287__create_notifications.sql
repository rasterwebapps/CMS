-- BR-53: In-app notification feed (first shipped slice of BR-28's notification-sending backend).
-- A notification is a broadcast-style alert (not tied to one specific user); visibility to a
-- given user is computed at read time from their role/permissions and their own preferences
-- (see NotificationPreferenceService). Per-user dismissal is tracked separately below so the
-- same alert can be independently dismissed by each admin who sees it.
CREATE TABLE notifications (
    id           BIGSERIAL PRIMARY KEY,
    category_key VARCHAR(50)  NOT NULL,
    title        VARCHAR(200) NOT NULL,
    message      TEXT         NOT NULL,
    link         VARCHAR(255),
    source_type  VARCHAR(50),
    source_id    BIGINT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at  TIMESTAMPTZ
);

-- Idempotency for source-driven alerts (e.g. the term-overdue job): only one active
-- (unresolved) notification per underlying source+category at a time.
CREATE UNIQUE INDEX uq_notifications_active_source
    ON notifications (source_type, source_id, category_key) WHERE resolved_at IS NULL;

CREATE INDEX idx_notifications_category_unresolved
    ON notifications (category_key) WHERE resolved_at IS NULL;

CREATE TABLE notification_dismissals (
    id              BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notifications(id),
    user_id         VARCHAR(255) NOT NULL,
    dismissed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_dismissal UNIQUE (notification_id, user_id)
);

CREATE INDEX idx_notification_dismissals_user ON notification_dismissals (user_id);
