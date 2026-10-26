-- ============================================================================
--  OC-290: correct Year-1 fee for YADHU KRISHNA K.P (student 243, adm 2024650051)
--  in B.Sc Nursing (2024-25 batch) on live 209.
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Scope:
--    1. installment_fees (alloc 147, student 243):
--         id 1169 (Year 1 - First Semester):  150000.00 -> 125000.00
--         id 1170 (Year 1 - Second Semester): 150000.00 -> 125000.00
--         (Years 2-4 remain 100000.00 per semester)
--    2. student_fee_allocations (id 147, student 243):
--         total_fee / net_fee: 900000.00 -> 850000.00 (matches sum of installments)
--    3. fee_installments & payment_receipts (student 243):
--         Re-aligns payment rows across semesters so per-semester paid &
--         outstanding reflect the ₹2,50,000 Year-1 fee and carry the ₹50,000
--         forward so Year 2 - Second Semester (id 1172) becomes 100% paid
--         (100000.00), while keeping total paid at ₹4,50,000.00 and every
--         receipt total unchanged:
--         - RCP-2024-00339 (100,000 on 2024-08-15):
--             id 1870 (on 1169): 100000.00 -> 75000.00
--             insert split row on 1170: 25000.00
--             receipt 1536 -> 'Year 1 - First Semester, Year 1 - Second Semester'
--         - RCP-2026-00369 (100,000 on 2026-05-25):
--             consolidate id 1873 (50k on 1170) + id 1874 (50k on 1171)
--             -> id 1874 = 100000.00 on 1171, delete id 1873
--             receipt 1539 -> 'Year 2 - First Semester'
--         - RCP-2026-00809 (100,000 on 2026-10-23):
--             consolidate id 3000 (50k on 1171) + id 3001 (50k on 1172)
--             -> id 3001 = 100000.00 on 1172, delete id 3000
--             receipt 2471 -> 'Year 2 - Second Semester'
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 147 AND a.student_id = 243;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount
FROM installment_fees i
WHERE i.allocation_id = 147
ORDER BY i.year_number, i.sequence;

-- 1. Update Year 1 installment_fees (1169, 1170: 150000 -> 125000)
UPDATE installment_fees
   SET amount = 125000.00,
       updated_at = now()
 WHERE id IN (1169, 1170)
   AND allocation_id = 147
   AND amount = 150000.00;

-- 2. Update student_fee_allocations (id 147: 900000 -> 850000)
UPDATE student_fee_allocations
   SET total_fee = 850000.00,
       net_fee = 850000.00,
       updated_at = now()
 WHERE id = 147
   AND student_id = 243
   AND total_fee = 900000.00
   AND net_fee = 900000.00
   AND discount_amount = 0.00;

-- 3. Re-align fee_installments and payment_receipts
-- 3a. RCP-2024-00339 (100,000 -> 75,000 on 1169 + 25,000 on 1170)
UPDATE fee_installments
   SET amount_paid = 75000.00,
       updated_at = now()
 WHERE id = 1870 AND student_id = 243 AND semester_fee_id = 1169
   AND receipt_number = 'RCP-2024-00339' AND amount_paid = 100000.00;

INSERT INTO fee_installments (
    semester_fee_id, student_id, amount_paid, payment_date, payment_mode,
    receipt_number, transaction_reference, remarks, created_at, updated_at
)
SELECT 1170, student_id, 25000.00, payment_date, payment_mode,
       receipt_number, transaction_reference, remarks, created_at, now()
  FROM fee_installments
 WHERE id = 1870
   AND NOT EXISTS (
       SELECT 1 FROM fee_installments
        WHERE student_id = 243 AND semester_fee_id = 1170 AND receipt_number = 'RCP-2024-00339'
   );

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - First Semester, Year 1 - Second Semester'
 WHERE id = 1536 AND payer_type = 'STUDENT' AND payer_id = 243
   AND receipt_number = 'RCP-2024-00339' AND amount_paid = 100000.00;

-- 3b. RCP-2026-00369 (100,000 -> all on 1171 Year 2 - First Semester)
DELETE FROM fee_installments
 WHERE id = 1873 AND student_id = 243 AND semester_fee_id = 1170
   AND receipt_number = 'RCP-2026-00369' AND amount_paid = 50000.00;

