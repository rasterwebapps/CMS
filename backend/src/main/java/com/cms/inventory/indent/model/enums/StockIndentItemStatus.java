package com.cms.inventory.indent.model.enums;

/** Line lifecycle — same shape as {@code PurchaseRequisitionItemStatus}, minus the ORDERED state (nothing downstream picks this up). */
public enum StockIndentItemStatus {
    PENDING,
    /** Approved AND posted — an ISSUE stock movement decreased the issuing location's balance. */
    APPROVED,
    REJECTED
}
