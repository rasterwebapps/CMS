-- ============================================================================
--  OC-291: correct Year-1 and Year-2 fees for STADIL JOSE (student 234,
--  adm 2024650046) in B.Sc Nursing (2024-25 batch) on live 209.
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Scope:
--    1. installment_fees (alloc 138, student 234):
--         id 1097 (Year 1 - First Semester):  150000.00 -> 125000.00
--         id 1098 (Year 1 - Second Semester): 150000.00 -> 125000.00
--         id 1099 (Year 2 - First Semester):  125000.00 -> 100000.00
--         id 1100 (Year 2 - Second Semester): 125000.00 -> 100000.00
--         (Years 3-4 remain 100000.00 per semester)
--    2. student_fee_allocations (id 138, student 234):
--         total_fee / net_fee: 1050000.00 -> 850000.00 (matches sum of installments)
--    3. fee_installments & payment_receipts (student 234):
--         Re-aligns payment rows across semesters so per-semester paid &
--         outstanding reflect the ₹2,50,000 Year-1 fee and ₹2,00,000 Year-2 fee,
--         carrying ₹50,000 from Year 1 into Year 2 - First Semester (id 1099 ->
--         85000.00 paid, 15000.00 outstanding), while keeping total paid at
--         ₹3,35,000.00 and every receipt total unchanged:
--         - RCP-2025-00492 (100,000 on 2025-06-04):
--             id 1889 (on 1097): 100000.00 -> 75000.00
--             insert split row on 1098: 25000.00
--             receipt 1550 -> 'Year 1 - First Semester, Year 1 - Second Semester'
--         - RCP-2026-00372 (90,000 on 2026-03-24):
--             id 1891 (on 1098): 55000.00 -> 5000.00
--             id 1892 (on 1099): 35000.00 -> 85000.00
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 138 AND a.student_id = 234;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount
FROM installment_fees i
WHERE i.allocation_id = 138
ORDER BY i.year_number, i.sequence;

-- 1. Update Year 1 (1097, 1098: 150000 -> 125000) & Year 2 (1099, 1100: 125000 -> 100000)
UPDATE installment_fees
   SET amount = 125000.00,
       updated_at = now()
 WHERE id IN (1097, 1098)
   AND allocation_id = 138
   AND amount = 150000.00;

UPDATE installment_fees
   SET amount = 100000.00,
       updated_at = now()
 WHERE id IN (1099, 1100)
   AND allocation_id = 138
   AND amount = 125000.00;

-- 2. Update student_fee_allocations (id 138: 1050000 -> 850000)
UPDATE student_fee_allocations
   SET total_fee = 850000.00,
       net_fee = 850000.00,
       updated_at = now()
 WHERE id = 138
   AND student_id = 234
   AND total_fee = 1050000.00
   AND net_fee = 1050000.00
   AND discount_amount = 0.00;

-- 3. Re-align fee_installments and payment_receipts
-- 3a. RCP-2025-00492 (100,000 -> 75,000 on 1097 + 25,000 on 1098)
UPDATE fee_installments
   SET amount_paid = 75000.00,
       updated_at = now()
 WHERE id = 1889 AND student_id = 234 AND semester_fee_id = 1097
   AND receipt_number = 'RCP-2025-00492' AND amount_paid = 100000.00;

INSERT INTO fee_installments (
    semester_fee_id, student_id, amount_paid, payment_date, payment_mode,
    receipt_number, transaction_reference, remarks, created_at, updated_at
)
SELECT 1098, student_id, 25000.00, payment_date, payment_mode,
       receipt_number, transaction_reference, remarks, created_at, now()
  FROM fee_installments
 WHERE id = 1889
   AND NOT EXISTS (
       SELECT 1 FROM fee_installments
        WHERE student_id = 234 AND semester_fee_id = 1098 AND receipt_number = 'RCP-2025-00492'
   );

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - First Semester, Year 1 - Second Semester'
 WHERE id = 1550 AND payer_type = 'STUDENT' AND payer_id = 234
   AND receipt_number = 'RCP-2025-00492' AND amount_paid = 100000.00;

-- 3b. RCP-2026-00372 (90,000 -> 5,000 on 1098 + 85,000 on 1099)
UPDATE fee_installments
   SET amount_paid = 5000.00,
       updated_at = now()
 WHERE id = 1891 AND student_id = 234 AND semester_fee_id = 1098
   AND receipt_number = 'RCP-2026-00372' AND amount_paid = 55000.00;

