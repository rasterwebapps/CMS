-- BR-30: Fee structure groups — holds the 4 admission dimensions
-- One group → many FeeStructure items (one per FeeType)
CREATE TABLE fee_structure_groups (
    id               BIGSERIAL    PRIMARY KEY,
    program_id       BIGINT       NOT NULL REFERENCES programs(id),
    academic_year_id BIGINT       NOT NULL REFERENCES academic_years(id),
    course_id        BIGINT       REFERENCES courses(id),
    quota            VARCHAR(20)  NOT NULL,
    fee_state_id     BIGINT       NOT NULL REFERENCES fee_states(id),
    gender           VARCHAR(10)  NOT NULL,
    student_type     VARCHAR(20)  NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_fee_structure_group
        UNIQUE (program_id, academic_year_id, course_id, quota, fee_state_id, gender, student_type)
);

CREATE INDEX idx_fsg_program_year ON fee_structure_groups(program_id, academic_year_id);
