-- V159: Migrate country storage from free-text to country_id FK on
--       enquiries, faculty, and students tables.

-- ── 1. Enquiries: replace VARCHAR country column with FK ──────────────────────

ALTER TABLE enquiries ADD COLUMN country_id BIGINT;

UPDATE enquiries e
SET country_id = lc.id
FROM location_countries lc
WHERE LOWER(TRIM(COALESCE(e.country, ''))) = LOWER(lc.name);

-- Default any unmatched rows to India
UPDATE enquiries SET country_id = 1 WHERE country_id IS NULL;

ALTER TABLE enquiries
    ADD CONSTRAINT fk_enquiries_country
    FOREIGN KEY (country_id) REFERENCES location_countries(id);

ALTER TABLE enquiries DROP COLUMN country;

-- ── 2. Faculty: add country_id (defaults to India) ────────────────────────────

ALTER TABLE faculty ADD COLUMN country_id BIGINT DEFAULT 1;
UPDATE faculty SET country_id = 1;
ALTER TABLE faculty ALTER COLUMN country_id SET NOT NULL;
ALTER TABLE faculty ALTER COLUMN country_id DROP DEFAULT;
ALTER TABLE faculty
    ADD CONSTRAINT fk_faculty_country
    FOREIGN KEY (country_id) REFERENCES location_countries(id);

-- ── 3. Students: add country_id (defaults to India) ──────────────────────────

ALTER TABLE students ADD COLUMN country_id BIGINT DEFAULT 1;
UPDATE students SET country_id = 1;
ALTER TABLE students ALTER COLUMN country_id SET NOT NULL;
ALTER TABLE students ALTER COLUMN country_id DROP DEFAULT;
ALTER TABLE students
    ADD CONSTRAINT fk_students_country
    FOREIGN KEY (country_id) REFERENCES location_countries(id);
