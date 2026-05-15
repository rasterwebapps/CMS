ALTER TABLE app_users ADD COLUMN IF NOT EXISTS cover_photo       BYTEA;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS cover_photo_type  VARCHAR(50);
