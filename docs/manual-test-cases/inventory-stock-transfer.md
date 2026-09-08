# Inventory Stock Movement — Stock Transfer Manual Test Cases

Covers the second Phase 3 ("Receiving & Stock Movement") slice of the Inventory Management module
(`docs/inventory-management/`): "Receiving & Stock Movement" → Stock Transfers.

## TC-INV-STOCK-006: Start a transfer and add a product

**Preconditions:**
- Two Inventory Locations exist, with the source location holding on-hand stock of a product.
- Logged in as a user holding `INVENTORY_STOCK_TRANSFER_MANAGE`.

**Steps:**
1. Go to Receiving & Stock Movement → Stock Transfers → New Transfer.
2. Select different Source and Destination locations, keep today's date, Save.
3. On the transfer detail screen, add the product with a quantity less than what's on hand at the
   source.

**Expected Result:**
- Status shows "Draft" throughout.
- Selecting the same location for both Source and Destination is blocked with a clear message.
- The product appears on the sheet with the entered quantity; adding the same product twice is
  blocked.

**Status:** NOT TESTED

## TC-INV-STOCK-007: Complete posts a matched decrease/increase and carries value across

**Preconditions:**
- A Draft transfer with one line exists (TC-INV-STOCK-006). Note the source location's current
  Stock Balance value-on-hand for the product before continuing.

**Steps:**
1. Click Complete Transfer and confirm.
2. Check the Stock Balance report for the product at both the source and destination locations.

**Expected Result:**
- Status changes to "Completed"; the add-product row and Remove buttons disappear.
- Source location's on-hand quantity decreased by the transferred quantity; destination's on-hand
  quantity increased by the same amount.
- The destination's value-on-hand reflects the source's weighted-average unit cost at the time of
  transfer (not zero) — value is carried across, not lost.

**Status:** NOT TESTED

## TC-INV-STOCK-008: Transferring more than what's on hand is blocked

**Preconditions:**
- A Draft transfer exists with a line quantity greater than the source location's current on-hand
  quantity for that product (add the line, then let stock at the source drop below it via another
  movement, or add a quantity you know exceeds what's shown on Stock Balance).

**Steps:**
1. Click Complete Transfer.

**Expected Result:**
- Rejected with a clear message about the resulting negative on-hand quantity — the same guard
  Record Stock Movement already enforces; no partial posting happens (neither location's balance
  changes).

**Status:** NOT TESTED

## TC-INV-STOCK-009: Cancel is only available while Draft

**Preconditions:**
- A Draft transfer exists.

**Steps:**
1. Click Cancel Transfer and confirm.

**Expected Result:**
- Status changes to "Cancelled"; the transfer remains visible in the unfiltered list (not
  deleted). Cancel is not offered once a transfer is Completed or already Cancelled.

**Status:** NOT TESTED

## TC-INV-STOCK-010: Transfers list filters by location and status

**Preconditions:**
- Transfers exist across at least two locations (as either source or destination) and at least
  two different statuses.

**Steps:**
1. Go to Stock Transfers and filter by one location, then by one status.

**Expected Result:**
- The location filter matches a transfer whether that location is the source or the destination;
  the status filter narrows further.

**Status:** NOT TESTED
