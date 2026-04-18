-- V50: Rename course_id to subject_id in experiments and lab_schedules tables
-- These tables were missed during the V37 courses→subjects restructure

ALTER TABLE experiments RENAME COLUMN course_id TO subject_id;
ALTER TABLE lab_schedules RENAME COLUMN course_id TO subject_id;
