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

## TC-INV-PROD-001: Product code is auto-generated from the category's short code

**Preconditions:**
- Logged in as a user holding `INVENTORY_PRODUCT_MANAGE`.
- Category "Chemicals" exists with Short Code "CHM" (see `inventory-catalog-category-uom.md`) and at least one Uom exists.

**Steps:**
1. Navigate to Stock Management → Products → Add Product.
2. Before selecting a Category, confirm the (read-only) Product Code field shows a placeholder like "Select a category to generate a code".
3. Select Category "Chemicals" and confirm the Product Code field updates to a live preview, e.g. "CHM-000001" (or the next free sequence number for that category).
4. Enter Name "Test Item", select any Base UOM.
5. Save.

**Expected Result:**
- Product is created with the previewed code exactly as shown; the Code field is not editable at any point.
- The Products list shows the new product with that code, its category, and UOM code.

**Status:** NOT TESTED

## TC-INV-PROD-001b: Category with no short code blocks product creation with a clear message

**Preconditions:**
- A category exists that predates this feature and has no Short Code set (or temporarily clear an existing category's short code via Edit Category, then Cancel to restore it afterward).

**Steps:**
1. Add Product, select that category.

**Expected Result:**
- The Product Code field shows an inline error naming the category and pointing to Manage Categories to set one — no code is generated, and Save is effectively blocked (the backend rejects it the same way if attempted).

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

## TC-INV-PROD-005: Product code is unique by construction; name is unique per category

**Preconditions:**
- A product exists under category "Chemicals" (TC-INV-PROD-001), named "Test Item", with code e.g. "CHM-000001".

**Steps:**
1. Add a second product under "Chemicals" and confirm its previewed/assigned code is the next sequence number (e.g. "CHM-000002") — never a repeat.
2. Add another product named "Test Item" under "Chemicals".
3. Add another product named "Test Item" under a *different* category.

**Expected Result:**
- Step 1: codes never collide — each new product under the same category gets the next number, since the server owns generation end to end.
- Step 2 is blocked by the async name-uniqueness check.
- Step 3 succeeds — name uniqueness is scoped to category, not global.

**Status:** NOT TESTED

## TC-INV-PROD-005b: Editing a product never changes its code, even across a category move

**Preconditions:**
- A product exists under "Chemicals" with code "CHM-000001".

**Steps:**
1. Edit the product, confirm the Product Code field shows "CHM-000001" as read-only with the hint "Assigned automatically when the product was created — never changes".
2. Change its Category to a different one (e.g. "Lab Consumables" / short code "LAB") and Save.
3. Reopen the product for edit.

**Expected Result:**
- The product keeps code "CHM-000001" throughout — moving categories never regenerates or reformats it.

**Status:** NOT TESTED

## TC-INV-PROD-005c: Regenerate Product Codes (admin action)

**Preconditions:**
- Logged in as a user holding `INVENTORY_PRODUCT_REGENERATE_CODES` (defaults to the same tier as `INVENTORY_PRODUCT_MANAGE`).
- At least one product exists whose code doesn't match the current `<ShortCode>-<sequence>` pattern for its category (e.g. left over from before this feature, or after TC-INV-PROD-005b's category move).
- **This mutates every product's code — take a database backup first if running against anything beyond a local/throwaway dataset**, per the project's production-data-safety policy.

**Steps:**
1. From the Products list, click "Regenerate Codes" (only visible with the permission above).
2. Review the confirmation dialog's summary (count of products/categories affected).
3. Confirm.
4. If any category referenced by an existing product has no Short Code set, instead expect a blocking error before any confirmation dialog appears.

**Expected Result:**
- Step 3: every affected product's code updates to a fresh, gap-free per-category sequence (oldest product first), a success toast reports the count, and the list refreshes to show the new codes. Codes assigned to products going forward continue from the new highest number per category.
- Step 4: a clear error names every category still missing a short code — nothing is changed until every category with products has one set (Manage Categories).

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
