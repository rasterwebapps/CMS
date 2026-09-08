# Inventory Stock Tracking Manual Test Cases

Covers the third Phase 1 slice of the Inventory Management module (`docs/inventory-management/`):
Inventory Locations, and the Stock Balance / Record Stock Movement screens under "Stock Management".
Builds on `inventory-catalog-category-uom.md` and `inventory-catalog-product.md` — at least one
Category, Uom, and Product must already exist.

## TC-INV-LOC-001: Create an inventory location wrapping a Room

**Preconditions:**
- Logged in as a user holding `INVENTORY_LOCATION_MANAGE`.

**Steps:**
1. Navigate to Stock Management → Locations → Add Location.
2. Select a Zone, then select a Room within that zone (the Room dropdown is disabled until a Zone is chosen).
3. Enter a Virtual Name (e.g. "Main Store"), leave Role as "Store".
4. Save.

**Expected Result:**
- Location is created; the list shows its Room, Zone, Role, and Status.

**Status:** NOT TESTED

## TC-INV-LOC-002: Virtual name must be globally unique; the same Room can back multiple locations

**Preconditions:**
- "Main Store" exists (TC-INV-LOC-001), wrapping a specific Room.

**Steps:**
1. Attempt to add another location also named "Main Store" (any room).
2. Add a different location, name "Second Store", pointing at the *same* Room as "Main Store".

**Expected Result:**
- Step 1 is blocked by the async uniqueness check on Virtual Name.
- Step 2 succeeds — two locations may legitimately share one physical Room (e.g. two labs sharing a room), only the virtual name needs to be unique.

**Status:** NOT TESTED

## TC-INV-STOCK-001: Record a Receipt and see it reflected in Stock Balance

**Preconditions:**
- Logged in as a user holding `INVENTORY_STOCK_MANAGE`.
- At least one Product and one active Inventory Location exist.

**Steps:**
1. Navigate to Stock Management → Stock Balance → Record Movement.
2. Select a Product and a Location, Transaction Type "Receipt", Quantity 50, Unit Cost 10.
3. Save.
4. Return to the Stock Balance list.

**Expected Result:**
- The movement is recorded; the Stock Balance list now shows Qty On Hand = 50, Value On Hand = 500.00 for that Product/Location.

**Status:** NOT TESTED

## TC-INV-STOCK-002: A second Receipt at the same Product/Location accumulates, not replaces

**Preconditions:**
- TC-INV-STOCK-001 already recorded (Qty On Hand = 50).

**Steps:**
1. Record another Receipt for the same Product and Location: Quantity 20, Unit Cost 12.

**Expected Result:**
- Stock Balance now shows Qty On Hand = 70, Value On Hand = 740.00 (500 + 20×12) — a single balance row, not a second one.

**Status:** NOT TESTED

## TC-INV-STOCK-003: A Disposal reduces on-hand qty and value using the current average cost

**Preconditions:**
- TC-INV-STOCK-002 already recorded (Qty On Hand = 70, Value On Hand = 740.00 → average unit cost ≈ 10.57).

**Steps:**
1. Record a Disposal for the same Product/Location: Quantity 10, leave Unit Cost blank.

**Expected Result:**
- Qty On Hand drops to 60. Value On Hand drops by roughly 10 × 10.57 ≈ 105.71 (the current weighted-average unit cost, not a FIFO/FEFO calculation — real batch-level costing is a later phase).

**Status:** NOT TESTED

## TC-INV-STOCK-004: A movement that would drive stock negative is rejected

**Preconditions:**
- A Product/Location combination with a known Qty On Hand (e.g. 60 from TC-INV-STOCK-003).

**Steps:**
1. Attempt a Disposal for Quantity 1000 (far more than what's on hand) at that same Product/Location.

**Expected Result:**
- The movement is rejected with a message about a negative resulting quantity; Stock Balance is unchanged.

**Status:** NOT TESTED

## TC-INV-STOCK-005: Adjustment direction controls the sign

**Steps:**
1. Record an Adjustment, Direction "Increase", Quantity 5, for a Product/Location with an existing balance.
2. Record an Adjustment, Direction "Decrease", Quantity 5, same Product/Location.

**Expected Result:**
- Step 1 increases Qty On Hand by 5; step 2 decreases it back by 5 — net unchanged.

**Status:** NOT TESTED

## TC-INV-STOCK-006: Batch/serial number creates a distinct balance row

**Steps:**
1. Record a Receipt for a Product/Location with Batch/Serial No. "BATCH-A", Expiry Date set, Quantity 20.
2. Record a second Receipt for the *same* Product/Location with no batch/serial number, Quantity 15.

**Expected Result:**
- Stock Balance shows two separate rows for that Product/Location — one for "BATCH-A" (qty 20, with its expiry date shown) and one with no batch (qty 15) — not merged into one.

**Status:** NOT TESTED

## TC-INV-STOCK-007: Stock Balance filters by Product and Location

**Steps:**
1. On the Stock Balance screen, filter by a specific Product, then clear it and filter by a specific Location.

**Expected Result:**
- The list narrows correctly in each case; clearing a filter (back to "All") restores the full list.

**Status:** NOT TESTED
