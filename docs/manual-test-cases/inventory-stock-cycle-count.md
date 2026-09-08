# Inventory Stock — Cycle Count (Physical Stock Count) Manual Test Cases

Covers the last Phase 1 todo of the Inventory Management module (`docs/inventory-management/`):
physical stock counts / reconciliation, under "Stock Management" → Cycle Counts. Builds on
`inventory-stock-tracking.md` — Locations and at least one recorded stock movement must already
exist so there's a balance to count against.

## TC-INV-CC-001: Start a Full Location count and confirm blind entry

**Preconditions:**
- Logged in as a user holding `INVENTORY_CYCLE_COUNT_MANAGE`.
- "Main Store" location has at least two products with a recorded balance (see `inventory-stock-tracking.md`).

**Steps:**
1. Go to Stock Management → Cycle Counts → New Count.
2. Select Location "Main Store", leave Scope as "Full Location", pick today's date, and start the count.
3. On the count sheet, confirm every product currently balanced at "Main Store" appears as a line.

**Expected Result:**
- The count sheet is created in DRAFT status with one line per product holding a balance at the location.
- No system/expected quantity is shown anywhere on the entry screen — only a blank "Counted Qty" input per line.

**Status:** NOT TESTED

## TC-INV-CC-002: Add and remove an ad-hoc line while in DRAFT

**Preconditions:**
- A DRAFT cycle count exists (TC-INV-CC-001), and at least one product exists that has no balance at "Main Store".

**Steps:**
1. On the DRAFT count's detail screen, use "Add to Sheet" to add a product not already on the sheet.
2. Confirm it appears with a blank Counted Qty input.
3. Remove a different line using its remove (✕) button.

**Expected Result:**
- The added product appears immediately; the removed product's line disappears immediately.
- A product already on the sheet does not appear again in the "Add a product…" picker.

**Status:** NOT TESTED

## TC-INV-CC-003: Submit reveals variance only after submission (blind count)

**Preconditions:**
- A DRAFT cycle count exists with at least two lines. Note each product's actual on-hand quantity from the Stock Balance report beforehand (e.g. Product A = 50).

**Steps:**
1. Enter a Counted Qty that matches the known system quantity for one line (e.g. 50 for Product A).
2. Enter a Counted Qty that differs from the known system quantity for another line (e.g. 45 for a product known to be at 50).
3. Click Submit Count and confirm.

**Expected Result:**
- Before submitting, no system/expected quantity or variance was visible anywhere for either line.
- After submitting, the count moves to SUBMITTED; the matching line shows status "Matched" (auto-closed, no action needed); the differing line shows status "Pending Review" with its System Qty, Counted Qty, and Variance (e.g. -5) all now visible.

**Status:** NOT TESTED

## TC-INV-CC-004: Submitting with an uncounted line is blocked (or warns)

**Preconditions:**
- A DRAFT cycle count exists with at least one line left blank (never counted).

**Steps:**
1. Click Submit Count without entering a Counted Qty for every line.

**Expected Result:**
- The system either blocks submission with a clear message naming the uncounted product, or explicitly warns how many products are still uncounted before letting the user proceed — never submits silently treating a blank as zero.

**Status:** NOT TESTED

## TC-INV-CC-005: Approve a variance posts a stock adjustment

**Preconditions:**
- A SUBMITTED cycle count has a line in "Pending Review" status (from TC-INV-CC-003).
- Logged in as a user holding `INVENTORY_CYCLE_COUNT_APPROVE`.
- Note the product's current Qty On Hand from the Stock Balance report before approving.

**Steps:**
1. On the pending line, optionally enter a resolution note, then click Approve.
2. Go to Stock Management → Stock Balance and check the same product/location's Qty On Hand.

**Expected Result:**
- The line's status changes to "Approved" and shows who approved it and when.
- The Stock Balance report's Qty On Hand for that product/location has changed by exactly the variance amount (a found-extra variance increases it; a found-missing variance decreases it).
- Once every line on the count is in a terminal state (Matched/Approved/Rejected), the count's own status changes to COMPLETED automatically.

**Status:** NOT TESTED

## TC-INV-CC-006: Reject a variance posts no stock change

**Preconditions:**
- A SUBMITTED cycle count has a line in "Pending Review" status.
- Logged in as a user holding `INVENTORY_CYCLE_COUNT_APPROVE`.
- Note the product's current Qty On Hand before rejecting.

**Steps:**
1. On the pending line, click Reject.
2. Check the same product/location's Qty On Hand in Stock Balance.

**Expected Result:**
- The line's status changes to "Rejected".
- The Stock Balance report's Qty On Hand for that product/location is unchanged.

**Status:** NOT TESTED

## TC-INV-CC-007: A user without Approve permission cannot resolve variances

**Preconditions:**
- A SUBMITTED cycle count has a line in "Pending Review" status.
- Logged in as a user holding `INVENTORY_CYCLE_COUNT_MANAGE` but not `INVENTORY_CYCLE_COUNT_APPROVE`.

**Steps:**
1. Open the SUBMITTED count's detail screen.

**Expected Result:**
- The pending line shows its status but no Approve/Reject controls — only a note that it's awaiting approval.

**Status:** NOT TESTED

## TC-INV-CC-008: Cancel a DRAFT count

**Preconditions:**
- A DRAFT cycle count exists with at least one line.

**Steps:**
1. Click Cancel Count and confirm.

**Expected Result:**
- The count's status changes to CANCELLED; no stock movement is posted for any line.
- The cycle counts list shows it as Cancelled; it can no longer be edited.

**Status:** NOT TESTED

## TC-INV-CC-009: Cycle counts list filters by location and status

**Preconditions:**
- At least one cycle count exists in each of DRAFT, SUBMITTED, and COMPLETED status, across two different locations.

**Steps:**
1. Go to Stock Management → Cycle Counts.
2. Filter by one location, then by one status, then both together.

**Expected Result:**
- The list narrows correctly for each filter and combination; the "Pending Review" count column shows a highlighted count only for counts that still have unresolved variances.

**Status:** NOT TESTED
