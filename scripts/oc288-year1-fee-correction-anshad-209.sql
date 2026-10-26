-- ============================================================================
--  OC-288: correct Year-1 fee for ANSHAD C M (student 108, adm 2024650012)
--  in B.Sc Nursing (2024-25 batch) on live 209.
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Scope:
--    1. installment_fees (alloc 79, student 108):
--         id 625 (Year 1 - First Semester):  135000.00 -> 125000.00
--         id 626 (Year 1 - Second Semester): 135000.00 -> 125000.00
--         (Year 2: 100000 + 100000; Year 3: 80000 + 100000; Year 4: 100000 + 100000)
--    2. student_fee_allocations (id 79, student 108):
--         total_fee / net_fee: 870000.00 -> 830000.00 (matches sum of installments)
--    3. fee_installments & payment_receipts (student 108):
--         Re-aligns split payment rows across semesters so per-semester paid &
--         outstanding reflect the ₹2,50,000 Year-1 fee and carry the ₹20,000
--         into Year 3 - First Semester (id 629), while keeping total paid at
--         ₹4,70,000.00 and every receipt total unchanged:
--         - RCP-2024-00043 (50,000): consolidate id 410 (10k on 625) + id 411 (40k on 626)
--           -> id 411 = 50000.00 on 626, delete id 410; receipt 409 -> 'Year 1 - Second Semester'
--         - RCP-2026-00119 (35,000): consolidate id 413 (20k on 626) + id 414 (15k on 627)
--           -> id 414 = 35000.00 on 627, delete id 413; receipt 411 -> 'Year 2 - First Semester'
--         - RCP-2026-00638 (50,000): consolidate id 2858 (20k on 627) + id 2859 (30k on 628)
--           -> id 2859 = 50000.00 on 628, delete id 2858; receipt 2300 -> 'Year 2 - Second Semester'
--         - RCP-2026-00811 (20,000, SCHOLARSHIP): move id 3004 from semester_fee_id 628 -> 629;
--           receipt 2473 -> 'Year 3 - First Semester'
--    4. fee_demands (id 61, enrollment 71, Term 5 / Year 3 - First Semester):
--         paid_amount: 0.00 -> 20000.00, status: 'UNPAID' -> 'PARTIAL'
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 79 AND a.student_id = 108;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount
FROM installment_fees i
WHERE i.allocation_id = 79
ORDER BY i.year_number, i.sequence;

-- 1. Update Year 1 installment_fees (625, 626: 135000 -> 125000)
UPDATE installment_fees
   SET amount = 125000.00,
       updated_at = now()
 WHERE id IN (625, 626)
   AND allocation_id = 79
   AND amount = 135000.00;

-- 2. Update student_fee_allocations (id 79: 870000 -> 830000)
UPDATE student_fee_allocations
   SET total_fee = 830000.00,
       net_fee = 830000.00,
       updated_at = now()
 WHERE id = 79
   AND student_id = 108
   AND total_fee = 870000.00
   AND net_fee = 870000.00
   AND discount_amount = 0.00;

-- 3. Re-align split fee_installments rows and payment_receipts.installments_covered
-- 3a. RCP-2024-00043 (50,000 on 2024-08-15 -> all on 626 Year 1 - Second Semester)
DELETE FROM fee_installments
 WHERE id = 410 AND student_id = 108 AND semester_fee_id = 625
   AND receipt_number = 'RCP-2024-00043' AND amount_paid = 10000.00;

UPDATE fee_installments
   SET amount_paid = 50000.00,
       updated_at = now()
 WHERE id = 411 AND student_id = 108 AND semester_fee_id = 626
   AND receipt_number = 'RCP-2024-00043' AND amount_paid = 40000.00;

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - Second Semester'
 WHERE id = 409 AND payer_type = 'STUDENT' AND payer_id = 108
   AND receipt_number = 'RCP-2024-00043' AND amount_paid = 50000.00;

-- 3b. RCP-2026-00119 (35,000 on 2026-03-24 -> all on 627 Year 2 - First Semester)
DELETE FROM fee_installments
 WHERE id = 413 AND student_id = 108 AND semester_fee_id = 626
   AND receipt_number = 'RCP-2026-00119' AND amount_paid = 20000.00;

UPDATE fee_installments
   SET amount_paid = 35000.00,
       updated_at = now()
 WHERE id = 414 AND student_id = 108 AND semester_fee_id = 627
   AND receipt_number = 'RCP-2026-00119' AND amount_paid = 15000.00;

UPDATE payment_receipts
   SET installments_covered = 'Year 2 - First Semester'
 WHERE id = 411 AND payer_type = 'STUDENT' AND payer_id = 108
   AND receipt_number = 'RCP-2026-00119' AND amount_paid = 35000.00;

-- 3c. RCP-2026-00638 (50,000 on 2026-09-22 -> all on 628 Year 2 - Second Semester)
DELETE FROM fee_installments
 WHERE id = 2858 AND student_id = 108 AND semester_fee_id = 627
   AND receipt_number = 'RCP-2026-00638' AND amount_paid = 20000.00;

