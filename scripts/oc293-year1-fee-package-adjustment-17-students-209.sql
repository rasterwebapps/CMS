-- ============================================================================
--  OC-293: Correct Year-1 Fee Package for 17 Students (2025-26 Batch) on Live 209
--  per admin-supplied fee sheet (first_year_fee_package_adjustment_list.xls)
--
--  One-off production data correction -- NOT a Flyway migration; do not re-run
--  elsewhere. Production data -- see CLAUDE.md data-safety rules.
--
--  Pre-run backup: /docker_data/backups/cms_pre_year1_fee_adjust_20261029_141733.dump
--
--  Target Year-1 Fee Packages:
--    - Male students   (12): ₹1,90,000 (Sem 1: ₹95,000, Sem 2: ₹95,000)
--    - Female students ( 5): ₹1,80,000 (Sem 1: ₹90,000, Sem 2: ₹90,000)
--
--  Scope:
--    1. installment_fees (34 rows across 17 students):
--       - 24 rows for 12 male students: 147,500.00 -> 95,000.00 each
--       - 10 rows for 5 female students: 147,500.00 -> 90,000.00 each
--    2. student_fee_allocations (17 rows):
--       - 10 male students: 1,015,000.00 -> 910,000.00
--       - 1 male student (AMAL KRISHNA A): 985,000.00 -> 910,000.00 (resolves pre-existing drift)
--       - 1 male student (ADHIL D): 985,000.00 -> 880,000.00
--       - 5 female students: 985,000.00 -> 870,000.00 each
--    3. Payment Re-alignments for 3 students with payments beyond Year 1 Sem 1:
--       - ELJO ABY (Total paid: ₹1,95,000):
--         * RCP-1627 (150k): 85k on Y1S1 (145), 65k on Y1S2 (146)
--         * RCP-2026-00866 (30k): 25k on Y1S2 (146), 5k carried to Y2S1 (147)
--         * fee_demands id 114 (Term 3): paid_amount 0 -> 5,000.00, status -> PARTIAL
--       - NANDHANA S (Total paid: ₹1,90,000):
--         * RCP-2025-00642 (65k): consolidated onto Y1S1 (273)
--         * RCP-2026-00636 (100k): 90k on Y1S2 (274), 10k carried to Y2S1 (275)
--         * fee_demands id 1 (Term 3): paid_amount 0 -> 10,000.00, status -> PARTIAL
--       - ADHIL D (Total paid: ₹2,55,000):
--         * RCP-2025-00643 (45k): moved to Y1S1 (321), completing Y1S1 at ₹95,000
--         * RCP-2026-00555 (160k): 95k on Y1S2 (322), 65k carried to Y2S1 (323)
--         * fee_demands id 134 (Term 3): paid_amount 0 -> 65,000.00, status -> PARTIAL
--
--  Guards & Invariants:
--    - Transaction aborted and rolled back if any installment, allocation,
--      or payment allocation does not match expected target amounts.
-- ============================================================================
\set ON_ERROR_STOP on

BEGIN;

-- Pre-check (read-only): current state of 34 installment rows + 17 allocations
SELECT i.id, a.student_id, s.admission_number, s.first_name, i.installment_label, i.amount AS current_amount
FROM installment_fees i
JOIN student_fee_allocations a ON a.id = i.allocation_id
JOIN students s ON s.id = a.student_id
WHERE s.admission_number IN (
    '2025650046', '2025650048', '2025650015', '2025650018', '2025650019',
    '2025650043', '2025650040', '2025650051', '2025650049', '2025650058',
    '2025650034', '2025650035', '2025650041', '2025650047', '2025650059',
    '2025650064', '2025650060'
) AND i.year_number = 1
ORDER BY s.admission_number, i.sequence;

