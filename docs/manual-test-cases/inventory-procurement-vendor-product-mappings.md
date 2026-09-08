# Inventory Procurement — Vendor Product Rates Manual Test Cases

Covers the second Phase 2 ("Purchasing & Suppliers") slice of the Inventory Management module
(`docs/inventory-management/`): "Purchasing & Suppliers" → Vendor Product Rates, plus the new
Product Rate Lines section added to the existing Rate Contract form.

## TC-INV-PROC-008: Create a Vendor Product Rate

**Preconditions:**
- Supplier "SUP-0001" and a Product both exist.
- Logged in as a user holding `INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE`.

**Steps:**
1. Go to Purchasing & Suppliers → Vendor Product Rates → Add Vendor Rate.
2. Select Supplier "SUP-0001", search and select a Product, enter Unit Price 100.00.
3. Save.

**Expected Result:**
- The rate appears in the list showing the product, supplier, unit price "INR 100.00", and an
  Effective Price equal to the unit price with no "Contract" tag.

**Status:** NOT TESTED

## TC-INV-PROC-009: One active rate per (Supplier, Product) pair

**Preconditions:**
- A Vendor Product Rate for Supplier "SUP-0001" / a given Product already exists and is Active (TC-INV-PROC-008).

**Steps:**
1. Attempt to add another Vendor Product Rate for the same Supplier and the same Product.

**Expected Result:**
- The Product field shows an inline "already exists" validation error as soon as both Supplier and
  Product are chosen, and Save is blocked.
- Deactivating the existing rate first, then retrying the same Supplier/Product combination,
  succeeds.

**Status:** NOT TESTED

## TC-INV-PROC-010: Rate Contract override — active, in-window contract wins

**Preconditions:**
- A Rate Contract for Supplier "SUP-0001" exists, is Active, and its date range includes today.
- A Vendor Product Rate links that Rate Contract, Unit Price 100.00, for Product "P1".

**Steps:**
1. Edit the Rate Contract → add a Product Rate Line for "P1" with Negotiated Rate 85.00 → Save.
2. Open the Vendor Product Rates list.

**Expected Result:**
- The row for "P1" shows Unit Price "100.00" but Effective Price "85.00" tagged "Contract".
- Removing the rate line (or deactivating the contract, or editing the contract's dates so today
  falls outside its range) and reloading the list reverts the Effective Price back to "100.00" with
  no "Contract" tag.

**Status:** NOT TESTED

## TC-INV-PROC-011: Rate Contract must belong to the same Supplier

**Preconditions:**
- Two suppliers exist, each with their own Rate Contract.

**Steps:**
1. Start adding a Vendor Product Rate for Supplier A.
2. Attempt to submit with Supplier B's Rate Contract selected (e.g. via a stale form state).

**Expected Result:**
- The save is rejected with a clear message that the selected rate contract does not belong to the
  chosen supplier. In normal use the Rate Contract dropdown only ever lists contracts for the
  currently selected Supplier and resets whenever the Supplier changes.

**Status:** NOT TESTED

## TC-INV-PROC-012: A product can only have one rate line per Rate Contract

**Preconditions:**
- A Rate Contract exists.

**Steps:**
1. Edit the Rate Contract → add two Product Rate Lines both pointing at the same Product.
2. Save.

**Expected Result:**
- The save is rejected with a clear message rather than silently keeping only one line.

**Status:** NOT TESTED

## TC-INV-PROC-013: Preferred supplier flag and deactivation

**Preconditions:**
- At least one Vendor Product Rate exists.

**Steps:**
1. Edit it, check "Preferred supplier for this product", Save.
2. Deactivate it from the list.

**Expected Result:**
- The list shows a "Preferred" tag on that row after step 1.
- After step 2, the Status badge changes to "Inactive"; the row remains visible in the unfiltered
  list (not deleted) and can be reactivated the same way. Reactivating it when another active rate
  already exists for the same Supplier/Product pair is blocked with the same "already exists"
  message as TC-INV-PROC-009.

**Status:** NOT TESTED
