-- Clinical venues had no capacity concept, unlike classrooms/labs/rooms -- needed so the
-- capacity-planning/staffing hard-block can compare a clinical session's cohort strength
-- against the venue the same way it already does for classrooms and labs.

ALTER TABLE clinical_venues ADD COLUMN capacity INTEGER;
ALTER TABLE clinical_venues ADD CONSTRAINT chk_clinical_venues_capacity CHECK (capacity IS NULL OR capacity > 0);