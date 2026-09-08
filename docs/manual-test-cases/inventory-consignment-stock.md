# Inventory Gate Pass, Vendor-Owned Stock & Service Requests — Consignment Stock Manual Test Cases

Covers Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests") second slice of the
Inventory Management module (`docs/inventory-management/`): vendor-owned ("consignment") stock.

## TC-INV-CONSIGN-001: Create a consignment agreement

**Preconditions:**
- A Supplier and an Inventory Location exist.
- Logged in as a user holding `INVENTORY_CONSIGNMENT_MANAGE`.

**Steps:**
1. Go to Gate Pass & Service Requests → Consignment Agreements → New Agreement.
2. Select the supplier and location, enter an Agreement Number, a Start Date, and Save.
3. Edit the same agreement and set an End Date before the Start Date, Save.

**Steps Expected Result:**
- Step 2: the agreement is created, active by default.
- Step 3: rejected with a clear message that the end date cannot be before the start date.

**Status:** NOT TESTED

## TC-INV-CONSIGN-002: Receive consignment stock — posts to the main stock ledger immediately

**Preconditions:**
- An active consignment agreement exists for a Product/Location.
- Logged in as a user holding `INVENTORY_CONSIGNMENT_MANAGE`.
- Note the product's current on-hand quantity at that location via the Stock Balance report.

**Steps:**
1. Go to Consignment Stock → Receive Stock, select the agreement and product, enter a Quantity
   and a Consignment Price / Unit, Save.
2. Re-check the Stock Balance report for that product/location.

**Expected Result:**
- Step 1: a new Consignment Stock Line appears with Received = the entered quantity, Consumed =
  0, and Vendor-Owned On Hand = the same quantity.
- Step 2: the product's on-hand quantity has increased by the received quantity — receiving
  consignment stock makes it immediately usable, even though it isn't owned yet.

**Status:** NOT TESTED

## TC-INV-CONSIGN-003: A second receipt against the same agreement/product accumulates on one line

**Preconditions:**
- A consignment stock line already exists from a prior receipt (TC-INV-CONSIGN-002).

**Steps:**
1. Receive additional stock against the same agreement and the same product, with a new
   Consignment Price / Unit.

**Expected Result:**
- No second row is created — the existing line's Received quantity increases by the new amount,
  and its Consignment Price is updated to the latest value entered (not averaged).

**Status:** NOT TESTED

## TC-INV-CONSIGN-004: Record consumption transfers ownership without a second stock movement

**Preconditions:**
- A consignment stock line with Vendor-Owned On Hand > 0 exists.
- Logged in as a user holding `INVENTORY_CONSIGNMENT_CONVERT`.
- Note the product's current on-hand quantity at that location via the Stock Balance report.

**Steps:**
1. On the Consignment Stock list, click Consume on the line, enter a quantity less than or equal
   to the on-hand balance, Save.
2. Re-check the Stock Balance report for that product/location.
3. Attempt to record consumption of a quantity greater than the line's current Vendor-Owned On
   Hand balance.

**Expected Result:**
- Step 1: the line's Consumed quantity increases and Vendor-Owned On Hand decreases by the same
  amount; Received quantity is unchanged.
- Step 2: the product's on-hand quantity in the Stock Balance report is **unchanged** by this
  action — recording consumption is a financial reconciliation only, it does not itself move any
  physical stock.
- Step 3: rejected with a clear message naming the actual amount still available.

**Status:** NOT TESTED

## TC-INV-CONSIGN-005: Permission gating — Manage vs. Convert are separate operations

**Preconditions:**
- User A holds only `INVENTORY_CONSIGNMENT_MANAGE`; User B holds only
  `INVENTORY_CONSIGNMENT_CONVERT`. A consignment stock line with on-hand balance exists.

**Steps:**
1. As User A, open the Consignment Stock list — confirm the Receive Stock button is shown, and
   no Consume action is offered on any row.
2. As User B, open the same list — confirm no Receive Stock button is shown, and the Consume
   action is offered on rows with on-hand balance.
3. As User B, attempt to call the receive endpoint directly.
4. As User A, attempt to call the consume endpoint directly.

**Expected Result:**
- Steps 1–2: the UI only ever offers the action the logged-in user actually holds permission for.
- Steps 3–4: both rejected server-side with a clear message naming the missing permission — the
  UI gating is not the only enforcement.

**Status:** NOT TESTED

## TC-INV-CONSIGN-006: Stock cannot be received against an inactive agreement

**Preconditions:**
- A consignment agreement exists; its Is Active flag is set to false via an edit.
- Logged in as a user holding `INVENTORY_CONSIGNMENT_MANAGE`.

**Steps:**
1. Attempt to receive stock against the now-inactive agreement.

**Expected Result:**
- Rejected with a clear message that stock cannot be received against an inactive agreement.

**Status:** NOT TESTED
