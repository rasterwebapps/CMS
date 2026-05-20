-- V149: Create India states and districts master tables.
--
-- These tables back the cascading State → District dropdowns used throughout
-- admission, enquiry, student and faculty forms. Admin users can add/edit entries.

CREATE TABLE india_states (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    code       VARCHAR(10)  NOT NULL UNIQUE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE india_districts (
    id         BIGSERIAL    PRIMARY KEY,
    state_id   BIGINT       NOT NULL REFERENCES india_states(id),
    name       VARCHAR(100) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (state_id, name)
);

CREATE INDEX idx_india_districts_state_id ON india_districts(state_id);
CREATE INDEX idx_india_states_is_active   ON india_states(is_active);
CREATE INDEX idx_india_districts_active   ON india_districts(is_active);

