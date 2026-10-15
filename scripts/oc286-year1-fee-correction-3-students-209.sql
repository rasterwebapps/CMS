-- ============================================================================
--  OC-286: correct Year-1 fee for 3 students in the 2025-26 batch, per an
--  admin-supplied handwritten fee sheet (live 209).
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Scope: 3 students, Year-1 (Sem 1 + Sem 2) installment_fees rows + their
--  3 student_fee_allocations (total_fee / net_fee, so each still equals the
--  sum of its installments). Years 2+ are untouched -- already correct.
--
--    Student 48 GAADHA PRASAD P (2025650042), alloc 42:
--      Y1 Sem1 329: 142500.00 -> 127500.00
--      Y1 Sem2 330: 142500.00 -> 127500.00
--      total_fee/net_fee: 975000.00 -> 945000.00
--      (Sem1 already paid 127500.00 exactly -- no overpayment, no change needed there.)
--
--    Student 8 RAGHUPATHI C (2025650007), alloc 7:
--      Y1 Sem1 49: 132500.00 -> 112500.00
--      Y1 Sem2 50: 132500.00 -> 112500.00
--      total_fee/net_fee: 910000.00 -> 750000.00
--      (Sem1 already fully paid at 132500.00, 20000.00 more than the new amount --
--       per business decision, this is left as a carry-forward credit against the
--       next due installment, same as the system's usual overpayment handling.
--       Also corrects a pre-existing total_fee/sum(installments) mismatch of
--       120000.00 that predates this fix and is unrelated to the Year-1 change --
--       per business decision, total_fee must equal sum(installments).)
--
--    Student 24 THABASUM N (2025650023), alloc 23:
--      Y1 Sem1 177: 127500.00 -> 107500.00
--      Y1 Sem2 178: 127500.00 -> 107500.00
--      total_fee/net_fee: 870000.00 -> 710000.00
--      (Sem1 already fully paid at 127500.00, 20000.00 more than the new amount --
--       same carry-forward treatment as Raghupathi above. Same pre-existing
--       120000.00 total_fee mismatch, corrected per the same business decision.)
--
--  Guards: every UPDATE matches on the CURRENT exact amount (re-run = no-op);
--  aborts (ROLLBACK) unless all 6 installments and all 3 allocations end at
--  their target values and each allocation's total_fee/net_fee equals the sum
--  of its installments.
--
--  Run order (operator):
--    1. pg_dump backup first:
--         docker exec skscms209-postgres-1 pg_dump -U cms_user -Fc cms > /docker_data/backups/cms_pre_oc286_year1fix_$(date +%Y%m%d_%H%M%S).dump
--       and confirm the file is non-empty (ls -la) before continuing.
--    2. docker exec -i skscms209-postgres-1 psql -U cms_user -d cms -f - < oc286-year1-fee-correction-3-students-209.sql
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only): current state of the 6 installment rows + 3 allocations.
SELECT i.id, a.student_id, i.installment_label, i.amount AS current_amount, i.due_date
FROM installment_fees i JOIN student_fee_allocations a ON a.id = i.allocation_id
WHERE i.id IN (329, 330, 49, 50, 177, 178)
ORDER BY a.student_id, i.id;

SELECT a.id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum
FROM student_fee_allocations a WHERE a.id IN (42, 7, 23) ORDER BY a.id;

CREATE TEMP TABLE _inst (id bigint PRIMARY KEY, student_id bigint, old_amt numeric, new_amt numeric) ON COMMIT DROP;
INSERT INTO _inst (id, student_id, old_amt, new_amt) VALUES
    -- Gaadha Prasad (student 48): Y1 Sem1 / Sem2
    (329, 48, 142500.00, 127500.00),
    (330, 48, 142500.00, 127500.00),
    -- Raghupathi C (student 8): Y1 Sem1 / Sem2
    (49,   8, 132500.00, 112500.00),
    (50,   8, 132500.00, 112500.00),
    -- Thabasum N (student 24): Y1 Sem1 / Sem2
    (177, 24, 127500.00, 107500.00),
    (178, 24, 127500.00, 107500.00);

CREATE TEMP TABLE _alloc (id bigint PRIMARY KEY, old_total numeric, new_total numeric) ON COMMIT DROP;
INSERT INTO _alloc (id, old_total, new_total) VALUES
    (42, 975000.00, 945000.00),  -- Gaadha Prasad
    (7,  910000.00, 750000.00),  -- Raghupathi C
    (23, 870000.00, 710000.00);  -- Thabasum N

WITH upd AS (
    UPDATE installment_fees i
       SET amount = t.new_amt,
           updated_at = now()
      FROM _inst t
     WHERE i.id = t.id
       AND i.amount = t.old_amt          -- guard: only if still in the pre-fix state
    RETURNING i.id
)
SELECT count(*) AS installments_updated FROM upd \gset

WITH upd AS (
    UPDATE student_fee_allocations a
       SET total_fee = t.new_total,
           net_fee = t.new_total,
           updated_at = now()
      FROM _alloc t
     WHERE a.id = t.id
       AND a.total_fee = t.old_total
       AND a.net_fee = t.old_total
       AND a.discount_amount = 0
    RETURNING a.id
)
SELECT count(*) AS allocations_updated FROM upd \gset

-- Abort the whole transaction unless every targeted row ended at its correct
-- value, and each allocation's total_fee/net_fee equals the sum of its installments.
DO $$
DECLARE
    bad int;
BEGIN
    SELECT count(*) INTO bad FROM _inst t JOIN installment_fees i ON i.id = t.id WHERE i.amount <> t.new_amt;
    IF bad <> 0 THEN
        RAISE EXCEPTION 'Aborted: % installment(s) not at target amount', bad;
    END IF;

    SELECT count(*) INTO bad FROM _alloc t JOIN student_fee_allocations a ON a.id = t.id
     WHERE a.total_fee <> t.new_total OR a.net_fee <> t.new_total
        OR a.total_fee <> (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id);
    IF bad <> 0 THEN
        RAISE EXCEPTION 'Aborted: % allocation(s) wrong or total_fee <> sum of installments', bad;
    END IF;
END $$;

\echo 'installments_updated (6 on first run, 0 on re-run):' :installments_updated
\echo 'allocations_updated (3 on first run, 0 on re-run):' :allocations_updated

-- Post-check (read-only): should show corrected amounts.
SELECT i.id, a.student_id, i.installment_label, i.amount AS amount_now, i.due_date
FROM installment_fees i JOIN student_fee_allocations a ON a.id = i.allocation_id
WHERE i.id IN (329, 330, 49, 50, 177, 178)
ORDER BY a.student_id, i.id;

SELECT a.id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum
FROM student_fee_allocations a WHERE a.id IN (42, 7, 23) ORDER BY a.id;

COMMIT;
