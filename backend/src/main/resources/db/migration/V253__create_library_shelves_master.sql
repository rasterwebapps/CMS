-- V253: Library shelf tiers master (a shelf level within a rack, e.g. Top/Middle/Bottom),
-- scoped to a rack. Uniqueness is per-rack (not global) since two racks may each have a "Tier 1".

CREATE TABLE library_shelves (
    id          BIGSERIAL       PRIMARY KEY,
    rack_id     BIGINT          NOT NULL REFERENCES library_racks(id),
    name        VARCHAR(200)    NOT NULL,
    code        VARCHAR(50)     NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_library_shelves_name UNIQUE (rack_id, name),
    CONSTRAINT uq_library_shelves_code UNIQUE (rack_id, code)
);

CREATE INDEX idx_library_shelves_rack ON library_shelves (rack_id);
