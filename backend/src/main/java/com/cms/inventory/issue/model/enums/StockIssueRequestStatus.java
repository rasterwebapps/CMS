package com.cms.inventory.issue.model.enums;

/** Header lifecycle — same shape as {@code PurchaseRequisitionStatus} (the closest in-repo precedent). */
public enum StockIssueRequestStatus {
    DRAFT,
    SUBMITTED,
    COMPLETED,
    CANCELLED
}
