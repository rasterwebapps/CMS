-- Partial step toward the "hard cutover" BR-49 explicitly deferred (see V272's comment): links
-- any pre-existing lab_schedules row to its real Batch when an unambiguous match exists, without
-- touching batch_name or requiring one. A row is linked only when exactly one Batch shares its
-- (term_instance_id, subject_id via course_offerings, normalized name) — never guessed across
-- multiple same-named batches in different course offerings for the same subject/term.
--
-- Does NOT drop batch_name and does NOT make batch_id required: the Lab Schedule form still
-- accepts free-text batch names alongside the roster dropdown (lab-schedule-form.component.html),
-- so batch_name remains load-bearing. Dropping it is a separate, deliberate future decision once
-- the frontend stops accepting free text.
--
-- Idempotent: the batch_id IS NULL guard means re-running this migration, or a future one over
-- rows typed after this ran, is always a no-op for rows already linked.

WITH candidate AS (
    SELECT b.id AS batch_id, b.term_instance_id, co.subject_id, LOWER(TRIM(b.name)) AS name_norm
    FROM batches b
    JOIN course_offerings co ON co.id = b.course_offering_id
),
unambiguous AS (
    SELECT term_instance_id, subject_id, name_norm, MIN(batch_id) AS batch_id
    FROM candidate
    GROUP BY term_instance_id, subject_id, name_norm
    HAVING COUNT(*) = 1
)
UPDATE lab_schedules ls
SET batch_id = u.batch_id
FROM unambiguous u
WHERE ls.batch_id IS NULL
  AND ls.term_instance_id = u.term_instance_id
  AND ls.subject_id = u.subject_id
  AND LOWER(TRIM(ls.batch_name)) = u.name_norm;
