-- ============================================================
-- V250: Journals join the Accession Register + become issuable
--   1. library_periodicals gains accession_number + per-copy status
--      (mirrors library_books; existing rows are left untouched —
--      accession_number stays NULL and copies_count keeps its
--      current aggregate value until a librarian edits that row)
--   2. library_issues can now point at either a book OR a journal
-- ============================================================

-- ------------------------------------------------------------
-- 1. PERIODICALS — per-copy accession number + status
-- ------------------------------------------------------------
ALTER TABLE library_periodicals
    ADD COLUMN accession_number VARCHAR(30),
    ADD COLUMN status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

ALTER TABLE library_periodicals
    ADD CONSTRAINT uq_library_periodicals_accession UNIQUE (accession_number),
    ADD CONSTRAINT chk_library_periodicals_item_status CHECK (status IN ('AVAILABLE', 'ISSUED', 'LOST', 'DAMAGED', 'WITHDRAWN'));

CREATE INDEX idx_library_periodicals_accession    ON library_periodicals (accession_number);
CREATE INDEX idx_library_periodicals_item_status  ON library_periodicals (status);

-- ------------------------------------------------------------
-- 2. CIRCULATION — allow book_id OR periodical_id (exactly one)
-- ------------------------------------------------------------
ALTER TABLE library_issues
    ALTER COLUMN book_id DROP NOT NULL,
    ADD COLUMN periodical_id BIGINT REFERENCES library_periodicals(id);

ALTER TABLE library_issues
    ADD CONSTRAINT chk_library_issues_item_fk CHECK (
        (book_id IS NOT NULL AND periodical_id IS NULL) OR
        (book_id IS NULL AND periodical_id IS NOT NULL)
    );

CREATE INDEX idx_library_issues_periodical ON library_issues (periodical_id);

-- Replace the book-only active-issue uniqueness with one covering either item
DROP INDEX idx_library_issues_active_book;

CREATE UNIQUE INDEX idx_library_issues_active_book
    ON library_issues (book_id)
    WHERE status IN ('ISSUED', 'OVERDUE') AND book_id IS NOT NULL;

CREATE UNIQUE INDEX idx_library_issues_active_periodical
    ON library_issues (periodical_id)
    WHERE status IN ('ISSUED', 'OVERDUE') AND periodical_id IS NOT NULL;
