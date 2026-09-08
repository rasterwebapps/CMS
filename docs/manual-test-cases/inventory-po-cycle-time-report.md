# Inventory Reporting & Dashboards — Purchase Order Cycle-Time Report Manual Test Cases

Covers a follow-on Phase 8 slice of the Inventory Management module
(`docs/inventory-management/`), deliberately deferred at the Purchase Order Aging Report (its
own decision log flagged Cycle-Time as a distinct metric, real separately-scoped work): average
days from a purchase order being raised to being fully received, by supplier.

## TC-INV-PCT-001: Cycle time is computed from PO date to the last confirmed GRN

**Preconditions:**
- A Purchase Order was raised on a known date and became fully received (status Completed)
  after one or more Goods Receipts were confirmed, the last confirmation happening on a known
  later date.
- Logged in as a user holding `INVENTORY_PURCHASE_ORDER_VIEW`.

**Steps:**
1. Go to Purchasing & Suppliers → PO Cycle-Time Report.
2. Compute the expected cycle time by hand: days between the PO's own Order Date and the date
   its last Goods Receipt was confirmed.
3. Compare against that PO's contribution to its supplier's Average Cycle Time.

**Expected Result:**
- The supplier's average reflects the hand-computed cycle time (averaged with any other
  Completed orders from the same supplier) — never measured against `expectedDeliveryDate` or
  any other date.

**Status:** NOT TESTED

## TC-INV-PCT-002: Only fully-received (Completed) orders count

**Preconditions:**
- A Purchase Order is still open (Ordered/In Progress/Partially Completed), and a separate one
  was Force-Closed before every line was received.

**Steps:**
1. View the report.

**Expected Result:**
- Neither order contributes to any supplier's average or count — cycle time is only meaningful
  for an order that was genuinely fully received.

**Status:** NOT TESTED

## TC-INV-PCT-003: Supplier averages and the overall average are both correct

**Preconditions:**
- Two suppliers each have two or more Completed orders with different cycle times.

**Steps:**
1. Manually average each supplier's own orders' cycle times.
2. Manually average every Completed order's cycle time across all suppliers combined.
3. Compare both against the report.

**Expected Result:**
- Each supplier's row shows its own correctly-averaged cycle time and order count.
- The banner's overall average matches the grand average across every Completed order (not an
  average of the per-supplier averages, which would weight suppliers unequally).

**Status:** NOT TESTED

## TC-INV-PCT-004: Empty state when nothing has been fully received yet

**Preconditions:**
- No Purchase Order in the system has ever reached Completed status.

**Steps:**
1. View the report.

**Expected Result:**
- An empty state is shown ("No fully-received orders yet") rather than an empty table or a
  divide-by-zero error.

**Status:** NOT TESTED

## TC-INV-PCT-005: Permission gating reuses the existing Purchase Order permission

**Preconditions:**
- A user holding neither `INVENTORY_PURCHASE_ORDER_VIEW` nor `INVENTORY_PURCHASE_ORDER_MANAGE`.

**Steps:**
1. Attempt to navigate to the PO Cycle-Time Report route directly.
2. Attempt to call the report endpoint directly.

**Expected Result:**
- Both rejected — this report deliberately reuses the same permission that already gates the
  Purchase Order list and the PO Aging Report, not a new dedicated one.

**Status:** NOT TESTED
