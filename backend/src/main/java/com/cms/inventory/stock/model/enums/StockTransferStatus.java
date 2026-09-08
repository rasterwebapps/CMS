package com.cms.inventory.stock.model.enums;

/**
 * Simple lifecycle for a {@code StockTransfer} — no approval gate this phase, consistent with
 * Purchase Order (real multi-level/parallel approval routing is Phase 6 scope, built once).
 * {@code CANCELLED} is reachable only from {@code DRAFT}, same rule as every other DRAFT-first
 * header in this module (Purchase Requisition, Cycle Count). See the "Stock Transfer slice"
 * decision-log entry.
 */
public enum StockTransferStatus {
    /** Being built — lines can still be added/removed; nothing has posted to stock yet. */
    DRAFT,
    /** Posted — a TRANSFER movement pair (decrease at source, increase at destination) for every line. */
    COMPLETED,
    /** Abandoned before completion. Reachable only from DRAFT. */
    CANCELLED
}
