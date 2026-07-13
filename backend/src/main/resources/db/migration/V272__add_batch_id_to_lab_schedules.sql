-- Additive-then-deprecate: lab_schedules keeps its existing free-text batch_name column so
-- existing rows and the current conflict-check queries keep working unchanged, while new/edited
-- rows also populate the real batch_id FK. A hard cutover (dropping batch_name) is out of scope
-- for this pass.

ALTER TABLE lab_schedules ADD COLUMN batch_id BIGINT REFERENCES batches(id);

CREATE INDEX idx_lab_schedules_batch_id ON lab_schedules(batch_id);
