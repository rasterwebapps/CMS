# Inventory Budgets & Approvals — Budget Allocation Manual Test Cases

Covers the first Phase 6 ("Budgets & Approvals") slice of the Inventory Management module
(`docs/inventory-management/`): "Budgets & Approvals" → Budgets.

## TC-INV-BUDGET-001: Create a budget and see consumed spend computed live

**Preconditions:**
- A location has at least one sent Purchase Order (status "Ordered" or further) with a known
  total amount, dated within a period you're about to define (see
  `inventory-procurement-purchase-order.md`).
- Logged in as a user holding `INVENTORY_BUDGET_MANAGE`.

**Steps:**
1. Go to Budgets & Approvals → Budgets → New Budget.
2. Select that location, set the period to include the order's date, enter an Allocated Amount
   comfortably above the order's total, Save.

**Expected Result:**
- The budget appears with Consumed showing the sent order's total (or the sum, if more than one
  order in range); Remaining = Allocated − Consumed; the consumed bar fills proportionally.
- A still-"Pending" (not yet sent) order at that location/period does **not** count toward
  Consumed — only committed (sent) spend counts.
- Attempting a Period End before the Period Start is rejected with a clear message.

**Status:** NOT TESTED

## TC-INV-BUDGET-002: Over-allocation is surfaced, never blocked

**Preconditions:**
- A budget exists whose Allocated Amount is lower than the location's actual committed spend in
  that period (edit an existing budget's allocated amount down, or create a new low-allocation
  budget covering an already-heavily-spent period).

**Steps:**
1. View the budget in the list.
2. Separately, submit and send a new Purchase Order at that location/period that would exceed the
   budget.

**Expected Result:**
- Step 1: Remaining shows a negative amount in red with an "Over" chip; the consumed bar shows
  danger coloring.
- Step 2: the Purchase Order submits and sends successfully — this slice is informational only,
  never blocking a real workflow (per the standing decision that hard enforcement is a separate,
  not-yet-built escalation).

**Status:** NOT TESTED

## TC-INV-BUDGET-003: Force-closed orders still count as committed spend

**Preconditions:**
- A Purchase Order at a budgeted location/period was force-closed (see
  `inventory-procurement-purchase-order.md`) after being sent.

**Steps:**
1. View the budget covering that order's date.

**Expected Result:**
- The force-closed order's total still counts toward Consumed — force-closing stops further
  progress on an order, it doesn't erase the spend already committed by sending it.

**Status:** NOT TESTED

## TC-INV-BUDGET-004: List filters by location and active status

**Preconditions:**
- Budgets exist across at least two locations, and at least one marked inactive (edit one and
  clear its Active flag via a direct API call, or note this is checked once a toggle exists).

**Steps:**
1. Filter by one location, then toggle "Active only".

**Expected Result:**
- Each filter narrows the list correctly.

**Status:** NOT TESTED
