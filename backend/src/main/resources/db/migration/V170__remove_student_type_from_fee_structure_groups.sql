-- Student type is no longer a fee dimension.
-- Day scholar vs hosteler cost is implicit: HOSTEL_FEE row = hosteler surcharge.
-- Clear fee structure data and narrow the unique constraint to 6 fields.

DELETE FROM fee_structure_year_amounts;
DELETE FROM fee_structures;
DELETE FROM fee_structure_groups;

ALTER TABLE fee_structure_groups DROP CONSTRAINT uq_fee_structure_group;
ALTER TABLE fee_structure_groups DROP COLUMN student_type;

ALTER TABLE fee_structure_groups
    ADD CONSTRAINT uq_fee_structure_group
    UNIQUE (program_id, academic_year_id, course_id, quota, fee_state_id, gender);
