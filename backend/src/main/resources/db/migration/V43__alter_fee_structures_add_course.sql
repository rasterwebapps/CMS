-- Add course_id FK to fee_structures (required)
ALTER TABLE fee_structures ADD COLUMN course_id BIGINT REFERENCES courses(id);
