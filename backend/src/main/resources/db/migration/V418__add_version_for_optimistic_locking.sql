-- Two write paths in the Assign Faculty dialog (batch name/capacity/coordinator, and Theory
-- section/cohort faculty) had no concurrency protection at all -- a full-replace PUT with no
-- version/updatedAt precondition, so two admins editing the same offering at once would silently
-- last-write-wins overwrite each other with no warning. Adds a JPA @Version column to both tables
-- so a stale save is rejected with a clear conflict instead of silently clobbering.

ALTER TABLE batches ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE course_offering_section_faculty ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
