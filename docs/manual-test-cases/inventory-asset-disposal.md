# Inventory Equipment & Asset Management — Disposal Manual Test Cases

Covers the fourth and final Phase 5 ("Equipment & Asset Management") slice of the Inventory
Management module (`docs/inventory-management/`): "Equipment & Asset Management" → Asset Register
→ Dispose action.

## TC-INV-ASSET-013: Dispose requires a reason and writes off on-hand stock

**Preconditions:**
- An asset exists whose product also has an on-hand Stock Balance entry (received via GRN) at
  the asset's location — note that on-hand quantity first.
- Logged in as a user holding `INVENTORY_ASSET_DISPOSE`.

**Steps:**
1. On the Asset Register list, click Dispose for that asset.
2. Attempt to confirm with the Reason field left blank.
3. Enter a Reason (and optionally a Disposal Value), confirm.
4. Check the Stock Balance report for the product/location.

**Expected Result:**
- Step 2: the Dispose button stays disabled until a reason is entered.
- Step 3: the asset's status becomes "DISPOSED"; the Dispose action and the inline status select
  both disappear for that row, replaced by a "DISPOSED" label.
- Step 4: the product's on-hand quantity at that location decreased by exactly 1 unit — the
  write-off uses the existing DISPOSAL stock movement, same as a manual write-off.

**Status:** NOT TESTED

## TC-INV-ASSET-014: Dispose is a no-op on stock when nothing is on hand

**Preconditions:**
- An asset exists whose product has **no** on-hand Stock Balance entry at the asset's location
  (or the product was never bulk stock-tracked at all).

**Steps:**
1. Dispose the asset with a reason.
2. Check the Stock Balance report.

**Expected Result:**
- The asset disposes successfully; no stock movement is posted and no error occurs — there was
  nothing to write off.

**Status:** NOT TESTED

## TC-INV-ASSET-015: Cannot dispose an already-disposed asset; inline status never offers DISPOSED

**Preconditions:**
- A DISPOSED asset exists.

**Steps:**
1. View the asset in the Asset Register list.
2. Attempt to dispose the same asset again via a direct API call.

**Expected Result:**
- Step 1: the row shows the "DISPOSED" label, not an editable status dropdown, and no Dispose
  action button — disposal is a one-way action from the UI.
- Step 2: rejected with a clear "already been disposed" message.
- Separately, confirm the inline status dropdown on a non-disposed asset's row never offers
  "DISPOSED" as an option — disposal must go through the dedicated dialog, never a bare status
  flip.

**Status:** NOT TESTED
