-- BR-30: Refactor fee_structures to reference fee_structure_groups
-- Clears existing data (approved — fee structures will be re-entered via UI).
-- fee_payments references fee_structures via NOT NULL FK, so it must be cleared first.
-- fee_payments is the legacy payment system superseded by the unified payment_receipts table (V122).
DELETE FROM fee_payments;
DELETE FROM fee_structure_year_amounts;
DELETE FROM fee_structures;

-- Drop old dimension columns
ALTER TABLE fee_structures
    DROP COLUMN IF EXISTS program_id,
    DROP COLUMN IF EXISTS academic_year_id,
    DROP COLUMN IF EXISTS course_id;

-- Add group FK
ALTER TABLE fee_structures
    ADD COLUMN fee_structure_group_id BIGINT NOT NULL
        REFERENCES fee_structure_groups(id) ON DELETE CASCADE;

-- Add unique constraint: one row per fee type per group
ALTER TABLE fee_structures
    ADD CONSTRAINT uq_fee_structure_group_fee_type
        UNIQUE (fee_structure_group_id, fee_type);

CREATE INDEX idx_fs_group ON fee_structures(fee_structure_group_id);
