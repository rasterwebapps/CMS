# Inventory Requests, Issues & Returns — Stock Issue Request Manual Test Cases

Covers the first Phase 4 ("Requests, Issues & Returns") slice of the Inventory Management module
(`docs/inventory-management/`): "Requests, Issues & Returns" → Stock Issue Requests.

**Naming note for the tester:** this is distinct from "Purchase Requisitions" (Phase 2, requests
to *buy* from a supplier) — this screen requests already-on-hand stock be issued from one location
to another. Don't confuse the two screens/permissions while testing.

## TC-INV-ISSUE-001: Start a request and add a product

**Preconditions:**
- Two Inventory Locations exist; the issuing location holds on-hand stock of a product.
- Logged in as a user holding `INVENTORY_ISSUE_REQUEST_MANAGE`.

**Steps:**
1. Go to Requests, Issues & Returns → Stock Issue Requests → New Request.
2. Select different Requesting and Issuing locations, keep today's date, Save.
3. On the request detail screen, add the product with a requested quantity.

**Expected Result:**
- Status shows "Draft" throughout.
- Selecting the same location for both Requesting and Issuing is blocked with a clear message.
- The product appears with the entered quantity; adding the same product twice is blocked; a line
  can be removed while still Draft.

**Status:** NOT TESTED

## TC-INV-ISSUE-002: Submit locks the list; Approve posts real stock movement

**Preconditions:**
- A Draft request with one line exists (TC-INV-ISSUE-001). Note the issuing location's current
  on-hand quantity for the product before continuing.
- Logged in as a user holding `INVENTORY_ISSUE_REQUEST_APPROVE` for the approve step.

**Steps:**
1. Click Submit Request and confirm.
2. Approve the line.
3. Check the Stock Balance report for the product at the issuing location.

**Expected Result:**
- Step 1: status changes to "Submitted"; the add-product row and Remove buttons disappear; the
  line shows "Pending".
- Step 2: the line shows "Approved"; the header automatically changes to "Completed" once every
  line is resolved (single-line request).
- Step 3: the issuing location's on-hand quantity decreased by the approved quantity — approving
  a line here has a real stock consequence, unlike Purchase Requisition's own approve (which only
  reaches a sign-off state).

**Status:** NOT TESTED

## TC-INV-ISSUE-003: Approving is blocked when the issuing location doesn't have enough stock

**Preconditions:**
- A Submitted request with a Pending line whose requested quantity exceeds the issuing location's
  current on-hand quantity.

**Steps:**
1. Attempt to approve that line.

**Expected Result:**
- Rejected with a clear message about the resulting negative on-hand quantity (the same guard
  Record Stock Movement already enforces). The line stays "Pending" — nothing is posted, and the
  request does not move to "Completed".

**Status:** NOT TESTED

## TC-INV-ISSUE-004: Reject requires no stock movement; header completes once every line resolved

**Preconditions:**
- A Submitted request with two Pending lines.

**Steps:**
1. Approve the first line.
2. Reject the second line (optionally with a reason).

**Expected Result:**
- After step 1: the issuing location's stock decreases; the header stays "Submitted" (one line
  still Pending).
- After step 2: the second line shows "Rejected" with no stock change; the header automatically
  changes to "Completed" now that every line is resolved.

**Status:** NOT TESTED

## TC-INV-ISSUE-005: Cancel is only available while Draft; list filters by location and status

**Preconditions:**
- A Draft request exists; separately, requests exist across at least two locations and statuses.

**Steps:**
1. Click Cancel Request and confirm.
2. Go to the Stock Issue Requests list and filter by one location, then by one status.

**Expected Result:**
- Step 1: status changes to "Cancelled"; the request remains visible in the unfiltered list.
- Step 2: the location filter matches a request whether that location is the requester or the
  issuer; the status filter narrows further.

**Status:** NOT TESTED
