# Inventory Catalog — Categories & Units of Measure Manual Test Cases

Covers the first Phase 1 slice of the Inventory Management module (`docs/inventory-management/`):
the Item Categories and Units of Measure masters under the "Stock Management" nav group.

## TC-INV-CAT-001: Create a top-level category

**Preconditions:**
- Logged in as a user holding `INVENTORY_CATEGORY_MANAGE`.

**Steps:**
1. Navigate to Stock Management → Categories.
2. Click "Add Category".
3. Enter a unique name (e.g. "Lab Consumables"), leave Parent Category as "— Top level —".
4. Save.

**Expected Result:**
- Category is created and appears in the list with no Parent Category shown.
- List and card views both render it correctly.

**Status:** NOT TESTED

## TC-INV-CAT-002: Create a sub-category and see it nested

**Preconditions:**
- At least one top-level category exists (e.g. "Lab Consumables").

**Steps:**
1. Add Category → name "Chemicals" → Parent Category = "Lab Consumables".
2. Save.
3. Filter the Categories list by Parent Category = "Lab Consumables".

**Expected Result:**
- "Chemicals" is created with Parent Category "Lab Consumables".
- Filtering by that parent shows "Chemicals" in the results.

**Status:** NOT TESTED

## TC-INV-CAT-003: Category name uniqueness is scoped per parent, not global

**Preconditions:**
- Two top-level categories exist, e.g. "Lab Consumables" and "IT Assets".

**Steps:**
1. Add Category "Cables" under "IT Assets" → Save.
2. Add Category "Cables" under "Lab Consumables" → Save.
3. Attempt to add another "Cables" also under "IT Assets".

**Expected Result:**
- Steps 1 and 2 both succeed (same name, different parent).
- Step 3 is blocked by the async uniqueness check on the Name field ("Checking…" then a duplicate error) and the Save button stays disabled until the name is changed.

**Status:** NOT TESTED

## TC-INV-CAT-004: Cannot move a category under itself or its own sub-category (cycle prevention)

**Preconditions:**
- A 2-level chain exists: "Lab Consumables" → "Chemicals".

**Steps:**
1. Edit "Lab Consumables".
2. Confirm the Parent Category dropdown does not offer "Lab Consumables" itself.
3. Confirm the dropdown does not offer "Chemicals" (its own child) as a parent option either.

**Expected Result:**
- Neither the category being edited nor any of its descendants appear as selectable parent options — the picker makes a cycle impossible to construct from the UI, not just rejected server-side.

**Status:** NOT TESTED

## TC-INV-CAT-005: Cannot delete a category that has sub-categories

**Preconditions:**
- "Lab Consumables" has at least one sub-category ("Chemicals").

**Steps:**
1. Attempt to delete "Lab Consumables" (via API, or once a delete affordance exists in the UI).

**Expected Result:**
- The delete is rejected with a message indicating sub-categories must be moved or deleted first.

**Status:** NOT TESTED

## TC-INV-CAT-006: Deactivate / reactivate a category

**Steps:**
1. From the Categories list, toggle a category's status to Inactive via the confirm dialog.
2. Toggle it back to Active.

**Expected Result:**
- Status badge updates accordingly in both card and table views after each toggle, with a success toast.

**Status:** NOT TESTED

## TC-INV-UOM-001: Create a unit of measure

**Preconditions:**
- Logged in as a user holding `INVENTORY_UOM_MANAGE`.

**Steps:**
1. Navigate to Stock Management → Units of Measure.
2. Click "Add UOM".
3. Enter Name "Kilogram", Code "kg" (lowercase, with a leading/trailing space).
4. Save.

**Expected Result:**
- Code is auto-uppercased and trimmed to "KG" as you type.
- The unit is created and listed with code "KG".

**Status:** NOT TESTED

## TC-INV-UOM-002: Code and name must each be globally unique

**Preconditions:**
- A UOM with code "KG" / name "Kilogram" already exists.

**Steps:**
1. Add UOM with code "KG" (any name).
2. Add UOM with a different code but name "Kilogram".

**Expected Result:**
- Both attempts are blocked by the respective async uniqueness check (code-exists / name-exists), each with its own inline error.

**Status:** NOT TESTED

## TC-INV-UOM-003: Deactivate / reactivate a unit of measure

**Steps:**
1. From the Units of Measure list, toggle a unit's status to Inactive, then back to Active.

**Expected Result:**
- Status badge and toast behave the same as TC-INV-CAT-006.

**Status:** NOT TESTED

## TC-INV-NAV-001: Stock Management nav group is separate from the legacy Inventory Management group

**Preconditions:**
- Logged in as a user holding both legacy `INVENTORY_VIEW`/`INVENTORY_MANAGE` and the new `INVENTORY_CATEGORY_VIEW`/`INVENTORY_UOM_VIEW` permissions.

**Steps:**
1. Open the sidenav.
2. Locate "Inventory Management" (legacy — Inventory, Maintenance) and "Stock Management" (new — Categories, Units of Measure) as two distinct top-level groups.

**Expected Result:**
- Both groups are visible and distinct; the legacy "Inventory" screen (`/inventory`) is unaffected by this change.

**Status:** NOT TESTED
