-- Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests") first slice — tracks a product
-- or asset physically leaving/entering the premises outside a normal stock movement (repair
-- send-outs, event/sports equipment loan-out, a vendor's own tool brought in for an on-site job).
-- See ER_DIAGRAM_AND_MODULE_BOUNDARIES.md section 6 and the "Gate Pass slice" decision-log entry.

CREATE TABLE gate_passes (
    id                        BIGSERIAL PRIMARY KEY,
    direction                 VARCHAR(20)  NOT NULL CHECK (direction IN ('OUTWARD', 'INWARD')),
    returnable                BOOLEAN      NOT NULL,
    product_id                BIGINT       REFERENCES products(id),
    asset_id                  BIGINT       REFERENCES assets(id),
    location_id               BIGINT       NOT NULL REFERENCES inventory_locations(id),
    quantity                  NUMERIC(14,3) NOT NULL,
    reason                    VARCHAR(500) NOT NULL,
    party_name                VARCHAR(200) NOT NULL,
    party_contact             VARCHAR(100),
    linked_purchase_order_id  BIGINT       REFERENCES purchase_orders(id),
    pass_date                 DATE         NOT NULL,
    expected_return_date      DATE,
    actual_return_date        DATE,
    status                    VARCHAR(20)  NOT NULL DEFAULT 'PENDING_APPROVAL'
                                   CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'GATE_VERIFIED', 'RETURNED', 'CLOSED')),
    notes                     VARCHAR(500),
    created_by                VARCHAR(255),
    created_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by               VARCHAR(255),
    approved_at               TIMESTAMP,
    rejected_by               VARCHAR(255),
    rejected_at               TIMESTAMP,
    rejection_reason          VARCHAR(500),
    gate_verified_by          VARCHAR(255),
    gate_verified_at          TIMESTAMP,
    returned_by               VARCHAR(255),
    returned_at               TIMESTAMP,
    updated_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT gate_passes_exactly_one_target CHECK (
        (product_id IS NOT NULL AND asset_id IS NULL) OR (product_id IS NULL AND asset_id IS NOT NULL)
    )
);

CREATE INDEX idx_gate_passes_location ON gate_passes(location_id);
CREATE INDEX idx_gate_passes_status ON gate_passes(status);
CREATE INDEX idx_gate_passes_product ON gate_passes(product_id);
CREATE INDEX idx_gate_passes_asset ON gate_passes(asset_id);
