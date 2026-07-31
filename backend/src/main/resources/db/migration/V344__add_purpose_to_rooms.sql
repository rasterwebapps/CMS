-- Room-level purpose classification — the actual source of truth for "what is this room for",
-- replacing the Block/Floor/Zone isHostel cascade for that purpose (isHostel/genderRestriction
-- stay exactly as they are; nothing there is removed or migrated). Nullable, since existing rooms
-- (including the auto-created "Main" default room per Zone) have no value to backfill a guess for.

ALTER TABLE rooms ADD COLUMN purpose_category_id BIGINT REFERENCES room_purpose_categories(id);
ALTER TABLE rooms ADD COLUMN sub_type_id BIGINT REFERENCES room_sub_types(id);

CREATE INDEX idx_rooms_purpose_category ON rooms(purpose_category_id);
CREATE INDEX idx_rooms_sub_type ON rooms(sub_type_id);

-- Backfill: any room already designated a hostel room (hostel_rooms.room_id) is classified
-- Residential automatically — sub-type is left NULL, the admin refines it later via the Room form.
-- Idempotent and safe to re-run: only touches rows that don't already have a purpose set.
UPDATE rooms
SET purpose_category_id = (SELECT id FROM room_purpose_categories WHERE code = 'RESIDENTIAL')
WHERE purpose_category_id IS NULL
  AND EXISTS (SELECT 1 FROM hostel_rooms hr WHERE hr.room_id = rooms.id);
