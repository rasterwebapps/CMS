# Inventory Equipment & Asset Management — Maintenance & Service Contracts Manual Test Cases

Covers the second Phase 5 ("Equipment & Asset Management") slice of the Inventory Management
module (`docs/inventory-management/`): "Equipment & Asset Management" → Maintenance Schedules and
→ Service Contracts.

## TC-INV-ASSET-005: Create a recurring maintenance schedule and mark it performed

**Preconditions:**
- An asset exists (see `inventory-asset-register.md`).
- Logged in as a user holding `INVENTORY_ASSET_MAINTENANCE_MANAGE`.

**Steps:**
1. Go to Equipment & Asset Management → Maintenance Schedules → New Schedule.
2. Select the asset, choose "Recurring", enter a recurrence interval (e.g. 90 days) and a Next
   Due Date, Save.
3. From the list, click Mark Performed on that schedule.

**Expected Result:**
- Step 2: the schedule appears in the list showing "Every 90 days".
- Choosing "One-off" hides the recurrence-interval field entirely; choosing "Recurring" without a
  value there is blocked on submit.
- Step 3: Last Performed updates to today; Next Due Date automatically advances by the recurrence
  interval from the performed date (not simply +90 days from the old due date) — the schedule
  remains active and still appears in future listings.

**Status:** NOT TESTED

## TC-INV-ASSET-006: One-off schedule deactivates once performed

**Preconditions:**
- A "One-off" schedule exists.

**Steps:**
1. Mark it performed.
2. Reload the list with "Active only" checked.

**Expected Result:**
- The schedule no longer appears once "Active only" is checked (a one-off schedule has nothing
  to recur to, so it's deactivated rather than left dangling).

**Status:** NOT TESTED

## TC-INV-ASSET-007: Overdue surfaces live, no background job

**Preconditions:**
- An active schedule exists with a Next Due Date in the past.

**Steps:**
1. View the Maintenance Schedules list and toggle "Overdue only".

**Expected Result:**
- The overdue schedule is visibly flagged and shown when the toggle is on; a schedule with a
  future due date never shows as overdue.

**Status:** NOT TESTED

## TC-INV-ASSET-008: Create a service contract linked to an existing Supplier

**Preconditions:**
- An asset and at least one approved Supplier exist.

**Steps:**
1. Go to Equipment & Asset Management → Service Contracts → New Contract.
2. Select the asset and supplier, enter a Start Date, an End Date before today, Save.

**Expected Result:**
- The contract is created; the list shows it flagged "Expired" since its End Date has passed.
- Attempting an End Date before the Start Date is rejected with a clear message.
- The Supplier picker reuses the existing Supplier master — no separate vendor entry was created.

**Status:** NOT TESTED
