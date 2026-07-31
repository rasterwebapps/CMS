-- Clinical Venue master (R3 Phase 2) — hospital wards/departments used for CLINICAL sessions,
-- kept separate from the campus Lab/Classroom masters since a clinical posting site isn't a
-- campus room. Mirrors the `classrooms` table shape.

CREATE TABLE clinical_venues (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    hospital_name   VARCHAR(255),
    department      VARCHAR(255),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
