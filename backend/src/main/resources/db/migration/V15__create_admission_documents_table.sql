CREATE TABLE admission_documents (
    id                  BIGSERIAL PRIMARY KEY,
    admission_id        BIGINT                      NOT NULL REFERENCES admissions(id),
    document_type       VARCHAR(255)                NOT NULL,
    file_name           VARCHAR(255),
    storage_key         VARCHAR(255),
    uploaded_at         TIMESTAMP,
    original_submitted  BOOLEAN,
    verified_by         VARCHAR(255),
    verified_at         TIMESTAMP,
    verification_status VARCHAR(255)                NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE    NOT NULL
);
