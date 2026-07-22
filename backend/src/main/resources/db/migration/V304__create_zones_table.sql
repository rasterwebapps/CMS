-- Campus Infrastructure hierarchy, level 3: belongs to exactly one Floor. warden_id is optional
-- and independent per zone, so a mixed floor can have separate wardens per wing.
--
-- is_hostel + gender_restriction: see V300 — this is the leaf level for the cascade (Room has no
-- such field). Can be set directly on a Zone even if its parent Floor/Block are not hostel-marked,
-- to cover a single hostel wing inside an otherwise non-hostel floor/block.

CREATE TABLE zones (
    id                  BIGSERIAL     PRIMARY KEY,
    floor_id            BIGINT        NOT NULL REFERENCES floors(id),
    name                VARCHAR(100)  NOT NULL,
    is_hostel           BOOLEAN       NOT NULL DEFAULT FALSE,
    gender_restriction  VARCHAR(20),
    warden_id           BIGINT        REFERENCES faculty(id),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_zones_floor_name UNIQUE (floor_id, name),
    CONSTRAINT chk_zones_gender_restriction CHECK (gender_restriction IN ('BOYS', 'GIRLS') OR gender_restriction IS NULL)
);

CREATE INDEX idx_zones_floor_id  ON zones(floor_id);
CREATE INDEX idx_zones_warden_id ON zones(warden_id);
