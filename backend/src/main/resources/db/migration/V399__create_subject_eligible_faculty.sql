-- Admin-curated widening of who can be assigned as a Subject's primary/secondary/section faculty,
-- additive to (never a replacement for) the existing Speciality-match rule in FacultyEligibility --
-- see that class for the actual eligibility logic. An empty set here means Speciality-match-only,
-- identical to behavior before this table existed; this is deliberately NOT the same "hard-gate on
-- empty" behavior subject_eligible_labs/subject_eligible_clinical_venues actually have.

CREATE TABLE subject_eligible_faculty (
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    faculty_id BIGINT NOT NULL REFERENCES faculty(id) ON DELETE CASCADE,
    PRIMARY KEY (subject_id, faculty_id)
);
