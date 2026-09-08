# Inventory Receiving — Goods Receipt Manual Test Cases

Covers the first Phase 3 ("Receiving & Stock Movement") slice of the Inventory Management module
(`docs/inventory-management/`): "Receiving & Stock Movement" → Goods Receipts.

## TC-INV-RECV-001: Start a receipt against a sent order and add a line

**Preconditions:**
- A Purchase Order in status "Ordered" exists with at least one line (see
  `inventory-procurement-purchase-order.md`).
- Logged in as a user holding `INVENTORY_GRN_MANAGE`.

**Steps:**
1. Go to Receiving & Stock Movement → Goods Receipts → New Receipt.
2. Select the order, keep today's date, Save.
3. On the receipt detail screen, add a line for the order's product with a received quantity less
   than the ordered quantity.

**Expected Result:**
- Status shows "Draft" throughout.
- The line appears on the sheet with the entered quantity; unit cost defaults from the order
  line's own unit price when left blank.
- The order-line picker only lists lines that still have an open (unreceived) quantity.

**Status:** NOT TESTED

## TC-INV-RECV-002: Over-receipt is blocked; removing a line reopens the quantity

**Preconditions:**
- A Draft receipt with an open order line exists (TC-INV-RECV-001).

**Steps:**
1. Attempt to add a line with a received quantity greater than what's still open on the order line.
2. Add a valid line instead, then remove it.

**Expected Result:**
- Step 1 is rejected with a clear message naming the still-open quantity.
- Step 2: after removing the line, the same order line's full open quantity is available to add
  again.

**Status:** NOT TESTED

## TC-INV-RECV-003: Confirm posts stock and advances the order's status

**Preconditions:**
- A Draft receipt with one line for less than the full ordered quantity exists.
- The order has exactly one line (for a clean "fully received" check in step 2).

**Steps:**
1. Click Confirm Receipt and confirm the dialog.
2. Check the Stock Balance report for the product/location; check the order's own status.
3. Separately, raise and confirm a second receipt for the remaining open quantity on the same line.

**Expected Result:**
- Step 1: the receipt status changes to "Confirmed"; the add-line row and Remove buttons
  disappear; a read-only "Received" column would show on the parent order's own line sheet.
- Step 2: Stock Balance's on-hand quantity increased by the received quantity at the order's
  location; the order's status changed to "In Progress" (partial receipt, single-line order).
- Step 3: once the line's full ordered quantity has been received, the order's status changes to
  "Completed".
- Confirming an already-confirmed receipt, or a receipt with zero lines, is blocked with a clear
  message.

**Status:** NOT TESTED

## TC-INV-RECV-004: Batch/serial number and expiry are captured and posted

**Preconditions:**
- A Draft receipt with an open order line for a product that tracks batches.

**Steps:**
1. Add a line with a Batch/Serial No. and an Expiry Date filled in, then confirm the receipt.

**Expected Result:**
- The line shows the entered batch/serial number and expiry date on the sheet.
- After confirming, the Stock Balance report shows the on-hand quantity attributed to that batch
  (a new `StockBatch` row is created the same way Record Stock Movement already does).

**Status:** NOT TESTED

## TC-INV-RECV-005: Multi-line order reaches "Partially Completed" before "Completed"

**Preconditions:**
- A Purchase Order with two lines exists, both "Ordered".

**Steps:**
1. Raise and confirm a receipt that fully receives only the first line.
2. Raise and confirm a second receipt that fully receives the second line.

**Expected Result:**
- After step 1: the order's status is "Partially Completed" (one line fully received, one not).
- After step 2: the order's status is "Completed" (every line fully received).

**Status:** NOT TESTED
