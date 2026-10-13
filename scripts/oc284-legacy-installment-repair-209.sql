-- ============================================================================
--  OC-284 follow-up: restore legacy installment amounts that the boarding-status
--  switch sync wrongly repriced on PAST-DUE terms (live 209, 2026-10-13).
--
--  EXECUTED on live 209 on 2026-10-13 (~16:25 IST): 8 rows repaired, COMMIT, psql exit 0.
--  Pre-run backup: /docker_data/backups/cms_pre_oc284_repair_20261013_162321.dump
--  One-off production data repair (JIRA OC-285) -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Root cause: FeeFinalizationService.syncSemesterFeesForStudentTypeSwitch repriced every
--  not-fully-paid installment, including terms whose collection date had already passed.
--  Fixed in code (due-date cutoff + never-below-paid); this script repairs the damage.
--
--  Source of truth for "before" values: /docker_data/backups/cms_pre_oc284b_20261012_154757.dump
--  (taken before any of the switches). The same values appear in every earlier backup too.
--
--  Scope: ONLY the 8 installment_fees rows below (all past-due at repair time). Deliberately
--  NOT touched:
--    * future-term installments the switch also repriced (18 rows across students 8, 24, 108, 199, 228, 255, 259) -- that
--      is the designed switch behaviour; flagged separately for a business decision.
--    * student_boarding_status_switches history (immutable audit trail).
--    * student_fee_allocations.total_fee (never changed; still the true original total).
--    * fee_installments (payments) -- no payment data is modified.
--
--  Run order (operator):
--    1. pg_dump backup first:
--         docker exec <pg> pg_dump -U cms_user -Fc cms > /docker_data/backups/cms_pre_oc284_repair_$(date +%Y%m%d_%H%M%S).dump
--       and confirm the file is non-empty (ls -la) before continuing.
--    2. psql -U cms_user -d cms -f oc284-legacy-installment-repair-209.sql
--       Every UPDATE is guarded on the CURRENT amount, so re-running is a no-op, and the
--       transaction aborts (nothing committed) if the affected row count is not exactly as expected.
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only): current state of the 8 rows.
SELECT i.id, a.student_id, i.installment_label, i.amount AS current_amount, i.due_date
FROM installment_fees i JOIN student_fee_allocations a ON a.id = i.allocation_id
WHERE i.id IN (329, 330, 50, 177, 178, 628, 1052, 1270)
ORDER BY a.student_id, i.id;

CREATE TEMP TABLE _repair (id bigint PRIMARY KEY, student_id bigint, wrong_amount numeric, correct_amount numeric) ON COMMIT DROP;
INSERT INTO _repair (id, student_id, wrong_amount, correct_amount) VALUES
    -- Gaadha Prasad P (student 48): Y1 Sem 1 / Sem 2
    (329, 48, 120000.00, 142500.00),
    (330, 48, 140000.00, 142500.00),
    -- student 8: Y1 Sem 2
    (50,   8, 125000.00, 132500.00),
    -- student 24: Y1 Sem 1 / Sem 2
    (177, 24, 120000.00, 127500.00),
    (178, 24, 120000.00, 127500.00),
    -- student 108: Y2 Sem 2 (was raised by +25,000 on a past-due, partly paid term)
    (628, 108, 125000.00, 100000.00),
    -- student 228: Y2 Sem 2 (raised +5,000 on a past-due term by a Hosteler->Day Scholar switch)
    (1052, 228, 105000.00, 100000.00),
    -- student 255: Y3 Sem 2 (raised +5,000 on a past-due term by a Hosteler->Day Scholar switch)
    (1270, 255, 105000.00, 100000.00);

WITH upd AS (
    UPDATE installment_fees i
       SET amount = r.correct_amount,
           updated_at = now()
      FROM _repair r
     WHERE i.id = r.id
       AND i.amount = r.wrong_amount          -- guard: only if still in the wrong state
    RETURNING i.id
)
SELECT count(*) AS rows_repaired FROM upd \gset

-- Abort the whole transaction unless exactly 8 rows were repaired on first run
-- (or 0 on an idempotent re-run, when every row is already correct).
DO $$
DECLARE
    already_correct int;
BEGIN
    SELECT count(*) INTO already_correct
      FROM installment_fees i JOIN _repair r ON r.id = i.id
     WHERE i.amount = r.correct_amount;
    IF already_correct <> 8 THEN
        RAISE EXCEPTION 'Repair aborted: expected all 8 rows at correct_amount after update, found % -- rolling back', already_correct;
    END IF;
END $$;

\echo 'rows_repaired (8 on first run, 0 on re-run):' :rows_repaired

-- Post-check (read-only): should show correct amounts.
SELECT i.id, a.student_id, i.installment_label, i.amount AS amount_now, i.due_date
FROM installment_fees i JOIN student_fee_allocations a ON a.id = i.allocation_id
WHERE i.id IN (329, 330, 50, 177, 178, 628, 1052, 1270)
ORDER BY a.student_id, i.id;

-- Sanity: Gaadha's installments must sum back to the allocation total (9,75,000).
SELECT a.total_fee AS allocation_total, sum(i.amount) AS installments_sum
FROM student_fee_allocations a JOIN installment_fees i ON i.allocation_id = a.id
WHERE a.student_id = 48 GROUP BY a.total_fee;

COMMIT;
