-- Government-mandated minimum age restriction per program.
-- Default values (17 years, 31 Dec) match the typical nursing admission rule.
-- Admins must review and update via the Program form for each program.
ALTER TABLE programs
  ADD COLUMN minimum_age_years INTEGER NOT NULL DEFAULT 17,
  ADD COLUMN age_cutoff_day    INTEGER NOT NULL DEFAULT 31,
  ADD COLUMN age_cutoff_month  INTEGER NOT NULL DEFAULT 12;
