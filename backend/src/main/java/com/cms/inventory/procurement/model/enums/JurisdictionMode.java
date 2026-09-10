package com.cms.inventory.procurement.model.enums;

/**
 * Which set of {@code TaxSubType} components applies to a transaction, resolved by comparing the
 * supplier's state against {@code InventoryTaxJurisdictionSetting.homeState} — same-state buys
 * are {@code INTRASTATE} (CGST + SGST split), cross-state buys are {@code INTERSTATE} (IGST). See
 * the "GAP-02 pickup" decision-log entry.
 */
public enum JurisdictionMode {
    INTERSTATE,
    INTRASTATE
}
