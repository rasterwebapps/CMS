-- V127: Audit history for document state transitions.
-- Records every meaningful change (upload, verification, rejection, re-upload)
-- without duplicating binary file data — metadata only.

CREATE TABLE faculty_document_history (
    id                  BIGSERIAL    PRIMARY KEY,
    faculty_document_id BIGINT       NOT NULL REFERENCES faculty_documents(id) ON DELETE CASCADE,
    faculty_id          BIGINT       NOT NULL REFERENCES faculty(id) ON DELETE CASCADE,
    document_type       VARCHAR(80)  NOT NULL,
    previous_status     VARCHAR(40),
    new_status          VARCHAR(40)  NOT NULL,
    file_name           VARCHAR(255),
    file_size           BIGINT,
    content_type        VARCHAR(100),
    remarks             TEXT,
    changed_by          VARCHAR(255),
    changed_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fdh_faculty_document_id ON faculty_document_history(faculty_document_id);
CREATE INDEX idx_fdh_faculty_id          ON faculty_document_history(faculty_id);

CREATE TABLE enquiry_document_history (
    id                  BIGSERIAL    PRIMARY KEY,
    enquiry_document_id BIGINT       NOT NULL REFERENCES enquiry_documents(id) ON DELETE CASCADE,
    enquiry_id          BIGINT       REFERENCES enquiries(id) ON DELETE SET NULL,
    admission_id        BIGINT       REFERENCES admissions(id) ON DELETE SET NULL,
    document_type       VARCHAR(100) NOT NULL,
    previous_status     VARCHAR(50),
    new_status          VARCHAR(50)  NOT NULL,
    file_name           VARCHAR(255),
    file_size           BIGINT,
    content_type        VARCHAR(100),
    remarks             TEXT,
    changed_by          VARCHAR(255),
    changed_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_edh_enquiry_document_id ON enquiry_document_history(enquiry_document_id);
CREATE INDEX idx_edh_enquiry_id          ON enquiry_document_history(enquiry_id)   WHERE enquiry_id   IS NOT NULL;
CREATE INDEX idx_edh_admission_id        ON enquiry_document_history(admission_id) WHERE admission_id IS NOT NULL;
