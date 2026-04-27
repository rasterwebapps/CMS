CREATE TABLE calendar_events (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255)             NOT NULL,
    description      TEXT,
    start_date       DATE                     NOT NULL,
    end_date         DATE                     NOT NULL,
    event_type       VARCHAR(20)              NOT NULL,
    academic_year_id BIGINT                   NOT NULL REFERENCES academic_years(id),
    semester_id      BIGINT                   REFERENCES semesters(id),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL
);
