-- V160 — Add admission_category column to students table.
-- Tracks whether a student was admitted via Management or Counselling quota.
-- Nullable: existing students will have NULL until data is backfilled.

ALTER TABLE students
    ADD COLUMN IF NOT EXISTS admission_category VARCHAR(20);
