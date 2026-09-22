-- Category short code (e.g. "STA" for Stationery) — the prefix half of the auto-generated
-- Product code pattern <CategoryShortCode>-<sequence> (e.g. STA-000001). Nullable: existing
-- categories predate this feature and have none until an admin sets one via Manage Categories;
-- the application layer requires it on every create/update going forward (CategoryRequest).
-- Unique index is a partial index (like uq_categories_name_root, V422) so multiple NULLs — i.e.
-- every not-yet-migrated legacy category — don't collide with each other.

ALTER TABLE categories ADD COLUMN short_code VARCHAR(10);

CREATE UNIQUE INDEX uq_categories_short_code ON categories(short_code) WHERE short_code IS NOT NULL;
