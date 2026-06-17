-- V219: Staff Referrer master + Faculty commission amount
--
-- 1. Create staff_referrers table (sister-concern staff who refer students).
-- 2. Add referred_staff_id FK to enquiries; migrate existing referred_staff_name
--    text values into the new table (de-duplicated by name), then drop the
--    text column.
-- 3. Add commission_amount to the faculty table for per-faculty commission
--    overrides when the FACULTY referral type is used.

-- ── 1. staff_referrers table ─────────────────────────────────────────────────
CREATE TABLE staff_referrers (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(255)      NOT NULL,
    phone                VARCHAR(30),
    email                VARCHAR(255),
    institution          VARCHAR(255),
    commission_amount    NUMERIC(12, 2),
    is_active            BOOLEAN           NOT NULL DEFAULT TRUE,
    pan_number           VARCHAR(20),
    aadhaar_number       VARCHAR(20),
    bank_account_number  VARCHAR(40),
    bank_ifsc_code       VARCHAR(20),
    bank_branch          VARCHAR(150),
    bank_name            VARCHAR(150),
    bank_account_holder  VARCHAR(150),
    bank_account_type    VARCHAR(20),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_staff_referrers_name_ci ON staff_referrers (LOWER(name));

-- ── 2. referred_staff_id on enquiries ───────────────────────────────────────
ALTER TABLE enquiries
    ADD COLUMN IF NOT EXISTS referred_staff_id BIGINT REFERENCES staff_referrers(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_enquiries_referred_staff ON enquiries(referred_staff_id)
    WHERE referred_staff_id IS NOT NULL;

-- Migrate existing free-text names: create one StaffReferrer per distinct name.
INSERT INTO staff_referrers (name, created_at, updated_at)
SELECT DISTINCT TRIM(referred_staff_name), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM enquiries
WHERE referred_staff_name IS NOT NULL
  AND TRIM(referred_staff_name) <> '';

-- Point existing enquiries at the newly created StaffReferrer rows.
UPDATE enquiries e
SET referred_staff_id = sr.id
FROM staff_referrers sr
WHERE LOWER(TRIM(e.referred_staff_name)) = LOWER(sr.name)
  AND e.referred_staff_name IS NOT NULL;

-- Drop the now-migrated text column.
ALTER TABLE enquiries DROP COLUMN IF EXISTS referred_staff_name;

-- ── 3. Faculty commission amount ─────────────────────────────────────────────
ALTER TABLE faculty
    ADD COLUMN IF NOT EXISTS commission_amount NUMERIC(12, 2);
