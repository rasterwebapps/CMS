# Inventory Catalog & UOM — Product Image Manual Test Cases

Covers the "Also outstanding" `ProductImage` slice of the Inventory Management module
(`docs/inventory-management/`), deferred at the original Product/CategoryAttribute slice and
picked up now, following the `FloorPlanService`/`MinioStorageService` precedent.

## TC-INV-PIMG-001: Upload the first photo automatically becomes primary

**Preconditions:**
- An existing Product with no photos yet.
- Logged in as a user holding `INVENTORY_PRODUCT_IMAGE_MANAGE`.

**Steps:**
1. Open the product's Edit screen, upload a photo via the Photos panel.

**Expected Result:**
- The photo appears in the gallery, marked "Primary" — the very first photo for a product
  always becomes primary automatically, with no extra step required.

**Status:** NOT TESTED

## TC-INV-PIMG-002: Only one photo is ever primary at a time

**Preconditions:**
- A product with at least two uploaded photos, neither newly uploaded marked primary explicitly.

**Steps:**
1. Click the star/"set primary" action on a non-primary photo.
2. Reload the gallery.

**Expected Result:**
- The clicked photo becomes Primary; the previously-primary photo loses the badge — exactly one
  photo is ever marked primary for a given product, never zero (once at least one exists) and
  never more than one.

**Status:** NOT TESTED

## TC-INV-PIMG-003: Deleting the primary photo promotes another automatically

**Preconditions:**
- A product with at least two photos, one of them primary.

**Steps:**
1. Delete the primary photo.
2. Reload the gallery.

**Expected Result:**
- One of the remaining photos is automatically promoted to primary — the product is never left
  with photos but no primary one.

**Status:** NOT TESTED

## TC-INV-PIMG-004: File validation

**Preconditions:**
- Logged in as a user holding `INVENTORY_PRODUCT_IMAGE_MANAGE`.

**Steps:**
1. Attempt to upload a non-image file (e.g. a `.pdf` or `.docx`).
2. Attempt to upload an image file larger than 5MB.

**Expected Result:**
- Both rejected with a clear message — only image files up to 5MB are accepted.

**Status:** NOT TESTED

## TC-INV-PIMG-005: Permission gating — Manage is distinct from View

**Preconditions:**
- User A holds only `INVENTORY_PRODUCT_VIEW` (no image-manage permission); User B holds
  `INVENTORY_PRODUCT_IMAGE_MANAGE`. A product with an existing photo.

**Steps:**
1. As User A, open the product — confirm the photos are visible, but no Upload/Delete/Set
   Primary controls are shown.
2. As User A, attempt to call the upload/delete endpoints directly.
3. As User B, confirm the Upload/Delete/Set Primary controls are shown and work.

**Expected Result:**
- Step 1: read-only gallery, no management controls.
- Step 2: both rejected server-side with a clear message — viewing a product's photos does not
  imply the ability to manage them.
- Step 3: all controls work as expected.

**Status:** NOT TESTED

## TC-INV-PIMG-006: Deleting a product's last photo empties the gallery cleanly

**Preconditions:**
- A product with exactly one photo (primary by definition).

**Steps:**
1. Delete that photo.

**Expected Result:**
- The gallery shows its empty state ("No photos yet") with the Upload control still available —
  no error, no orphaned "Primary" badge on nothing.

**Status:** NOT TESTED
