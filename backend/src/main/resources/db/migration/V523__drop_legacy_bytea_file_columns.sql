-- V523: Drop the legacy bytea columns that held file bytes directly in
-- Postgres, from before the MinIO migration (V249 added storage_key /
-- profile_photo_key / cover_photo_key as the MinIO-backed replacement).
--
-- Every row's MinIO copy has been verified in production (0 rows remain with
-- non-null file_data/profile_photo/cover_photo there), and the application no
-- longer reads or writes these columns anywhere (the bytea fallback in
-- EnquiryDocumentService/AdmissionDocumentService/FacultyDocumentService/
-- ProfileService was removed in the same change that adds this migration).

ALTER TABLE enquiry_documents DROP COLUMN IF EXISTS file_data;
ALTER TABLE faculty_documents DROP COLUMN IF EXISTS file_data;
ALTER TABLE app_users DROP COLUMN IF EXISTS profile_photo;
ALTER TABLE app_users DROP COLUMN IF EXISTS cover_photo;
