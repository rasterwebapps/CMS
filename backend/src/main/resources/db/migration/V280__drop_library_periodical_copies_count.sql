-- V280: Remove the legacy copies_count column from library_periodicals.
-- Every periodical row saved through the app since V250 has a mandatory,
-- unique accession_number and the backend always forces copies_count=1
-- once one is present — so the field has been dead on any row anyone has
-- touched since then. Confirmed via a read-only production check
-- (SELECT COUNT(*) FROM library_periodicals WHERE accession_number IS NULL)
-- that no un-migrated legacy row remains, so it's safe to drop outright.

ALTER TABLE library_periodicals DROP COLUMN copies_count;
