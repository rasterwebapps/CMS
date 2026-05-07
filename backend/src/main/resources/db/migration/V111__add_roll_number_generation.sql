-- V111: Add roll number generation support
-- Add roll_number_code column to courses table to store the 2-digit code used in roll numbers
ALTER TABLE courses ADD COLUMN roll_number_code VARCHAR(10);

-- Create roll_number_sequences table to track the last sequence number per course per year
CREATE TABLE roll_number_sequences (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    academic_year INTEGER NOT NULL,
    last_sequence INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_roll_number_sequences_course_year UNIQUE (course_id, academic_year),
    CONSTRAINT fk_roll_number_sequences_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Add index on course_id and academic_year for faster lookups
CREATE INDEX idx_roll_number_sequences_course_year ON roll_number_sequences(course_id, academic_year);