UPDATE fee_installments
   SET amount_paid = 50000.00,
       updated_at = now()
 WHERE id = 2859 AND student_id = 108 AND semester_fee_id = 628
   AND receipt_number = 'RCP-2026-00638' AND amount_paid = 30000.00;

UPDATE payment_receipts
   SET installments_covered = 'Year 2 - Second Semester'
 WHERE id = 2300 AND payer_type = 'STUDENT' AND payer_id = 108
   AND receipt_number = 'RCP-2026-00638' AND amount_paid = 50000.00;

-- 3d. RCP-2026-00811 (20,000 SCHOLARSHIP on 2026-10-23 -> move to 629 Year 3 - First Semester)
UPDATE fee_installments
   SET semester_fee_id = 629,
       updated_at = now()
 WHERE id = 3004 AND student_id = 108 AND semester_fee_id = 628
   AND receipt_number = 'RCP-2026-00811' AND amount_paid = 20000.00;

UPDATE payment_receipts
   SET installments_covered = 'Year 3 - First Semester'
 WHERE id = 2473 AND payer_type = 'STUDENT' AND payer_id = 108
   AND receipt_number = 'RCP-2026-00811' AND amount_paid = 20000.00;

-- 4. Update fee_demands (id 61, Term 5 / Year 3 - First Semester)
UPDATE fee_demands
   SET paid_amount = 20000.00,
       status = 'PARTIAL',
       updated_at = now()
 WHERE id = 61
   AND student_term_enrollment_id = 71
   AND total_amount = 80000.00
   AND paid_amount = 0.00
   AND status = 'UNPAID';

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
    v_y2s1_paid numeric;
    v_y2s2_paid numeric;
    v_y3s1_paid numeric;
    v_demand_paid numeric;
    v_demand_status text;
BEGIN
    SELECT total_fee, net_fee INTO v_total_fee, v_net_fee
      FROM student_fee_allocations WHERE id = 79 AND student_id = 108;
    SELECT sum(amount) INTO v_inst_sum
      FROM installment_fees WHERE allocation_id = 79;
    SELECT sum(amount_paid) INTO v_total_paid
      FROM fee_installments WHERE student_id = 108 AND refunded_at IS NULL;

    IF v_total_fee <> 830000.00 OR v_net_fee <> 830000.00 OR v_inst_sum <> 830000.00 THEN
        RAISE EXCEPTION 'Aborted: allocation/installment sum mismatch (total_fee=%, net_fee=%, inst_sum=%)',
            v_total_fee, v_net_fee, v_inst_sum;
    END IF;

    IF v_total_paid <> 470000.00 THEN
        RAISE EXCEPTION 'Aborted: total_paid changed from 470000.00 to %', v_total_paid;
    END IF;

    SELECT amount INTO v_y1s1_amt FROM installment_fees WHERE id = 625;
    SELECT amount INTO v_y1s2_amt FROM installment_fees WHERE id = 626;
    IF v_y1s1_amt <> 125000.00 OR v_y1s2_amt <> 125000.00 THEN
        RAISE EXCEPTION 'Aborted: Year 1 installments not at 125000.00 (%, %)', v_y1s1_amt, v_y1s2_amt;
    END IF;

    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s1_paid FROM fee_installments WHERE semester_fee_id = 625 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s2_paid FROM fee_installments WHERE semester_fee_id = 626 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y2s1_paid FROM fee_installments WHERE semester_fee_id = 627 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y2s2_paid FROM fee_installments WHERE semester_fee_id = 628 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y3s1_paid FROM fee_installments WHERE semester_fee_id = 629 AND refunded_at IS NULL;

    IF v_y1s1_paid <> 125000.00 OR v_y1s2_paid <> 125000.00 OR v_y2s1_paid <> 100000.00 OR v_y2s2_paid <> 100000.00 OR v_y3s1_paid <> 20000.00 THEN
        RAISE EXCEPTION 'Aborted: unexpected per-semester paid amounts (%, %, %, %, %)',
            v_y1s1_paid, v_y1s2_paid, v_y2s1_paid, v_y2s2_paid, v_y3s1_paid;
    END IF;

    SELECT paid_amount, status INTO v_demand_paid, v_demand_status FROM fee_demands WHERE id = 61;
    IF v_demand_paid <> 20000.00 OR v_demand_status <> 'PARTIAL' THEN
        RAISE EXCEPTION 'Aborted: fee_demand 61 not updated properly (paid=%, status=%)', v_demand_paid, v_demand_status;
    END IF;
END $$;

-- Post-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 79 AND a.student_id = 108;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount AS fee_amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount,
       i.amount - COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS outstanding_amount
FROM installment_fees i
WHERE i.allocation_id = 79
ORDER BY i.year_number, i.sequence;

SELECT id, student_term_enrollment_id, total_amount, paid_amount, total_amount - paid_amount AS outstanding_amount, status
FROM fee_demands
WHERE id = 61;

COMMIT;
