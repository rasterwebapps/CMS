-- Starter data for the Room Purpose Classification masters, matching the standard ERP room
-- taxonomy the college signed off on. Admins can rename/extend both tiers afterward via the new
-- master screens — this just avoids starting from a completely empty dropdown.

INSERT INTO room_purpose_categories (name, code, is_residential, description) VALUES
    ('Academic',                 'ACADEMIC',    FALSE, 'Classrooms and teaching spaces used by Timetable & Admissions'),
    ('Residential (Hostel)',     'RESIDENTIAL', TRUE,  'Hostel bedrooms and warden spaces used by Hostel Management'),
    ('Administrative & Staff',   'ADMIN_STAFF', FALSE, 'Offices and staff spaces used by HR & Asset Management'),
    ('Knowledge & Resource',     'LIBRARY',     FALSE, 'Library and reading spaces used by Library Management'),
    ('Dining & Refreshment',     'DINING',      FALSE, 'Canteen and dining spaces used by Food Services'),
    ('Hygiene & Utility',        'UTILITY',     FALSE, 'Washrooms and utility spaces used by Facility Maintenance'),
    ('Sports & Recreation',      'SPORTS',      FALSE, 'Sports and event spaces used by Extracurricular Management')
ON CONFLICT (code) DO NOTHING;

INSERT INTO room_sub_types (purpose_category_id, name, code, description)
SELECT c.id, v.name, v.code, v.description
FROM room_purpose_categories c
JOIN (VALUES
    ('ACADEMIC',    'Classroom',            'CLASSROOM',    NULL),
    ('ACADEMIC',    'Smart Class',          'SMART_CLASS',  NULL),
    ('ACADEMIC',    'Lecture Hall',         'LECTURE_HALL', NULL),
    ('ACADEMIC',    'Computer Lab',         'COMPUTER_LAB', NULL),
    ('ACADEMIC',    'Physics Lab',          'PHYSICS_LAB',  NULL),
    ('ACADEMIC',    'Exam Hall',            'EXAM_HALL',    NULL),

    ('RESIDENTIAL', 'Student Bedroom',      'STUDENT_BEDROOM', NULL),
    ('RESIDENTIAL', 'Warden Room',          'WARDEN_ROOM',      NULL),
    ('RESIDENTIAL', 'Guest Room',           'GUEST_ROOM',       NULL),
    ('RESIDENTIAL', 'Dormitory',            'DORMITORY',        NULL),
    ('RESIDENTIAL', 'Study Hall',           'STUDY_HALL',       NULL),

    ('ADMIN_STAFF', 'Principal Office',     'PRINCIPAL_OFFICE', NULL),
    ('ADMIN_STAFF', 'Staff Room',           'STAFF_ROOM',        NULL),
    ('ADMIN_STAFF', 'HOD Office',           'HOD_OFFICE',        NULL),
    ('ADMIN_STAFF', 'Reception',            'RECEPTION',         NULL),
    ('ADMIN_STAFF', 'Conference Room',      'CONFERENCE_ROOM',   NULL),
    ('ADMIN_STAFF', 'Accounts Desk',        'ACCOUNTS_DESK',     NULL),

    ('LIBRARY',     'Main Library',         'MAIN_LIBRARY',   NULL),
    ('LIBRARY',     'Reading Room',         'READING_ROOM',   NULL),
    ('LIBRARY',     'Media Center',         'MEDIA_CENTER',   NULL),
    ('LIBRARY',     'Archive Room',         'ARCHIVE_ROOM',   NULL),

    ('DINING',      'Canteen',              'CANTEEN',       NULL),
    ('DINING',      'Dining Hall',          'DINING_HALL',   NULL),
    ('DINING',      'Kitchen',              'KITCHEN',       NULL),
    ('DINING',      'Mess',                 'MESS',          NULL),
    ('DINING',      'Cafeteria',            'CAFETERIA',     NULL),

    ('UTILITY',     'Student Washroom',     'STUDENT_WASHROOM', NULL),
    ('UTILITY',     'Staff Washroom',       'STAFF_WASHROOM',   NULL),
    ('UTILITY',     'Janitor Closet',       'JANITOR_CLOSET',   NULL),
    ('UTILITY',     'Electrical Room',      'ELECTRICAL_ROOM',  NULL),
    ('UTILITY',     'Server Room',          'SERVER_ROOM',      NULL),

    ('SPORTS',      'Gymnasium',            'GYMNASIUM',         NULL),
    ('SPORTS',      'Indoor Sports Hall',   'INDOOR_SPORTS_HALL', NULL),
    ('SPORTS',      'Auditorium',           'AUDITORIUM',        NULL),
    ('SPORTS',      'Activity Room',        'ACTIVITY_ROOM',     NULL)
) AS v(category_code, name, code, description) ON c.code = v.category_code
ON CONFLICT (purpose_category_id, name) DO NOTHING;
