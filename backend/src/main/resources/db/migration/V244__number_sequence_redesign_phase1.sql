-- =============================================================================
-- V244: Number Sequence Redesign — Phase 1 (Schema only)
-- =============================================================================
-- Creates two new tables to replace the single application_number_sequences table:
--   number_series_definitions  — one row per series type (what you configure)
--   number_sequence_counters   — one row per (series, scope period) (auto-managed)
--
-- Seeds definitions for all 5 currently-known series and migrates their existing
-- counter rows exactly (no gaps, no restarts).
--
-- Adds APP_TIMEZONE config key so the engine can compute period boundaries in the
-- correct timezone (IST for this client; any IANA zone for others).
--
-- OLD TABLE: application_number_sequences is left untouched as a safety net.
-- It will be dropped in Phase 6 after full validation.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. App timezone — used by Phase 2 engine for all day/month/year boundaries
-- -----------------------------------------------------------------------------
INSERT INTO system_configurations
    (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at)
VALUES (
    'app.timezone',
    'Asia/Kolkata',
    'IANA timezone ID used for sequence period boundary resets (day, month, year, financial year). '
    || 'Examples: Asia/Kolkata, UTC, America/New_York. Change requires app restart to clear cache.',
    'STRING',
    'SYSTEM',
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (config_key) DO NOTHING;


-- -----------------------------------------------------------------------------
-- 2. Series definitions — one row per series type, editable from the UI
-- -----------------------------------------------------------------------------
CREATE TABLE number_series_definitions (
    id               BIGSERIAL                NOT NULL,
    series_code      VARCHAR(50)              NOT NULL,
    series_name      VARCHAR(100)             NOT NULL,
    scope_type       VARCHAR(30)              NOT NULL,
    prefix           VARCHAR(30),
    separator        VARCHAR(5)               NOT NULL DEFAULT '-',
    sequence_padding INTEGER                  NOT NULL DEFAULT 4,
    description      TEXT,
    is_active        BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_number_series_definitions    PRIMARY KEY (id),
    CONSTRAINT uk_number_series_definitions    UNIQUE      (series_code),
    CONSTRAINT chk_nsd_scope_type             CHECK (scope_type IN (
        'NONE',
        'CALENDAR_DAY',
        'CALENDAR_MONTH',
        'CALENDAR_YEAR',
        'FINANCIAL_MONTH',
        'FINANCIAL_YEAR',
        'ACADEMIC_YEAR',
        'COURSE',
        'ACADEMIC_YEAR_COURSE'
    )),
    CONSTRAINT chk_nsd_padding_positive       CHECK (sequence_padding > 0)
);

COMMENT ON TABLE  number_series_definitions                IS 'Configurable definitions for each application number series (prefix, scope, format).';
COMMENT ON COLUMN number_series_definitions.series_code    IS 'Unique machine key, e.g. RECEIPT_NUMBER. Immutable once counters exist.';
COMMENT ON COLUMN number_series_definitions.scope_type     IS 'Controls period granularity and reset behaviour. Immutable once counters exist.';
COMMENT ON COLUMN number_series_definitions.prefix         IS 'Static prefix, e.g. RCP. NULL for series where scope_key itself forms the prefix (ADMISSION_NUMBER).';
COMMENT ON COLUMN number_series_definitions.separator      IS 'Separator between number components. Empty string for no separator.';
COMMENT ON COLUMN number_series_definitions.sequence_padding IS 'Zero-pad width for the sequence integer, e.g. 5 → 00001.';


-- -----------------------------------------------------------------------------
-- 3. Sequence counters — one row per (series, scope_key), auto-created on first use
-- -----------------------------------------------------------------------------
CREATE TABLE number_sequence_counters (
    id              BIGSERIAL                NOT NULL,
    series_code     VARCHAR(50)              NOT NULL,
    scope_key       VARCHAR(100)             NOT NULL,
    last_sequence   INTEGER                  NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_number_sequence_counters   PRIMARY KEY (id),
    CONSTRAINT uk_nsc_series_scope           UNIQUE      (series_code, scope_key),
    CONSTRAINT fk_nsc_series_code            FOREIGN KEY (series_code)
                                             REFERENCES  number_series_definitions (series_code),
    CONSTRAINT chk_nsc_last_sequence_gte_zero CHECK (last_sequence >= 0)
);

CREATE INDEX idx_nsc_series_scope ON number_sequence_counters (series_code, scope_key);

COMMENT ON TABLE  number_sequence_counters              IS 'Auto-managed counters per (series, period). One row per scope_key, created on first number generation.';
COMMENT ON COLUMN number_sequence_counters.scope_key    IS 'Period identifier: GLOBAL, 2026, 2526, 20260702, 65, 2526-65, etc. Determined by scope_type at generation time.';
COMMENT ON COLUMN number_sequence_counters.last_sequence IS 'Counter value after the last generated number. Next number uses last_sequence + 1.';


-- -----------------------------------------------------------------------------
-- 4. Seed definitions for all currently-known series
--
-- ADMISSION_NUMBER:
--   prefix = NULL — scope_key itself (startYear || admissionNumberCode) acts as prefix.
--   separator = '' — no separator; format is {scopeKey}{seq} e.g. 2026650001.
--   scope_type = ACADEMIC_YEAR_COURSE — one counter per (academic year × course).
--
-- RECEIPT / REFUND / COMMISSION / DISBURSEMENT:
--   prefix = short code, separator = '-', scope_type = CALENDAR_YEAR.
--   Format: RCP-2026-00001.
--   (DISBURSEMENT scope_type can be changed to FINANCIAL_YEAR via the UI in Phase 4
--    once the new engine is live — changing it now would orphan existing 2026 counters.)
-- -----------------------------------------------------------------------------
INSERT INTO number_series_definitions
    (series_code, series_name, scope_type, prefix, separator, sequence_padding, description)
VALUES
    (
        'ADMISSION_NUMBER',
        'Admission Number',
        'ACADEMIC_YEAR_COURSE',
        NULL,
        '',
        4,
        'Permanent student admission reference. '
        || 'Number format: {startYear}{admissionNumberCode}{seq} — e.g. 2026650001. '
        || 'One counter per academic year × course; retro-admits use the year from the chosen admission date.'
    ),
    (
        'RECEIPT_NUMBER',
        'Receipt Number',
        'CALENDAR_YEAR',
        'RCP',
        '-',
        5,
        'Global receipt number generated for every payment receipt. Format: RCP-{year}-{seq}.'
    ),
    (
        'REFUND_NUMBER',
        'Refund Number',
        'CALENDAR_YEAR',
        'RFD',
        '-',
        5,
        'Global refund number generated for every payment reversal. Format: RFD-{year}-{seq}.'
    ),
    (
        'COMMISSION_NUMBER',
        'Commission Number',
        'CALENDAR_YEAR',
        'COM',
        '-',
        5,
        'Global commission number generated when a commission payout is pushed to OneBook. Format: COM-{year}-{seq}.'
    ),
    (
        'DISBURSEMENT_NUMBER',
        'Scholarship Disbursement Number',
        'CALENDAR_YEAR',
        'DSB',
        '-',
        5,
        'Global disbursement number generated when a scholarship disbursement is pushed to OneBook. Format: DSB-{year}-{seq}.'
    )
ON CONFLICT (series_code) DO NOTHING;


-- -----------------------------------------------------------------------------
-- 5. Migrate existing counters from application_number_sequences
--
-- Copies exact last_sequence values — no gaps, no restarts.
-- Only migrates series_codes that have a definition above (FK constraint).
-- GREATEST() guard makes this re-runnable without risk of rolling back a counter.
-- -----------------------------------------------------------------------------
INSERT INTO number_sequence_counters
    (series_code, scope_key, last_sequence, created_at, updated_at)
SELECT
    series_code,
    scope_key,
    last_sequence,
    created_at,
    updated_at
FROM application_number_sequences
WHERE series_code IN (
    'ADMISSION_NUMBER',
    'RECEIPT_NUMBER',
    'REFUND_NUMBER',
    'COMMISSION_NUMBER',
    'DISBURSEMENT_NUMBER'
)
ON CONFLICT (series_code, scope_key) DO UPDATE
    SET last_sequence = GREATEST(number_sequence_counters.last_sequence, EXCLUDED.last_sequence),
        updated_at    = CURRENT_TIMESTAMP;
