package com.cms.inventory.stock.model.enums;

/**
 * How a {@code CycleCount}'s line set was initially populated — informational only, not enforced
 * afterward: both scopes allow adding/removing lines while the count is still in DRAFT. See the
 * 2026-09-08 "Cycle Count slice" decision-log entry.
 */
public enum CycleCountScope {
    /** Auto-populated with every product currently holding a balance at the location. */
    FULL_LOCATION,
    /** Started empty (or from a partial set); products are added one at a time. */
    AD_HOC
}
