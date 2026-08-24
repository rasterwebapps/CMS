-- Class Teacher / Class Incharge: one faculty per CohortSection (a Theory classroom-capacity
-- split, term-scoped already via cohort_sections.term_instance_id), created here as a direct
-- nullable column rather than a sparse override table -- unlike course_offering_section_faculty
-- (which overrides per course-offering AND falls back to a primary), a class incharge has no
-- offering/primary to fall back to and there is exactly one per section, so a 1:1 column is the
-- simplest fit. Structurally created in Capacity Planner (the section row itself); WHO is
-- incharge is assigned later in Assign Faculty, same split as batches.coordinator_faculty_id.
ALTER TABLE cohort_sections
    ADD COLUMN class_incharge_faculty_id BIGINT REFERENCES faculty(id);
