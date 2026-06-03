ALTER TABLE faculty ADD COLUMN nrts_number VARCHAR(50);
CREATE UNIQUE INDEX idx_faculty_nrts_number ON faculty (nrts_number) WHERE nrts_number IS NOT NULL;
