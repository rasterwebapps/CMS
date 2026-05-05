-- V93: Extend faculty profile with personal/identity/bank/address/experience data
--      and add a faculty_documents table for scanned uploads.

-- 1. Identity & demographics
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS faculty_type           VARCHAR(40);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS pan_number             VARCHAR(20);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS aadhaar_number         VARCHAR(20);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS date_of_birth          DATE;
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS gender                 VARCHAR(20);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS marital_status         VARCHAR(20);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS nationality            VARCHAR(100);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS religion               VARCHAR(100);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS blood_group            VARCHAR(20);

-- 2. Bank details
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS bank_account_number    VARCHAR(40);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS bank_ifsc_code         VARCHAR(20);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS bank_branch            VARCHAR(150);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS bank_name              VARCHAR(150);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS bank_account_holder    VARCHAR(150);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS bank_account_type      VARCHAR(20);

-- 3. Address (mirrors Address @Embeddable used by Student)
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS postal_address         VARCHAR(500);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS street                 VARCHAR(255);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS city                   VARCHAR(100);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS district               VARCHAR(100);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS state                  VARCHAR(100);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS pincode                VARCHAR(20);

-- 4. Experience breakdown (years, single decimal precision)
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS teaching_exp_ug_years   NUMERIC(5,1);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS teaching_exp_pg_years   NUMERIC(5,1);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS teaching_exp_phd_years  NUMERIC(5,1);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS clinical_exp_ug_years   NUMERIC(5,1);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS clinical_exp_pg_years   NUMERIC(5,1);
ALTER TABLE faculty ADD COLUMN IF NOT EXISTS clinical_exp_phd_years  NUMERIC(5,1);

-- 5. Faculty documents table (scanned uploads, one per (faculty, document_type))
CREATE TABLE IF NOT EXISTS faculty_documents (
    id              BIGSERIAL    PRIMARY KEY,
    faculty_id      BIGINT       NOT NULL REFERENCES faculty(id) ON DELETE CASCADE,
    document_type   VARCHAR(80)  NOT NULL,
    status          VARCHAR(40)  NOT NULL DEFAULT 'NOT_UPLOADED',
    remarks         VARCHAR(500),
    verified_by     VARCHAR(100),
    verified_at     TIMESTAMPTZ,
    file_name       VARCHAR(255),
    content_type    VARCHAR(100),
    file_size       BIGINT,
    file_data       BYTEA,
    uploaded_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_faculty_document_type UNIQUE (faculty_id, document_type)
);

CREATE INDEX IF NOT EXISTS idx_faculty_documents_faculty_id ON faculty_documents(faculty_id);
