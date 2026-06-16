-- V218: Add document category (MANDATORY / OPTIONAL) to the per-program document requirements.
--
-- All existing rows are non-destructively promoted to MANDATORY so current
-- program configurations are preserved without any data loss.

ALTER TABLE program_document_types
    ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'MANDATORY';
