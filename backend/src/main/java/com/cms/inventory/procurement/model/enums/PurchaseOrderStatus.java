package com.cms.inventory.procurement.model.enums;

/**
 * Lifecycle of a {@code PurchaseOrder} — mirrors IHMS's own receipt-progress-driven PO states
 * rather than a plain Draft/Sent/Closed set (the original "Phase 2 kickoff" plan, superseded by
 * the "Purchase Requisition slice" entry once IHMS was reviewed). No approval gate anywhere in
 * this lifecycle — real multi-level/parallel approval routing is Phase 6 scope, built once. See
 * the "Purchase Order slice" decision-log entry.
 */
public enum PurchaseOrderStatus {
    /** Being built — lines can still be added/removed. Not yet sent to the supplier. */
    PENDING,
    /** Sent to the supplier. Lines are locked; nothing has been received against it yet. */
    ORDERED,
    /** At least one line has a partial receipt, but no line is fully received yet. */
    IN_PROGRESS,
    /** At least one line is fully received, but at least one other line is not yet. */
    PARTIALLY_COMPLETED,
    /** Every line has been received in full. */
    COMPLETED,
    /** Manually closed out before every line was fully received (short-shipment, discontinued, etc.). */
    FORCE_CLOSED
}
