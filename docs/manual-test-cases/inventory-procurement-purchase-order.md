# Inventory Procurement — Purchase Order Manual Test Cases

Covers the sixth and final Phase 2 ("Purchasing & Suppliers") slice of the Inventory Management
module (`docs/inventory-management/`): "Purchasing & Suppliers" → Purchase Orders.

## TC-INV-PROC-026: Start a Purchase Order and pick up approved requisition lines

**Preconditions:**
- A Supplier and an Inventory Location exist.
- A Purchase Requisition for that location has at least one line already `APPROVED` (see
  `inventory-procurement-purchase-requisition.md`).
- Logged in as a user holding `INVENTORY_PURCHASE_ORDER_MANAGE`.

**Steps:**
1. Go to Purchasing & Suppliers → Purchase Orders → New Order.
2. Select the Supplier and the Location the approved line belongs to, keep today's date, Save.
3. On the order detail screen, the approved line appears in the "Approved Requisition Line"
   picker. Select it and Add Line.

**Expected Result:**
- Status shows "Pending" throughout.
- The picker only ever lists `APPROVED` lines for the order's own location.
- The added line's quantity defaults to the requisition line's requested quantity; unit price
  auto-fills from the active `VendorProductMapping` rate for that supplier/product if one exists.
- The source requisition line's status flips to "Ordered" and it no longer appears in this or any
  other order's picker for that location (double-booking is blocked).

**Status:** NOT TESTED

## TC-INV-PROC-027: Unit price required when no vendor rate is on file

**Preconditions:**
- An approved requisition line exists for a product with **no** active `VendorProductMapping` for
  the chosen supplier.

**Steps:**
1. Attempt to add that line to a Pending order without entering a Unit Price.

**Expected Result:**
- The add is rejected with a clear message that no vendor rate is on file and a price must be
  entered manually. Entering a price manually succeeds.

**Status:** NOT TESTED

## TC-INV-PROC-028: Tax rule applies to the line total; removing a line reopens the requisition line

**Preconditions:**
- A Pending order with the add-line picker available.
- At least one active Tax Rule exists.

**Steps:**
1. Add a line with a Tax Rule selected.
2. Remove that line from the order.

**Expected Result:**
- After step 1: the line shows the tax amount and a line total of (qty × unit price) + tax,
  computed to 2 decimal places.
- After step 2: the line disappears from the order sheet, and its source requisition line's
  status reverts from "Ordered" back to "Approved" — it becomes available again in this or another
  order's picker.

**Status:** NOT TESTED

## TC-INV-PROC-029: Send Order locks the line sheet

**Preconditions:**
- A Pending order with at least one line exists (TC-INV-PROC-026).

**Steps:**
1. Click Send Order and confirm.

**Expected Result:**
- Status changes to "Ordered"; the add-line row and per-line Remove buttons disappear, replaced by
  a read-only "Received" column (0 until Goods Receipt exists).
- Sending an order with zero lines is blocked with a clear message; the button itself is disabled
  when the sheet is empty.

**Status:** NOT TESTED

## TC-INV-PROC-030: Force Close requires a reason and is blocked once already closed

**Preconditions:**
- An Ordered purchase order exists.
- Logged in as a user holding `INVENTORY_PURCHASE_ORDER_FORCE_CLOSE`.

**Steps:**
1. Attempt to Force Close with the reason field left blank.
2. Enter a reason and Force Close.
3. Attempt to Force Close the same order again.

**Expected Result:**
- Step 1: the Force Close button stays disabled until a reason is entered.
- Step 2: status changes to "Force Closed"; the reason, actor, and timestamp are shown on the
  summary card.
- Step 3: rejected with a clear message that the order is already closed; the Force Close section
  no longer renders once an order is Completed or Force Closed.

**Status:** NOT TESTED

## TC-INV-PROC-031: Orders list filters by supplier and status

**Preconditions:**
- Orders exist across at least two suppliers and at least two different statuses.

**Steps:**
1. Go to Purchase Orders and filter by one supplier, then by one status.

**Expected Result:**
- Only matching orders are shown for each filter; the Total column shows the order's currency
  code alongside the sum of every line's line total.

**Status:** NOT TESTED
