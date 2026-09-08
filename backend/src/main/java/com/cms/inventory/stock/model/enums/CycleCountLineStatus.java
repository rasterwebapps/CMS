package com.cms.inventory.stock.model.enums;

/**
 * Lifecycle of one {@code CycleCountLine}. See the 2026-09-08 "Cycle Count slice" decision-log
 * entry for the full flow.
 */
public enum CycleCountLineStatus {
    /** Added to the sheet, not yet counted (or the count header is still DRAFT). */
    PENDING_COUNT,
    /** Counted quantity matched the system snapshot exactly — auto-closed, no approval needed. */
    MATCHED,
    /** Counted quantity differs from the system snapshot — awaiting approve/reject. */
    PENDING_REVIEW,
    /** Variance approved and posted as a Stock Ledger ADJUSTMENT. */
    APPROVED,
    /** Variance dismissed as a counting error — no stock change. */
    REJECTED
}
