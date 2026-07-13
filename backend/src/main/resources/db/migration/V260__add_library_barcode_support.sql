-- V260: Barcode generation support for Library books & periodicals.
--   1. Periodicals' accession_number was left nullable by V250 ("existing rows are left
--      untouched"). It now becomes mandatory + unique like books, since it's the basis
--      the new barcode column defaults from. Existing NULL rows are backfilled with a
--      deterministic 'JRN-{id}' value (guaranteed unique per-table, and distinct in shape
--      from the app's own '{year}-{sequence}' generator, so it can't collide with it).
--   2. Both tables gain a barcode column, separate from accession_number, because some
--      items need custom prefixes/suffixes that don't fit the accession-number format.
--      Existing rows default barcode = accession_number (same default new rows get at
--      creation time in LibraryBookService/LibraryPeriodicalService).
--   3. Configurable label print size (mm), reused for every batch print job.
--   4. Printing is its own operation — dedicated permissions, not folded into MANAGE.

-- ------------------------------------------------------------
-- 1. PERIODICALS — backfill + enforce accession_number NOT NULL
-- ------------------------------------------------------------
UPDATE library_periodicals
SET accession_number = 'JRN-' || id
WHERE accession_number IS NULL;

ALTER TABLE library_periodicals
    ALTER COLUMN accession_number SET NOT NULL;

-- ------------------------------------------------------------
-- 2. BARCODE COLUMNS — add, backfill, constrain
-- ------------------------------------------------------------
ALTER TABLE library_books
    ADD COLUMN barcode VARCHAR(30);

ALTER TABLE library_periodicals
    ADD COLUMN barcode VARCHAR(30);

UPDATE library_books
SET barcode = accession_number
WHERE barcode IS NULL;

UPDATE library_periodicals
SET barcode = accession_number
WHERE barcode IS NULL;

ALTER TABLE library_books
    ADD CONSTRAINT uq_library_books_barcode UNIQUE (barcode);

ALTER TABLE library_periodicals
    ADD CONSTRAINT uq_library_periodicals_barcode UNIQUE (barcode);

CREATE INDEX idx_library_books_barcode       ON library_books (barcode);
CREATE INDEX idx_library_periodicals_barcode ON library_periodicals (barcode);

-- ------------------------------------------------------------
-- 3. LABEL SIZE SETTINGS — configurable per sticker stock purchased
-- ------------------------------------------------------------
INSERT INTO library_settings (setting_key, setting_value, display_name, description, data_type) VALUES
    ('barcode_label_width_mm',  '50', 'Barcode Label Width (mm)',  'Printable label width in millimetres, matching the sticker stock in use',  'INTEGER'),
    ('barcode_label_height_mm', '25', 'Barcode Label Height (mm)', 'Printable label height in millimetres, matching the sticker stock in use', 'INTEGER')
ON CONFLICT (setting_key) DO NOTHING;

-- ------------------------------------------------------------
-- 4. PERMISSIONS — printing barcode labels is its own operation,
--    auto-assigned to roles that already hold that screen's own MANAGE permission
--    (manage-tier, not granted to FACULTY/STUDENT view-only access, matching V257)
-- ------------------------------------------------------------
INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('LIBRARY_CATALOGUE_PRINT_BARCODE',  'Print Book Barcode Labels',    'LIBRARY', 'Book Catalogue', 4, CURRENT_TIMESTAMP),
    ('LIBRARY_PERIODICAL_PRINT_BARCODE', 'Print Journal Barcode Labels', 'LIBRARY', 'Journals',        4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_CATALOGUE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_CATALOGUE_PRINT_BARCODE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_PERIODICAL_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_PERIODICAL_PRINT_BARCODE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
