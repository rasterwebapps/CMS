package com.cms.inventory.procurement.model.enums;

/**
 * Lifecycle of a {@code PurchaseRequisition} header. Mirrors {@code CycleCountStatus}'s shape —
 * see the "Purchase Requisition slice" decision-log entry for the full flow.
 */
public enum PurchaseRequisitionStatus {
    /** Being built — lines can be added/removed. */
    DRAFT,
    /** Lines are locked in; PENDING lines await approve/reject. */
    SUBMITTED,
    /** Every line is in a terminal state (APPROVED or REJECTED). */
    COMPLETED,
    /** Abandoned before submission — only reachable from DRAFT. */
    CANCELLED
}
