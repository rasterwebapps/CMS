# Inventory Reporting & Dashboards — Inventory Dashboard Manual Test Cases

Covers Phase 8's ("Reporting & Dashboards") first slice of the Inventory Management module
(`docs/inventory-management/`): a single at-a-glance overview screen, computed live across the
whole module.

## TC-INV-DASH-001: Every tile reflects real, current data

**Preconditions:**
- At least one record exists in each of: an open Purchase Order, a submitted Purchase
  Requisition, a pending Wanted List item, an in-progress Approval Instance, an overdue
  (gate-verified, returnable, past-due) Gate Pass, an overdue (issued, past-due) Loanable Item
  Issue, an open Service Ticket (one of them Urgent), an over-allocated Budget, a Consignment
  Stock Line with an outstanding (unconsumed) balance, and an Asset Under Maintenance.
- Logged in as a user holding `INVENTORY_DASHBOARD_VIEW`.

**Steps:**
1. Go to Stock Management → Dashboard.
2. Compare each tile's count/value against the real underlying screen's own filtered list for
   that same condition (e.g. Gate Passes filtered to Overdue only).

**Expected Result:**
- Every tile's number matches the corresponding filtered list's count exactly — nothing is
  stale, cached, or precomputed; each figure is a live query.

**Status:** NOT TESTED

## TC-INV-DASH-002: Tiles are clickable and route to the right screen

**Preconditions:**
- Logged in as a user holding `INVENTORY_DASHBOARD_VIEW` and view access to the underlying
  screens.

**Steps:**
1. Click each tile in turn.

**Expected Result:**
- Each tile navigates to the correct underlying list screen (Purchase Orders, Purchase
  Requisitions, Wanted List, Approvals, Gate Passes, Loanable Item Issues, Service Tickets,
  Budgets, Consignment Stock, Assets) — no dead links, no wrong destination.

**Status:** NOT TESTED

## TC-INV-DASH-003: Zero-state tiles render cleanly, no "accent" flag when there's nothing to flag

**Preconditions:**
- No overdue Gate Passes, no overdue Loanable Item Issues, no Urgent open Service Tickets, no
  over-allocated Budgets exist anywhere.

**Steps:**
1. Load the dashboard.

**Expected Result:**
- All four tiles show 0, and none of them render with the accent/warning styling — the accent
  only appears once the underlying count is actually greater than zero.

**Status:** NOT TESTED

## TC-INV-DASH-004: Permission gating

**Preconditions:**
- A user who does not hold `INVENTORY_DASHBOARD_VIEW` (but may hold other Inventory view
  permissions).

**Steps:**
1. Attempt to navigate to the Dashboard route directly.
2. Attempt to call the dashboard endpoint directly.

**Expected Result:**
- Both rejected — holding some other Inventory permission does not implicitly grant dashboard
  access; it is its own dedicated permission.

**Status:** NOT TESTED

## TC-INV-DASH-005: Refresh re-fetches live data

**Preconditions:**
- The dashboard is open with some non-zero tiles.

**Steps:**
1. In another session/tab, change the state behind one tile (e.g. resolve an open Service
   Ticket, or mark an overdue Gate Pass returned).
2. Click Refresh on the dashboard (without reloading the page).

**Expected Result:**
- The affected tile's value updates to reflect the new state without a full page reload.

**Status:** NOT TESTED
