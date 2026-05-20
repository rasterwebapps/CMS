-- Add admission_number_code to courses for use in auto-generated admission numbers
-- Format: {year}{admission_number_code}{4-digit-seq} e.g. 2026650001
ALTER TABLE courses
    ADD COLUMN admission_number_code VARCHAR(20);

-- Add configurable separator and scope-inclusion flag to application_number_sequences
-- separator: character(s) between parts (default '-' preserves existing receipt format)
-- include_scope_in_number: when false, format is {prefix}{seq} without scopeKey in the output
ALTER TABLE application_number_sequences
    ADD COLUMN separator VARCHAR(5) NOT NULL DEFAULT '-',
    ADD COLUMN include_scope_in_number BOOLEAN NOT NULL DEFAULT TRUE;
