-- V152: Add direct FK columns from app_users to students and faculty.
--
-- Replaces the fragile email-matching identity resolution in ProfileService
-- with an explicit, immutable link set when the admin creates a user account
-- for a student or faculty member.
--
-- Both columns are nullable: admin / support / other role users have neither.

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS student_id BIGINT REFERENCES students(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS faculty_id BIGINT REFERENCES faculty(id)  ON DELETE SET NULL;

-- Each student / faculty can have at most one app_user account.
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_users_student_id
    ON app_users (student_id) WHERE student_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_users_faculty_id
    ON app_users (faculty_id) WHERE faculty_id IS NOT NULL;
