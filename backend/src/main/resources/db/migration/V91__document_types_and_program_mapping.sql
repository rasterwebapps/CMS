-- V91: Refactor document types and add per-program required documents mapping.
--
-- Renames UNDERTAKING_DOCUMENT → PROVISIONAL_CERTIFICATE in existing rows,
-- and creates a new join table linking programs to their required document types.

-- 1. Rename UNDERTAKING_DOCUMENT to PROVISIONAL_CERTIFICATE in both document tables.
UPDATE enquiry_documents
   SET document_type = 'PROVISIONAL_CERTIFICATE'
 WHERE document_type = 'UNDERTAKING_DOCUMENT';

UPDATE admission_documents
   SET document_type = 'PROVISIONAL_CERTIFICATE'
 WHERE document_type = 'UNDERTAKING_DOCUMENT';

-- 2. Create the program → required document types join table.
CREATE TABLE program_document_types (
    program_id    BIGINT       NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    CONSTRAINT pk_program_document_types PRIMARY KEY (program_id, document_type),
    CONSTRAINT fk_pdt_program FOREIGN KEY (program_id)
        REFERENCES programs (id) ON DELETE CASCADE
);

CREATE INDEX idx_pdt_program ON program_document_types (program_id);

