package com.cms.inventory.procurement.model.enums;

/**
 * Lifecycle of a {@code WantedListItem} — one auto-computed reorder shortage line, the ERP-standard
 * "Planned Order" equivalent in this module's procurement chain (Wanted List → Purchase Requisition
 * → Purchase Order). See the "Wanted List slice" decision-log entry for the full flow.
 */
public enum WantedListItemStatus {
    /** Auto-computed by the shortage job, awaiting a planner's decision. */
    PENDING,
    /** Acknowledged but held for later — the job won't re-flag this pair while a line stays here;
     *  a planner moves it back to PENDING with {@code reopen}. */
    DEFERRED,
    /** Dismissed — won't be reordered off this line; terminal. */
    REJECTED,
    /** Picked up into a Purchase Requisition (alone or together with other selected lines for the
     *  same location); terminal, keeps a link to the requisition/line it created. */
    CONVERTED
}
