-- BR-30: Fee states master for multi-dimension fee structure
-- Represents fee-segment categories (not address states)
CREATE TABLE fee_states (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    is_fallback BOOLEAN      NOT NULL DEFAULT FALSE,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed: Tamil Nadu (default) and Other State (fallback)
INSERT INTO fee_states (name, code, is_default, is_fallback, sort_order)
VALUES
    ('Tamil Nadu',  'TAMIL_NADU',  TRUE,  FALSE, 1),
    ('Other State', 'OTHER_STATE', FALSE, TRUE,  2);
