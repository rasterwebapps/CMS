CREATE TABLE semester_fees (
    id              BIGSERIAL PRIMARY KEY,
    allocation_id   BIGINT                      NOT NULL REFERENCES student_fee_allocations(id),
    year_number     INTEGER                     NOT NULL,
    semester_label  VARCHAR(100)                NOT NULL,
    amount          NUMERIC(12,2)               NOT NULL,
    due_date        DATE                        NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE    NOT NULL
);
