-- ============================================================================
--  OC-289: correct Year-1 fee for SHEFIN ANTO SHAJI (student 228, adm 2024650042)
--  in B.Sc Nursing (2024-25 batch) on live 209.
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Pre-run backup: /docker_data/backups/cms_pre_shefin_year1fix_20261023_172044.dump
--
--  Scope:
--    1. installment_fees (alloc 132, student 228):
--         id 1049 (Year 1 - First Semester):  150000.00 -> 135000.00
--         id 1050 (Year 1 - Second Semester): 150000.00 -> 135000.00
--         (Year 2: 100000 + 100000; Year 3: 85000 + 85000; Year 4: 85000 + 85000)
--    2. student_fee_allocations (id 132, student 228):
--         total_fee / net_fee: 900000.00 -> 810000.00 (matches sum of installments)
--         has_hostel_fee: true -> false (student_type = DAY_SCHOLAR)
--    3. fee_installments & payment_receipts (student 228):
--         Re-aligns payment rows across semesters so per-semester paid &
--         outstanding reflect the ₹2,70,000 Year-1 fee and carry the ₹30,000
--         into Year 2 - Second Semester (id 1052), while keeping total paid at
--         ₹4,00,000.00 and every receipt total unchanged:
--         - RCP-2024-00337 (70,000 on 2024-09-23):
--             id 1863 (on 1049): 70000.00 -> 55000.00
--             insert split row on 1050: 15000.00
--             receipt 1529 -> 'Year 1 - First Semester, Year 1 - Second Semester'
--         - RCP-2025-00486 (50,000 on 2025-10-22):
--             id 1864 (on 1050): 50000.00 -> 20000.00
--             insert split row on 1051: 30000.00
--             receipt 1530 -> 'Year 1 - Second Semester, Year 2 - First Semester'
--         - RCP-2026-00368 (50,000 on 2026-06-01):
--             id 1868 (on 1051): 50000.00 -> 20000.00
--             insert split row on 1052: 30000.00
--             receipt 1534 -> 'Year 2 - First Semester, Year 2 - Second Semester'
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee, a.has_hostel_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 132 AND a.student_id = 228;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount
FROM installment_fees i
WHERE i.allocation_id = 132
ORDER BY i.year_number, i.sequence;

-- 1. Update Year 1 installment_fees (1049, 1050: 150000 -> 135000)
UPDATE installment_fees
   SET amount = 135000.00,
       updated_at = now()
 WHERE id IN (1049, 1050)
   AND allocation_id = 132
   AND amount = 150000.00;

-- 2. Update student_fee_allocations (id 132: 900000 -> 810000, has_hostel_fee -> false)
UPDATE student_fee_allocations
   SET total_fee = 810000.00,
       net_fee = 810000.00,
       has_hostel_fee = false,
       updated_at = now()
 WHERE id = 132
   AND student_id = 228
   AND total_fee = 900000.00
   AND net_fee = 900000.00
   AND discount_amount = 0.00;

-- 3. Re-align fee_installments and payment_receipts
-- 3a. RCP-2024-00337 (70,000 -> 55,000 on 1049 + 15,000 on 1050)
UPDATE fee_installments
   SET amount_paid = 55000.00,
       updated_at = now()
 WHERE id = 1863 AND student_id = 228 AND semester_fee_id = 1049
   AND receipt_number = 'RCP-2024-00337' AND amount_paid = 70000.00;

INSERT INTO fee_installments (
    semester_fee_id, student_id, amount_paid, payment_date, payment_mode,
    receipt_number, transaction_reference, remarks, created_at, updated_at
)
SELECT 1050, student_id, 15000.00, payment_date, payment_mode,
       receipt_number, transaction_reference, remarks, created_at, now()
  FROM fee_installments
 WHERE id = 1863
   AND NOT EXISTS (
       SELECT 1 FROM fee_installments
        WHERE student_id = 228 AND semester_fee_id = 1050 AND receipt_number = 'RCP-2024-00337'
   );

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - First Semester, Year 1 - Second Semester'
 WHERE id = 1529 AND payer_type = 'STUDENT' AND payer_id = 228
   AND receipt_number = 'RCP-2024-00337' AND amount_paid = 70000.00;

-- 3b. RCP-2025-00486 (50,000 -> 20,000 on 1050 + 30,000 on 1051)
UPDATE fee_installments
   SET amount_paid = 20000.00,
       updated_at = now()
 WHERE id = 1864 AND student_id = 228 AND semester_fee_id = 1050
   AND receipt_number = 'RCP-2025-00486' AND amount_paid = 50000.00;

