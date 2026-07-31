-- Persisted display order for Blocks (within a Branch), enabling drag-to-reorder in the Campus
-- Setup skyline view — same pattern as V338 (Zones within a Floor, Rooms within a Zone). Blocks
-- were previously always sorted alphabetically (name) with no way to express a custom order.

ALTER TABLE blocks ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0;

-- Backfill existing rows with their current alphabetical position so drag-reordering starts from
-- what's already on screen, not an arbitrary reset to all-zero.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY branch_id ORDER BY name) - 1 AS rn
    FROM blocks
)
UPDATE blocks SET order_index = ranked.rn
FROM ranked
WHERE blocks.id = ranked.id;

CREATE INDEX idx_blocks_branch_order ON blocks(branch_id, order_index);
