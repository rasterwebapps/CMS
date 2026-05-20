-- V155: Add has_hostel_fee flag to student_fee_allocations.

ALTER TABLE student_fee_allocations
    ADD COLUMN IF NOT EXISTS has_hostel_fee BOOLEAN NOT NULL DEFAULT FALSE;
