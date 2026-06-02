-- V191 — Add management_closed and management_closed_date to cohorts.
-- Mirrors the counselling_closed fields added in V189, enabling per-quota
-- manual lock for both management and government counselling seats.

ALTER TABLE cohorts
    ADD COLUMN management_closed      BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN management_closed_date DATE               DEFAULT NULL;
