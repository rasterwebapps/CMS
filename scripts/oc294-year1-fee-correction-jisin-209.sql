-- ============================================================================
--  OC-294: correct Year-1 fee for JISIN M.J (student 13, adm 2025650012)
--  in B.Sc Nursing (2025-26 batch) on live 209.
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Pre-run backup: /docker_data/backups/cms_pre_jisin_fee_adjust_20261029_144958.dump
--
--  Scope:
--    1. installment_fees (alloc 12, student 13):
--         id 89 (Year 1 - First Semester):  147500.00 -> 95000.00
--         id 90 (Year 1 - Second Semester): 147500.00 -> 95000.00
--         (Years 2-4 remain 120000.00 per semester)
--    2. student_fee_allocations (id 12, student 13):
--         total_fee / net_fee: 1015000.00 -> 910000.00 (matches sum of installments)
--    3. fee_installments & payment_receipts:
--         Student has paid 10000.00 towards Year 1 - First Semester (id 89).
--         Since 10000.00 <= 95000.00, no payment re-alignment is needed.
--         Year 1 - First Semester outstanding becomes: 95000.00 - 10000.00 = 85000.00.
--         Year 1 - Second Semester outstanding becomes: 95000.00.
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 12 AND a.student_id = 13;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount
FROM installment_fees i
WHERE i.allocation_id = 12
ORDER BY i.year_number, i.sequence;

-- 1. Update Year 1 installment_fees (89, 90: 147500 -> 95000)
UPDATE installment_fees
   SET amount = 95000.00,
       updated_at = now()
 WHERE id IN (89, 90)
   AND allocation_id = 12
   AND amount = 147500.00;

-- 2. Update student_fee_allocations (id 12: 1015000 -> 910000)
UPDATE student_fee_allocations
   SET total_fee = 910000.00,
       net_fee = 910000.00,
       updated_at = now()
 WHERE id = 12
   AND student_id = 13
   AND total_fee = 1015000.00
   AND net_fee = 1015000.00
   AND discount_amount = 0.00;

-- In-transaction invariant assertions (abort and rollback on any discrepancy)
DO $$
DECLARE
    v_total_fee numeric;
    v_net_fee numeric;
    v_inst_sum numeric;
    v_total_paid numeric;
    v_y1s1_amt numeric;
    v_y1s2_amt numeric;
    v_y1s1_paid numeric;
    v_y1s2_paid numeric;
    v_y2s1_amt numeric;
BEGIN
    SELECT total_fee, net_fee INTO v_total_fee, v_net_fee
      FROM student_fee_allocations WHERE id = 12 AND student_id = 13;
    SELECT sum(amount) INTO v_inst_sum
      FROM installment_fees WHERE allocation_id = 12;
    SELECT sum(amount_paid) INTO v_total_paid
      FROM fee_installments WHERE student_id = 13 AND refunded_at IS NULL;

    IF v_total_fee <> 910000.00 OR v_net_fee <> 910000.00 OR v_inst_sum <> 910000.00 THEN
        RAISE EXCEPTION 'Aborted: allocation/installment sum mismatch (total_fee=%, net_fee=%, inst_sum=%)',
            v_total_fee, v_net_fee, v_inst_sum;
    END IF;

    IF v_total_paid <> 10000.00 THEN
        RAISE EXCEPTION 'Aborted: total_paid changed from 10000.00 to %', v_total_paid;
    END IF;

    SELECT amount INTO v_y1s1_amt FROM installment_fees WHERE id = 89;
    SELECT amount INTO v_y1s2_amt FROM installment_fees WHERE id = 90;
    IF v_y1s1_amt <> 95000.00 OR v_y1s2_amt <> 95000.00 THEN
        RAISE EXCEPTION 'Aborted: Year 1 installments not at 95000.00 (%, %)', v_y1s1_amt, v_y1s2_amt;
    END IF;

    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s1_paid FROM fee_installments WHERE semester_fee_id = 89 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s2_paid FROM fee_installments WHERE semester_fee_id = 90 AND refunded_at IS NULL;

    IF v_y1s1_paid <> 10000.00 OR v_y1s2_paid <> 0.00 THEN
        RAISE EXCEPTION 'Aborted: unexpected per-semester paid amounts (%, %)',
            v_y1s1_paid, v_y1s2_paid;
    END IF;

    SELECT amount INTO v_y2s1_amt FROM installment_fees WHERE id = 91;
    IF v_y2s1_amt <> 120000.00 THEN
        RAISE EXCEPTION 'Aborted: Year 2 Sem 1 installment changed unexpectedly: %', v_y2s1_amt;
    END IF;
END $$;

-- Post-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 12 AND a.student_id = 13;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount AS fee_amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount,
       i.amount - COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS outstanding_amount
FROM installment_fees i
WHERE i.allocation_id = 12
ORDER BY i.year_number, i.sequence;

COMMIT;
