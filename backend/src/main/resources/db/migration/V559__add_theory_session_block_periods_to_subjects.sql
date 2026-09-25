-- How many consecutive periods one single THEORY session must occupy for this subject --
-- the THEORY counterpart of lab_session_block_periods/clinical_session_block_periods
-- (V406). Unlike those two (no upper bound), this is capped at 2: user-specified rule
-- is "max 2 continuous hours for any single theory subject", so 3+ back-to-back THEORY
-- periods are never valid for any subject, not just today's data. Default 1 = today's
-- existing hardcoded behavior (CurriculumHoursCalculator#resolveBlockSize always
-- returned 1 for THEORY before this column existed) -- fully backward compatible, no
-- data migration needed.

ALTER TABLE subjects ADD COLUMN theory_session_block_periods INTEGER NOT NULL DEFAULT 1;

ALTER TABLE subjects ADD CONSTRAINT chk_subjects_theory_session_block_periods
    CHECK (theory_session_block_periods BETWEEN 1 AND 2);
