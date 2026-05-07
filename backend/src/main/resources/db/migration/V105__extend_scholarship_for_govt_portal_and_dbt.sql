-- V105: Extends scholarship tables to match Tamil Nadu / Indian govt portal requirements.
--
-- Adds:
--   • scholarship_types: application_mode, portal_name, portal_url, eligible_from_year, eligible_to_year
--   • student_scholarship_eligibility: aadhaar_number, bank_account_number, bank_ifsc, bank_name,
--                                       bank_branch, dbt_linked
--   • student_scholarships: govt_sanction_number, sanction_date, sanctioned_by
--
-- Rationale: Govt schemes (NSP, ePass TN, TNSMS) require Aadhaar-linked bank for DBT
-- (Direct Benefit Transfer) disbursement. Some schemes are restricted by year of study
-- (e.g. First Graduate is typically Year-1 only). Sanction tracking is needed because
-- "Approved by college" and "Sanctioned by govt portal" are two distinct steps.

ALTER TABLE scholarship_types
    ADD COLUMN application_mode    VARCHAR(20) NOT NULL DEFAULT 'INSTITUTION',
    ADD COLUMN portal_name         VARCHAR(50),
    ADD COLUMN portal_url          VARCHAR(255),
    ADD COLUMN eligible_from_year  INTEGER,
    ADD COLUMN eligible_to_year    INTEGER;

ALTER TABLE student_scholarship_eligibility
    ADD COLUMN aadhaar_number      VARCHAR(12),
    ADD COLUMN bank_account_number VARCHAR(30),
    ADD COLUMN bank_ifsc           VARCHAR(15),
    ADD COLUMN bank_name           VARCHAR(100),
    ADD COLUMN bank_branch         VARCHAR(100),
    ADD COLUMN dbt_linked          BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE student_scholarships
    ADD COLUMN govt_sanction_number VARCHAR(50),
    ADD COLUMN sanction_date        DATE,
    ADD COLUMN sanctioned_by        VARCHAR(100);

-- ── Update seeded scholarship types with realistic TN metadata ────────────────
UPDATE scholarship_types SET application_mode = 'GOVT_PORTAL', portal_name = 'ePass Tamil Nadu',
    portal_url = 'https://tnepass.tn.gov.in', eligible_from_year = 1
    WHERE code = 'SC_GOVT';

UPDATE scholarship_types SET application_mode = 'GOVT_PORTAL', portal_name = 'ePass Tamil Nadu',
    portal_url = 'https://tnepass.tn.gov.in', eligible_from_year = 1
    WHERE code = 'ST_GOVT';

UPDATE scholarship_types SET application_mode = 'GOVT_PORTAL', portal_name = 'NSP',
    portal_url = 'https://scholarships.gov.in', eligible_from_year = 1
    WHERE code = 'OBC_GOVT';

UPDATE scholarship_types SET application_mode = 'GOVT_PORTAL', portal_name = 'ePass Tamil Nadu',
    portal_url = 'https://tnepass.tn.gov.in', eligible_from_year = 1
    WHERE code = 'BC_STATE';

UPDATE scholarship_types SET application_mode = 'INSTITUTION', eligible_from_year = 1, eligible_to_year = 1
    WHERE code = 'FIRST_GRAD';

UPDATE scholarship_types SET application_mode = 'INSTITUTION'
    WHERE code IN ('EWS', 'MERIT');

CREATE INDEX idx_scholarship_types_application_mode ON scholarship_types(application_mode);
CREATE INDEX idx_student_scholarships_sanction_number ON student_scholarships(govt_sanction_number);

