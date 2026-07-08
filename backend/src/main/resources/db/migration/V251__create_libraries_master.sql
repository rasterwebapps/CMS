-- V251: Libraries master — schema groundwork for future multi-library support.
-- Only one library exists today (seeded below); this table lets library_shelves
-- and library_books scope to a specific physical library without a later migration.

CREATE TABLE libraries (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(200)    NOT NULL,
    code        VARCHAR(50)     NOT NULL,
    address     VARCHAR(500),
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_libraries_name UNIQUE (name),
    CONSTRAINT uq_libraries_code UNIQUE (code)
);

INSERT INTO libraries (name, code) VALUES ('Main Library', 'MAIN');
