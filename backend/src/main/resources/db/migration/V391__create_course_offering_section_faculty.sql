-- Per-section Theory faculty override for a Course Offering. Sparse by design: a row only exists
-- for a section whose Theory delivery diverges from the offering's own primary faculty_id — a
-- section with no row here falls back to the offering's primary, matching exactly how
-- batches.coordinator_faculty_id (V271) already works for LAB/CLINICAL. Scoped to
-- cohort_sections (a Theory classroom-capacity split), not batches, since batches carry
-- capacity/roster/venue fields that don't apply to "who lectures this section" and are already a
-- distinct, smaller-grained concept (practical sub-groups within a section).
CREATE TABLE course_offering_section_faculty (
    id                  BIGSERIAL PRIMARY KEY,
    course_offering_id  BIGINT NOT NULL REFERENCES course_offerings(id),
    cohort_section_id   BIGINT NOT NULL REFERENCES cohort_sections(id),
    faculty_id          BIGINT NOT NULL REFERENCES faculty(id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (course_offering_id, cohort_section_id)
);

CREATE INDEX idx_course_offering_section_faculty_offering_id ON course_offering_section_faculty(course_offering_id);
