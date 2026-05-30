-- BR-28: User notification preferences (per user, per category)
CREATE TABLE user_notification_preferences (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    category_key VARCHAR(50) NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    channel     VARCHAR(20)  NOT NULL DEFAULT 'IN_APP',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_notification_pref UNIQUE (user_id, category_key)
);

CREATE INDEX idx_unp_user_id ON user_notification_preferences (user_id);
