# Inventory Procurement — Wanted List Manual Test Cases

Covers the fourth Phase 2 ("Purchasing & Suppliers") slice of the Inventory Management module
(`docs/inventory-management/`): "Purchasing & Suppliers" → Wanted List. This is the ERP-standard
MRP "Planned Order" step ahead of Purchase Requisition — a scheduled shortage check (plus a manual
"Run Now") nets each product's configured reorder level against current stock and whatever's
already open on a Purchase Requisition.

## TC-INV-PROC-019: Run Now flags a product below its reorder level

**Preconditions:**
- A Product has `reorderLevel`/`reorderQty` configured and its stock at a Location is below that
  level (e.g. adjust stock down via Stock Movement first).
- No unresolved Wanted List line already exists for that (Product, Location) pair.
- Logged in as a user holding `INVENTORY_WANTED_LIST_RUN`.

**Steps:**
1. Go to Purchasing & Suppliers → Wanted List → Run Now.

**Expected Result:**
- A toast reports how many new lines were found (at least 1).
- The product appears with Status "Pending", showing On Hand, On Order (0 if nothing's been
  requested), Reorder Level, and a Suggested Qty of at least the product's configured Reorder Qty.
- Running it again immediately does not create a duplicate line for the same pair.

**Status:** NOT TESTED

## TC-INV-PROC-020: On-order quantity is netted — an already-requested shortage isn't re-flagged

**Preconditions:**
- A Pending Wanted List line exists for a (Product, Location) pair (TC-INV-PROC-019).
- A Purchase Requisition for that same Location, with a line for that same Product, has been
  Submitted (see `inventory-procurement-purchase-requisition.md`).

**Steps:**
1. Reject or otherwise clear the original Wanted List line (so the pair is free to be re-evaluated
   on the next run — or wait for a pair not yet flagged).
2. With the Submitted requisition line still open (Pending or Approved), run Wanted List → Run Now
   again for a scenario where the on-hand quantity alone would still be short.

**Expected Result:**
- The new line's On Order column reflects the open requisition's requested quantity.
- If that requested quantity is enough to cover the shortfall once netted against on-hand stock, no
  new line is created for that pair.

**Status:** NOT TESTED

## TC-INV-PROC-021: Defer holds a line, Reopen brings it back

**Preconditions:**
- A Pending Wanted List line exists.
- Logged in as a user holding `INVENTORY_WANTED_LIST_MANAGE`.

**Steps:**
1. Click Defer on the line.
2. Click Reopen on the same line.

**Expected Result:**
- After step 1: Status changes to "Deferred"; running Wanted List → Run Now again does not create a
  duplicate line for the same (Product, Location) pair while it stays Deferred.
- After step 2: Status returns to "Pending".

**Status:** NOT TESTED

## TC-INV-PROC-022: Reject requires a structured reason

**Preconditions:**
- A Pending or Deferred Wanted List line exists.
- Logged in as a user holding `INVENTORY_WANTED_LIST_MANAGE`.

**Steps:**
1. Click Reject; in the dialog, pick a reason (e.g. "Reorder level needs recalibrating") and
   optionally add notes.

**Expected Result:**
- Status changes to "Rejected"; the chosen reason and notes are visible on the line.
- The pair becomes eligible again only if the shortfall is later re-evaluated after the rejection
  is cleared — a Rejected line is terminal and is not re-created by the next run while it exists.

**Status:** NOT TESTED

## TC-INV-PROC-023: Convert one or several lines into a Purchase Requisition

**Preconditions:**
- At least two Pending Wanted List lines exist for the **same** Location.
- Logged in as a user holding `INVENTORY_WANTED_LIST_CONVERT`.

**Steps:**
1. Select both lines via their checkboxes.
2. Click "Convert to Requisition"; in the dialog, optionally adjust one line's quantity, then
   confirm.

**Expected Result:**
- A new Purchase Requisition is created for that Location, already Submitted, with one line per
  selected product at the (possibly adjusted) quantity.
- Both Wanted List lines now show Status "Converted", each linking to the requisition it created.
- The app navigates to the new requisition's detail screen.

**Status:** NOT TESTED

## TC-INV-PROC-024: Selection is restricted to a single location

**Preconditions:**
- Pending Wanted List lines exist across at least two different Locations.

**Steps:**
1. Select a line for Location A, then attempt to select a line for Location B.

**Expected Result:**
- The second selection is blocked with a clear message; only lines for Location A remain selected.

**Status:** NOT TESTED

## TC-INV-PROC-025: Wanted List filters by location and status

**Preconditions:**
- Wanted List lines exist across at least two locations and at least two different statuses.

**Steps:**
1. Go to Wanted List and filter by one location, then by one status.

**Expected Result:**
- Only matching lines are shown for each filter.

**Status:** NOT TESTED
