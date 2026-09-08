package com.cms.inventory.gatepass.model.enums;

/**
 * A gate pass has two distinct actors, per the reference architecture ({@code
 * ER_DIAGRAM_AND_MODULE_BOUNDARIES.md} §6): whoever approves the request, and whoever physically
 * verifies the item at the gate (security) — these are always different steps even when the same
 * person happens to hold both permissions. {@code GATE_VERIFIED} is the "in effect" state for a
 * returnable pass (the window during which it can go overdue); a non-returnable pass skips
 * straight to {@code CLOSED} once verified, since there's nothing further to track. See the
 * "Gate Pass slice" decision-log entry.
 */
public enum GatePassStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    GATE_VERIFIED,
    RETURNED,
    CLOSED
}