CREATE TEMP TABLE _inst (id bigint PRIMARY KEY, alloc_id bigint, student_id bigint, old_amt numeric, new_amt numeric) ON COMMIT DROP;
INSERT INTO _inst (id, alloc_id, student_id, old_amt, new_amt) VALUES
    (361, 46, 52, 147500.00, 95000.00) /* AKSHAY S Y1 Sem 1 */,
    (362, 46, 52, 147500.00, 95000.00) /* AKSHAY S Y1 Sem 2 */,
    (377, 48, 54, 147500.00, 95000.00) /* ABHIJITH S Y1 Sem 1 */,
    (378, 48, 54, 147500.00, 95000.00) /* ABHIJITH S Y1 Sem 2 */,
    (113, 15, 16, 147500.00, 95000.00) /* ABHISHEK P S Y1 Sem 1 */,
    (114, 15, 16, 147500.00, 95000.00) /* ABHISHEK P S Y1 Sem 2 */,
    (137, 18, 19, 147500.00, 95000.00) /* ABIN THOMAS Y1 Sem 1 */,
    (138, 18, 19, 147500.00, 95000.00) /* ABIN THOMAS Y1 Sem 2 */,
    (145, 19, 20, 147500.00, 95000.00) /* ELJO ABY Y1 Sem 1 */,
    (146, 19, 20, 147500.00, 95000.00) /* ELJO ABY Y1 Sem 2 */,
    (337, 43, 49, 147500.00, 95000.00) /* ASHIK A Y1 Sem 1 */,
    (338, 43, 49, 147500.00, 95000.00) /* ASHIK A Y1 Sem 2 */,
    (313, 40, 46, 147500.00, 95000.00) /* ARJUN B S Y1 Sem 1 */,
    (314, 40, 46, 147500.00, 95000.00) /* ARJUN B S Y1 Sem 2 */,
    (401, 51, 61, 147500.00, 95000.00) /* VISHNU SANTHOSH Y1 Sem 1 */,
    (402, 51, 61, 147500.00, 95000.00) /* VISHNU SANTHOSH Y1 Sem 2 */,
    (385, 49, 59, 147500.00, 95000.00) /* AMAL KRISHNA A Y1 Sem 1 */,
    (386, 49, 59, 147500.00, 95000.00) /* AMAL KRISHNA A Y1 Sem 2 */,
    (457, 58, 87, 147500.00, 90000.00) /* ANGEL ANNA CHERIAN Y1 Sem 1 */,
    (458, 58, 87, 147500.00, 90000.00) /* ANGEL ANNA CHERIAN Y1 Sem 2 */,
    (265, 34, 40, 147500.00, 90000.00) /* ANN SARA THOMAS Y1 Sem 1 */,
    (266, 34, 40, 147500.00, 90000.00) /* ANN SARA THOMAS Y1 Sem 2 */,
    (273, 35, 41, 147500.00, 90000.00) /* NANDHANA S Y1 Sem 1 */,
    (274, 35, 41, 147500.00, 90000.00) /* NANDHANA S Y1 Sem 2 */,
    (321, 41, 47, 147500.00, 95000.00) /* ADHIL     D Y1 Sem 1 */,
    (322, 41, 47, 147500.00, 95000.00) /* ADHIL     D Y1 Sem 2 */,
    (369, 47, 53, 147500.00, 95000.00) /* MRUDHUL JOHNSON Y1 Sem 1 */,
    (370, 47, 53, 147500.00, 95000.00) /* MRUDHUL JOHNSON Y1 Sem 2 */,
    (465, 59, 88, 147500.00, 95000.00) /* ADITHYAN C Y1 Sem 1 */,
    (466, 59, 88, 147500.00, 95000.00) /* ADITHYAN C Y1 Sem 2 */,
    (553, 70, 99, 147500.00, 90000.00) /* MANASA. L SAJI Y1 Sem 1 */,
    (554, 70, 99, 147500.00, 90000.00) /* MANASA. L SAJI Y1 Sem 2 */,
    (473, 60, 89, 147500.00, 90000.00) /* SUCHITHRA VC Y1 Sem 1 */,
    (474, 60, 89, 147500.00, 90000.00) /* SUCHITHRA VC Y1 Sem 2 */;

CREATE TEMP TABLE _alloc (id bigint PRIMARY KEY, student_id bigint, old_total numeric, old_net numeric, new_total numeric) ON COMMIT DROP;
INSERT INTO _alloc (id, student_id, old_total, old_net, new_total) VALUES
    (46, 52, 1015000.00, 1015000.00, 910000.00) /* AKSHAY S */,
    (48, 54, 1015000.00, 1015000.00, 910000.00) /* ABHIJITH S */,
    (15, 16, 1015000.00, 1015000.00, 910000.00) /* ABHISHEK P S */,
    (18, 19, 1015000.00, 1015000.00, 910000.00) /* ABIN THOMAS */,
    (19, 20, 1015000.00, 1015000.00, 910000.00) /* ELJO ABY */,
    (43, 49, 1015000.00, 1015000.00, 910000.00) /* ASHIK A */,
    (40, 46, 1015000.00, 1015000.00, 910000.00) /* ARJUN B S */,
    (51, 61, 1015000.00, 1015000.00, 910000.00) /* VISHNU SANTHOSH */,
    (49, 59, 985000.00, 985000.00, 910000.00) /* AMAL KRISHNA A */,
    (58, 87, 985000.00, 985000.00, 870000.00) /* ANGEL ANNA CHERIAN */,
    (34, 40, 985000.00, 985000.00, 870000.00) /* ANN SARA THOMAS */,
    (35, 41, 985000.00, 985000.00, 870000.00) /* NANDHANA S */,
    (41, 47, 985000.00, 985000.00, 880000.00) /* ADHIL     D */,
    (47, 53, 1015000.00, 1015000.00, 910000.00) /* MRUDHUL JOHNSON */,
    (59, 88, 1015000.00, 1015000.00, 910000.00) /* ADITHYAN C */,
    (70, 99, 985000.00, 985000.00, 870000.00) /* MANASA. L SAJI */,
    (60, 89, 985000.00, 985000.00, 870000.00) /* SUCHITHRA VC */;

