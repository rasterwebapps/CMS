-- Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests") third and final slice — general
-- internal complaint/service-request ticketing. service_ticket_categories is a configurable
-- lookup master (no hospital/college-specific category hard-coded, per the reference
-- architecture); service_tickets is the ticket itself. See ER_DIAGRAM_AND_MODULE_BOUNDARIES.md
-- section 6 and the "Service Ticket slice" decision-log entry.

CREATE TABLE service_ticket_categories (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_service_ticket_category_name UNIQUE (name)
);

CREATE TABLE service_tickets (
    id                    BIGSERIAL PRIMARY KEY,
    location_id           BIGINT       NOT NULL REFERENCES inventory_locations(id),
    category_id           BIGINT       NOT NULL REFERENCES service_ticket_categories(id),
    requested_by          VARCHAR(200) NOT NULL,
    priority              VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM'
                               CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status                VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                               CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    description           VARCHAR(1000) NOT NULL,
    assigned_to           VARCHAR(200),
    assigned_at           TIMESTAMP,
    resolution_notes      VARCHAR(1000),
    resolution_date       DATE,
    resolved_by           VARCHAR(255),
    feedback_rating       INTEGER      CHECK (feedback_rating IS NULL OR (feedback_rating BETWEEN 1 AND 5)),
    closed_by             VARCHAR(255),
    closed_at             TIMESTAMP,
    cancelled_by          VARCHAR(255),
    cancelled_at          TIMESTAMP,
    cancellation_reason   VARCHAR(500),
    created_by            VARCHAR(255),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_tickets_location ON service_tickets(location_id);
CREATE INDEX idx_service_tickets_category ON service_tickets(category_id);
CREATE INDEX idx_service_tickets_status ON service_tickets(status);
