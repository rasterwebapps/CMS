# Inventory Reporting & Dashboards — Price Comparison Report Manual Test Cases

Covers Phase 8's ("Reporting & Dashboards") fifth slice of the Inventory Management module
(`docs/inventory-management/`): every active supplier's rate for one product, side by side.

## TC-INV-PCOMP-001: Rows match the Vendor Product Rates screen exactly

**Preconditions:**
- A product has active Vendor Product Mapping rows from at least two suppliers, one with a
  linked, currently-active Rate Contract line overriding its list price.
- Logged in as a user holding `INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW`.

**Steps:**
1. Go to Purchasing & Suppliers → Price Comparison, select the product.
2. Compare each row against the same supplier's entry on the Vendor Product Rates list
   (filtered to that product).

**Expected Result:**
- Every field matches exactly — List Price, Effective Price, Source (Rate Contract vs.
  Standard), Currency, Lead Time, Preferred flag. This report reuses the exact same
  contract-override resolution the Vendor Product Rates screen already applies, not a second
  calculation.

**Status:** NOT TESTED

## TC-INV-PCOMP-002: Rows sort cheapest-first by effective price, not list price

**Preconditions:**
- A product has one supplier whose plain list price is higher than another's, but whose
  Effective Price (after a Rate Contract override) is actually the lowest.

**Steps:**
1. View the comparison for that product.

**Expected Result:**
- The supplier with the lowest Effective Price appears first and is marked "Cheapest," even
  though its List Price alone is not the lowest — sorting is always by Effective Price.

**Status:** NOT TESTED

## TC-INV-PCOMP-003: Inactive mappings never appear

**Preconditions:**
- A product has an active mapping from Supplier A and a deactivated (inactive) mapping from
  Supplier B.

**Steps:**
1. View the comparison for that product.

**Expected Result:**
- Only Supplier A's row appears — an inactive rate is never shown as if it were currently
  available.

**Status:** NOT TESTED

## TC-INV-PCOMP-004: No active rates shows a clear empty state

**Preconditions:**
- A product exists with no active Vendor Product Mapping from any supplier.

**Steps:**
1. Select that product.

**Expected Result:**
- A clear "No active supplier rates" empty state is shown, not an empty table or an error.

**Status:** NOT TESTED

## TC-INV-PCOMP-005: Permission gating reuses the existing Vendor Product Mapping permission

**Preconditions:**
- A user holding neither `INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW` nor
  `INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE`.

**Steps:**
1. Attempt to navigate to the Price Comparison route directly.

**Expected Result:**
- Rejected — this screen calls the exact same endpoint the Vendor Product Rates list already
  uses, gated by the same permission; no new backend endpoint or permission was introduced.

**Status:** NOT TESTED
