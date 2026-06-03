-- ============================================================
-- V100: Library Module — core tables
--   1. library_books       (Accession Register)
--   2. library_issues      (Circulation — student + staff)
--   3. library_fines       (Overdue fine log)
--   4. library_periodicals (Journal / Periodical Register)
--   5. library_settings    (Configuration key-value store)
-- ============================================================

-- ------------------------------------------------------------
-- 1. BOOK CATALOGUE (Accession Register)
-- ------------------------------------------------------------
CREATE TABLE library_books (
    id                  BIGSERIAL       PRIMARY KEY,
    accession_number    VARCHAR(30)     NOT NULL,
    entry_date          DATE,
    title               VARCHAR(500)    NOT NULL,
    authors             VARCHAR(500)    NOT NULL,
    publisher           VARCHAR(300),
    year_of_publication VARCHAR(20),
    edition             VARCHAR(100),
    isbn                VARCHAR(30),
    book_collation      VARCHAR(200),
    series              VARCHAR(200),
    call_number         VARCHAR(50),
    shelf_location      VARCHAR(20),
    subject_category    VARCHAR(100),
    source_of_supply    VARCHAR(20),        -- PURCHASE | DONATION | EXCHANGE
    vendor_donor_name   VARCHAR(200),
    bill_number         VARCHAR(50),
    bill_date           DATE,
    price_rs            NUMERIC(10, 2),
    status              VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    remarks             TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_library_books_accession UNIQUE (accession_number),
    CONSTRAINT chk_library_books_status   CHECK (status IN ('AVAILABLE', 'ISSUED', 'LOST', 'DAMAGED', 'WITHDRAWN')),
    CONSTRAINT chk_library_books_source   CHECK (source_of_supply IS NULL OR source_of_supply IN ('PURCHASE', 'DONATION', 'EXCHANGE'))
);

CREATE INDEX idx_library_books_status          ON library_books (status);
CREATE INDEX idx_library_books_call_number     ON library_books (call_number);
CREATE INDEX idx_library_books_shelf_location  ON library_books (shelf_location);
CREATE INDEX idx_library_books_subject         ON library_books (subject_category);
CREATE INDEX idx_library_books_title           ON library_books USING gin (to_tsvector('english', title));

-- ------------------------------------------------------------
-- 2. CIRCULATION — ISSUE / RETURN / RENEWAL
--    Single table for both student and faculty issues.
--    member_type discriminator + nullable FK pair enforced by CHECK.
-- ------------------------------------------------------------
CREATE TABLE library_issues (
    id               BIGSERIAL       PRIMARY KEY,
    book_id          BIGINT          NOT NULL REFERENCES library_books(id),
    member_type      VARCHAR(10)     NOT NULL,   -- STUDENT | FACULTY
    student_id       BIGINT          REFERENCES students(id),
    faculty_id       BIGINT          REFERENCES faculty(id),
    issued_date      DATE            NOT NULL,
    due_date         DATE            NOT NULL,
    returned_date    DATE,
    renewal_count    INTEGER         NOT NULL DEFAULT 0,
    last_renewed_date DATE,
    status           VARCHAR(20)     NOT NULL DEFAULT 'ISSUED',
    issued_by        VARCHAR(100)    NOT NULL,   -- username of librarian
    returned_to      VARCHAR(100),               -- username of librarian who accepted return
    remarks          VARCHAR(500),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_library_issues_member_type CHECK (member_type IN ('STUDENT', 'FACULTY')),
    CONSTRAINT chk_library_issues_status      CHECK (status IN ('ISSUED', 'RETURNED', 'OVERDUE', 'LOST')),
    CONSTRAINT chk_library_issues_member_fk   CHECK (
        (member_type = 'STUDENT' AND student_id IS NOT NULL AND faculty_id IS NULL) OR
        (member_type = 'FACULTY' AND faculty_id IS NOT NULL AND student_id IS NULL)
    )
);

-- Enforce: one physical copy cannot be actively issued to two people simultaneously
CREATE UNIQUE INDEX idx_library_issues_active_book
    ON library_issues (book_id)
    WHERE status IN ('ISSUED', 'OVERDUE');

