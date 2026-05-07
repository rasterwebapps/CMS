ALTER TABLE students
    ADD COLUMN is_first_graduate BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN father_education VARCHAR(100),
    ADD COLUMN mother_education VARCHAR(100);

