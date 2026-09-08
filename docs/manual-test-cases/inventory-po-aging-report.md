# Inventory Reporting & Dashboards — Purchase Order Aging Report Manual Test Cases

Covers Phase 8's ("Reporting & Dashboards") third slice of the Inventory Management module
(`docs/inventory-management/`): every still-open purchase order bucketed by days outstanding.

## TC-INV-POAGE-001: Buckets reflect real days-since-order-date

**Preconditions:**
- Purchase Orders exist with `poDate` values landing in each of the four ranges: 0–30, 31–60,
  61–90, and 90+ days ago, all still in an open status (Pending, Ordered, In Progress, or
  Partially Completed).
- Logged in as a user holding `INVENTORY_PURCHASE_ORDER_VIEW`.

**Steps:**
1. Go to Purchasing & Suppliers → PO Aging Report.
2. Compare each bucket's Orders count against the real Purchase Order list filtered to that
   date range and an open status.

**Expected Result:**
- Every bucket's order count and total value match the real underlying data exactly. The 61–90
  and 90+ buckets render with a warning/danger visual treatment; 0–30 and 31–60 do not.

**Status:** NOT TESTED

## TC-INV-POAGE-002: Completed and Force-Closed orders never appear

**Preconditions:**
- A Purchase Order exists that is fully received (Completed), and another that was manually
  Force-Closed — both with a `poDate` more than 90 days ago.

**Steps:**
1. View the report.

**Expected Result:**
- Neither order contributes to any bucket, including the 90+ bucket — the report only ever
  covers orders that are genuinely still open (Pending/Ordered/In Progress/Partially Completed).

**Status:** NOT TESTED

## TC-INV-POAGE-003: Grand total matches the sum of all buckets

**Preconditions:**
- Open purchase orders exist across at least two age buckets.

**Steps:**
1. Manually sum the Orders and Total Value columns across all four bucket rows.
2. Compare against the Grand Total row.

**Expected Result:**
- The Grand Total row's order count and value exactly equal the sum of the four bucket rows.

**Status:** NOT TESTED

## TC-INV-POAGE-004: Empty state when nothing is open

**Preconditions:**
- Every Purchase Order in the system is Completed or Force-Closed.

**Steps:**
1. View the report.

**Expected Result:**
- An empty state is shown ("No open purchase orders") rather than four zero-value bucket rows.

**Status:** NOT TESTED

## TC-INV-POAGE-005: Permission gating reuses the existing Purchase Order permission

**Preconditions:**
- A user holding neither `INVENTORY_PURCHASE_ORDER_VIEW` nor `INVENTORY_PURCHASE_ORDER_MANAGE`.

**Steps:**
1. Attempt to navigate to the PO Aging Report route directly.
2. Attempt to call the report endpoint directly.

**Expected Result:**
- Both rejected — this report deliberately reuses the same permission that already gates the
  Purchase Order list, not a new dedicated one.

**Status:** NOT TESTED
