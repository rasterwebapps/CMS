-- Unlike the 6 labs V473 handled, "Computer lab" had zero legacy building/room_number data
-- recorded -- there was nothing real to derive a room from. The user explicitly authorized an
-- arbitrary placeholder assignment instead ("Assign computer lab to any physical location"),
-- overriding the earlier "never guess real institutional data" caution for this one case.
--
-- THIS IS NOT DERIVED FROM ANY REAL INSTITUTIONAL RECORD -- flagged here and in
-- docs/inventory-management/DECISION_LOG.md's "Lab room-assignment migration (V473/V474)" entry
-- so a future session doesn't mistake this Room as verified real-world fact. Correct it via the
-- normal Campus Infrastructure / Labs screens if the real physical room ever becomes known.
--
-- Placement: Main Block, Ground Floor, Main Zone (same base already used for two other labs in
-- V473), room_number "G-03" (next free in that series), sized to the lab's own capacity.
-- sub_type = "Computer Lab" -- unlike the nursing labs in V473, this one has a real matching
-- room_sub_type, so it's used (looked up by name, not hardcoded id).

INSERT INTO rooms (zone_id, room_number, capacity, description, is_active, order_index, purpose_category_id, sub_type_id, created_at, updated_at)
SELECT z.id, 'G-03', l.capacity,
       l.name || ' (placeholder room, V474 -- arbitrarily assigned per explicit user instruction, no real data existed)',
       true,
       COALESCE((SELECT MAX(r.order_index) + 1 FROM rooms r WHERE r.zone_id = z.id), 0),
       (SELECT id FROM room_purpose_categories WHERE name = 'Academic'),
       (SELECT id FROM room_sub_types WHERE name = 'Computer Lab'),
       NOW(), NOW()
FROM labs l
JOIN blocks b ON b.name = 'Main Block'
JOIN floors f ON f.block_id = b.id AND f.name = 'Ground Floor'
JOIN zones z ON z.floor_id = f.id AND z.name = 'Main Zone'
WHERE l.name = 'Computer lab' AND l.room_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.zone_id = z.id AND r.room_number = 'G-03');

UPDATE labs l
SET room_id = r.id, updated_at = NOW()
FROM rooms r
JOIN zones z ON r.zone_id = z.id
JOIN floors f ON z.floor_id = f.id
JOIN blocks b ON f.block_id = b.id
WHERE l.name = 'Computer lab' AND l.room_id IS NULL
  AND b.name = 'Main Block' AND f.name = 'Ground Floor' AND z.name = 'Main Zone'
  AND r.room_number = 'G-03';
