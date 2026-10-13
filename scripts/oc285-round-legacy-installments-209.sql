-- ============================================================================
--  OC-285: round legacy installment paise artifacts on live 209 (2026-10-13).
--  EXECUTED on live 209 2026-10-13 ~16:52 IST: 16 installments + 8 allocations updated, COMMIT, exit 0.
--  Pre-run backup: /docker_data/backups/cms_pre_oc285_rounding_20261013_165147.dump
--  One-off prod data fix -- NOT a Flyway migration; do not re-run elsewhere.
--
--  Requested by the business: term fees and the yearly total should be whole rupees and the two
--  terms of a year equal (e.g. 87,499.00 / 87,500.99 -> 87,500 / 87,500).
--  The paise drift is old (identical in every backup before the 13 Oct switches) -- it came from the
--  original year-fee split, not from the boarding-status switch bug.
--
--  Scope: 8 students, 16 installment_fees rows + their 8 student_fee_allocations
--  (total_fee / net_fee rounded so they keep equalling the sum of the installments).
--  DELIBERATELY EXCLUDED:
--    * 2023650048 DURAIPANDI S (alloc 171): both Y4 terms fully paid at 79,999.00 / 80,000.99;
--      rounding to 80,000 each would show a 1.00 shortfall + 0.99 excess. Needs a business decision.
--    * 2024650012 ANSHAD C M (alloc 79): genuine 20,000 difference, not rounding.
--  No payment rows (fee_installments) are touched.
--
--  Guards: every UPDATE matches on the CURRENT exact amount (re-run = no-op); aborts unless all 16
--  installments and all 8 allocations end at their target values and each allocation's total still
--  equals the sum of its installments.
-- ============================================================================
\set ON_ERROR_STOP on
BEGIN;

CREATE TEMP TABLE _inst (id bigint PRIMARY KEY, alloc bigint, old_amt numeric, new_amt numeric) ON COMMIT DROP;
INSERT INTO _inst VALUES
  (279, 35, 114999.99, 115000), (280, 35, 115000.00, 115000),
  (519, 65,  99999.99, 100000), (520, 65, 100000.00, 100000),
  (593, 75, 135000.00, 135000), (594, 75, 135000.01, 135000),
  (687, 86, 100000.00, 100000), (688, 86, 100000.01, 100000),
  (727, 91,  99999.99, 100000), (728, 91, 100000.00, 100000),
  (863, 108, 87499.00,  87500), (864, 108, 87500.99,  87500),
  (1983, 248, 64999.00, 65000), (1984, 248, 65000.99, 65000),
  (2087, 261, 67924.00, 67925), (2088, 261, 67925.98, 67925);

CREATE TEMP TABLE _alloc (id bigint PRIMARY KEY, old_total numeric, new_total numeric) ON COMMIT DROP;
INSERT INTO _alloc VALUES
  (35, 984999.99, 985000), (65, 869999.99, 870000), (75, 870000.01, 870000), (86, 920000.01, 920000),
  (91, 869999.99, 870000), (108, 699999.99, 700000), (248, 499999.99, 500000), (261, 519999.98, 520000);

-- Pre-check (read-only)
SELECT a.id alloc, a.total_fee, a.net_fee, a.discount_amount,
       (SELECT sum(amount) FROM installment_fees i WHERE i.allocation_id = a.id) installments_sum
FROM student_fee_allocations a WHERE a.id IN (SELECT id FROM _alloc) ORDER BY a.id;

WITH u AS (
  UPDATE installment_fees i SET amount = t.new_amt, updated_at = now()
    FROM _inst t WHERE i.id = t.id AND i.allocation_id = t.alloc AND i.amount = t.old_amt
  RETURNING i.id)
SELECT count(*) AS installments_updated FROM u \gset

WITH u AS (
  UPDATE student_fee_allocations a SET total_fee = t.new_total, net_fee = t.new_total, updated_at = now()
    FROM _alloc t WHERE a.id = t.id AND a.total_fee = t.old_total AND a.net_fee = t.old_total
                    AND a.discount_amount = 0
  RETURNING a.id)
SELECT count(*) AS allocations_updated FROM u \gset

DO $$
DECLARE bad int;
BEGIN
  SELECT count(*) INTO bad FROM _inst t JOIN installment_fees i ON i.id = t.id WHERE i.amount <> t.new_amt;
  IF bad <> 0 THEN RAISE EXCEPTION 'Aborted: % installment(s) not at target amount', bad; END IF;
  SELECT count(*) INTO bad FROM _alloc t JOIN student_fee_allocations a ON a.id = t.id
   WHERE a.total_fee <> t.new_total OR a.net_fee <> t.new_total
      OR a.total_fee <> (SELECT sum(i.amount) FROM installment_fees i WHERE i.allocation_id = a.id);
  IF bad <> 0 THEN RAISE EXCEPTION 'Aborted: % allocation(s) wrong or total <> sum of installments', bad; END IF;
END $$;

\echo installments_updated :installments_updated allocations_updated :allocations_updated

-- Post-check (read-only)
SELECT s.admission_number adm, i.year_number yr, i.sequence term, i.amount
FROM installment_fees i JOIN student_fee_allocations a ON a.id = i.allocation_id JOIN students s ON s.id = a.student_id
WHERE i.id IN (SELECT id FROM _inst) ORDER BY s.admission_number, i.sequence;
SELECT a.id alloc, a.total_fee, a.net_fee, (SELECT sum(amount) FROM installment_fees i WHERE i.allocation_id=a.id) installments_sum
FROM student_fee_allocations a WHERE a.id IN (SELECT id FROM _alloc) ORDER BY a.id;

COMMIT;
