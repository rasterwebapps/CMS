package com.cms.inventory.receiving.model.enums;

/**
 * Two-step save→confirm lifecycle for a {@code GoodsReceipt} — mirrors IHMS's own {@code
 * Purchase}/{@code PurchaseItem} draft→confirm shape rather than posting stock immediately on
 * create. See the "Goods Receipt slice" decision-log entry.
 */
public enum GoodsReceiptStatus {
    /** Being built — lines can still be added/removed; nothing has posted to stock yet. */
    DRAFT,
    /** Locked — every line has posted a RECEIPT stock movement and updated its PO line's receivedQty. */
    CONFIRMED
}