-- 1. Update installment_fees (34 rows: 17 students x 2 semesters)
WITH upd AS (
    UPDATE installment_fees i
       SET amount = t.new_amt,
           updated_at = now()
      FROM _inst t
     WHERE i.id = t.id
       AND i.allocation_id = t.alloc_id
       AND i.amount = t.old_amt
    RETURNING i.id
)
SELECT count(*) AS installments_updated FROM upd \gset

-- 2. Update student_fee_allocations (17 rows)
WITH upd AS (
    UPDATE student_fee_allocations a
       SET total_fee = t.new_total,
           net_fee = t.new_total,
           updated_at = now()
      FROM _alloc t
     WHERE a.id = t.id
       AND a.student_id = t.student_id
       AND a.total_fee = t.old_total
       AND a.net_fee = t.old_net
    RETURNING a.id
)
SELECT count(*) AS allocations_updated FROM upd \gset

-- 3. Re-align payments for the 3 students with multi-semester / overflow payments

-- 3a. ELJO ABY (student 20):
-- RCP-1627 (150k): FI 53 (137.5k -> 85k on 145) + FI 54 (12.5k -> 65k on 146)
UPDATE fee_installments
   SET amount_paid = 85000.00, updated_at = now()
 WHERE id = 53 AND student_id = 20 AND semester_fee_id = 145 AND receipt_number = '1627' AND amount_paid = 137500.00;

UPDATE fee_installments
   SET amount_paid = 65000.00, updated_at = now()
 WHERE id = 54 AND student_id = 20 AND semester_fee_id = 146 AND receipt_number = '1627' AND amount_paid = 12500.00;

-- RCP-2026-00866 (30k): FI 3069 (30k -> 25k on 146) + insert split row (5k on 147)
UPDATE fee_installments
   SET amount_paid = 25000.00, updated_at = now()
 WHERE id = 3069 AND student_id = 20 AND semester_fee_id = 146 AND receipt_number = 'RCP-2026-00866' AND amount_paid = 30000.00;

INSERT INTO fee_installments (
    semester_fee_id, student_id, amount_paid, payment_date, payment_mode,
    receipt_number, transaction_reference, remarks, created_at, updated_at
)
SELECT 147, student_id, 5000.00, payment_date, payment_mode,
       receipt_number, transaction_reference, remarks, created_at, now()
  FROM fee_installments
 WHERE id = 3069
   AND NOT EXISTS (
       SELECT 1 FROM fee_installments
        WHERE student_id = 20 AND semester_fee_id = 147 AND receipt_number = 'RCP-2026-00866'
   );

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - Second Semester, Year 2 - First Semester'
 WHERE id = 2528 AND payer_type = 'STUDENT' AND payer_id = 20
   AND receipt_number = 'RCP-2026-00866' AND amount_paid = 30000.00;

-- Update fee_demands for ELJO ABY (id 114, Term 3): paid_amount 0 -> 5000, status -> PARTIAL
UPDATE fee_demands
   SET paid_amount = 5000.00, status = 'PARTIAL', updated_at = now()
 WHERE id = 114 AND paid_amount = 0.00 AND status = 'UNPAID';


-- 3b. NANDHANA S (student 41):
-- RCP-2025-00642 (65k): consolidate FI 3078 (22.5k on 273) + FI 3079 (42.5k on 274) -> FI 3078 = 65k on 273, delete FI 3079
DELETE FROM fee_installments
 WHERE id = 3079 AND student_id = 41 AND semester_fee_id = 274 AND receipt_number = 'RCP-2025-00642' AND amount_paid = 42500.00;

UPDATE fee_installments
   SET amount_paid = 65000.00, updated_at = now()
 WHERE id = 3078 AND student_id = 41 AND semester_fee_id = 273 AND receipt_number = 'RCP-2025-00642' AND amount_paid = 22500.00;

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - First Semester'
 WHERE id = 2537 AND payer_type = 'STUDENT' AND payer_id = 41
   AND receipt_number = 'RCP-2025-00642' AND amount_paid = 65000.00;

