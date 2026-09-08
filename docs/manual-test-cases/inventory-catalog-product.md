# Inventory Catalog — Products & Category Attributes Manual Test Cases

Covers the second Phase 1 slice of the Inventory Management module (`docs/inventory-management/`):
the Product master and per-category custom attributes, under "Stock Management" → Products.
Builds on `inventory-catalog-category-uom.md` — Categories and Units of Measure must already exist.

## TC-INV-ATTR-001: Add a custom attribute to a category

**Preconditions:**
- Logged in as a user holding `INVENTORY_CATEGORY_MANAGE`.
- A category exists, e.g. "Chemicals".

**Steps:**
1. Edit the "Chemicals" category.
2. In the Attributes section, add: Name "Shelf Life", Data Type "TEXT", not required.
3. Add a second attribute: Name "Hazard Class", Data Type "ENUM", Allowed Values "Flammable, Corrosive, Toxic", marked Required.

**Expected Result:**
- Both attributes appear in the list immediately after saving, each showing its data type and a "Required" badge where applicable.

**Status:** NOT TESTED

## TC-INV-ATTR-002: Cannot delete an attribute that's already in use on a product

**Preconditions:**
- "Chemicals" has attribute "Hazard Class" (from TC-INV-ATTR-001).
- A product exists under "Chemicals" with a value set for "Hazard Class" (see TC-INV-PROD-002).

**Steps:**
1. Attempt to delete the "Hazard Class" attribute from the Chemicals category.

**Expected Result:**
- Deletion is blocked with a message naming the attribute and explaining it's still in use by a product.

**Status:** NOT TESTED

## TC-INV-PROD-001: Create a product with basic fields only

**Preconditions:**
- Logged in as a user holding `INVENTORY_PRODUCT_MANAGE`.
- At least one Category and one Uom exist.

**Steps:**
1. Navigate to Stock Management → Products → Add Product.
2. Enter Code "TST-001" (lowercase, with spaces — should auto-uppercase/strip), Name "Test Item".
3. Select any Category and Base UOM.
4. Save.

**Expected Result:**
- Product is created; code shown uppercased with no spaces; appears in the Products list with its category and UOM code shown.

**Status:** NOT TESTED

## TC-INV-PROD-002: Category attributes render dynamically and save correctly

**Preconditions:**
- "Chemicals" category has attributes "Shelf Life" (TEXT) and "Hazard Class" (ENUM, required) — TC-INV-ATTR-001.

**Steps:**
1. Add Product under category "Chemicals".
2. Confirm a "Category Attributes" section appears with a text field for "Shelf Life" and a dropdown for "Hazard Class" (options: Flammable/Corrosive/Toxic).
3. Try to save without selecting "Hazard Class".
4. Select "Corrosive" for Hazard Class, enter "24 months" for Shelf Life, and save.

**Expected Result:**
- Step 3 is blocked by the required-field validator on Hazard Class.
- Step 4 succeeds; reopening the product for edit shows both values pre-filled correctly.

**Status:** NOT TESTED

## TC-INV-PROD-003: Switching category swaps the attribute set

**Steps:**
1. On the Add Product form, select category "Chemicals" (attributes: Shelf Life, Hazard Class) and enter a value for Shelf Life.
2. Change the Category dropdown to a different category with no attributes (or a different attribute set).
3. Switch back to "Chemicals".

**Expected Result:**
- Step 2: the Category Attributes section updates to reflect the new category (empty or its own attributes) — no leftover fields from "Chemicals".
- Step 3: the Shelf Life field reappears pre-filled with the value entered in step 1 (not lost).

**Status:** NOT TESTED

## TC-INV-PROD-004: Alternate names (aliases)

**Steps:**
1. On a product form, click "+ Add Alternate Name" twice, enter "IV Cannula" and "IV Catheter".
2. Save, then reopen the product for edit.

**Expected Result:**
- Both aliases persist and are shown pre-filled; each has its own Remove button that deletes just that row.

**Status:** NOT TESTED

## TC-INV-PROD-005: Product code is globally unique; name is unique per category

**Preconditions:**
- A product "TST-001" exists under category "Chemicals" (TC-INV-PROD-001), named "Test Item".

**Steps:**
1. Add another product with code "TST-001" under any category.
2. Add another product named "Test Item" under "Chemicals".
3. Add another product named "Test Item" under a *different* category.

**Expected Result:**
- Steps 1 and 2 are blocked by the respective async uniqueness check.
- Step 3 succeeds — name uniqueness is scoped to category, not global.

**Status:** NOT TESTED

## TC-INV-PROD-006: Classification flags and asset-only fields

**Steps:**
1. On a product form, check "Asset".
2. Confirm "Depreciation Rate" and "Warranty Period" fields appear.
3. Uncheck "Asset".

**Expected Result:**
- The two asset-only fields appear only while "Asset" is checked (their entered values are simply not sent if unchecked, not necessarily cleared from the UI — confirm this is acceptable or flag as a follow-up).

**Status:** NOT TESTED

## TC-INV-PROD-007: Deactivate / reactivate a product

**Steps:**
1. From the Products list, toggle a product's status to Inactive, then back to Active.

**Expected Result:**
- Status badge and toast behave the same as the Category/UOM masters.

**Status:** NOT TESTED
