-- V296: Seed additional faculty records so Course Offerings has a fuller pool to assign from.
-- Idempotent: ON CONFLICT (employee_code) DO NOTHING, matches the V45 seed-data pattern.
-- Speciality is assigned by round-robin over whichever specialities actually exist in this
-- environment (never assume the V45 demo set of 5 -- some environments only have one).

WITH spec_ranked AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn, COUNT(*) OVER () AS cnt
    FROM specialities
),
new_faculty (employee_code, first_name, last_name, email, phone, designation_code, specialization, lab_expertise, joining_date, seq) AS (
    VALUES
        ('FAC009', 'Meera',   'Iyer',        'meera.iyer@cms.edu',         '9876500009', 'SENIOR_LECTURER',     'Obstetric Nursing',        'Obstetric Lab',          '2017-07-01'::date, 1),
        ('FAC010', 'Arjun',   'Menon',       'arjun.menon@cms.edu',        '9876500010', 'ASSOCIATE_PROFESSOR', 'Community Nursing',        NULL,                     '2019-02-15'::date, 2),
        ('FAC011', 'Divya',   'Krishnan',    'divya.krishnan@cms.edu',     '9876500011', 'PROFESSOR',           'Medical-Surgical Nursing', 'Skills Lab',             '2016-11-01'::date, 3),
        ('FAC012', 'Karthik', 'Subramaniam', 'karthik.subramaniam@cms.edu','9876500012', 'ASSISTANT_PROFESSOR', 'Pediatric Nursing',        NULL,                     '2020-08-10'::date, 4),
        ('FAC013', 'Swathi',  'Reddy',       'swathi.reddy@cms.edu',       '9876500013', 'LECTURER',            'General Nursing',          'Nursing Foundation Lab', '2021-06-01'::date, 5),
        ('FAC014', 'Vikram',  'Das',         'vikram.das@cms.edu',         '9876500014', 'TEACHING_ASSISTANT',  'Midwifery',                NULL,                     '2022-04-01'::date, 6),
        ('FAC015', 'Nandini', 'Pillai',      'nandini.pillai@cms.edu',     '9876500015', 'NURSING_TUTOR',       'Community Health Nursing', NULL,                     '2023-01-15'::date, 7),
        ('FAC016', 'Harish',  'Gowda',       'harish.gowda@cms.edu',       '9876500016', 'GUEST_FACULTY',       'Medical-Surgical Nursing', 'Anatomy Lab',            '2018-09-01'::date, 8),
        ('FAC017', 'Pooja',   'Shetty',      'pooja.shetty@cms.edu',       '9876500017', 'VISITING_FACULTY',    'Pediatric Nursing',        'Skills Lab',             '2019-05-20'::date, 9),
        ('FAC018', 'Naveen',  'Kumar',       'naveen.kumar@cms.edu',       '9876500018', 'ASSOCIATE_PROFESSOR', 'General Nursing',          NULL,                     '2024-03-01'::date, 10)
)
INSERT INTO faculty (employee_code, first_name, last_name, email, phone, speciality_id, designation_id, specialization, lab_expertise, joining_date, status, country_id, created_at, updated_at)
SELECT nf.employee_code, nf.first_name, nf.last_name, nf.email, nf.phone,
       sr.id, d.id, nf.specialization, nf.lab_expertise, nf.joining_date, 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM new_faculty nf
JOIN designations d ON d.code = nf.designation_code
JOIN spec_ranked sr ON sr.rn = ((nf.seq - 1) % sr.cnt) + 1
ON CONFLICT (employee_code) DO NOTHING;
