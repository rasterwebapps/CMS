# Inventory Equipment & Asset Management — Depreciation Manual Test Cases

Covers the third Phase 5 ("Equipment & Asset Management") slice of the Inventory Management
module (`docs/inventory-management/`): standard straight-line depreciation, computed live and
shown on the Asset Register list. No new screen — the existing Asset Register gained a "Book
Value" column.

## TC-INV-ASSET-009: Book value computes correctly for a fully-specified asset

**Preconditions:**
- Register an asset with Purchase Value 120000, a Purchase Date exactly 12 months ago, Useful
  Life (months) 60, and Salvage Value 12000.

**Steps:**
1. View the asset in the Asset Register list.

**Expected Result:**
- Book Value shows approximately 98,400 — straight-line: (120000 − 12000) / 60 = 1800/month;
  1800 × 12 months elapsed = 21,600 accumulated depreciation; 120000 − 21,600 = 98,400.

**Status:** NOT TESTED

## TC-INV-ASSET-010: Missing depreciation inputs show "—", not a misleading zero

**Preconditions:**
- Register an asset leaving Purchase Value, Purchase Date, or Useful Life blank (any one of the
  three).

**Steps:**
1. View the asset in the Asset Register list.

**Expected Result:**
- Book Value shows "—", not "0.00" — depreciation is only computed when purchase value,
  purchase date, and useful life are all present.

**Status:** NOT TESTED

## TC-INV-ASSET-011: Book value never drops below salvage value

**Preconditions:**
- Register an asset with a Purchase Date well beyond its Useful Life in the past (e.g. useful
  life 12 months, purchase date 5 years ago) and a nonzero Salvage Value.

**Steps:**
1. View the asset in the Asset Register list.

**Expected Result:**
- Book Value equals the Salvage Value exactly — depreciation stops accumulating once the asset's
  useful life has fully elapsed, it never continues past salvage value.

**Status:** NOT TESTED

## TC-INV-ASSET-012: No salvage value defaults to zero, not a missing-data case

**Preconditions:**
- Register an asset with Purchase Value, Purchase Date, and Useful Life set, but Salvage Value
  left blank.

**Steps:**
1. View the asset in the Asset Register list.

**Expected Result:**
- Book Value computes normally (depreciation is still applicable), treating the missing salvage
  value as zero rather than blocking the computation.

**Status:** NOT TESTED
