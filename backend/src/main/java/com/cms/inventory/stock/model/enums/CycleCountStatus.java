package com.cms.inventory.stock.model.enums;

/**
 * Lifecycle of a {@code CycleCount} header. See the 2026-09-08 "Cycle Count slice" decision-log
 * entry for the full flow.
 */
public enum CycleCountStatus {
    /** Count sheet is being built and counted — lines can be added/removed/entered, blind (no
     *  system quantity shown). */
    DRAFT,
    /** Counts are locked in and variances computed; nonzero-variance lines await approve/reject. */
    SUBMITTED,
    /** Every line is in a terminal state (MATCHED, APPROVED, or REJECTED). */
    COMPLETED,
    /** Abandoned before submission — only reachable from DRAFT. */
    CANCELLED
}
