# Inventory Requests, Issues & Returns — Internal Return Manual Test Cases

Covers the second Phase 4 ("Requests, Issues & Returns") slice of the Inventory Management
module (`docs/inventory-management/`): returning previously-issued stock back to the issuing
location, from an approved Stock Issue Request line.

## TC-INV-ISSUE-006: Return part of an issued line

**Preconditions:**
- A Stock Issue Request line has been approved (issued) — see
  `inventory-issue-stock-issue-request.md`. Note the issuing location's current on-hand quantity
  for the product before continuing.
- Logged in as a user holding `INVENTORY_ISSUE_REQUEST_RETURN`.

**Steps:**
1. On the request detail screen, enter a quantity less than the issued quantity in the Return
   field for that line and click Return.
2. Check the Stock Balance report for the product at the issuing location.

**Expected Result:**
- The line's resolution column shows the returned quantity alongside the original issue detail
  (e.g. "Issued · 3 returned").
- The issuing location's on-hand quantity increased by the returned quantity — the opposite
  direction from Supplier Return, which decreases stock.

**Status:** NOT TESTED

## TC-INV-ISSUE-007: Over-return is blocked; return field disappears once fully returned

**Preconditions:**
- An approved line with a quantity already partially returned (TC-INV-ISSUE-006).

**Steps:**
1. Attempt to return more than what's still outstanding (requested minus already returned).
2. Return exactly the remaining outstanding quantity instead.

**Expected Result:**
- Step 1 is rejected with a clear message naming the still-returnable quantity.
- Step 2 succeeds; the Return input and button disappear for that line once the full requested
  quantity has been returned (nothing left to return).

**Status:** NOT TESTED

## TC-INV-ISSUE-008: Return is only offered on approved lines

**Preconditions:**
- A Stock Issue Request with one Pending line and one Rejected line.

**Steps:**
1. View the request detail screen as a user holding `INVENTORY_ISSUE_REQUEST_RETURN`.

**Expected Result:**
- No Return control appears for the Pending or Rejected lines — only an Approved line offers it.
  Attempting the return action directly against a non-approved line (e.g. via a direct API call)
  is rejected with a clear message.

**Status:** NOT TESTED
