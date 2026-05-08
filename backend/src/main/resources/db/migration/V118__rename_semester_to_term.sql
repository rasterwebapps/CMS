-- V118: Rename semester-related tables and columns to neutral/term-based names
-- This migration renames DB objects to remove "semester" terminology throughout the schema.

-- 1. Rename curriculum_semester_courses → curriculum_term_courses
ALTER TABLE curriculum_semester_courses RENAME TO curriculum_term_courses;

-- 2. Rename semester_fees → installment_fees
ALTER TABLE semester_fees RENAME TO installment_fees;

-- 3. Rename semester_results → term_results
ALTER TABLE semester_results RENAME TO term_results;

-- 4. Rename semester_number columns
ALTER TABLE curriculum_term_courses RENAME COLUMN semester_number TO term_number;
ALTER TABLE course_offerings RENAME COLUMN semester_number TO term_number;
ALTER TABLE student_term_enrollments RENAME COLUMN semester_number TO term_number;
ALTER TABLE scholarship_disbursements RENAME COLUMN semester_number TO term_number;

-- 5. Rename installment_fees columns
ALTER TABLE installment_fees RENAME COLUMN semester_label TO installment_label;
ALTER TABLE installment_fees RENAME COLUMN semester_sequence TO sequence;

-- 6. Rename semester column in students → year_of_study
ALTER TABLE students RENAME COLUMN semester TO year_of_study;

-- 7. Rename semester column in subjects → term_number
ALTER TABLE subjects RENAME COLUMN semester TO term_number;

-- 8. Rename semester_wise_fees column in enquiries → term_wise_fees
ALTER TABLE enquiries RENAME COLUMN semester_wise_fees TO term_wise_fees;

-- 9. Migrate assessment_pattern enum value SEMESTER → TERM_BASED in programs
UPDATE programs SET assessment_pattern = 'TERM_BASED' WHERE assessment_pattern = 'SEMESTER';

