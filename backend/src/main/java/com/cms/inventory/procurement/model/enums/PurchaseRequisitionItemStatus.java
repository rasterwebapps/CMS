package com.cms.inventory.procurement.model.enums;

/**
 * Lifecycle of one {@code PurchaseRequisitionItem}. See the "Purchase Requisition slice"
 * decision-log entry for the full flow.
 */
public enum PurchaseRequisitionItemStatus {
    /** Added to the requisition, awaiting approve/reject (or the header is still DRAFT). */
    PENDING,
    /** Approved — the item is ready to be picked up into a Purchase Order (once that slice exists). */
    APPROVED,
    /** Rejected — will not be purchased against this requisition. */
    REJECTED
}
