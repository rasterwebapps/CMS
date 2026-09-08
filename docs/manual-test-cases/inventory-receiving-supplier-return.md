# Inventory Receiving — Return to Supplier Manual Test Cases

Covers the third and final Phase 3 ("Receiving & Stock Movement") slice of the Inventory
Management module (`docs/inventory-management/`): "Receiving & Stock Movement" → Supplier Returns.

## TC-INV-RECV-011: Start a return against a confirmed receipt and add a line

**Preconditions:**
- A confirmed Goods Receipt exists with at least one received line (see
  `inventory-receiving-goods-receipt.md`).
- Logged in as a user holding `INVENTORY_SUPPLIER_RETURN_MANAGE`.

**Steps:**
1. Go to Receiving & Stock Movement → Supplier Returns → New Return.
2. Select the confirmed receipt, keep today's date, optionally pick a Reason, Save.
3. On the return detail screen, add a line for less than the full received quantity.

**Expected Result:**
- Status shows "Draft" throughout; only confirmed receipts appear in the picker (a still-Draft
  receipt is not offered).
- The line appears with the entered quantity; the picker only lists lines still holding a
  returnable (not-yet-fully-returned) quantity.

**Status:** NOT TESTED

## TC-INV-RECV-012: Over-return is blocked; removing a line reopens the quantity

**Preconditions:**
- A Draft return with an open receipt line exists.

**Steps:**
1. Attempt to add a line with a returned quantity greater than what's still returnable.
2. Add a valid line instead, then remove it.

**Expected Result:**
- Step 1 is rejected with a clear message naming the still-returnable quantity.
- Step 2: after removing the line, the same receipt line's full open quantity is available to add
  again.

**Status:** NOT TESTED

## TC-INV-RECV-013: Complete posts a decrease and nets against the order's received quantity

**Preconditions:**
- A Draft return with one line exists, against an order whose status is currently "Completed"
  (every line fully received) — return a partial quantity from that fully-received line.

**Steps:**
1. Click Complete Return and confirm.
2. Check the Stock Balance report for the product/location; check the source order's own status.

**Expected Result:**
- Return status changes to "Completed"; the add-line row and Remove buttons disappear.
- Stock Balance's on-hand quantity decreased by the returned quantity at the receipt's location.
- The order's status correctly reverts from "Completed" to "Partially Completed", reflecting the
  true accepted quantity after the return.
- Confirming an already-completed return, or one with zero lines, is blocked with a clear message.

**Status:** NOT TESTED

## TC-INV-RECV-014: Cancel is only available while Draft

**Preconditions:**
- A Draft return exists.

**Steps:**
1. Click Cancel Return and confirm.

**Expected Result:**
- Status changes to "Cancelled"; the return remains visible in the unfiltered list (not deleted).
- Cancel is not offered once a return is Completed or already Cancelled.

**Status:** NOT TESTED

## TC-INV-RECV-015: Returns list filters by status

**Preconditions:**
- Returns exist across at least two different statuses.

**Steps:**
1. Go to Supplier Returns and filter by one status.

**Expected Result:**
- Only matching returns are shown.

**Status:** NOT TESTED