-- RCP-2026-00636 (100k): FI 2855 (100k on 273 -> 90k on 274) + insert split row (10k on 275)
UPDATE fee_installments
   SET semester_fee_id = 274, amount_paid = 90000.00, updated_at = now()
 WHERE id = 2855 AND student_id = 41 AND semester_fee_id = 273 AND receipt_number = 'RCP-2026-00636' AND amount_paid = 100000.00;

INSERT INTO fee_installments (
    semester_fee_id, student_id, amount_paid, payment_date, payment_mode,
    receipt_number, transaction_reference, remarks, created_at, updated_at
)
SELECT 275, student_id, 10000.00, payment_date, payment_mode,
       receipt_number, transaction_reference, remarks, created_at, now()
  FROM fee_installments
 WHERE id = 2855
   AND NOT EXISTS (
       SELECT 1 FROM fee_installments
        WHERE student_id = 41 AND semester_fee_id = 275 AND receipt_number = 'RCP-2026-00636'
   );

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - Second Semester, Year 2 - First Semester'
 WHERE id = 2298 AND payer_type = 'STUDENT' AND payer_id = 41
   AND receipt_number = 'RCP-2026-00636' AND amount_paid = 100000.00;

-- Update fee_demands for NANDHANA S (id 1, Term 3): paid_amount 0 -> 10000, status -> PARTIAL
UPDATE fee_demands
   SET paid_amount = 10000.00, status = 'PARTIAL', updated_at = now()
 WHERE id = 1 AND paid_amount = 0.00 AND status = 'UNPAID';


-- 3c. ADHIL D (student 47):
-- RCP-2025-00643 (45k): FI 3080 (45k on 322 -> move to 321)
UPDATE fee_installments
   SET semester_fee_id = 321, updated_at = now()
 WHERE id = 3080 AND student_id = 47 AND semester_fee_id = 322 AND receipt_number = 'RCP-2025-00643' AND amount_paid = 45000.00;

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - First Semester'
 WHERE id = 2538 AND payer_type = 'STUDENT' AND payer_id = 47
   AND receipt_number = 'RCP-2025-00643' AND amount_paid = 45000.00;

-- RCP-2026-00555 (160k): FI 2757 (97.5k on 321 -> 95k on 322) + FI 2758 (62.5k on 322 -> 65k on 323)
UPDATE fee_installments
   SET semester_fee_id = 322, amount_paid = 95000.00, updated_at = now()
 WHERE id = 2757 AND student_id = 47 AND semester_fee_id = 321 AND receipt_number = 'RCP-2026-00555' AND amount_paid = 97500.00;

UPDATE fee_installments
   SET semester_fee_id = 323, amount_paid = 65000.00, updated_at = now()
 WHERE id = 2758 AND student_id = 47 AND semester_fee_id = 322 AND receipt_number = 'RCP-2026-00555' AND amount_paid = 62500.00;

UPDATE payment_receipts
   SET installments_covered = 'Year 1 - Second Semester, Year 2 - First Semester'
 WHERE id = 2213 AND payer_type = 'STUDENT' AND payer_id = 47
   AND receipt_number = 'RCP-2026-00555' AND amount_paid = 160000.00;

-- Update fee_demands for ADHIL D (id 134, Term 3): paid_amount 0 -> 65000, status -> PARTIAL
UPDATE fee_demands
   SET paid_amount = 65000.00, status = 'PARTIAL', updated_at = now()
 WHERE id = 134 AND paid_amount = 0.00 AND status = 'UNPAID';

-- 4. Invariant Assertions
DO $$
DECLARE
    v_bad int;
    v_total_paid numeric;
