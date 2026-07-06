-- V249: Add storage_key columns to faculty_documents and app_users for MinIO references.
-- file_data / profile_photo / cover_photo columns are NOT dropped here — they are kept
-- as a safety fallback and will be nulled out manually once MinIO migration is verified.

ALTER TABLE faculty_documents
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(500);

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS profile_photo_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS cover_photo_key   VARCHAR(500);
