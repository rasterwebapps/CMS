-- Inventory Management (Release 3) — Phase 6 "Budgets & Approvals" second and largest slice:
-- Multi-level approval routing. A generic, standalone approval-chain engine — workflows (with
-- sequential/parallel steps referencing the platform's existing permissions table) and instances
-- (actual documents routed through one, with per-step actions). Deliberately NOT a rewrite of
-- purchase_requisitions/purchase_orders' own existing single-permission approve/reject — see
-- docs/inventory-management/DECISION_LOG.md's "Multi-level approval routing slice" entry for the
-- full scope boundary. Column names verified against V426's inventory_locations, the existing
-- permissions table, and V434/V438's purchase_requisitions/purchase_orders.

CREATE TABLE approval_workflows (
    id            BIGSERIAL     PRIMARY KEY,
    name          VARCHAR(150)  NOT NULL,
    document_type VARCHAR(30)   NOT NULL,
    location_id   BIGINT        REFERENCES inventory_locations(id),
    min_amount    NUMERIC(14,2),
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_approval_workflows_document_type CHECK (document_type IN ('PURCHASE_REQUISITION', 'PURCHASE_ORDER'))
);

CREATE INDEX idx_approval_workflows_document_type ON approval_workflows(document_type);
CREATE INDEX idx_approval_workflows_location ON approval_workflows(location_id);

CREATE TABLE approval_workflow_steps (
    id            BIGSERIAL     PRIMARY KEY,
    workflow_id   BIGINT        NOT NULL REFERENCES approval_workflows(id) ON DELETE CASCADE,
    step_order    INTEGER       NOT NULL,
    step_name     VARCHAR(150)  NOT NULL,
    permission_id BIGINT        NOT NULL REFERENCES permissions(id)
);

CREATE INDEX idx_approval_workflow_steps_workflow ON approval_workflow_steps(workflow_id);

CREATE TABLE approval_instances (
    id                       BIGSERIAL     PRIMARY KEY,
    workflow_id              BIGINT        NOT NULL REFERENCES approval_workflows(id),
    purchase_requisition_id  BIGINT        REFERENCES purchase_requisitions(id),
    purchase_order_id        BIGINT        REFERENCES purchase_orders(id),
    status                   VARCHAR(20)   NOT NULL DEFAULT 'IN_PROGRESS',
    current_step_order       INTEGER       NOT NULL,
    initiated_by             VARCHAR(255),
    initiated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    completed_at             TIMESTAMPTZ,
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_approval_instances_status CHECK (status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_approval_instances_one_document CHECK (
        (purchase_requisition_id IS NOT NULL AND purchase_order_id IS NULL)
        OR (purchase_requisition_id IS NULL AND purchase_order_id IS NOT NULL)
    )
);

CREATE INDEX idx_approval_instances_workflow ON approval_instances(workflow_id);
CREATE INDEX idx_approval_instances_requisition ON approval_instances(purchase_requisition_id);
CREATE INDEX idx_approval_instances_order ON approval_instances(purchase_order_id);
CREATE INDEX idx_approval_instances_status ON approval_instances(status);

CREATE TABLE approval_actions (
    id               BIGSERIAL     PRIMARY KEY,
    instance_id      BIGINT        NOT NULL REFERENCES approval_instances(id) ON DELETE CASCADE,
    workflow_step_id BIGINT        NOT NULL REFERENCES approval_workflow_steps(id),
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    acted_by         VARCHAR(255),
    acted_at         TIMESTAMPTZ,
    notes            VARCHAR(500),
    CONSTRAINT chk_approval_actions_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_approval_actions_instance ON approval_actions(instance_id);
CREATE INDEX idx_approval_actions_workflow_step ON approval_actions(workflow_step_id);
