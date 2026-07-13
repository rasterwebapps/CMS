-- Choice-based elective support for INC-compliant curricula: a group of mutually-exclusive
-- subject offerings within one curriculum version + term, from which a student picks exactly one
-- (e.g. "Semester VII Elective Group A: Community Health OR School Health Nursing").
-- Groups are scoped to a single curriculum version + term; curriculum_term_courses rows that
-- belong to the same group (added in V266) are the group's mutually-exclusive options.

CREATE TABLE curriculum_elective_groups (
    id                      BIGSERIAL PRIMARY KEY,
    curriculum_version_id   BIGINT NOT NULL REFERENCES curriculum_versions(id),
    term_number             INTEGER NOT NULL,
    group_name              VARCHAR(150) NOT NULL,
    group_code              VARCHAR(50),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (curriculum_version_id, term_number, group_code)
);

CREATE INDEX idx_curriculum_elective_groups_version_term
    ON curriculum_elective_groups(curriculum_version_id, term_number);
