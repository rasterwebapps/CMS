# Inventory Gate Pass, Vendor-Owned Stock & Service Requests — Gate Pass Manual Test Cases

Covers Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests") first slice of the
Inventory Management module (`docs/inventory-management/`): the outward/inward Gate Pass.

## TC-INV-GATE-PASS-001: Create a returnable outward gate pass against a product

**Preconditions:**
- A Product and an Inventory Location exist.
- Logged in as a user holding `INVENTORY_GATE_PASS_MANAGE`.

**Steps:**
1. Go to Gate Pass & Service Requests → Gate Passes → New Gate Pass.
2. Direction "Outward", Returnable checked, Item Type "Product", select the product and location,
   enter Quantity, Reason, Party Name, and an Expected Return Date on or after the Pass Date, Save.

**Expected Result:**
- The pass is created with status "Pending Approval".
- Attempting to submit with no Expected Return Date while Returnable is checked is rejected with
  a clear message.
- Attempting to submit with an Expected Return Date before the Pass Date is rejected.

**Status:** NOT TESTED

## TC-INV-GATE-PASS-002: Create a non-returnable inward gate pass against an asset

**Preconditions:**
- An Asset exists with status "Available"; an Inventory Location exists.
- Logged in as a user holding `INVENTORY_GATE_PASS_MANAGE`.

**Steps:**
1. New Gate Pass → Direction "Inward", Returnable unchecked, Item Type "Asset", select the asset,
   fill the remaining required fields, Save.

**Expected Result:**
- The pass is created with status "Pending Approval" and no Expected Return Date is required or
  stored.
- The list and detail screens show the asset's tag (not a product name/code).

**Status:** NOT TESTED

## TC-INV-GATE-PASS-003: Approval and gate verification are two distinct, separately gated steps

**Preconditions:**
- A pending gate pass exists.
- User A holds only `INVENTORY_GATE_PASS_APPROVE`; User B holds only `INVENTORY_GATE_PASS_VERIFY`.

**Steps:**
1. As User A, open the pass — confirm only Approve/Reject controls are shown (no Verify control)
   — and approve it.
2. As User B, reopen the pass — confirm only the Verify at Gate control is shown (no
   Approve/Reject) — and verify it.

**Expected Result:**
- Step 1: status becomes "Approved"; the Approved-by/at fields are stamped.
- Step 2: for a returnable pass, status becomes "Gate Verified"; for a non-returnable one, status
  goes straight to "Closed". The Gate-verified-by/at fields are stamped in both cases.
- Attempting to call either endpoint directly as a user who lacks that specific permission is
  rejected server-side with a clear message, even if they hold the other gate-pass permission.

**Status:** NOT TESTED

## TC-INV-GATE-PASS-004: Rejection requires a reason and stops the flow

**Preconditions:**
- A pending gate pass exists.
- Logged in as a user holding `INVENTORY_GATE_PASS_APPROVE`.

**Steps:**
1. Attempt to reject with no reason entered.
2. Enter a reason and reject.
3. Attempt to approve or verify the same (now rejected) pass via a direct API call.

**Expected Result:**
- Step 1: blocked client-side (and rejected server-side if attempted directly) with a clear
  message that a reason is required.
- Step 2: status becomes "Rejected"; the reason is shown on the detail screen.
- Step 3: both rejected — a rejected pass is terminal, not resumable.

**Status:** NOT TESTED

## TC-INV-GATE-PASS-005: Overdue surfaces without any background job, non-returnable passes never do

**Preconditions:**
- A returnable, gate-verified pass exists whose Expected Return Date is in the past; a second,
  gate-verified returnable pass exists with a future Expected Return Date; a third, non-returnable
  pass exists and has been verified (so it is already "Closed").

**Steps:**
1. View the Gate Passes list and the first pass's own detail screen.
2. Toggle the "Overdue only" filter on the list.

**Expected Result:**
- Only the first pass shows an "Overdue" indicator, computed live from today vs. the Expected
  Return Date — never a stored/stale flag. The future-dated pass never shows overdue. The
  non-returnable, Closed pass is never eligible to be overdue (it has no Expected Return Date at
  all).
- The "Overdue only" filter shows only the first pass.

**Status:** NOT TESTED

## TC-INV-GATE-PASS-006: Mark returned closes a returnable pass; a non-returnable one has no such action

**Preconditions:**
- A returnable, gate-verified pass exists.
- Logged in as a user holding `INVENTORY_GATE_PASS_RETURN`.

**Steps:**
1. On the returnable pass's detail screen, optionally enter return notes, click Mark Returned and
   confirm.
2. Attempt to mark the same pass returned again via a direct API call.
3. Open an already-Closed, non-returnable pass's detail screen.

**Expected Result:**
- Step 1: status becomes "Returned"; Actual Return Date and the returning user are shown; the
  Mark Returned control disappears.
- Step 2: rejected with a clear message — only a gate-verified, returnable pass can be marked
  returned.
- Step 3: no Mark Returned control is ever offered on a non-returnable pass.

**Status:** NOT TESTED

## TC-INV-GATE-PASS-007: Exactly one of product or asset must be set

**Preconditions:**
- A user holding `INVENTORY_GATE_PASS_MANAGE`, calling the create endpoint directly.

**Steps:**
1. Attempt to create a gate pass with both a `productId` and an `assetId` set.
2. Attempt to create a gate pass with neither set.

**Expected Result:**
- Both rejected server-side with a clear message — a gate pass always references exactly one
  target, matching the reference architecture's own "ProductId or AssetId" shape.

**Status:** NOT TESTED
