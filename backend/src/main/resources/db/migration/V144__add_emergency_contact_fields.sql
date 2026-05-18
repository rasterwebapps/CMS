-- Emergency contact fields — faculty, students, app_users
-- Single mandatory contact: name, relationship, phone

ALTER TABLE faculty
    ADD COLUMN IF NOT EXISTS emergency_contact_name         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS emergency_contact_relationship VARCHAR(50),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone        VARCHAR(20);

ALTER TABLE students
    ADD COLUMN IF NOT EXISTS emergency_contact_name         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS emergency_contact_relationship VARCHAR(50),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone        VARCHAR(20);

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS emergency_contact_name         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS emergency_contact_relationship VARCHAR(50),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone        VARCHAR(20);