BEGIN
    -- Check that all 34 installments match target
    SELECT count(*) INTO v_bad FROM _inst t JOIN installment_fees i ON i.id = t.id WHERE i.amount <> t.new_amt;
    IF v_bad <> 0 THEN
        RAISE EXCEPTION 'Aborted: % installment(s) not at target amount', v_bad;
    END IF;

    -- Check that all 17 allocations match target and sum of installments
    SELECT count(*) INTO v_bad FROM _alloc t JOIN student_fee_allocations a ON a.id = t.id
     WHERE a.total_fee <> t.new_total OR a.net_fee <> t.new_total
        OR a.total_fee <> (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id);
    IF v_bad <> 0 THEN
        RAISE EXCEPTION 'Aborted: % allocation(s) wrong or total_fee <> sum of installments', v_bad;
    END IF;

    -- Check total paid for ELJO ABY (should be 195000.00)
    SELECT sum(amount_paid) INTO v_total_paid FROM fee_installments WHERE student_id = 20 AND refunded_at IS NULL;
    IF v_total_paid <> 195000.00 THEN
        RAISE EXCEPTION 'Aborted: ELJO ABY total paid mismatch: %', v_total_paid;
    END IF;

    -- Check total paid for NANDHANA S (should be 190000.00)
    SELECT sum(amount_paid) INTO v_total_paid FROM fee_installments WHERE student_id = 41 AND refunded_at IS NULL;
    IF v_total_paid <> 190000.00 THEN
        RAISE EXCEPTION 'Aborted: NANDHANA S total paid mismatch: %', v_total_paid;
    END IF;

    -- Check total paid for ADHIL D (should be 255000.00)
    SELECT sum(amount_paid) INTO v_total_paid FROM fee_installments WHERE student_id = 47 AND refunded_at IS NULL;
    IF v_total_paid <> 255000.00 THEN
        RAISE EXCEPTION 'Aborted: ADHIL D total paid mismatch: %', v_total_paid;
    END IF;

    -- Check specific semester paid for ELJO ABY (95k on 145, 95k on 146, 5k on 147)
    IF (SELECT sum(amount_paid) FROM fee_installments WHERE semester_fee_id = 145 AND refunded_at IS NULL) <> 95000.00 OR
       (SELECT sum(amount_paid) FROM fee_installments WHERE semester_fee_id = 146 AND refunded_at IS NULL) <> 95000.00 OR
       (SELECT sum(amount_paid) FROM fee_installments WHERE semester_fee_id = 147 AND refunded_at IS NULL) <> 5000.00 THEN
        RAISE EXCEPTION 'Aborted: ELJO ABY per-semester paid distribution incorrect';
    END IF;

    -- Check specific semester paid for NANDHANA S (90k on 273, 90k on 274, 10k on 275)
    IF (SELECT sum(amount_paid) FROM fee_installments WHERE semester_fee_id = 273 AND refunded_at IS NULL) <> 90000.00 OR
       (SELECT sum(amount_paid) FROM fee_installments WHERE semester_fee_id = 274 AND refunded_at IS NULL) <> 90000.00 OR
       (SELECT sum(amount_paid) FROM fee_installments WHERE semester_fee_id = 275 AND refunded_at IS NULL) <> 10000.00 THEN
        RAISE EXCEPTION 'Aborted: NANDHANA S per-semester paid distribution incorrect';
    END IF;

    -- Check specific semester paid for ADHIL D (95k on 321, 95k on 322, 65k on 323)
    IF (SELECT sum(amount_paid) FROM fee_installments WHERE semester_fee_id = 321 AND refunded_at IS NULL) <> 95000.00 OR
       (SELECT sum(amount_paid) FROM fee_installments WHERE semester_fee_id = 322 AND refunded_at IS NULL) <> 95000.00 OR
       (SELECT sum(amount_paid) FROM fee_installments WHERE semester_fee_id = 323 AND refunded_at IS NULL) <> 65000.00 THEN
        RAISE EXCEPTION 'Aborted: ADHIL D per-semester paid distribution incorrect';
    END IF;
END $$;

\echo 'installments_updated (34 on first run, 0 on re-run): ' :installments_updated
\echo 'allocations_updated (17 on first run, 0 on re-run): ' :allocations_updated

-- Post-check (read-only): summary across all 17 students
SELECT 
    s.admission_number,
    s.first_name || ' ' || s.last_name AS name,
    i.year_number,
    i.sequence,
    i.installment_label,
    i.amount AS fee_amount,
    COALESCE(p.paid_amt, 0) AS paid_amount,
    i.amount - COALESCE(p.paid_amt, 0) AS outstanding_amount
FROM installment_fees i
JOIN student_fee_allocations a ON a.id = i.allocation_id
JOIN students s ON s.id = a.student_id
LEFT JOIN (
    SELECT semester_fee_id, sum(amount_paid) AS paid_amt
    FROM fee_installments
    WHERE refunded_at IS NULL
    GROUP BY semester_fee_id
) p ON p.semester_fee_id = i.id
WHERE s.admission_number IN (
    '2025650046', '2025650048', '2025650015', '2025650018', '2025650019',
    '2025650043', '2025650040', '2025650051', '2025650049', '2025650058',
    '2025650034', '2025650035', '2025650041', '2025650047', '2025650059',
    '2025650064', '2025650060'
)
ORDER BY s.admission_number, i.year_number, i.sequence;

COMMIT;
