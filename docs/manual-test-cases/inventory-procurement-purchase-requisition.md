# Inventory Procurement — Purchase Requisition Manual Test Cases

Covers the third Phase 2 ("Purchasing & Suppliers") slice of the Inventory Management module
(`docs/inventory-management/`): "Purchasing & Suppliers" → Purchase Requisitions.

## TC-INV-PROC-014: Start a Purchase Requisition and add products

**Preconditions:**
- An Inventory Location and at least two Products exist.
- Logged in as a user holding `INVENTORY_PURCHASE_REQUISITION_MANAGE`.

**Steps:**
1. Go to Purchasing & Suppliers → Purchase Requisitions → New Requisition.
2. Select the Location, keep today's date, Save.
3. On the requisition detail screen, add two products with requested quantities (e.g. 10 and 5).

**Expected Result:**
- The requisition shows Status "Draft" and both products listed with their requested quantities.
- Adding the same product a second time is blocked with a clear message.
- Removing a line while still Draft removes it from the sheet immediately.

**Status:** NOT TESTED

## TC-INV-PROC-015: Submit locks the product list and starts per-line review

**Preconditions:**
- A Draft requisition with at least one line exists (TC-INV-PROC-014).

**Steps:**
1. Click Submit Requisition and confirm.

**Expected Result:**
- Status changes to "Submitted"; the add-product row and per-line Remove buttons disappear.
- Every line shows Status "Pending", with "Awaiting approval" shown to a user who does not hold
  `INVENTORY_PURCHASE_REQUISITION_APPROVE`.
- Submitting a requisition with zero lines is blocked with a clear message.

**Status:** NOT TESTED

## TC-INV-PROC-016: Approve/reject per line — header completes once every line is resolved

**Preconditions:**
- A Submitted requisition with two Pending lines exists (TC-INV-PROC-015).
- Logged in as a user holding `INVENTORY_PURCHASE_REQUISITION_APPROVE`.

**Steps:**
1. Approve the first line (optionally with a resolution note).
2. Reject the second line (optionally with a reason).

**Expected Result:**
- After step 1: the first line shows Status "Approved" with the approver and timestamp; the
  requisition itself is still "Submitted" (one line still Pending).
- After step 2: the second line shows Status "Rejected"; the requisition header automatically
  changes to "Completed" now that every line is resolved.
- Attempting to approve or reject an already-resolved line again is blocked with a clear message.

**Status:** NOT TESTED

## TC-INV-PROC-017: Cancel is only available while Draft

**Preconditions:**
- A Draft requisition exists.

**Steps:**
1. Click Cancel Requisition and confirm.
2. Separately, start a new requisition, submit it, then attempt to cancel it via a direct API call
   (or note that no Cancel button is shown once Submitted).

**Expected Result:**
- Step 1: Status changes to "Cancelled"; the requisition remains visible in the unfiltered list
  (not deleted).
- Step 2: cancelling a non-Draft requisition is blocked with a clear message; the UI itself does
  not offer the action once Submitted/Completed/Cancelled.

**Status:** NOT TESTED

## TC-INV-PROC-018: Requisitions list filters by location and status

**Preconditions:**
- Requisitions exist across at least two locations and at least two different statuses.

**Steps:**
1. Go to Purchase Requisitions and filter by one location, then by one status.

**Expected Result:**
- Only matching requisitions are shown for each filter; the Pending column shows a highlighted
  count only when a Submitted requisition has at least one line still Pending.

**Status:** NOT TESTED
