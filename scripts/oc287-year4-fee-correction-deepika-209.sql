-- ============================================================================
--  OC-287: correct Year-4 fee for DEEPIKA M MADESHWARAN (student 333,
--  adm 2022650055) in B.Sc Nursing, per admin instruction (live 209).
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Scope: 1 student, Year-4 (Sem 1 + Sem 2) installment_fees rows + their
--  1 student_fee_allocation (total_fee / net_fee, so it still equals the
--  sum of its installments). Years 1-3 are untouched -- already correct and
--  fully paid.
--
--    Student 333 DEEPIKA M MADESHWARAN (2022650055), alloc 237:
--      Y4 Sem1 1895: 80000.00 -> 65000.00
--      Y4 Sem2 1896: 80000.00 -> 65000.00
--      total_fee/net_fee: 640000.00 -> 610000.00
--      (Neither installment has any payment recorded yet -- confirmed 0 rows
--       in fee_installments for ids 1895/1896 -- so no overpayment/carry-
--       forward concern.)
--
--  Guards: every UPDATE matches on the CURRENT exact amount (re-run = no-op);
--  aborts (ROLLBACK) unless both installments and the allocation end at their
--  target values and the allocation's total_fee/net_fee equals the sum of
--  its installments.
--
--  Run order (operator):
--    1. pg_dump backup first:
--         docker exec skscms209-postgres-1 pg_dump -U cms_user -Fc cms > /docker_data/backups/cms_pre_oc287_year4fix_$(date +%Y%m%d_%H%M%S).dump
--       and confirm the file is non-empty (ls -la) before continuing.
--    2. docker exec -i skscms209-postgres-1 psql -U cms_user -d cms -f - < oc287-year4-fee-correction-deepika-209.sql
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only): current state of the 2 installment rows + allocation.
SELECT i.id, a.student_id, i.installment_label, i.amount AS current_amount, i.due_date
FROM installment_fees i JOIN student_fee_allocations a ON a.id = i.allocation_id
WHERE i.id IN (1895, 1896)
ORDER BY i.id;

SELECT a.id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum
FROM student_fee_allocations a WHERE a.id = 237;

CREATE TEMP TABLE _inst (id bigint PRIMARY KEY, old_amt numeric, new_amt numeric) ON COMMIT DROP;
INSERT INTO _inst (id, old_amt, new_amt) VALUES
    (1895, 80000.00, 65000.00),  -- Year 4 - First Semester
    (1896, 80000.00, 65000.00);  -- Year 4 - Second Semester

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
       SET total_fee = 610000.00,
           net_fee = 610000.00,
           updated_at = now()
     WHERE a.id = 237
       AND a.total_fee = 640000.00
       AND a.net_fee = 640000.00
       AND a.discount_amount = 0
    RETURNING a.id
)
SELECT count(*) AS allocations_updated FROM upd \gset

-- Abort the whole transaction unless both rows ended at their correct value,
-- and the allocation's total_fee/net_fee equals the sum of its installments.
DO $$
DECLARE
    bad int;
BEGIN
    SELECT count(*) INTO bad FROM _inst t JOIN installment_fees i ON i.id = t.id WHERE i.amount <> t.new_amt;
    IF bad <> 0 THEN
        RAISE EXCEPTION 'Aborted: % installment(s) not at target amount', bad;
    END IF;

    SELECT count(*) INTO bad FROM student_fee_allocations a
     WHERE a.id = 237
       AND (a.total_fee <> 610000.00 OR a.net_fee <> 610000.00
            OR a.total_fee <> (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id));
    IF bad <> 0 THEN
        RAISE EXCEPTION 'Aborted: allocation wrong or total_fee <> sum of installments';
    END IF;
END $$;

\echo 'installments_updated (2 on first run, 0 on re-run):' :installments_updated
\echo 'allocations_updated (1 on first run, 0 on re-run):' :allocations_updated

-- Post-check (read-only): should show corrected amounts.
SELECT i.id, a.student_id, i.installment_label, i.amount AS amount_now, i.due_date
FROM installment_fees i JOIN student_fee_allocations a ON a.id = i.allocation_id
WHERE i.id IN (1895, 1896)
ORDER BY i.id;

SELECT a.id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum
FROM student_fee_allocations a WHERE a.id = 237;

COMMIT;
