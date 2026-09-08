# Inventory Equipment & Asset Management — Asset Register Manual Test Cases

Covers the first Phase 5 ("Equipment & Asset Management") slice of the Inventory Management
module (`docs/inventory-management/`): "Equipment & Asset Management" → Asset Register.

## TC-INV-ASSET-001: Register a new asset

**Preconditions:**
- A Product and an Inventory Location exist.
- Logged in as a user holding `INVENTORY_ASSET_MANAGE`.

**Steps:**
1. Go to Equipment & Asset Management → Asset Register → Register Asset.
2. Select the Product and Location, enter a unique Asset Tag, Save.

**Expected Result:**
- The asset appears in the list with Status "AVAILABLE".
- Attempting to register a second asset with the same Asset Tag (case-insensitive) shows a
  real-time "already in use" validation error while typing, and is blocked on submit if bypassed.

**Status:** NOT TESTED

## TC-INV-ASSET-002: Status can move non-linearly

**Preconditions:**
- An asset in status "AVAILABLE" exists.

**Steps:**
1. From the list, change the asset's status to "IN_USE".
2. Change it again to "UNDER_MAINTENANCE".
3. Change it back to "IN_USE".

**Expected Result:**
- Every transition succeeds immediately (no confirmation dialog, no blocked path) — asset status
  is open-ended, not a strict linear workflow. The list and detail views reflect the current
  status right away.

**Status:** NOT TESTED

## TC-INV-ASSET-003: Edit preserves the asset tag uniqueness check against itself

**Preconditions:**
- Two assets, A and B, exist with different asset tags.

**Steps:**
1. Edit asset A and re-save it without changing its own asset tag.
2. Edit asset A and attempt to change its asset tag to asset B's tag.

**Expected Result:**
- Step 1 succeeds (an asset's own tag doesn't collide with itself).
- Step 2 is rejected with a clear "already in use" message.

**Status:** NOT TESTED

## TC-INV-ASSET-004: List filters by location, status, and free-text search

**Preconditions:**
- Assets exist across at least two locations, at least two statuses, and with distinct asset
  tags/serial numbers.

**Steps:**
1. Filter by one location, then by one status, then search by a partial asset tag or serial
   number.

**Expected Result:**
- Each filter narrows the list correctly; the search matches partial, case-insensitive text
  against either the asset tag or the serial number.

**Status:** NOT TESTED
