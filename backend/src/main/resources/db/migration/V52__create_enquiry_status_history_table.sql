CREATE TABLE enquiry_status_history (
    id          BIGSERIAL PRIMARY KEY,
    enquiry_id  BIGINT NOT NULL REFERENCES enquiries(id),
    from_status VARCHAR(50),
    to_status   VARCHAR(50) NOT NULL,
    changed_by  VARCHAR(255) NOT NULL,
    changed_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    remarks     TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
