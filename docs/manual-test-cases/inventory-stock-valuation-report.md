# Inventory Reporting & Dashboards — Stock Valuation Report Manual Test Cases

Covers Phase 8's ("Reporting & Dashboards") second slice of the Inventory Management module
(`docs/inventory-management/`): total on-hand value grouped by category.

## TC-INV-SVAL-001: Category totals match the underlying Stock Balance data

**Preconditions:**
- At least two categories, each with two or more products holding a non-zero stock balance at
  one or more locations.
- Logged in as a user holding `INVENTORY_STOCK_VIEW`.

**Steps:**
1. Go to Stock Management → Stock Valuation.
2. For each category row, independently sum `valueOnHand` for every Stock Balance row whose
   product belongs to that category (via the Stock Balance report, filtered by product).
3. Compare against the report's Total Value column for that category.

**Expected Result:**
- Every category's Total Value matches the independently-summed figure exactly — the report is
  a live rollup, never a stale/cached number.
- The Products column shows the count of *distinct products* holding stock in that category —
  not a summed quantity (quantities aren't safely summable across products with different
  units of measure).

**Status:** NOT TESTED

## TC-INV-SVAL-002: Grand total matches the sum of category rows

**Preconditions:**
- The report has at least two category rows.

**Steps:**
1. Manually sum the Total Value column across every category row shown.
2. Compare against the Grand Total row.

**Expected Result:**
- The Grand Total row's value and product count exactly equal the sum of the category rows
  above it.

**Status:** NOT TESTED

## TC-INV-SVAL-003: Location filter narrows the valuation correctly

**Preconditions:**
- The same product holds stock at two different locations, with different quantities/values.

**Steps:**
1. View the report with no location filter (All Locations).
2. Apply the location filter to just one of the two locations.

**Expected Result:**
- Step 1: the category total includes value from both locations.
- Step 2: the category total drops to reflect only that location's stock — never double-counts
  or omits the other location's contribution incorrectly.

**Status:** NOT TESTED

## TC-INV-SVAL-004: A category with zero stock never appears

**Preconditions:**
- A category exists with active products, but none of them currently hold any stock balance
  anywhere.

**Steps:**
1. View the report.

**Expected Result:**
- That category does not appear as a row at all (not even a zero-value row) — the rollup is
  built from `StockBalance` rows that actually exist, not from every category in the catalog.

**Status:** NOT TESTED

## TC-INV-SVAL-005: Permission gating reuses the existing Stock permission

**Preconditions:**
- A user holding neither `INVENTORY_STOCK_VIEW` nor `INVENTORY_STOCK_MANAGE`.

**Steps:**
1. Attempt to navigate to the Stock Valuation route directly.
2. Attempt to call the report endpoint directly.

**Expected Result:**
- Both rejected — this report deliberately reuses the same permission that already gates the
  Stock Balance list (it's a rollup of the same underlying data), not a new dedicated one.

**Status:** NOT TESTED
