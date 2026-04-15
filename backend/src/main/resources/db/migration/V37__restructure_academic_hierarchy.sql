-- V37: Restructure academic hierarchy for nursing college three-level architecture
-- Program (academic tier) ↔ Department (many-to-many shared hubs)
-- Course (degree offering) under Program
-- Subject (individual paper) under Course + Department

-- Step 1: Add program_level to programs and drop department_id
ALTER TABLE programs ADD COLUMN program_level VARCHAR(255) NOT NULL DEFAULT 'UNDERGRADUATE';
ALTER TABLE programs ALTER COLUMN program_level DROP DEFAULT;
ALTER TABLE programs DROP COLUMN department_id;

-- Step 2: Remove degree_type and duration_years from programs (moved to courses)
ALTER TABLE programs DROP COLUMN degree_type;
ALTER TABLE programs DROP COLUMN duration_years;

-- Step 3: Create program_departments join table (many-to-many)
CREATE TABLE program_departments (
    program_id BIGINT NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    department_id BIGINT NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    PRIMARY KEY (program_id, department_id)
);

-- Step 4: Rename courses table to subjects
ALTER TABLE courses RENAME TO subjects;

-- Step 5: Update foreign key column names in dependent tables (course_id → subject_id)
ALTER TABLE attendances RENAME COLUMN course_id TO subject_id;
ALTER TABLE lab_attendances RENAME COLUMN course_id TO subject_id;
ALTER TABLE examinations RENAME COLUMN course_id TO subject_id;
ALTER TABLE syllabi RENAME COLUMN course_id TO subject_id;

-- Step 6: Create new courses table (degree offerings)
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL UNIQUE,
    degree_type VARCHAR(255) NOT NULL,
    duration_years INTEGER NOT NULL,
    program_id BIGINT NOT NULL REFERENCES programs(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Step 7: Add department_id to subjects (which department teaches this subject)
ALTER TABLE subjects ADD COLUMN department_id BIGINT REFERENCES departments(id);

-- Step 8: Rename program_id to course_id in subjects (subjects belong to a course/degree, not a program)
ALTER TABLE subjects RENAME COLUMN program_id TO course_id;

-- Step 9: Add course_id and specialization_department_id to students
ALTER TABLE students ADD COLUMN course_id BIGINT REFERENCES courses(id);
ALTER TABLE students ADD COLUMN specialization_department_id BIGINT REFERENCES departments(id);
