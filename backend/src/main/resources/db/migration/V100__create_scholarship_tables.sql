CREATE TABLE scholarship_types (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(20)  NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    description         TEXT,
    govt_scheme         BOOLEAN      NOT NULL DEFAULT FALSE,
    scheme_code         VARCHAR(50),
    discount_type       VARCHAR(20)  NOT NULL,
    discount_value      NUMERIC(12,2),
    max_amount_per_year NUMERIC(12,2),
    renewal_required    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE student_scholarship_eligibility (
    id                                    BIGSERIAL PRIMARY KEY,
    student_id                            BIGINT      NOT NULL UNIQUE REFERENCES students(id) ON DELETE CASCADE,
    is_first_graduate                     BOOLEAN     NOT NULL DEFAULT FALSE,
    is_merit_based                        BOOLEAN     NOT NULL DEFAULT FALSE,
    is_sports_quota                       BOOLEAN     NOT NULL DEFAULT FALSE,
    is_economically_weaker                BOOLEAN     NOT NULL DEFAULT FALSE,
    annual_family_income                  NUMERIC(12,2),
    income_certificate_number             VARCHAR(50),
    income_cert_issuing_authority         VARCHAR(100),
    income_cert_issue_date                DATE,
    community_certificate_number          VARCHAR(50),
    comm_cert_issuing_authority           VARCHAR(100),
    comm_cert_issue_date                  DATE,
    first_graduate_certificate_number     VARCHAR(50),
    first_grad_cert_issuing_authority     VARCHAR(100),
    first_grad_cert_issue_date            DATE,
    father_education                      VARCHAR(100),
    mother_education                      VARCHAR(100),
    verified_by                           VARCHAR(100),
    verified_at                           TIMESTAMPTZ,
    verification_remarks                  TEXT,
    created_at                            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE student_scholarships (
    id                       BIGSERIAL PRIMARY KEY,
    student_id               BIGINT      NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    scholarship_type_id      BIGINT      NOT NULL REFERENCES scholarship_types(id),
    academic_year_id         BIGINT      NOT NULL REFERENCES academic_years(id),
    application_date         DATE        NOT NULL DEFAULT CURRENT_DATE,
    application_remarks      TEXT,
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by              VARCHAR(100),
    approved_at              TIMESTAMPTZ,
    rejection_reason         TEXT,
    approved_amount          NUMERIC(12,2),
    disbursement_frequency   VARCHAR(20),
    valid_from               DATE,
    valid_till               DATE,
    renewed_from_id          BIGINT REFERENCES student_scholarships(id),
    created_by               VARCHAR(100),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_student_scholarship_year UNIQUE (student_id, academic_year_id)
);

CREATE TABLE scholarship_disbursements (
    id                         BIGSERIAL PRIMARY KEY,
    student_scholarship_id     BIGINT        NOT NULL REFERENCES student_scholarships(id) ON DELETE CASCADE,
    academic_year_id           BIGINT        REFERENCES academic_years(id),
    semester_number            INTEGER,
    amount                     NUMERIC(12,2) NOT NULL,
    disbursement_date          DATE          NOT NULL,
    disbursement_mode          VARCHAR(20)   NOT NULL,
    transaction_reference      VARCHAR(100),
    cheque_number              VARCHAR(50),
    bank_name                  VARCHAR(100),
    remarks                    TEXT,
    disbursed_by               VARCHAR(100),
    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scholarship_types_code ON scholarship_types(code);
CREATE INDEX idx_student_scholarships_student ON student_scholarships(student_id);
CREATE INDEX idx_student_scholarships_status ON student_scholarships(status);
CREATE INDEX idx_student_scholarships_year ON student_scholarships(academic_year_id);
CREATE INDEX idx_scholarship_disbursements_application ON scholarship_disbursements(student_scholarship_id);

