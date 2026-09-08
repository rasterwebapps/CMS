package com.cms.inventory.asset.model.enums;

/**
 * Asset lifecycle — the plan's own sketch listed only {@code IN_USE}/{@code UNDER_MAINTENANCE}/
 * {@code RETIRED}/{@code DISPOSED} and asked to check IHMS's own asset-status shape first, per
 * the "Reference architecture pivot" rule; that check could not be done in this autonomous
 * session (no access to the IHMS codebase from here — flagged for a future session that does).
 * {@code AVAILABLE} was added as the default initial state so an asset can be registered before
 * it's deployed anywhere, matching the standard ERP asset-register pattern (Available → In Use →
 * Under Maintenance → Retired/Disposed) — a small, defensible ERP-standard default, not a guess
 * at IHMS's specific shape. See the "Asset register slice" decision-log entry.
 */
public enum AssetStatus {
    /** Registered but not yet deployed/assigned anywhere. */
    AVAILABLE,
    IN_USE,
    UNDER_MAINTENANCE,
    RETIRED,
    DISPOSED
}
