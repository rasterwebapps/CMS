package com.cms.inventory.receiving.model.enums;

/** Structured reason for a {@code SupplierReturn} — same shape as {@code WantedListRejectionReason}. */
public enum SupplierReturnReason {
    DEFECTIVE,
    WRONG_ITEM,
    DAMAGED_IN_TRANSIT,
    QUALITY_ISSUE,
    OTHER
}
