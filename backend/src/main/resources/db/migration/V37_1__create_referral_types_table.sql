CREATE TABLE referral_types (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255)                NOT NULL,
    code             VARCHAR(100)                NOT NULL UNIQUE,
    guideline_value  NUMERIC(10,2)               NOT NULL DEFAULT 0,
    description      VARCHAR(500),
    is_active        BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default referral types
INSERT INTO referral_types (name, code, guideline_value, description, is_active, created_at, updated_at) VALUES
('Walk-In', 'WALK_IN', 0, 'Direct walk-in enquiry', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Phone', 'PHONE', 0, 'Phone enquiry', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Online', 'ONLINE', 0, 'Online enquiry', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Agent Referral', 'AGENT_REFERRAL', 0, 'Referred by external agent', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Staff', 'STAFF', 0, 'Referred by staff member', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Alumni', 'ALUMNI', 0, 'Referred by alumni', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Parent', 'PARENT', 0, 'Referred by parent', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Advertisement', 'ADVERTISEMENT', 0, 'Through advertisement', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
