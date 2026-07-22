-- R2-4.1.3: non-binding room request, captured at enquiry and/or admission, distinct from the
-- binding RoomAllocation (V309). enquiry_id/student_id are both nullable but never both null --
-- a preference starts against an enquiry and the same row is carried forward (student_id filled
-- in) when that enquiry converts, rather than duplicated, since the request can keep changing.

CREATE TABLE room_preferences (
    id                     BIGSERIAL     PRIMARY KEY,
    enquiry_id             BIGINT        REFERENCES enquiries(id),
    student_id             BIGINT        REFERENCES students(id),
    preferred_room_type_id BIGINT        NOT NULL REFERENCES hostel_room_types(id),
    preferred_zone_id      BIGINT        REFERENCES zones(id),
    status                 VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    remarks                VARCHAR(500),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_room_preferences_owner CHECK (enquiry_id IS NOT NULL OR student_id IS NOT NULL),
    CONSTRAINT uq_room_preferences_enquiry UNIQUE (enquiry_id),
    CONSTRAINT uq_room_preferences_student UNIQUE (student_id)
);

CREATE INDEX idx_room_preferences_room_type_id ON room_preferences(preferred_room_type_id);
CREATE INDEX idx_room_preferences_zone_id ON room_preferences(preferred_zone_id);
