-- V45: Insert comprehensive seed data for all screens
-- Idempotent: skips if departments already exist

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM departments LIMIT 1) THEN
        RAISE NOTICE 'Seed data already present – skipping V45.';
        RETURN;
    END IF;

    -- ── 1. Referral Types (ensure seeded) ───────────────────────────────────
    INSERT INTO referral_types (name, code, commission_amount, has_commission, description, is_active, created_at, updated_at)
    VALUES
        ('Walk-In',        'WALK_IN',       0,    FALSE, 'Direct walk-in enquiry',      TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Phone',          'PHONE',         0,    FALSE, 'Phone enquiry',               TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Online',         'ONLINE',        0,    FALSE, 'Online enquiry',              TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Agent Referral', 'AGENT_REFERRAL',5000, TRUE,  'Referred by external agent',  TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Staff',          'STAFF',         2000, TRUE,  'Referred by staff member',    TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Alumni',         'ALUMNI',        1000, TRUE,  'Referred by alumni',          TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Parent',         'PARENT',        0,    FALSE, 'Referred by parent',          TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Advertisement',  'ADVERTISEMENT', 0,    FALSE, 'Through advertisement',       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (code) DO NOTHING;

    -- ── 2. Departments ───────────────────────────────────────────────────────
    INSERT INTO departments (name, code, description, hod_name, created_at, updated_at) VALUES
        ('General Nursing',          'GN',  'Core clinical nursing education and practice',       'Dr. Priya Sharma',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Midwifery & Obstetrics',   'MO',  'Maternal and newborn care education',                'Dr. Lakshmi Devi',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Community Health Nursing', 'CHN', 'Public health and community nursing',                'Dr. Anitha Rao',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Medical-Surgical Nursing', 'MSN', 'Clinical nursing in medical and surgical wards',     'Dr. Rajesh Kumar',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Pediatric Nursing',        'PN',  'Child health nursing and neonatal care',             'Dr. Meena Pillai',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ── 3. Programs ──────────────────────────────────────────────────────────
    INSERT INTO programs (name, code, program_level, duration_years, created_at, updated_at) VALUES
        ('B.Sc Nursing',                   'BSC_NURS', 'UNDERGRADUATE', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('M.Sc Nursing',                   'MSC_NURS', 'POSTGRADUATE',  2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('General Nursing & Midwifery',    'GNM',      'DIPLOMA',       3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ── 4. Program-Department Mappings ──────────────────────────────────────
    INSERT INTO program_departments (program_id, department_id)
    SELECT p.id, d.id FROM programs p, departments d
    WHERE (p.code = 'BSC_NURS' AND d.code IN ('GN', 'MSN', 'PN'))
       OR (p.code = 'MSC_NURS' AND d.code IN ('GN', 'MSN'))
       OR (p.code = 'GNM'      AND d.code IN ('GN', 'MO'));

    -- ── 5. Academic Years ────────────────────────────────────────────────────
    INSERT INTO academic_years (name, start_date, end_date, is_current, created_at, updated_at) VALUES
        ('2023-2024', '2023-06-01'::date, '2024-05-31'::date, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('2024-2025', '2024-06-01'::date, '2025-05-31'::date, TRUE,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('2025-2026', '2025-06-01'::date, '2026-05-31'::date, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ── 6. Semesters ─────────────────────────────────────────────────────────
    INSERT INTO semesters (name, academic_year_id, start_date, end_date, semester_number, created_at, updated_at)
    SELECT 'Odd Semester 2023-2024',  id, '2023-06-01'::date, '2023-11-30'::date, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM academic_years WHERE name = '2023-2024'
    UNION ALL
    SELECT 'Even Semester 2023-2024', id, '2024-01-01'::date, '2024-05-31'::date, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM academic_years WHERE name = '2023-2024'
    UNION ALL
    SELECT 'Odd Semester 2024-2025',  id, '2024-06-01'::date, '2024-11-30'::date, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM academic_years WHERE name = '2024-2025'
    UNION ALL
    SELECT 'Even Semester 2024-2025', id, '2025-01-01'::date, '2025-05-31'::date, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM academic_years WHERE name = '2024-2025';

    -- ── 7. Courses ───────────────────────────────────────────────────────────
    INSERT INTO courses (name, code, specialization, program_id, created_at, updated_at)
    SELECT 'B.Sc Nursing – Generalist',          'BSC-NURS-GEN', NULL,                    id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs WHERE code = 'BSC_NURS'
    UNION ALL
    SELECT 'M.Sc Nursing – Adult Health',        'MSC-AHN',      'Adult Health Nursing',  id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs WHERE code = 'MSC_NURS'
    UNION ALL
    SELECT 'M.Sc Nursing – Child Care',        'MSC-PED',      'Child Care',  id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs WHERE code = 'MSC_NURS'
    UNION ALL
    SELECT 'GNM – General Nursing & Midwifery',  'GNM-GEN',      NULL,                    id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs WHERE code = 'GNM';

    -- ── 8. Subjects ──────────────────────────────────────────────────────────
    -- Fix: V37 renamed the old courses table to subjects and renamed program_id → course_id
    -- but did not update the FK constraint. It still references programs(id) instead of
    -- the new courses(id) table. Drop and recreate with the correct reference.
    ALTER TABLE subjects DROP CONSTRAINT IF EXISTS courses_program_id_fkey;
    ALTER TABLE subjects ADD CONSTRAINT subjects_course_id_fkey
        FOREIGN KEY (course_id) REFERENCES courses(id);

    INSERT INTO subjects (name, code, credits, theory_credits, lab_credits, course_id, department_id, semester, created_at, updated_at)
    SELECT 'Anatomy & Physiology',       'BSC-SUB-001', 4, 3, 1, c.id, d.id, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'BSC-NURS-GEN' AND d.code = 'GN'
    UNION ALL
    SELECT 'Biochemistry',               'BSC-SUB-002', 3, 2, 1, c.id, d.id, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'BSC-NURS-GEN' AND d.code = 'MSN'
    UNION ALL
    SELECT 'Nursing Foundations Theory', 'BSC-SUB-003', 4, 4, 0, c.id, d.id, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'BSC-NURS-GEN' AND d.code = 'GN'
    UNION ALL
    SELECT 'Nursing Foundations Lab',    'BSC-SUB-004', 2, 0, 2, c.id, d.id, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'BSC-NURS-GEN' AND d.code = 'GN'
    UNION ALL
    SELECT 'Microbiology & Parasitology','BSC-SUB-005', 3, 2, 1, c.id, d.id, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'BSC-NURS-GEN' AND d.code = 'MSN'
    UNION ALL
    SELECT 'Medical-Surgical Nursing I', 'BSC-SUB-006', 4, 3, 1, c.id, d.id, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'BSC-NURS-GEN' AND d.code = 'MSN'
    UNION ALL
    SELECT 'Community Health Nursing',   'BSC-SUB-007', 3, 2, 1, c.id, d.id, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'BSC-NURS-GEN' AND d.code = 'CHN'
    UNION ALL
    SELECT 'Advanced Nursing Practice',  'MSC-SUB-001', 4, 3, 1, c.id, d.id, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'MSC-AHN' AND d.code = 'GN'
    UNION ALL
    SELECT 'Research Methodology',       'MSC-SUB-002', 3, 3, 0, c.id, d.id, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'MSC-AHN' AND d.code = 'MSN'
    UNION ALL
    SELECT 'Basic Nursing Concepts',     'GNM-SUB-001', 4, 3, 1, c.id, d.id, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM courses c, departments d WHERE c.code = 'GNM-GEN' AND d.code = 'GN';

    -- ── 9. Faculty ───────────────────────────────────────────────────────────
    INSERT INTO faculty (employee_code, first_name, last_name, email, phone, department_id, designation, specialization, lab_expertise, joining_date, status, created_at, updated_at)
    SELECT 'FAC001', 'Priya',     'Sharma',   'priya.sharma@cms.edu',    '9876500001', d.id, 'HOD',                 'General Nursing',          'Nursing Simulation',       '2015-06-01'::date, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'GN'  UNION ALL
    SELECT 'FAC002', 'Lakshmi',   'Devi',     'lakshmi.devi@cms.edu',    '9876500002', d.id, 'HOD',                 'Midwifery',                'Obstetric Lab',            '2014-07-01'::date, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'MO'  UNION ALL
    SELECT 'FAC003', 'Anitha',    'Rao',      'anitha.rao@cms.edu',      '9876500003', d.id, 'PROFESSOR',           'Community Health',         'Community Lab',            '2016-08-01'::date, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'CHN' UNION ALL
    SELECT 'FAC004', 'Rajesh',    'Kumar',    'rajesh.kumar@cms.edu',    '9876500004', d.id, 'ASSOCIATE_PROFESSOR', 'Medical-Surgical Nursing', 'Anatomy Lab',              '2018-01-15'::date, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'MSN' UNION ALL
    SELECT 'FAC005', 'Kavitha',   'Nair',     'kavitha.nair@cms.edu',    '9876500005', d.id, 'ASSISTANT_PROFESSOR', 'Fundamentals of Nursing',  NULL,                       '2020-06-01'::date, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'GN'  UNION ALL
    SELECT 'FAC006', 'Deepa',     'Thomas',   'deepa.thomas@cms.edu',    '9876500006', d.id, 'LECTURER',            'Pediatric Nursing',        NULL,                       '2021-03-01'::date, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'PN'  UNION ALL
    SELECT 'FAC007', 'Suresh',    'Babu',     'suresh.babu@cms.edu',     '9876500007', d.id, 'LAB_INSTRUCTOR',      'Nursing Simulation',       'Nursing Foundation Lab',   '2019-09-01'::date, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'GN'  UNION ALL
    SELECT 'FAC008', 'Ramya',     'Krishnan', 'ramya.krishnan@cms.edu',  '9876500008', d.id, 'ASSISTANT_PROFESSOR', 'Clinical Nursing',         'Skills Lab',               '2022-01-10'::date, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'MSN';

    -- ── 10. Labs ─────────────────────────────────────────────────────────────
    INSERT INTO labs (name, lab_type, department_id, building, room_number, capacity, status, created_at, updated_at)
    SELECT 'Nursing Foundation Lab', 'OTHER',    d.id, 'Block A', 'A-101', 30, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'GN'  UNION ALL
    SELECT 'Anatomy Lab',            'BIOLOGY',  d.id, 'Block B', 'B-201', 25, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'MSN' UNION ALL
    SELECT 'Computer Lab',           'COMPUTER', d.id, 'Block C', 'C-301', 40, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'GN'  UNION ALL
    SELECT 'Community Health Lab',   'OTHER',    d.id, 'Block D', 'D-101', 20, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM departments d WHERE d.code = 'CHN';

    -- ── 11. Lab Slots ─────────────────────────────────────────────────────────
    INSERT INTO lab_slots (name, start_time, end_time, slot_order, is_active, created_at, updated_at) VALUES
        ('Morning Slot 1',   '08:00:00', '10:00:00', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Morning Slot 2',   '10:00:00', '12:00:00', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Afternoon Slot 1', '13:00:00', '15:00:00', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Afternoon Slot 2', '15:00:00', '17:00:00', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ── 12. Lab Incharge Assignments ─────────────────────────────────────────
    INSERT INTO lab_incharge_assignments (lab_id, assignee_id, assignee_name, role, assigned_date, created_at, updated_at)
    SELECT l.id, f.id, 'Suresh Babu',    'LAB_INCHARGE', '2024-06-01'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM labs l, faculty f WHERE l.name = 'Nursing Foundation Lab' AND f.employee_code = 'FAC007' UNION ALL
    SELECT l.id, f.id, 'Rajesh Kumar',   'LAB_INCHARGE', '2024-06-01'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM labs l, faculty f WHERE l.name = 'Anatomy Lab'            AND f.employee_code = 'FAC004' UNION ALL
    SELECT l.id, f.id, 'Priya Sharma',   'LAB_INCHARGE', '2024-06-01'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM labs l, faculty f WHERE l.name = 'Computer Lab'           AND f.employee_code = 'FAC001' UNION ALL
    SELECT l.id, f.id, 'Ramya Krishnan', 'TECHNICIAN',   '2024-06-01'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM labs l, faculty f WHERE l.name = 'Nursing Foundation Lab' AND f.employee_code = 'FAC008';

    -- ── 13. Students ──────────────────────────────────────────────────────────
    INSERT INTO students (roll_number, first_name, last_name, email, phone, program_id, semester, admission_date, lab_batch, status, date_of_birth, gender, nationality, religion, community_category, blood_group, father_name, mother_name, parent_mobile, created_at, updated_at)
    SELECT '2024BSC001', 'Aishwarya', 'Rajput',   'aishwarya.rajput@student.cms.edu',  '8765400001', p.id, 1, '2024-06-10'::date, 'Batch A', 'ACTIVE', '2004-03-15'::date, 'FEMALE', 'Indian', 'Hindu',    'BC',  'O_POSITIVE',  'Ramesh Rajput',  'Sunita Rajput',  '9876540001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'BSC_NURS' UNION ALL
    SELECT '2024BSC002', 'Bhavana',   'Menon',    'bhavana.menon@student.cms.edu',     '8765400002', p.id, 1, '2024-06-12'::date, 'Batch A', 'ACTIVE', '2004-07-22'::date, 'FEMALE', 'Indian', 'Hindu',    'OC',  'A_POSITIVE',  'Gopal Menon',    'Geetha Menon',   '9876540002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'BSC_NURS' UNION ALL
    SELECT '2023BSC003', 'Chandrika', 'Pillai',   'chandrika.pillai@student.cms.edu',  '8765400003', p.id, 2, '2023-06-10'::date, 'Batch B', 'ACTIVE', '2003-11-05'::date, 'FEMALE', 'Indian', 'Christian','OC',  'B_POSITIVE',  'Suresh Pillai',  'Vimala Pillai',  '9876540003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'BSC_NURS' UNION ALL
    SELECT '2023BSC004', 'Divya',     'Nair',     'divya.nair@student.cms.edu',        '8765400004', p.id, 2, '2023-06-12'::date, 'Batch B', 'ACTIVE', '2003-04-18'::date, 'FEMALE', 'Indian', 'Hindu',    'OC',  'AB_POSITIVE', 'Sathish Nair',   'Rekha Nair',     '9876540004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'BSC_NURS' UNION ALL
    SELECT '2022BSC005', 'Ezhilarasi','Thangaraj', 'ezhilarasi.t@student.cms.edu',     '8765400005', p.id, 3, '2022-06-08'::date, 'Batch A', 'ACTIVE', '2002-09-12'::date, 'FEMALE', 'Indian', 'Hindu',    'MBC', 'O_NEGATIVE',  'Thangaraj S',    'Kavitha T',      '9876540005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'BSC_NURS' UNION ALL
    SELECT '2024GNM001', 'Fathima',   'Begum',    'fathima.begum@student.cms.edu',     '8765400006', p.id, 1, '2024-06-14'::date, 'Batch A', 'ACTIVE', '2004-01-25'::date, 'FEMALE', 'Indian', 'Muslim',   'BC',  'B_POSITIVE',  'Abdul Begum',    'Noor Begum',     '9876540006', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'GNM'      UNION ALL
    SELECT '2024GNM002', 'Geetha',    'Kumari',   'geetha.kumari@student.cms.edu',     '8765400007', p.id, 1, '2024-06-16'::date, 'Batch B', 'ACTIVE', '2004-06-08'::date, 'FEMALE', 'Indian', 'Hindu',    'SC',  'A_POSITIVE',  'Murugan K',      'Selvi K',        '9876540007', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'GNM'      UNION ALL
    SELECT '2024MSC001', 'Harini',    'Sundaram',  'harini.sundaram@student.cms.edu',  '8765400008', p.id, 1, '2024-06-15'::date, NULL,      'ACTIVE', '2000-05-30'::date, 'FEMALE', 'Indian', 'Hindu',    'OC',  'O_POSITIVE',  'Sundaram V',     'Padma S',        '9876540008', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'MSC_NURS' UNION ALL
    SELECT '2021BSC006', 'Indira',    'Mohan',    'indira.mohan@student.cms.edu',      '8765400009', p.id, 4, '2021-06-07'::date, NULL,      'ON_LEAVE','2001-08-17'::date, 'FEMALE', 'Indian', 'Hindu',    'BC',  'A_NEGATIVE',  'Mohan D',        'Devi M',         '9876540009', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'BSC_NURS' UNION ALL
    SELECT '2024BSC007', 'Jayanthi',  'Krishnan', 'jayanthi.krishnan@student.cms.edu', '8765400010', p.id, 1, '2024-06-18'::date, 'Batch A', 'ACTIVE', '2004-12-02'::date, 'FEMALE', 'Indian', 'Hindu',    'MBC', 'B_NEGATIVE',  'Krishnan R',     'Meena K',        '9876540010', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p WHERE p.code = 'BSC_NURS';

    -- ── 14. Admissions ────────────────────────────────────────────────────────
    INSERT INTO admissions (student_id, academic_year_from, academic_year_to, application_date, status, declaration_place, declaration_date, parent_consent_given, applicant_consent_given, created_at, updated_at)
    SELECT s.id, 2024, 2025, '2024-06-10'::date, 'APPROVED', 'Chennai',    '2024-06-10'::date, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM students s WHERE s.roll_number = '2024BSC001' UNION ALL
    SELECT s.id, 2024, 2025, '2024-06-12'::date, 'APPROVED', 'Coimbatore', '2024-06-12'::date, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM students s WHERE s.roll_number = '2024BSC002' UNION ALL
    SELECT s.id, 2024, 2025, '2024-06-15'::date, 'APPROVED', 'Madurai',    '2024-06-15'::date, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM students s WHERE s.roll_number = '2024MSC001';

    -- ── 15. Academic Qualifications ───────────────────────────────────────────
    INSERT INTO academic_qualifications (admission_id, qualification_type, school_name, major_subject, total_marks, percentage, month_and_year_of_passing, university_or_board, created_at, updated_at)
    SELECT a.id, 'SSLC',  'St. Mary''s Matric School',     'Science',        500, 91.20, 'March 2020', 'Tamil Nadu SSLC Board',                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM admissions a JOIN students s ON a.student_id = s.id WHERE s.roll_number = '2024BSC001' UNION ALL
    SELECT a.id, 'HSC',   'St. Mary''s Higher Sec School', 'Biology Group',  600, 88.50, 'March 2022', 'Tamil Nadu HSC Board',                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM admissions a JOIN students s ON a.student_id = s.id WHERE s.roll_number = '2024BSC001' UNION ALL
    SELECT a.id, 'SSLC',  'GRK Matric School',             'Science',        500, 95.00, 'March 2020', 'Tamil Nadu SSLC Board',                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM admissions a JOIN students s ON a.student_id = s.id WHERE s.roll_number = '2024BSC002' UNION ALL
    SELECT a.id, 'HSC',   'GRK Higher Sec School',         'Biology Group',  600, 92.00, 'March 2022', 'Tamil Nadu HSC Board',                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM admissions a JOIN students s ON a.student_id = s.id WHERE s.roll_number = '2024BSC002' UNION ALL
    SELECT a.id, 'DEGREE','GRK Nursing College',            'B.Sc Nursing',  1000, 78.60, 'April 2022', 'Tamil Nadu Dr. MGR Medical University',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM admissions a JOIN students s ON a.student_id = s.id WHERE s.roll_number = '2024MSC001';

    -- ── 16. Agents ────────────────────────────────────────────────────────────
    INSERT INTO agents (name, phone, email, area, locality, is_active, created_at, updated_at) VALUES
        ('Srinivas Education Services', '9876543210', 'srinivas.edu@gmail.com',  'Coimbatore', 'RS Puram',   TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Global Nursing Academy',      '9123456780', 'global.nursing@gmail.com','Chennai',    'Anna Nagar', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('Tamil Nadu Education Hub',    '9988776655', 'tneduhub@gmail.com',      'Madurai',    'SS Colony',  TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ── 17. Agent Commission Guidelines ──────────────────────────────────────
    INSERT INTO agent_commission_guidelines (agent_id, program_id, locality_type, suggested_commission, created_at, updated_at)
    SELECT ag.id, p.id, 'LOCAL',    15000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM agents ag, programs p WHERE ag.name = 'Srinivas Education Services' AND p.code = 'BSC_NURS' UNION ALL
    SELECT ag.id, p.id, 'DISTRICT', 12000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM agents ag, programs p WHERE ag.name = 'Srinivas Education Services' AND p.code = 'BSC_NURS' UNION ALL
    SELECT ag.id, p.id, 'LOCAL',     8000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM agents ag, programs p WHERE ag.name = 'Srinivas Education Services' AND p.code = 'GNM'      UNION ALL
    SELECT ag.id, p.id, 'STATE',    10000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM agents ag, programs p WHERE ag.name = 'Global Nursing Academy'      AND p.code = 'BSC_NURS' UNION ALL
    SELECT ag.id, p.id, 'DISTRICT',  7000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM agents ag, programs p WHERE ag.name = 'Tamil Nadu Education Hub'    AND p.code = 'GNM';

    -- ── 18. Fee Structures + Year Amounts ─────────────────────────────────────
    INSERT INTO fee_structures (program_id, academic_year_id, fee_type, amount, description, is_mandatory, is_active, created_at, updated_at)
    SELECT p.id, ay.id, 'TUITION', 95000.00, 'B.Sc Nursing 4-Year Tuition Fee 2024-25', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, academic_years ay WHERE p.code = 'BSC_NURS' AND ay.name = '2024-2025' UNION ALL
    SELECT p.id, ay.id, 'LAB_FEE', 12000.00, 'B.Sc Nursing Lab Fee 2024-25',            TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, academic_years ay WHERE p.code = 'BSC_NURS' AND ay.name = '2024-2025' UNION ALL
    SELECT p.id, ay.id, 'TUITION', 65000.00, 'GNM 3-Year Tuition Fee 2024-25',          TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, academic_years ay WHERE p.code = 'GNM'      AND ay.name = '2024-2025' UNION ALL
    SELECT p.id, ay.id, 'TUITION', 45000.00, 'M.Sc Nursing 2-Year Tuition Fee 2024-25', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, academic_years ay WHERE p.code = 'MSC_NURS' AND ay.name = '2024-2025';

    INSERT INTO fee_structure_year_amounts (fee_structure_id, year_number, year_label, amount, created_at, updated_at)
    SELECT fs.id, 1, 'Year 1', 25000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'BSC_NURS' AND fs.fee_type = 'TUITION' UNION ALL
    SELECT fs.id, 2, 'Year 2', 25000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'BSC_NURS' AND fs.fee_type = 'TUITION' UNION ALL
    SELECT fs.id, 3, 'Year 3', 25000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'BSC_NURS' AND fs.fee_type = 'TUITION' UNION ALL
    SELECT fs.id, 4, 'Year 4', 20000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'BSC_NURS' AND fs.fee_type = 'TUITION' UNION ALL
    SELECT fs.id, 1, 'Year 1',  3000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'BSC_NURS' AND fs.fee_type = 'LAB_FEE' UNION ALL
    SELECT fs.id, 2, 'Year 2',  3000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'BSC_NURS' AND fs.fee_type = 'LAB_FEE' UNION ALL
    SELECT fs.id, 3, 'Year 3',  3000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'BSC_NURS' AND fs.fee_type = 'LAB_FEE' UNION ALL
    SELECT fs.id, 4, 'Year 4',  3000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'BSC_NURS' AND fs.fee_type = 'LAB_FEE' UNION ALL
    SELECT fs.id, 1, 'Year 1', 22000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'GNM'      AND fs.fee_type = 'TUITION' UNION ALL
    SELECT fs.id, 2, 'Year 2', 22000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'GNM'      AND fs.fee_type = 'TUITION' UNION ALL
    SELECT fs.id, 3, 'Year 3', 21000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'GNM'      AND fs.fee_type = 'TUITION' UNION ALL
    SELECT fs.id, 1, 'Year 1', 23000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'MSC_NURS' AND fs.fee_type = 'TUITION' UNION ALL
    SELECT fs.id, 2, 'Year 2', 22000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE p.code = 'MSC_NURS' AND fs.fee_type = 'TUITION';

    -- ── 19. Fee Payments ──────────────────────────────────────────────────────
    INSERT INTO fee_payments (student_id, fee_structure_id, receipt_number, amount_paid, payment_date, payment_mode, status, created_at, updated_at)
    SELECT s.id, fs.id, 'RCP-2024-0001', 25000.00, '2024-06-15'::date, 'CASH',        'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM students s, fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE s.roll_number = '2024BSC001' AND p.code = 'BSC_NURS' AND fs.fee_type = 'TUITION' UNION ALL
    SELECT s.id, fs.id, 'RCP-2024-0002', 25000.00, '2024-06-18'::date, 'UPI',         'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM students s, fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE s.roll_number = '2024BSC002' AND p.code = 'BSC_NURS' AND fs.fee_type = 'TUITION' UNION ALL
    SELECT s.id, fs.id, 'RCP-2024-0003', 22000.00, '2024-06-20'::date, 'NET_BANKING', 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM students s, fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE s.roll_number = '2024GNM001' AND p.code = 'GNM'      AND fs.fee_type = 'TUITION' UNION ALL
    SELECT s.id, fs.id, 'RCP-2024-0004', 23000.00, '2024-06-22'::date, 'DEMAND_DRAFT','PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM students s, fee_structures fs JOIN programs p ON fs.program_id = p.id WHERE s.roll_number = '2024MSC001' AND p.code = 'MSC_NURS' AND fs.fee_type = 'TUITION';

    -- ── 20. Student Fee Allocations ───────────────────────────────────────────
    INSERT INTO student_fee_allocations (student_id, program_id, total_fee, discount_amount, discount_reason, agent_commission, net_fee, status, created_at, updated_at)
    SELECT s.id, p.id, 95000.00, 5000.00, 'Merit Scholarship', 0, 90000.00, 'FINALIZED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM students s, programs p WHERE s.roll_number = '2024BSC001' AND p.code = 'BSC_NURS' UNION ALL
    SELECT s.id, p.id, 65000.00, NULL,    NULL,                 0, 65000.00, 'FINALIZED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM students s, programs p WHERE s.roll_number = '2024GNM001' AND p.code = 'GNM';

    -- ── 21. Semester Fees ─────────────────────────────────────────────────────
    INSERT INTO semester_fees (allocation_id, year_number, semester_label, amount, due_date, created_at, updated_at)
    SELECT sfa.id, 1, 'Year 1', 25000.00, '2024-06-30'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM student_fee_allocations sfa JOIN students s ON sfa.student_id = s.id WHERE s.roll_number = '2024BSC001' UNION ALL
    SELECT sfa.id, 2, 'Year 2', 25000.00, '2025-06-30'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM student_fee_allocations sfa JOIN students s ON sfa.student_id = s.id WHERE s.roll_number = '2024BSC001' UNION ALL
    SELECT sfa.id, 3, 'Year 3', 25000.00, '2026-06-30'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM student_fee_allocations sfa JOIN students s ON sfa.student_id = s.id WHERE s.roll_number = '2024BSC001' UNION ALL
    SELECT sfa.id, 4, 'Year 4', 15000.00, '2027-06-30'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM student_fee_allocations sfa JOIN students s ON sfa.student_id = s.id WHERE s.roll_number = '2024BSC001' UNION ALL
    SELECT sfa.id, 1, 'Year 1', 22000.00, '2024-06-30'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM student_fee_allocations sfa JOIN students s ON sfa.student_id = s.id WHERE s.roll_number = '2024GNM001' UNION ALL
    SELECT sfa.id, 2, 'Year 2', 22000.00, '2025-06-30'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM student_fee_allocations sfa JOIN students s ON sfa.student_id = s.id WHERE s.roll_number = '2024GNM001' UNION ALL
    SELECT sfa.id, 3, 'Year 3', 21000.00, '2026-06-30'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM student_fee_allocations sfa JOIN students s ON sfa.student_id = s.id WHERE s.roll_number = '2024GNM001';

    -- ── 22. Equipment ─────────────────────────────────────────────────────────
    INSERT INTO equipment (name, asset_code, category, lab_id, status, created_at, updated_at)
    SELECT 'Desktop Computer',      'ASSET001', 'COMPUTER',    l.id, 'AVAILABLE',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Computer Lab'           UNION ALL
    SELECT 'Anatomy Model (Full)',   'ASSET002', 'ELECTRONIC',  l.id, 'AVAILABLE',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Anatomy Lab'             UNION ALL
    SELECT 'Stethoscope Set',        'ASSET003', 'MECHANICAL',  l.id, 'IN_USE',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Nursing Foundation Lab'  UNION ALL
    SELECT 'Blood Pressure Monitor', 'ASSET004', 'ELECTRONIC',  l.id, 'AVAILABLE',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Nursing Foundation Lab'  UNION ALL
    SELECT 'Projector System',       'ASSET005', 'ELECTRONIC',  l.id, 'UNDER_MAINTENANCE',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Computer Lab';

    -- ── 23. Inventory Items ───────────────────────────────────────────────────
    INSERT INTO inventory_items (name, item_code, lab_id, quantity, minimum_quantity, unit, last_restocked, created_at, updated_at)
    SELECT 'Surgical Gloves',  'INV001', l.id, 200, 50, 'Pairs',   '2024-10-01'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Nursing Foundation Lab' UNION ALL
    SELECT 'Cotton Rolls',     'INV002', l.id,  15,  5, 'Rolls',   '2024-10-01'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Nursing Foundation Lab' UNION ALL
    SELECT 'Bandages Box',     'INV003', l.id,  30, 10, 'Boxes',   '2024-10-05'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Nursing Foundation Lab' UNION ALL
    SELECT 'Printing Paper',   'INV004', l.id,  20,  5, 'Reams',   '2024-11-01'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Computer Lab'           UNION ALL
    SELECT 'Hand Sanitizer',   'INV005', l.id,  10,  3, 'Bottles', '2024-11-01'::date, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM labs l WHERE l.name = 'Computer Lab';

    -- ── 24. Maintenance Requests ──────────────────────────────────────────────
    INSERT INTO maintenance_requests (equipment_id, title, description, maintenance_type, priority, status, requested_by, assigned_to, request_date, scheduled_date, estimated_cost, created_at, updated_at)
    SELECT eq.id, 'Projector Lamp Replacement',
           'Projector lamp is not working. Needs replacement.',
           'CORRECTIVE', 'HIGH', 'IN_PROGRESS',
           (SELECT f.id FROM faculty f WHERE f.employee_code = 'FAC001'),
           (SELECT f.id FROM faculty f WHERE f.employee_code = 'FAC007'),
           '2025-01-10'::date, NULL, 3500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM equipment eq WHERE eq.asset_code = 'ASSET005' UNION ALL
    SELECT eq.id, 'Annual Computer Servicing',
           'Annual preventive maintenance for all computers in Computer Lab.',
           'PREVENTIVE', 'LOW', 'SCHEDULED',
           (SELECT f.id FROM faculty f WHERE f.employee_code = 'FAC001'),
           NULL,
           '2025-02-01'::date, '2025-02-15'::date, 1200.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM equipment eq WHERE eq.asset_code = 'ASSET001' UNION ALL
    SELECT eq.id, 'Stethoscope Calibration',
           'Routine calibration and cleaning of stethoscope sets.',
           'ROUTINE', 'MEDIUM', 'COMPLETED',
           (SELECT f.id FROM faculty f WHERE f.employee_code = 'FAC007'),
           NULL,
           '2024-12-05'::date, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM equipment eq WHERE eq.asset_code = 'ASSET003';

    -- ── 25. Examinations ──────────────────────────────────────────────────────
    INSERT INTO examinations (name, subject_id, exam_type, date, duration, max_marks, semester_id, created_at, updated_at)
    SELECT 'Anatomy Mid-Term',               sub.id, 'THEORY',    '2024-09-15'::date, 180, 100, sem.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM subjects sub, semesters sem WHERE sub.code = 'BSC-SUB-001' AND sem.name = 'Odd Semester 2024-2025'  UNION ALL
    SELECT 'Anatomy End-Term',               sub.id, 'THEORY',    '2024-11-20'::date, 180, 100, sem.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM subjects sub, semesters sem WHERE sub.code = 'BSC-SUB-001' AND sem.name = 'Odd Semester 2024-2025'  UNION ALL
    SELECT 'Nursing Foundations Practical',  sub.id, 'PRACTICAL', '2024-10-05'::date, 120,  50, sem.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM subjects sub, semesters sem WHERE sub.code = 'BSC-SUB-004' AND sem.name = 'Odd Semester 2024-2025'  UNION ALL
    SELECT 'Advanced Nursing Practice Viva', sub.id, 'VIVA',      '2024-10-20'::date,  60,  50, sem.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM subjects sub, semesters sem WHERE sub.code = 'MSC-SUB-001' AND sem.name = 'Odd Semester 2024-2025'  UNION ALL
    SELECT 'Basic Nursing Mid-Term',         sub.id, 'THEORY',    '2024-09-20'::date, 120,  75, sem.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM subjects sub, semesters sem WHERE sub.code = 'GNM-SUB-001' AND sem.name = 'Odd Semester 2024-2025';

    -- ── 26. Exam Results ──────────────────────────────────────────────────────
    INSERT INTO exam_results (examination_id, student_id, marks_obtained, grade, status, created_at, updated_at)
    SELECT ex.id, s.id,  72, 'B',  'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Anatomy Mid-Term' AND s.roll_number = '2024BSC001' UNION ALL
    SELECT ex.id, s.id,  85, 'A',  'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Anatomy Mid-Term' AND s.roll_number = '2024BSC002' UNION ALL
    SELECT ex.id, s.id,  68, 'C',  'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Anatomy Mid-Term' AND s.roll_number = '2023BSC003' UNION ALL
    SELECT ex.id, s.id,  90, 'A+', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Anatomy Mid-Term' AND s.roll_number = '2023BSC004' UNION ALL
    SELECT ex.id, s.id,  78, 'B+', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Anatomy End-Term' AND s.roll_number = '2024BSC001' UNION ALL
    SELECT ex.id, s.id,  91, 'A+', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Anatomy End-Term' AND s.roll_number = '2024BSC002' UNION ALL
    SELECT ex.id, s.id,  44, 'A',  'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Nursing Foundations Practical' AND s.roll_number = '2023BSC003' UNION ALL
    SELECT ex.id, s.id,  48, 'A+', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Nursing Foundations Practical' AND s.roll_number = '2023BSC004' UNION ALL
    SELECT ex.id, s.id,  43, 'A',  'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Advanced Nursing Practice Viva' AND s.roll_number = '2024MSC001' UNION ALL
    SELECT ex.id, s.id,  60, 'B+', 'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Basic Nursing Mid-Term' AND s.roll_number = '2024GNM001' UNION ALL
    SELECT ex.id, s.id,  55, 'B',  'PUBLISHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM examinations ex, students s WHERE ex.name = 'Basic Nursing Mid-Term' AND s.roll_number = '2024GNM002';

--     -- ── 27. Enquiries ─────────────────────────────────────────────────────────
--     INSERT INTO enquiries (name, email, phone, program_id, course_id, enquiry_date, status, agent_id, referral_type_id, remarks, fee_discussed_amount, finalized_total_fee, finalized_discount_amount, finalized_discount_reason, finalized_net_fee, finalized_by, converted_student_id, created_at, updated_at)
--     SELECT 'Aarav Patel',    'aarav.patel@email.com',    '9000000001', p.id, c.id, '2025-01-05', 'ENQUIRED',           ag.id, rt.id, 'Interested in B.Sc Nursing. Walk-in visit from Coimbatore.',         450000.00,     450000.00,     0.0,   NULL,                         NULL,     NULL,    NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, courses c, agents ag, referral_types rt WHERE p.code = 'BSC_NURS' AND c.code = 'BSC-NURS-GEN' AND rt.code = 'WALK_IN'       UNION ALL
--     SELECT 'Priya Krishnan', 'priya.k@email.com',        '9000000002', p.id, c.id, '2025-01-08', 'INTERESTED',         ag.id, rt.id, 'Called to enquire about GNM program. Interested in scholarship.',  65000.00, 450000.00,     0.0,   NULL,                         NULL,     NULL,    NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, courses c, agents ag,referral_types rt WHERE p.code = 'GNM'      AND c.code = 'GNM-GEN'       AND rt.code = 'PHONE'         UNION ALL
--     SELECT 'Vikram Reddy',   'vikram.r@email.com',       '9000000003', p.id, c.id, '2025-01-12', 'FEES_FINALIZED',     ag.id, rt.id, 'Agent referral from Srinivas Education Services.',                 95000.00, 95000.00, 5000.00,'Agent referral discount',    90000.00,'admin',  NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, courses c, agents ag,referral_types rt WHERE p.code = 'BSC_NURS' AND c.code = 'BSC-NURS-GEN' AND rt.code = 'AGENT_REFERRAL' AND ag.name = 'Srinivas Education Services' UNION ALL
--     SELECT 'Sneha Iyer',     'sneha.iyer@email.com',     '9000000004', p.id, c.id, '2024-12-10', 'CONVERTED',          ag.id, rt.id, 'Online enquiry, documents submitted and converted to student.',    450000.00,     450000.00,     0.0,   NULL,                         NULL,     NULL,    (SELECT s.id FROM students s WHERE s.roll_number = '2024GNM001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, courses c, agents ag,referral_types rt WHERE p.code = 'GNM' AND c.code = 'GNM-GEN' AND rt.code = 'ONLINE' UNION ALL
--     SELECT 'Raju Sharma',    'raju.sharma@email.com',    '9000000005', p.id, c.id, '2025-01-20', 'NOT_INTERESTED',     ag.id, rt.id, 'Visited campus but not interested due to distance from home.',    450000.00,     450000.00,     0.0,   NULL,                         NULL,     NULL,    NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, referral_types rt WHERE p.code = 'MSC_NURS' AND rt.code = 'WALK_IN'  UNION ALL
--     SELECT 'Meena Selvam',   'meena.selvam@email.com',   '9000000006', p.id, c.id, '2025-02-03', 'DOCUMENTS_SUBMITTED',ag.id, rt.id, 'All documents submitted. Pending approval.',                      95000.00, 450000.00,     0.0,   NULL,                         NULL,     NULL,    NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, courses c, agents ag,referral_types rt WHERE p.code = 'BSC_NURS' AND c.code = 'BSC-NURS-GEN' AND rt.code = 'PHONE'    UNION ALL
--     SELECT 'Arjun Verma',    'arjun.verma@email.com',    '9000000007', p.id, c.id, '2025-02-10', 'INTERESTED',         ag.id, rt.id, 'Walk-in from Trichy. Very interested in GNM course.',             450000.00,     450000.00,     0.0,   NULL,                         NULL,     NULL,    NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM programs p, courses c, agents ag,referral_types rt WHERE p.code = 'GNM'      AND c.code = 'GNM-GEN'       AND rt.code = 'WALK_IN';

    -- ── 28. Syllabi ───────────────────────────────────────────────────────────
    INSERT INTO syllabi (subject_id, version, theory_hours, lab_hours, tutorial_hours, objectives, content, text_books, reference_books, course_outcomes, is_active, created_at, updated_at)
    SELECT sub.id, 1, 45, 15, 10,
        'To understand human body structure and physiological functions.',
        'Unit 1: Organization of Human Body' || chr(10) || 'Unit 2: Skeletal System' || chr(10) || 'Unit 3: Muscular System' || chr(10) || 'Unit 4: Cardiovascular System' || chr(10) || 'Unit 5: Nervous System',
        'Anatomy & Physiology – Ross & Wilson; Human Anatomy – Gray''s',
        'Gray''s Anatomy; Principles of Anatomy – Tortora',
        'CO1: Describe body structures' || chr(10) || 'CO2: Explain physiological functions' || chr(10) || 'CO3: Apply anatomical knowledge in nursing practice',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM subjects sub WHERE sub.code = 'BSC-SUB-001' UNION ALL
    SELECT sub.id, 1, 0, 60, 0,
        'To develop practical nursing skills in a simulated environment.',
        'Unit 1: Bed Making' || chr(10) || 'Unit 2: Patient Hygiene' || chr(10) || 'Unit 3: Vital Signs' || chr(10) || 'Unit 4: Drug Administration' || chr(10) || 'Unit 5: Wound Care',
        'Fundamentals of Nursing – Potter & Perry; Clinical Nursing Procedures – Shirdi',
        'Taylor''s Fundamentals of Nursing; Kozier & Erb''s Fundamentals',
        'CO1: Perform basic nursing procedures' || chr(10) || 'CO2: Apply aseptic technique' || chr(10) || 'CO3: Document nursing care',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM subjects sub WHERE sub.code = 'BSC-SUB-004' UNION ALL
    SELECT sub.id, 1, 40, 20, 5,
        'To introduce core nursing concepts and professional values.',
        'Unit 1: Introduction to Nursing' || chr(10) || 'Unit 2: Health and Illness' || chr(10) || 'Unit 3: Patient Assessment' || chr(10) || 'Unit 4: Communication' || chr(10) || 'Unit 5: Safety and Infection Control',
        'Fundamentals of Nursing – Craven; Basic Concepts of Nursing – Henderson',
        'Nursing Theories – Meleis; Professional Nursing – Chitty',
        'CO1: Define nursing concepts' || chr(10) || 'CO2: Demonstrate basic patient assessment' || chr(10) || 'CO3: Practice therapeutic communication',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM subjects sub WHERE sub.code = 'GNM-SUB-001';

    -- ── 29. Experiments ───────────────────────────────────────────────────────
--     INSERT INTO experiments (subject_id, experiment_number, name, description, aim, apparatus, procedure, expected_outcome, learning_outcomes, estimated_duration_minutes, is_active, created_at, updated_at)
--     SELECT sub.id, 1, 'Vital Signs Measurement',
--         'Students practice measuring temperature, pulse, BP and SpO2',
--         'To measure and record vital signs accurately',
--         'Thermometer, BP cuff, stethoscope, pulse oximeter',
--         '1. Measure temperature orally' || chr(10) || '2. Palpate radial pulse for 1 min' || chr(10) || '3. Measure BP using sphygmomanometer' || chr(10) || '4. Measure SpO2',
--         'Accurate vital sign readings within normal ranges',
--         'Clinical accuracy in vital sign measurement; Documentation skills',
--         45, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
--     FROM subjects sub WHERE sub.code = 'BSC-SUB-004' UNION ALL
--     SELECT sub.id, 2, 'Bed Making Technique',
--         'Students practice occupied and unoccupied bed making',
--         'To perform bed making maintaining patient comfort and infection control',
--         'Hospital bed, linen set, draw sheet, mackintosh',
--         '1. Gather required linen' || chr(10) || '2. Strip bed' || chr(10) || '3. Apply bottom sheet using mitered corners' || chr(10) || '4. Position patient' || chr(10) || '5. Complete top linen',
--         'Neat and wrinkle-free bed maintaining patient comfort and safety',
--         'Manual dexterity in bed making; Infection control practices',
--         45, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
--     FROM subjects sub WHERE sub.code = 'BSC-SUB-004' UNION ALL
--     SELECT sub.id, 1, 'Skeletal System Identification',
--         'Students identify bones using anatomical models',
--         'To identify and label major bones of the human skeletal system',
--         'Full skeletal model, bone chart, labeling pins',
--         '1. Examine skull bones' || chr(10) || '2. Identify vertebral column bones' || chr(10) || '3. Label long bones' || chr(10) || '4. Sketch and label diagram',
--         'Correct identification of ≥ 90% of bones presented',
--         'Structural knowledge of skeletal system; Anatomical terminology',
--         60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
--     FROM subjects sub WHERE sub.code = 'BSC-SUB-001';

    -- ── 30. Lab Curriculum Mappings (CO-PO) ──────────────────────────────────
    INSERT INTO lab_curriculum_mappings (experiment_id, outcome_type, outcome_code, outcome_description, mapping_level, justification, created_at, updated_at)
    SELECT e.id, 'COURSE_OUTCOME',          'CO1', 'Measure vital signs accurately',          'HIGH',   'Direct skill practice',         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM experiments e WHERE e.name = 'Vital Signs Measurement' UNION ALL
    SELECT e.id, 'PROGRAM_OUTCOME',         'PO3', 'Apply clinical nursing skills',           'HIGH',   'Core clinical competency',      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM experiments e WHERE e.name = 'Vital Signs Measurement' UNION ALL
    SELECT e.id, 'COURSE_OUTCOME',          'CO1', 'Perform basic nursing procedures',        'HIGH',   'Direct lab practice',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM experiments e WHERE e.name = 'Bed Making Technique'    UNION ALL
    SELECT e.id, 'PROGRAM_OUTCOME',         'PO2', 'Demonstrate infection control practices', 'HIGH',   'Key PO for nursing safety',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM experiments e WHERE e.name = 'Bed Making Technique'    UNION ALL
    SELECT e.id, 'COURSE_OUTCOME',          'CO1', 'Describe body structures',                'HIGH',   'Anatomy identification',        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM experiments e WHERE e.name = 'Skeletal System Identification' UNION ALL
    SELECT e.id, 'PROGRAM_SPECIFIC_OUTCOME','PSO1','Apply anatomy in clinical care',          'MEDIUM', 'Foundation for clinical nursing',CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM experiments e WHERE e.name = 'Skeletal System Identification';

    -- ── 31. Lab Schedules ────────────────────────────────────────────────────
--     INSERT INTO lab_schedules (lab_id, course_id, faculty_id, lab_slot_id, batch_name, day_of_week, semester_id, is_active, created_at, updated_at)
--     SELECT l.id, c.id, f.id, ls.id, 'Batch A', 'MONDAY',    sem.id, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
--     FROM labs l, courses c, faculty f, lab_slots ls, semesters sem
--     WHERE l.name = 'Nursing Foundation Lab' AND c.code = 'BSC-NURS-GEN' AND f.employee_code = 'FAC007' AND ls.name = 'Morning Slot 1'   AND sem.name = 'Odd Semester 2024-2025' UNION ALL
--     SELECT l.id, c.id, f.id, ls.id, 'Batch B', 'WEDNESDAY', sem.id, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
--     FROM labs l, courses c, faculty f, lab_slots ls, semesters sem
--     WHERE l.name = 'Nursing Foundation Lab' AND c.code = 'BSC-NURS-GEN' AND f.employee_code = 'FAC008' AND ls.name = 'Morning Slot 2'   AND sem.name = 'Odd Semester 2024-2025' UNION ALL
--     SELECT l.id, c.id, f.id, ls.id, 'Batch A', 'TUESDAY',   sem.id, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
--     FROM labs l, courses c, faculty f, lab_slots ls, semesters sem
--     WHERE l.name = 'Anatomy Lab'            AND c.code = 'BSC-NURS-GEN' AND f.employee_code = 'FAC004' AND ls.name = 'Morning Slot 1'   AND sem.name = 'Odd Semester 2024-2025' UNION ALL
--     SELECT l.id, c.id, f.id, ls.id, 'Batch A', 'FRIDAY',    sem.id, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
--     FROM labs l, courses c, faculty f, lab_slots ls, semesters sem
--     WHERE l.name = 'Computer Lab'           AND c.code = 'GNM-GEN'      AND f.employee_code = 'FAC007' AND ls.name = 'Afternoon Slot 1' AND sem.name = 'Odd Semester 2024-2025';

END $$;
