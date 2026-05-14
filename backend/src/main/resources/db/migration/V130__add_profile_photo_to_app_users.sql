-- V130: Add profile photo storage to app_users.
-- Photos are stored as bytea blobs on the user row.
-- profile_photo_type stores MIME type (image/jpeg or image/png).
-- Maximum size (2 MB) is enforced at the application layer.
ALTER TABLE app_users
    ADD COLUMN profile_photo      BYTEA,
    ADD COLUMN profile_photo_type VARCHAR(50);

