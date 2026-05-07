-- V96: Add country, state, district to enquiries for location tracking.
ALTER TABLE enquiries ADD COLUMN IF NOT EXISTS country  VARCHAR(100);
ALTER TABLE enquiries ADD COLUMN IF NOT EXISTS state    VARCHAR(100);
ALTER TABLE enquiries ADD COLUMN IF NOT EXISTS district VARCHAR(100);
