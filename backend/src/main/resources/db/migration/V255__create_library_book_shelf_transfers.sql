-- V255: Audit log for book rack/shelf/library transfers. Modeled on student_program_transfers (V128).
-- Rack is stored alongside the leaf shelf tier so transfer history reads without extra joins.

CREATE TABLE library_book_shelf_transfers (
    id              BIGSERIAL    PRIMARY KEY,
    book_id         BIGINT       NOT NULL REFERENCES library_books(id) ON DELETE CASCADE,
    old_library_id  BIGINT       REFERENCES libraries(id),
    old_rack_id     BIGINT       REFERENCES library_racks(id),
    old_shelf_id    BIGINT       REFERENCES library_shelves(id),
    new_library_id  BIGINT       NOT NULL REFERENCES libraries(id),
    new_rack_id     BIGINT       REFERENCES library_racks(id),
    new_shelf_id    BIGINT       REFERENCES library_shelves(id),
    transferred_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    transferred_by  VARCHAR(255),
    notes           TEXT
);

CREATE INDEX idx_lbst_book_id ON library_book_shelf_transfers(book_id);
