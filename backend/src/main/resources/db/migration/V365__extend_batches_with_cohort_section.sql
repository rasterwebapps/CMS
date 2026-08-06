-- Optional link from a Lab/Clinical batch to the CohortSection sub-cohort it belongs to, once a
-- cohort's Theory room has been split into sections (see V364). Nullable so batches created
-- outside the Cohort Room Allocation flow, or belonging to an unsectioned (single-section)
-- allocation, are untouched.
ALTER TABLE batches ADD COLUMN cohort_section_id BIGINT REFERENCES cohort_sections(id);

CREATE INDEX idx_batches_cohort_section_id ON batches(cohort_section_id);
