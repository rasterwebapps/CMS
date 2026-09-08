package com.cms.inventory.stock.model.enums;

/**
 * The full transaction-type vocabulary from the ER draft. Only {@link #RECEIPT}, {@link #ADJUSTMENT},
 * and {@link #DISPOSAL} are reachable through the API/UI in this Phase 1 slice — the rest belong to
 * workflows (Requisition, Stock Transfer, Vendor Consignment) that don't exist yet. Kept as a full
 * enum now so later phases don't need a migration to widen a check constraint.
 */
public enum StockTxnType {
    RECEIPT,
    ISSUE,
    TRANSFER,
    ADJUSTMENT,
    RETURN,
    CONSIGNMENT_CONSUMPTION,
    DISPOSAL
}
