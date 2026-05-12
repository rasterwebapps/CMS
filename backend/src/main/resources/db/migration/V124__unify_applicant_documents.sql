-- Make enquiry_documents the canonical applicant document lifecycle table.
-- Existing enquiry document rows are retained and linked to admissions after conversion.
-- Existing admission_documents rows are backfilled here for compatibility, then runtime code
-- reads/writes only enquiry_documents for both enquiry and admission document APIs.

ALTER TABLE enquiry_documents ALTER COLUMN enquiry_id DROP NOT NULL;

ALTER TABLE enquiry_documents
    ADD COLUMN IF NOT EXISTS admission_id BIGINT REFERENCES admissions(id) ON DELETE SET NULL;

ALTER TABLE enquiry_documents
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(255);

ALTER TABLE enquiry_documents
    ADD COLUMN IF NOT EXISTS original_submitted BOOLEAN;

-- Link already-converted enquiry documents to their admission records.
UPDATE enquiry_documents ed
SET admission_id = a.id
FROM enquiries e
JOIN admissions a ON a.student_id = e.converted_student_id
WHERE ed.enquiry_id = e.id
  AND ed.admission_id IS NULL
  AND e.converted_student_id IS NOT NULL;

-- Preserve admission-document-only metadata on matching canonical rows.
UPDATE enquiry_documents ed
SET file_name = COALESCE(ed.file_name, ad.file_name),
    uploaded_at = COALESCE(ed.uploaded_at, ad.uploaded_at),
    storage_key = COALESCE(ed.storage_key, ad.storage_key),
    original_submitted = COALESCE(ed.original_submitted, ad.original_submitted),
    verified_by = COALESCE(ed.verified_by, ad.verified_by),
    verified_at = COALESCE(ed.verified_at, ad.verified_at),
    updated_at = CURRENT_TIMESTAMP
FROM admission_documents ad
WHERE ed.admission_id = ad.admission_id
  AND ed.document_type = ad.document_type;

-- Backfill admission documents that do not have an enquiry-origin canonical row.
INSERT INTO enquiry_documents (
    enquiry_id,
    admission_id,
    document_type,
    status,
    remarks,
    verified_by,
    verified_at,
    created_at,
    updated_at,
    file_name,
    content_type,
    file_size,
    file_data,
    uploaded_at,
    storage_key,
    original_submitted
)
SELECT
    NULL,
    ad.admission_id,
    ad.document_type,
    ad.verification_status,
    NULL,
    ad.verified_by,
    ad.verified_at,
    ad.created_at,
    ad.updated_at,
    ad.file_name,
    NULL,
    NULL,
    NULL,
    ad.uploaded_at,
    ad.storage_key,
    ad.original_submitted
FROM admission_documents ad
WHERE NOT EXISTS (
    SELECT 1
    FROM enquiry_documents ed
    WHERE ed.admission_id = ad.admission_id
      AND ed.document_type = ad.document_type
);

CREATE INDEX IF NOT EXISTS idx_enquiry_documents_admission ON enquiry_documents(admission_id);
CREATE INDEX IF NOT EXISTS idx_enquiry_documents_enquiry_type ON enquiry_documents(enquiry_id, document_type);
CREATE INDEX IF NOT EXISTS idx_enquiry_documents_admission_type ON enquiry_documents(admission_id, document_type);
