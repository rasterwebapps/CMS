package com.cms.inventory.procurement.model.enums;

/**
 * Lifecycle of one {@code PurchaseRequisitionItem}. See the "Purchase Requisition slice"
 * decision-log entry for the full flow.
 */
public enum PurchaseRequisitionItemStatus {
    /** Added to the requisition, awaiting approve/reject (or the header is still DRAFT). */
    PENDING,
    /** Approved — the item is ready to be picked up into a Purchase Order. */
    APPROVED,
    /** Rejected — will not be purchased against this requisition. */
    REJECTED,
    /**
     * Picked up into a {@code PurchaseOrderItem} — terminal, like {@code APPROVED}/{@code
     * REJECTED}. Added by the "Purchase Order slice"; prevents the same approved line being
     * ordered twice. See {@code PurchaseOrderService}.
     */
    ORDERED
}
