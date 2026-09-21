package com.cms.inventory.indent.model.enums;

/**
 * Line lifecycle — two decision points, two different actors (Phase D of the Stock Indent
 * auto-indent feature; see the "OC-206 reopened" decision-log entries). {@code PENDING} awaits
 * the department head; {@code APPROVED} then awaits the store's own fulfillment decision — it no
 * longer means "issued" (that was the pre-Phase-D shape, when approval and issuing were the same
 * step). {@code REJECTED} is the department head's own negative outcome; {@code DENIED} is the
 * store's. {@code FULFILLED} covers both store fulfillment paths (direct issue, or a transfer-in
 * from another location first) — {@code StockIndentItem.sourceTransfer} being non-null
 * distinguishes the latter, no separate status needed.
 */
public enum StockIndentItemStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Store issued the stock — directly, or after a transfer-in (see {@code sourceTransfer}). */
    FULFILLED,
    /** Store had no stock anywhere and raised a Purchase Requisition instead (see {@code
     *  raisedRequisition}/{@code raisedRequisitionItem}) — terminal; the eventual restock is a
     *  separate, disconnected event once that requisition's PO/GRN cycle completes. */
    PO_RAISED,
    /** Store declined to fulfill — distinct from the department head's own {@code REJECTED}. */
    DENIED
}
