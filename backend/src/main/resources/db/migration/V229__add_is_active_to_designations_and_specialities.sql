ALTER TABLE designations
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE specialities
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_designations_is_active ON designations (is_active);
CREATE INDEX IF NOT EXISTS idx_specialities_is_active ON specialities (is_active);

