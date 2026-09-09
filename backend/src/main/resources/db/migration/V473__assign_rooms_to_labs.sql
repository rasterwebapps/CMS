-- Backfills labs.room_id for the labs that have real legacy building/room_number free-text data
-- (entered previously, describing their actual physical location) but were never linked to a real
-- Room in Core Infrastructure -- the precondition blocking the InventoryItem -> Product/StockBalance
-- migration, tracked since 2026-09-07 in docs/inventory-management/DECISION_LOG.md. "Computer lab"
-- has no building/room_number recorded at all and is deliberately left unassigned -- see the
-- 2026-09-09 decision-log entry for this migration.
--
-- Idempotent throughout (NOT EXISTS guards on every insert, matched joins on the UPDATE) so it is
-- safe to run against any environment's Core Infrastructure data -- one where Main Block doesn't
-- have this exact Floor/Zone/Room structure simply sees the relevant INSERTs/UPDATE find no
-- matching parent and no-op, rather than guessing or corrupting anything.
--
-- Deliberately creates brand-new Room rows sized to each lab's own capacity, rather than reusing
-- the existing, much larger-capacity Main Block rooms (G-101 @ 80, F-101 @ 120, etc.) -- those have
-- mismatched room numbers and would be a materially wrong physical-room/capacity assignment.

-- 1. "Second Floor" doesn't exist yet under Main Block (only Ground Floor=0, First Floor=1, and
--    three Basements exist) -- OBG Lab/Medical Surgical Lab's legacy room_numbers (F2-01/F2-02)
--    need it.
INSERT INTO floors (block_id, name, floor_number, is_hostel, gender_restriction, is_active, is_basement, created_at, updated_at)
SELECT b.id, 'Second Floor', 2, false, NULL, true, false, NOW(), NOW()
FROM blocks b
WHERE b.name = 'Main Block'
  AND NOT EXISTS (SELECT 1 FROM floors f WHERE f.block_id = b.id AND f.name = 'Second Floor');

-- 2. Second Floor needs its own "Main Zone" (zones are scoped per floor), matching the naming
--    Ground Floor/First Floor already use for their own Main Zone.
INSERT INTO zones (floor_id, name, is_hostel, gender_restriction, warden_id, is_active, order_index, created_at, updated_at)
SELECT f.id, 'Main Zone', false, NULL, NULL, true,
       COALESCE((SELECT MAX(z.order_index) + 1 FROM zones z WHERE z.floor_id = f.id), 0),
       NOW(), NOW()
FROM floors f
JOIN blocks b ON f.block_id = b.id
WHERE b.name = 'Main Block' AND f.name = 'Second Floor'
  AND NOT EXISTS (SELECT 1 FROM zones z WHERE z.floor_id = f.id AND z.name = 'Main Zone');

-- 3. One new Room per lab, in the Main Zone of the floor its legacy room_number prefix implies
--    (G- -> Ground Floor, F1- -> First Floor, F2- -> the new Second Floor above), reusing each
--    lab's own already-recorded room_number and capacity. purpose_category = Academic (looked up
--    by name); no room_sub_type fits a nursing lab specifically among the existing options, so
--    left NULL rather than forcing a wrong one.
INSERT INTO rooms (zone_id, room_number, capacity, description, is_active, order_index, purpose_category_id, sub_type_id, created_at, updated_at)
SELECT z.id, l.room_number, l.capacity, l.name || ' (migrated from legacy Lab record, V473)', true,
       COALESCE((SELECT MAX(r.order_index) + 1 FROM rooms r WHERE r.zone_id = z.id), 0),
       (SELECT id FROM room_purpose_categories WHERE name = 'Academic'), NULL, NOW(), NOW()
FROM labs l
JOIN blocks b ON b.name = l.building
JOIN floors f ON f.block_id = b.id AND f.name = CASE
    WHEN l.room_number LIKE 'G-%' THEN 'Ground Floor'
    WHEN l.room_number LIKE 'F1-%' THEN 'First Floor'
    WHEN l.room_number LIKE 'F2-%' THEN 'Second Floor'
END
JOIN zones z ON z.floor_id = f.id AND z.name = 'Main Zone'
WHERE l.room_id IS NULL
  AND l.building IS NOT NULL AND l.room_number IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.zone_id = z.id AND r.room_number = l.room_number);

-- 4. Link each such lab to its newly created Room (Room has no back-reference to Lab, so re-derive
--    the same building/floor-prefix/room_number chain used to create it in step 3).
UPDATE labs l
SET room_id = r.id, updated_at = NOW()
FROM rooms r
JOIN zones z ON r.zone_id = z.id
JOIN floors f ON z.floor_id = f.id
JOIN blocks b ON f.block_id = b.id
WHERE l.room_id IS NULL
  AND l.building = b.name
  AND r.room_number = l.room_number
  AND z.name = 'Main Zone'
  AND f.name = CASE
      WHEN l.room_number LIKE 'G-%' THEN 'Ground Floor'
      WHEN l.room_number LIKE 'F1-%' THEN 'First Floor'
      WHEN l.room_number LIKE 'F2-%' THEN 'Second Floor'
  END;