UPDATE fee_installments
   SET amount_paid = 85000.00,
       updated_at = now()
 WHERE id = 1892 AND student_id = 234 AND semester_fee_id = 1099
   AND receipt_number = 'RCP-2026-00372' AND amount_paid = 35000.00;

-- In-transaction invariant assertions (abort and rollback on any discrepancy)
DO $$
DECLARE
    v_total_fee numeric;
    v_net_fee numeric;
    v_inst_sum numeric;
    v_total_paid numeric;
    v_y1s1_amt numeric;
    v_y1s2_amt numeric;
    v_y2s1_amt numeric;
    v_y2s2_amt numeric;
    v_y1s1_paid numeric;
    v_y1s2_paid numeric;
    v_y2s1_paid numeric;
    v_y2s2_paid numeric;
BEGIN
    SELECT total_fee, net_fee INTO v_total_fee, v_net_fee
      FROM student_fee_allocations WHERE id = 138 AND student_id = 234;
    SELECT sum(amount) INTO v_inst_sum
      FROM installment_fees WHERE allocation_id = 138;
    SELECT sum(amount_paid) INTO v_total_paid
      FROM fee_installments WHERE student_id = 234 AND refunded_at IS NULL;

    IF v_total_fee <> 850000.00 OR v_net_fee <> 850000.00 OR v_inst_sum <> 850000.00 THEN
        RAISE EXCEPTION 'Aborted: allocation/installment sum mismatch (total_fee=%, net_fee=%, inst_sum=%)',
            v_total_fee, v_net_fee, v_inst_sum;
    END IF;

    IF v_total_paid <> 335000.00 THEN
        RAISE EXCEPTION 'Aborted: total_paid changed from 335000.00 to %', v_total_paid;
    END IF;

    SELECT amount INTO v_y1s1_amt FROM installment_fees WHERE id = 1097;
    SELECT amount INTO v_y1s2_amt FROM installment_fees WHERE id = 1098;
    SELECT amount INTO v_y2s1_amt FROM installment_fees WHERE id = 1099;
    SELECT amount INTO v_y2s2_amt FROM installment_fees WHERE id = 1100;
    IF v_y1s1_amt <> 125000.00 OR v_y1s2_amt <> 125000.00 OR v_y2s1_amt <> 100000.00 OR v_y2s2_amt <> 100000.00 THEN
        RAISE EXCEPTION 'Aborted: Year 1/2 installments not at target amounts (%, %, %, %)',
            v_y1s1_amt, v_y1s2_amt, v_y2s1_amt, v_y2s2_amt;
    END IF;

    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s1_paid FROM fee_installments WHERE semester_fee_id = 1097 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y1s2_paid FROM fee_installments WHERE semester_fee_id = 1098 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y2s1_paid FROM fee_installments WHERE semester_fee_id = 1099 AND refunded_at IS NULL;
    SELECT COALESCE(sum(amount_paid), 0) INTO v_y2s2_paid FROM fee_installments WHERE semester_fee_id = 1100 AND refunded_at IS NULL;

    IF v_y1s1_paid <> 125000.00 OR v_y1s2_paid <> 125000.00 OR v_y2s1_paid <> 85000.00 OR v_y2s2_paid <> 0.00 THEN
        RAISE EXCEPTION 'Aborted: unexpected per-semester paid amounts (%, %, %, %)',
            v_y1s1_paid, v_y1s2_paid, v_y2s1_paid, v_y2s2_paid;
    END IF;
END $$;

-- Post-check (read-only)
SELECT a.id AS alloc_id, a.student_id, a.total_fee, a.net_fee,
       (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id) AS installments_sum,
       (SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.student_id = a.student_id AND fi.refunded_at IS NULL) AS total_paid
FROM student_fee_allocations a
WHERE a.id = 138 AND a.student_id = 234;

SELECT i.id, i.year_number, i.sequence, i.installment_label, i.amount AS fee_amount,
       COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS paid_amount,
       i.amount - COALESCE((SELECT sum(fi.amount_paid) FROM fee_installments fi WHERE fi.semester_fee_id = i.id AND fi.refunded_at IS NULL), 0) AS outstanding_amount
FROM installment_fees i
WHERE i.allocation_id = 138
ORDER BY i.year_number, i.sequence;

COMMIT;
