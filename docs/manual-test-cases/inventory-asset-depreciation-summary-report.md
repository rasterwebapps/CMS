# Inventory Reporting & Dashboards — Asset Depreciation Summary Report Manual Test Cases

Covers Phase 8's ("Reporting & Dashboards") fourth slice of the Inventory Management module
(`docs/inventory-management/`): purchase value, accumulated depreciation, and current book
value grouped by category.

## TC-INV-ADEP-001: Category totals match the sum of individual assets' figures

**Preconditions:**
- At least two categories, each with two or more assets that have full depreciation data
  (purchase value, purchase date, useful life, optional salvage value).
- Logged in as a user holding `INVENTORY_ASSET_VIEW`.

**Steps:**
1. Go to Equipment & Asset Management → Depreciation Summary.
2. For each category row, independently sum the Purchase Value, Accumulated Depreciation, and
   Current Book Value shown on each individual asset's own Asset Register entry.
3. Compare against the report's category row.

**Expected Result:**
- Every figure matches exactly — the report is a live rollup of the same per-asset figures the
  Asset Register already shows, computed by the exact same formula (never a second, drifted
  calculation).

**Status:** NOT TESTED

## TC-INV-ADEP-002: Assets with incomplete depreciation data still count, contribute zero

**Preconditions:**
- An asset exists in a category with a Purchase Value but no Useful Life Months set (so its own
  Asset Register entry shows depreciation as "not applicable").

**Steps:**
1. View the report for that asset's category.

**Expected Result:**
- The asset is included in the category's Assets count and its Purchase Value contributes to
  the Purchase Value total, but it contributes zero to Accumulated Depreciation and Current
  Book Value — it's never silently dropped from the count, and never fabricates a depreciation
  figure the underlying data doesn't support.

**Status:** NOT TESTED

## TC-INV-ADEP-003: Disposed assets are excluded entirely

**Preconditions:**
- An asset in a category has been moved to Disposed status.

**Steps:**
1. Note the category's totals before disposal.
2. Dispose the asset, then reload the report.

**Expected Result:**
- The category's Assets count decreases by one, and its Purchase Value/Accumulated
  Depreciation/Current Book Value totals drop by that asset's own contribution — a disposed
  asset is not part of the "still on the register" picture this report shows.

**Status:** NOT TESTED

## TC-INV-ADEP-004: Grand total matches the sum of category rows

**Preconditions:**
- The report has at least two category rows.

**Steps:**
1. Manually sum every numeric column across the category rows.
2. Compare against the Grand Total row.

**Expected Result:**
- The Grand Total row's asset count and every value column exactly equal the sum of the
  category rows above it.

**Status:** NOT TESTED

## TC-INV-ADEP-005: Permission gating reuses the existing Asset permission

**Preconditions:**
- A user holding neither `INVENTORY_ASSET_VIEW` nor `INVENTORY_ASSET_MANAGE`.

**Steps:**
1. Attempt to navigate to the Depreciation Summary route directly.
2. Attempt to call the report endpoint directly.

**Expected Result:**
- Both rejected — this report deliberately reuses the same permission that already gates the
  Asset Register, not a new dedicated one.

**Status:** NOT TESTED
