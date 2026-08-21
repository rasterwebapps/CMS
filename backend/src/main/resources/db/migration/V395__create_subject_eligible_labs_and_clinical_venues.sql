-- Which Labs/Clinical Venues are appropriate for a Subject's practical sessions -- previously no
-- such binding existed anywhere, so the auto-suggest algorithm and every manual picker offered any
-- active lab/venue for any subject's practical session, filtered only by capacity. Pure many-to-many
-- with no extra attributes (unlike lab_curriculum_mappings, which carries real payload columns), so
-- plain join tables. Soft preference, not a hard restriction -- see
-- TimetableCapacityPlanningService.suggestBatchesForSessionType.

CREATE TABLE subject_eligible_labs (
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    lab_id BIGINT NOT NULL REFERENCES labs(id) ON DELETE CASCADE,
    PRIMARY KEY (subject_id, lab_id)
);

CREATE TABLE subject_eligible_clinical_venues (
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    clinical_venue_id BIGINT NOT NULL REFERENCES clinical_venues(id) ON DELETE CASCADE,
    PRIMARY KEY (subject_id, clinical_venue_id)
);
