-- V498: SKSCON currently runs the full academic program on 13 teaching staff. Local dev only
-- had 10 active faculty (FAC009-FAC018 -- V45's FAC001-FAC008 never persisted here, since those
-- rows matched specialities by code 'MO'/'PN', which don't exist in this environment's
-- specialities table: GN/CHN/CHDN/MHN/OBGN/MSN). Adds 3 more to reach 13, filling the
-- specialities (CHDN, OBGN, MSN) that were thinnest, so the workload dashboard has real
-- speciality-eligible coverage to auto-assign across. Idempotent: ON CONFLICT (employee_code)
-- DO NOTHING, matches the V45/V296 seed-data pattern. Authored as V475 and renumbered to V498
-- before it was ever committed -- see V499's header for why.

WITH new_faculty (employee_code, first_name, last_name, email, phone, speciality_code, designation_code, specialization, lab_expertise, joining_date) AS (
    VALUES
        ('FAC019', 'Shalini', 'Bhat',   'shalini.bhat@cms.edu',  '9876500019', 'CHDN', 'LECTURER',            'Child Health Nursing',                'Pediatric Lab', '2023-06-01'::date),
        ('FAC020', 'Anand',   'Verma',  'anand.verma@cms.edu',   '9876500020', 'OBGN', 'ASSISTANT_PROFESSOR', 'Obstetrics and Gynaecological Nursing', 'Obstetric Lab', '2022-09-01'::date),
        ('FAC021', 'Sneha',   'Rao',    'sneha.rao@cms.edu',     '9876500021', 'MSN',  'LAB_INSTRUCTOR',      'Medical-Surgical Nursing',            'Skills Lab',    '2021-11-01'::date)
)
INSERT INTO faculty (employee_code, first_name, last_name, email, phone, speciality_id, designation_id, specialization, lab_expertise, joining_date, status, country_id, created_at, updated_at)
SELECT nf.employee_code, nf.first_name, nf.last_name, nf.email, nf.phone,
       s.id, d.id, nf.specialization, nf.lab_expertise, nf.joining_date, 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM new_faculty nf
JOIN specialities s ON s.code = nf.speciality_code
JOIN designations d ON d.code = nf.designation_code
ON CONFLICT (employee_code) DO NOTHING;