CREATE INDEX idx_library_issues_student   ON library_issues (student_id);
CREATE INDEX idx_library_issues_faculty   ON library_issues (faculty_id);
CREATE INDEX idx_library_issues_status    ON library_issues (status);
CREATE INDEX idx_library_issues_due_date  ON library_issues (due_date);
CREATE INDEX idx_library_issues_book      ON library_issues (book_id);

-- ------------------------------------------------------------
-- 3. FINE LOG  (tracked now; cashier integration is Phase 2)
-- ------------------------------------------------------------
CREATE TABLE library_fines (
    id            BIGSERIAL       PRIMARY KEY,
    issue_id      BIGINT          NOT NULL UNIQUE REFERENCES library_issues(id),
    overdue_days  INTEGER         NOT NULL,
    fine_per_day  NUMERIC(10, 2)  NOT NULL,
    total_fine    NUMERIC(10, 2)  NOT NULL,
    status        VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    waived_by     VARCHAR(100),
    collected_at  TIMESTAMP WITH TIME ZONE,
    remarks       VARCHAR(500),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_library_fines_status CHECK (status IN ('PENDING', 'WAIVED', 'COLLECTED'))
);

CREATE INDEX idx_library_fines_status ON library_fines (status);

-- ------------------------------------------------------------
-- 4. PERIODICALS / JOURNAL REGISTER
-- ------------------------------------------------------------
CREATE TABLE library_periodicals (
    id                   BIGSERIAL       PRIMARY KEY,
    journal_name         VARCHAR(300)    NOT NULL,
    journal_type         VARCHAR(20)     NOT NULL DEFAULT 'NATIONAL',  -- NATIONAL | INTERNATIONAL
    organization         VARCHAR(200),
    volume_number        VARCHAR(20),
    issue_number         VARCHAR(20),
    month_range          VARCHAR(30),    -- e.g. Jan-Jun, Jul-Dec
    year                 INTEGER,
    copies_count         INTEGER         NOT NULL DEFAULT 1,
    subscription_status  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    received_date        DATE,
    received_by          VARCHAR(100),
    remarks              VARCHAR(500),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_library_periodicals_type   CHECK (journal_type IN ('NATIONAL', 'INTERNATIONAL')),
    CONSTRAINT chk_library_periodicals_status CHECK (subscription_status IN ('ACTIVE', 'EXPIRED'))
);

CREATE INDEX idx_library_periodicals_name   ON library_periodicals (journal_name);
CREATE INDEX idx_library_periodicals_year   ON library_periodicals (year);
CREATE INDEX idx_library_periodicals_status ON library_periodicals (subscription_status);

-- ------------------------------------------------------------
-- 5. LIBRARY SETTINGS  (librarian-configurable, typed k-v store)
-- ------------------------------------------------------------
CREATE TABLE library_settings (
    id            BIGSERIAL       PRIMARY KEY,
    setting_key   VARCHAR(100)    NOT NULL,
    setting_value VARCHAR(200)    NOT NULL,
    display_name  VARCHAR(200)    NOT NULL,
    description   VARCHAR(500),
    data_type     VARCHAR(20)     NOT NULL DEFAULT 'STRING',  -- INTEGER | DECIMAL | STRING
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_library_settings_key  UNIQUE (setting_key),
    CONSTRAINT chk_library_settings_type CHECK (data_type IN ('INTEGER', 'DECIMAL', 'STRING'))
);

-- Default configuration values
INSERT INTO library_settings (setting_key, setting_value, display_name, description, data_type) VALUES
    ('student_loan_days',   '14',   'Student Loan Period (days)',      'Number of days a student may keep a book before it is due',               'INTEGER'),
    ('faculty_loan_days',   '30',   'Faculty Loan Period (days)',      'Number of days a faculty member may keep a book before it is due',        'INTEGER'),
    ('student_max_books',   '2',    'Max Books per Student',           'Maximum number of books a student may have issued at one time',           'INTEGER'),
    ('faculty_max_books',   '3',    'Max Books per Faculty',           'Maximum number of books a faculty member may have issued at one time',    'INTEGER'),
    ('fine_per_day',        '1.00', 'Fine per Overdue Day (₹)',        'Amount charged per day for each book returned after its due date',        'DECIMAL'),
    ('max_renewals',        '2',    'Maximum Renewals per Issue',      'How many times a borrower may renew the same book before a return is required', 'INTEGER');
