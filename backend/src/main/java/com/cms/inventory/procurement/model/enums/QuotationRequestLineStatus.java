package com.cms.inventory.procurement.model.enums;

/**
 * Lifecycle of one {@code QuotationRequestLine}. Each line is awarded independently — per-line,
 * not one winner for the whole request — so a request's lines can end up split across several
 * different awarded suppliers. See the "Quotation Request slice" decision-log entry.
 */
public enum QuotationRequestLineStatus {
    /** Awaiting supplier quotes and an award decision (or the header is still DRAFT). */
    PENDING,
    /** A winning {@code QuotationResponseLine} has been picked — ready to be picked up into a
     *  Purchase Order. */
    AWARDED,
    /** No award made on this line — e.g. no usable quote came back; terminal. */
    REJECTED,
    /**
     * Picked up into a {@code PurchaseOrderItem} — terminal, like {@code AWARDED}'s eventual
     * conversion. Mirrors {@code PurchaseRequisitionItemStatus.ORDERED}. See {@code
     * QuotationRequestService.convertAwardedLines}.
     */
    ORDERED
}
