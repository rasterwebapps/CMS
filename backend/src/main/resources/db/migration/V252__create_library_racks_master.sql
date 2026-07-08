-- V252: Library racks master (the physical rack/almirah unit), scoped to a library.
-- Uniqueness is per-library (not global) since two libraries may each have a "Rack A".

CREATE TABLE library_racks (
    id          BIGSERIAL       PRIMARY KEY,
    library_id  BIGINT          NOT NULL REFERENCES libraries(id),
    name        VARCHAR(200)    NOT NULL,
    code        VARCHAR(50)     NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_library_racks_name UNIQUE (library_id, name),
    CONSTRAINT uq_library_racks_code UNIQUE (library_id, code)
);

CREATE INDEX idx_library_racks_library ON library_racks (library_id);
