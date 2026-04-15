CREATE TABLE enquiry_documents (
    id                BIGSERIAL PRIMARY KEY,
    enquiry_id        BIGINT                      NOT NULL REFERENCES enquiries(id) ON DELETE CASCADE,
    document_type     VARCHAR(100)                NOT NULL,
    status            VARCHAR(50)                 NOT NULL,
    remarks           TEXT,
    verified_by       VARCHAR(255),
    verified_at       TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_enquiry_documents_enquiry ON enquiry_documents(enquiry_id);
