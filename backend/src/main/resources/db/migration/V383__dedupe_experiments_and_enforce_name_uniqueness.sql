-- V383: Deduplicate experiments and enforce (subject, name) uniqueness
--
-- The Angular Experiment form previously posted the wrong request field
-- (courseId instead of subjectId), so every save through the UI failed
-- validation and created nothing. scripts/seed_demo_data.py bypasses the UI
-- and posts directly to POST /experiments with the correct field names, and
-- was re-run multiple times against a database with no uniqueness
-- constraint on experiments — producing exact (subject_id, name) duplicates.
-- This migration keeps the earliest row per (subject_id, lower(name)) group,
-- re-points any dependent rows to the surviving row, drops the rest, and
-- adds a unique index so it can't recur.

-- Re-point lab_attendances / lab_continuous_evaluations to the canonical
-- (lowest id) experiment row before the duplicates are removed.
WITH canonical AS (
    SELECT id, MIN(id) OVER (PARTITION BY subject_id, lower(name)) AS keep_id
    FROM experiments
)
UPDATE lab_attendances a
SET experiment_id = c.keep_id
FROM canonical c
WHERE a.experiment_id = c.id AND c.id <> c.keep_id;

WITH canonical AS (
    SELECT id, MIN(id) OVER (PARTITION BY subject_id, lower(name)) AS keep_id
    FROM experiments
)
UPDATE lab_continuous_evaluations e
SET experiment_id = c.keep_id
FROM canonical c
WHERE e.experiment_id = c.id AND c.id <> c.keep_id;

-- lab_curriculum_mappings has its own UNIQUE (experiment_id, outcome_type, outcome_code) —
-- drop a duplicate-owned mapping row if the canonical experiment already has the same
-- outcome mapped, then re-point the remaining rows.
WITH canonical AS (
    SELECT id, MIN(id) OVER (PARTITION BY subject_id, lower(name)) AS keep_id
    FROM experiments
)
DELETE FROM lab_curriculum_mappings m
USING canonical c
WHERE m.experiment_id = c.id
  AND c.id <> c.keep_id
  AND EXISTS (
      SELECT 1 FROM lab_curriculum_mappings keep
      WHERE keep.experiment_id = c.keep_id
        AND keep.outcome_type = m.outcome_type
        AND keep.outcome_code = m.outcome_code
  );

WITH canonical AS (
    SELECT id, MIN(id) OVER (PARTITION BY subject_id, lower(name)) AS keep_id
    FROM experiments
)
UPDATE lab_curriculum_mappings m
SET experiment_id = c.keep_id
FROM canonical c
WHERE m.experiment_id = c.id AND c.id <> c.keep_id;

-- Drop the now-unreferenced duplicate experiment rows, keeping the earliest per subject+name.
DELETE FROM experiments e
USING (
    SELECT id, MIN(id) OVER (PARTITION BY subject_id, lower(name)) AS keep_id
    FROM experiments
) c
WHERE e.id = c.id AND c.id <> c.keep_id;

-- Prevent the same duplicate from recurring.
CREATE UNIQUE INDEX experiments_subject_id_name_uidx ON experiments (subject_id, lower(name));
