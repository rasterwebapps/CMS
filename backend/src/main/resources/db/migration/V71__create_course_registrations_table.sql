CREATE TABLE course_registrations (
    id                              BIGSERIAL PRIMARY KEY,
    student_term_enrollment_id      BIGINT NOT NULL REFERENCES student_term_enrollments(id),
    course_offering_id              BIGINT NOT NULL REFERENCES course_offerings(id),
    status                          VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (student_term_enrollment_id, course_offering_id)
);

CREATE INDEX idx_cr_enrollment ON course_registrations(student_term_enrollment_id);
CREATE INDEX idx_cr_offering ON course_registrations(course_offering_id);
