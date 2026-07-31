-- Persisted display order for Zones (within a Floor) and Rooms (within a Zone), enabling
-- drag-to-reorder in the Campus Setup skyline view. Both lists were previously always sorted
-- alphabetically (name / room_number) with no way to express a custom order.

ALTER TABLE zones ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0;
ALTER TABLE rooms ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0;

-- Backfill existing rows with their current alphabetical position so drag-reordering starts from
-- what's already on screen, not an arbitrary reset to all-zero.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY floor_id ORDER BY name) - 1 AS rn
    FROM zones
)
UPDATE zones SET order_index = ranked.rn
FROM ranked
WHERE zones.id = ranked.id;

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY zone_id ORDER BY room_number) - 1 AS rn
    FROM rooms
)
UPDATE rooms SET order_index = ranked.rn
FROM ranked
WHERE rooms.id = ranked.id;

CREATE INDEX idx_zones_floor_order ON zones(floor_id, order_index);
CREATE INDEX idx_rooms_zone_order ON rooms(zone_id, order_index);
