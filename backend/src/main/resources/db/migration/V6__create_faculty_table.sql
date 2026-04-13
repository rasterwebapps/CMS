CREATE TABLE faculty (
    id              BIGSERIAL PRIMARY KEY,
    employee_code   VARCHAR(255)                NOT NULL UNIQUE,
    first_name      VARCHAR(255)                NOT NULL,
    last_name       VARCHAR(255)                NOT NULL,
    email           VARCHAR(255)                NOT NULL UNIQUE,
    phone           VARCHAR(255),
    department_id   BIGINT                      NOT NULL REFERENCES departments(id),
    designation     VARCHAR(255)                NOT NULL,
    specialization  VARCHAR(255),
    lab_expertise   VARCHAR(1000),
    joining_date    DATE                        NOT NULL,
    status          VARCHAR(255)                NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE    NOT NULL
);
