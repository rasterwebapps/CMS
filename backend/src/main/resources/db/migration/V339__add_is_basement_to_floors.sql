-- Explicit skyline/earthline marker for a Floor, replacing the implicit "floor_number < 0 means
-- basement" convention — that convention proved fragile in practice: real data twice had a floor
-- literally named "Ground Floor"/"Underground" with a floor_number that didn't match the sign the
-- Campus Setup skyline diagram assumed, rendering it in the wrong place with no way to correct the
-- *display* without also renumbering the floor's actual ordering key.

ALTER TABLE floors ADD COLUMN is_basement BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill from the old convention so already-correct data doesn't change — only rows that were
-- already numbered against the convention (and thus already displaying wrong) need a human to now
-- flip this flag explicitly via the Floor edit form.
UPDATE floors SET is_basement = TRUE WHERE floor_number < 0;