INSERT INTO fee_installments (
    semester_fee_id, student_id, amount_paid, payment_date, payment_mode,
    receipt_number, transaction_reference, remarks, created_at, updated_at
)
SELECT 1051, student_id, 30000.00, payment_date, payment_mode,
       receipt_number, transaction_reference, remarks, created_at, now()
  FROM fee_installments
 WHERE id = 1864
   AND NOT EXISTS (
       SELECT 1 FROM fee_installments
        WHERE student_id = 228 AND semester_fee_id = 1051 AND receipt_number = 'RCP-2025-00486'
   );

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - Second Semester, Year 2 - First Semester'
 WHERE id = 1530 AND payer_type = 'STUDENT' AND payer_id = 228
   AND receipt_number = 'RCP-2025-00486' AND amount_paid = 50000.00;

-- 3c. RCP-2026-00368 (50,000 -> 20,000 on 1051 + 30,000 on 1052)
UPDATE fee_installments
   SET amount_paid = 20000.00,
       updated_at = now()
 WHERE id = 1868 AND student_id = 228 AND semester_fee_id = 1051
   AND receipt_number = 'RCP-2026-00368' AND amount_paid = 50000.00;

INSERT INTO fee_installments (
    semester_fee_id, student_id, amount_paid, payment_date, payment_mode,
    receipt_number, transaction_reference, remarks, created_at, updated_at
)
SELECT 1052, student_id, 30000.00, payment_date, payment_mode,
       receipt_number, transaction_reference, remarks, created_at, now()
  FROM fee_installments
 WHERE id = 1868
   AND NOT EXISTS (
       SELECT 1 FROM fee_installments
        WHERE student_id = 228 AND semester_fee_id = 1052 AND receipt_number = 'RCP-2026-00368'
   );

UPDATE payment_receipts
   SET installments_covered = 'Year 2 - First Semester, Year 2 - Second Semester'
 WHERE id = 1534 AND payer_type = 'STUDENT' AND payer_id = 228
   AND receipt_number = 'RCP-2026-00368' AND amount_paid = 50000.00;

-- In-transaction invariant assertions (abort and rollback on any discrepancy)
DO $$
DECLARE
    v_total_fee numeric;
    v_net_fee numeric;
    v_has_hostel boolean;
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
    SELECT total_fee, net_fee, has_hostel_fee INTO v_total_fee, v_net_fee, v_has_hostel
      FROM student_fee_allocations WHERE id = 132 AND student_id = 228;
    SELECT sum(amount) INTO v_inst_sum
      FROM installment_fees WHERE allocation_id = 132;
    SELECT sum(amount_paid) INTO v_total_paid
      FROM fee_installments WHERE student_id = 228 AND refunded_at IS NULL;

    IF v_total_fee <> 810000.00 OR v_net_fee <> 810000.00 OR v_inst_sum <> 810000.00 OR v_has_hostel <> false THEN
        RAISE EXCEPTION 'Aborted: allocation/installment sum mismatch (total_fee=%, net_fee=%, inst_sum=%, has_hostel=%)',
            v_total_fee, v_net_fee, v_inst_sum, v_has_hostel;
    END IF;

    IF v_total_paid <> 400000.00 THEN
        RAISE EXCEPTION 'Aborted: total_paid changed from 400000.00 to %', v_total_paid;
    END IF;

    SELECT amount INTO v_y1s1_amt FROM installment_fees WHERE id = 1049;
    SELECT amount INTO v_y1s2_amt FROM installment_fees WHERE id = 1050;
    IF v_y1s1_amt <> 135000.00 OR v_y1s2_amt <> 135000.00 THEN
        RAISE EXCEPTION 'Aborted: Year 1 installments not at 135000.00 (%, %)', v_y1s1_amt, v_y1s2_amt;
    END IF;

    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s1_paid FROM fee_installments WHERE semester_fee_id = 1049 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s2_paid FROM fee_installments WHERE semester_fee_id = 1050 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y2s1_paid FROM fee_installments WHERE semester_fee_id = 1051 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y2s2_paid FROM fee_installments WHERE semester_fee_id = 1052 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y3s1_paid FROM fee_installments WHERE semester_fee_id = 1053 AND refunded_at IS NULL;

    IF v_y1s1_paid <> 135000.00 OR v_y1s2_paid <> 135000.00 OR v_y2s1_paid <> 100000.00 OR v_y2s2_paid <> 30000.00 OR v_y3s1_paid <> 0.00 THEN
        RAISE EXCEPTION 'Aborted: unexpected per-semester paid amounts (%, %, %, %, %)',
            v_y1s1_paid, v_y1s2_paid, v_y2s1_paid, v_y2s2_paid, v_y3s1_paid;
    END IF;
END $$;

-- Post-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee, a.has_hostel_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 132 AND a.student_id = 228;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount AS fee_amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount,
       i.amount - COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS outstanding_amount
FROM installment_fees i
WHERE i.allocation_id = 132
ORDER BY i.year_number, i.sequence;

COMMIT;
