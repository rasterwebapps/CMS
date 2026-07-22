-- R2-M4.1a: Hostel Room Type master. Prices hostel fees by sharing capacity + AC/non-AC, per the
-- scoping decision that hostel fees are no longer flat — they vary by room type. Fee is per
-- academic year (fee_amount_per_year) to match the existing per-year cadence used elsewhere
-- (fee_structure_year_amounts.amount / semester_fees), so it composes with the fee-recalculation
-- engine without a unit-conversion step.

CREATE TABLE hostel_room_types (
    id                  BIGSERIAL     PRIMARY KEY,
    name                VARCHAR(100)  NOT NULL,
    code                VARCHAR(50)   NOT NULL,
    sharing_capacity    INTEGER       NOT NULL,
    is_ac               BOOLEAN       NOT NULL DEFAULT FALSE,
    fee_amount_per_year NUMERIC(12,2) NOT NULL,
    description         VARCHAR(500),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_hostel_room_types_name UNIQUE (name),
    CONSTRAINT uq_hostel_room_types_code UNIQUE (code),
    CONSTRAINT chk_hostel_room_types_sharing_capacity CHECK (sharing_capacity > 0),
    CONSTRAINT chk_hostel_room_types_fee_amount CHECK (fee_amount_per_year >= 0)
);