UPDATE fee_installments
   SET amount_paid = 100000.00,
       updated_at = now()
 WHERE id = 1874 AND student_id = 243 AND semester_fee_id = 1171
   AND receipt_number = 'RCP-2026-00369' AND amount_paid = 50000.00;

UPDATE payment_receipts
   SET installments_covered = 'Year 2 - First Semester'
 WHERE id = 1539 AND payer_type = 'STUDENT' AND payer_id = 243
   AND receipt_number = 'RCP-2026-00369' AND amount_paid = 100000.00;

-- 3c. RCP-2026-00809 (100,000 -> all on 1172 Year 2 - Second Semester)
DELETE FROM fee_installments
 WHERE id = 3000 AND student_id = 243 AND semester_fee_id = 1171
   AND receipt_number = 'RCP-2026-00809' AND amount_paid = 50000.00;

UPDATE fee_installments
   SET amount_paid = 100000.00,
       updated_at = now()
 WHERE id = 3001 AND student_id = 243 AND semester_fee_id = 1172
   AND receipt_number = 'RCP-2026-00809' AND amount_paid = 50000.00;

UPDATE payment_receipts
   SET installments_covered = 'Year 2 - Second Semester'
 WHERE id = 2471 AND payer_type = 'STUDENT' AND payer_id = 243
   AND receipt_number = 'RCP-2026-00809' AND amount_paid = 100000.00;

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
BEGIN
    SELECT total_fee, net_fee INTO v_total_fee, v_net_fee
      FROM student_fee_allocations WHERE id = 147 AND student_id = 243;
    SELECT sum(amount) INTO v_inst_sum
      FROM installment_fees WHERE allocation_id = 147;
    SELECT sum(amount_paid) INTO v_total_paid
      FROM fee_installments WHERE student_id = 243 AND refunded_at IS NULL;

    IF v_total_fee <> 850000.00 OR v_net_fee <> 850000.00 OR v_inst_sum <> 850000.00 THEN
        RAISE EXCEPTION 'Aborted: allocation/installment sum mismatch (total_fee=%, net_fee=%, inst_sum=%)',
            v_total_fee, v_net_fee, v_inst_sum;
    END IF;

    IF v_total_paid <> 450000.00 THEN
        RAISE EXCEPTION 'Aborted: total_paid changed from 450000.00 to %', v_total_paid;
    END IF;

    SELECT amount INTO v_y1s1_amt FROM installment_fees WHERE id = 1169;
    SELECT amount INTO v_y1s2_amt FROM installment_fees WHERE id = 1170;
    IF v_y1s1_amt <> 125000.00 OR v_y1s2_amt <> 125000.00 THEN
        RAISE EXCEPTION 'Aborted: Year 1 installments not at 125000.00 (%, %)', v_y1s1_amt, v_y1s2_amt;
    END IF;

    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s1_paid FROM fee_installments WHERE semester_fee_id = 1169 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s2_paid FROM fee_installments WHERE semester_fee_id = 1170 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y2s1_paid FROM fee_installments WHERE semester_fee_id = 1171 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y2s2_paid FROM fee_installments WHERE semester_fee_id = 1172 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y3s1_paid FROM fee_installments WHERE semester_fee_id = 1173 AND refunded_at IS NULL;

    IF v_y1s1_paid <> 125000.00 OR v_y1s2_paid <> 125000.00 OR v_y2s1_paid <> 100000.00 OR v_y2s2_paid <> 100000.00 OR v_y3s1_paid <> 0.00 THEN
        RAISE EXCEPTION 'Aborted: unexpected per-semester paid amounts (%, %, %, %, %)',
            v_y1s1_paid, v_y1s2_paid, v_y2s1_paid, v_y2s2_paid, v_y3s1_paid;
    END IF;
END $$;

-- Post-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 147 AND a.student_id = 243;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount AS fee_amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount,
       i.amount - COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS outstanding_amount
FROM installment_fees i
WHERE i.allocation_id = 147
ORDER BY i.year_number, i.sequence;

COMMIT;
