# Inventory Reporting & Dashboards — Budget vs. Actual Report Manual Test Cases

Covers Phase 8's ("Reporting & Dashboards") sixth and final planned slice of the Inventory
Management module (`docs/inventory-management/`): allocated vs. consumed spend rolled up by
location, across every currently-active budget.

## TC-INV-BVA-001: Location totals match the sum of that location's active budgets

**Preconditions:**
- At least two locations, each with two or more active Budgets.
- Logged in as a user holding `INVENTORY_BUDGET_VIEW`.

**Steps:**
1. Go to Budgets & Approvals → Budget vs. Actual.
2. For each location row, independently sum Allocated and Consumed across that location's own
   active Budget rows on the Budgets list.
3. Compare against the report's location row.

**Expected Result:**
- Every figure matches exactly, including Remaining (Allocated − Consumed) and the "Over"
  indicator (shown only when Consumed exceeds Allocated for that location's total).

**Status:** NOT TESTED

## TC-INV-BVA-002: Inactive budgets are excluded

**Preconditions:**
- A location has one active Budget and one deactivated (inactive) Budget.

**Steps:**
1. View the report for that location.

**Expected Result:**
- Only the active budget contributes to the location's totals — an inactive budget is not part
  of "currently active" spend tracking.

**Status:** NOT TESTED

## TC-INV-BVA-003: A location with multiple concurrent budgets sums across all of them

**Preconditions:**
- A location has two active budgets covering two different (even non-overlapping) periods.

**Steps:**
1. View the report for that location.

**Expected Result:**
- The location's row sums both budgets' Allocated and Consumed together into one combined
  figure — this report deliberately does not separate by period; each location gets one
  combined row across all of its currently-active budgets, regardless of period.

**Status:** NOT TESTED

## TC-INV-BVA-004: Grand total matches the sum of location rows

**Preconditions:**
- The report has at least two location rows.

**Steps:**
1. Manually sum every numeric column across the location rows shown.
2. Compare against the Grand Total row.

**Expected Result:**
- The Grand Total row's budget count and every value column exactly equal the sum of the
  location rows above it.

**Status:** NOT TESTED

## TC-INV-BVA-005: Permission gating reuses the existing Budget permission

**Preconditions:**
- A user holding neither `INVENTORY_BUDGET_VIEW` nor `INVENTORY_BUDGET_MANAGE`.

**Steps:**
1. Attempt to navigate to the Budget vs. Actual route directly.
2. Attempt to call the report endpoint directly.

**Expected Result:**
- Both rejected — this report deliberately reuses the same permission that already gates the
  Budgets list, not a new dedicated one.

**Status:** NOT TESTED
