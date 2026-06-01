-- V189 — Add counselling_closed and counselling_closed_date to cohorts.
-- Allows per-cohort declaration that government counselling rounds have ended
-- so that remaining unfilled counselling seats can be tracked as "govt lapsed".

ALTER TABLE cohorts
    ADD COLUMN counselling_closed      BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN counselling_closed_date DATE               DEFAULT NULL;
