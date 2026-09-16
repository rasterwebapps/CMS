package com.cms.inventory.procurement.model.enums;

/**
 * Lifecycle of a {@code QuotationRequest} header. Mirrors {@code PurchaseRequisitionStatus}'s
 * shape exactly. See the "Quotation Request slice" decision-log entry for the full flow.
 */
public enum QuotationRequestStatus {
    /** Being built — lines and invited suppliers can be added/removed. */
    DRAFT,
    /** Locked in — invited suppliers' quotes can now be recorded and lines awarded. */
    SUBMITTED,
    /** Every line is in a terminal state (AWARDED-and-ordered, or REJECTED). */
    COMPLETED,
    /** Abandoned before submission — only reachable from DRAFT. */
    CANCELLED
}
