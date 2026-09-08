package com.cms.inventory.procurement.model.enums;

/** Structured reason a planner dismisses a {@code WantedListItem} line, paired with optional
 *  free-text notes — same structured-reason-plus-notes pattern as {@code CycleCountLine}'s
 *  resolution. See the "Wanted List slice" decision-log entry. */
public enum WantedListRejectionReason {
    /** Already being ordered through another channel (e.g. a manually raised requisition). */
    ALREADY_ORDERED_ELSEWHERE,
    /** This product is being phased out — no point restocking it. */
    PRODUCT_DISCONTINUING,
    /** The product's configured reorder level/quantity itself needs correcting. */
    LEVEL_MISCALIBRATED,
    OTHER
}
