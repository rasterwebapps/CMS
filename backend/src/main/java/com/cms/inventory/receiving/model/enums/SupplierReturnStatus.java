package com.cms.inventory.receiving.model.enums;

/**
 * Simple lifecycle for a {@code SupplierReturn} — no approval gate this phase, same posture as
 * every other DRAFT-first header in this module. {@code CANCELLED} reachable only from {@code
 * DRAFT}. See the "Return to Supplier slice" decision-log entry.
 */
public enum SupplierReturnStatus {
    DRAFT,
    COMPLETED,
    CANCELLED
}
