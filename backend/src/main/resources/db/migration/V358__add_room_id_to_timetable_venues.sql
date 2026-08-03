-- Links Timetable's virtual venue masters (Classroom/Lab/ClinicalVenue) back to the
-- physical Campus Setup Room hierarchy (Organization->Branch->Block->Floor->Zone->Room).
-- Non-unique: one physical Room can carry multiple virtual identities over time
-- (e.g. Room 123 is sometimes a Classroom, sometimes used as a Seminar Hall), mirroring
-- the existing HostelRoom precedent. Nullable and additive: legacy rows keep their
-- free-text building/room_number fields until an admin explicitly links a Room.
ALTER TABLE classrooms ADD COLUMN room_id BIGINT REFERENCES rooms(id);
ALTER TABLE labs ADD COLUMN room_id BIGINT REFERENCES rooms(id);
ALTER TABLE clinical_venues ADD COLUMN room_id BIGINT REFERENCES rooms(id);

CREATE INDEX idx_classrooms_room_id ON classrooms(room_id);
CREATE INDEX idx_labs_room_id ON labs(room_id);
CREATE INDEX idx_clinical_venues_room_id ON clinical_venues(room_id);