-- Classroom master for THEORY session scheduling. Mirrors the existing `labs` table shape
-- minus the lab-specific lab_type/speciality_id scoping — classrooms are general-purpose rooms,
-- not speciality-scoped. First step of the timetable-generator effort: theory sessions had zero
-- physical-room concept before this (CourseOffering has no room/time fields at all).

CREATE TABLE classrooms (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL UNIQUE,
    building     VARCHAR(255),
    room_number  VARCHAR(255),
    capacity     INTEGER,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);
