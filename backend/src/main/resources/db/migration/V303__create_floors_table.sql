-- Campus Infrastructure hierarchy, level 2: belongs to exactly one Block. floor_number drives
-- display ordering (e.g. Ground=0, 1st=1, ...) and is unique per block alongside name.
--
-- is_hostel + gender_restriction: see V300 — same cascade-down-to-children behavior, scoped to
-- this floor's Zones.

CREATE TABLE floors (
    id                  BIGSERIAL     PRIMARY KEY,
    block_id            BIGINT        NOT NULL REFERENCES blocks(id),
    name                VARCHAR(100)  NOT NULL,
    floor_number        INTEGER       NOT NULL,
    is_hostel           BOOLEAN       NOT NULL DEFAULT FALSE,
    gender_restriction  VARCHAR(20),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_floors_block_name   UNIQUE (block_id, name),
    CONSTRAINT uq_floors_block_number UNIQUE (block_id, floor_number),
    CONSTRAINT chk_floors_gender_restriction CHECK (gender_restriction IN ('BOYS', 'GIRLS') OR gender_restriction IS NULL)
);

CREATE INDEX idx_floors_block_id ON floors(block_id);
