# Inventory Procurement — Tax Rules, Suppliers & Rate Contracts Manual Test Cases

Covers the first Phase 2 ("Purchasing & Suppliers") slice of the Inventory Management module
(`docs/inventory-management/`), under "Purchasing & Suppliers" → Suppliers / Rate Contracts / Tax
Rules.

## TC-INV-PROC-001: Create a Tax Rule

**Preconditions:**
- Logged in as a user holding `INVENTORY_TAX_RULE_MANAGE`.

**Steps:**
1. Go to Purchasing & Suppliers → Tax Rules → Add Tax Rule.
2. Enter Name "GST 18%", Rate 18.
3. Save.

**Expected Result:**
- The tax rule appears in the list with rate "18%" and status Active.
- Attempting to create a second tax rule with the exact same name is blocked with a clear message.

**Status:** NOT TESTED

## TC-INV-PROC-002: Create a Supplier — starts unapproved

**Preconditions:**
- Logged in as a user holding `INVENTORY_SUPPLIER_MANAGE`.

**Steps:**
1. Go to Purchasing & Suppliers → Suppliers → Add Supplier.
2. Enter Code "SUP-0001", Name "Acme Medical Supplies", and any optional contact/bank details.
3. Save.

**Expected Result:**
- The supplier appears in the list with Approval status "Pending" (not "Approved") and Status "Active".
- Attempting to create a second supplier with the exact same code is blocked with a clear message.

**Status:** NOT TESTED

## TC-INV-PROC-003: Approve a Supplier is a separate step from editing it

**Preconditions:**
- Supplier "SUP-0001" exists and is unapproved (TC-INV-PROC-002).
- Logged in as a user holding `INVENTORY_SUPPLIER_MANAGE` but **not** `INVENTORY_SUPPLIER_APPROVE`.

**Steps:**
1. Open the Suppliers list.

**Expected Result:**
- The supplier can still be edited/deactivated, but no Approve action is visible for this user.
- Logging in as a user who also holds `INVENTORY_SUPPLIER_APPROVE` shows an Approve action on the same row; clicking it and confirming changes the Approval badge to "Approved" and records an approval date.
- Attempting to approve an already-approved supplier again is blocked with a clear message.

**Status:** NOT TESTED

## TC-INV-PROC-004: Bank/tax details are masked for view-only users

**Preconditions:**
- Supplier "SUP-0001" has a bank account number and tax registration id filled in.
- Two users: one holding only `INVENTORY_SUPPLIER_VIEW`, one holding `INVENTORY_SUPPLIER_MANAGE`.

**Steps:**
1. As the view-only user, look up the supplier (list or any screen that surfaces supplier details from the API).
2. As the manage user, do the same.

**Expected Result:**
- The view-only user sees the bank account number and tax registration id masked to their last 4 characters (e.g. "••••1234"), not blank and not the full value.
- The manage user sees the full, unmasked values.
- The supplier's edit form (which itself requires `INVENTORY_SUPPLIER_MANAGE` to open) always shows full values — masking only applies to the read/view-only path.

**Status:** NOT TESTED

## TC-INV-PROC-005: Create a Rate Contract against a Supplier

**Preconditions:**
- Supplier "SUP-0001" exists.
- Logged in as a user holding `INVENTORY_RATE_CONTRACT_MANAGE`.

**Steps:**
1. Go to Purchasing & Suppliers → Rate Contracts → Add Rate Contract.
2. Select Supplier "SUP-0001", Start Date today, End Date one year later.
3. Save.

**Expected Result:**
- The rate contract appears in the list against "Acme Medical Supplies" with the correct start/end dates.
- Setting an End Date earlier than the Start Date is blocked with a clear message, both when creating and when editing.

**Status:** NOT TESTED

## TC-INV-PROC-006: Rate Contracts list filters by supplier

**Preconditions:**
- At least two suppliers each have at least one rate contract.

**Steps:**
1. Go to Rate Contracts and filter by one supplier.

**Expected Result:**
- Only that supplier's rate contracts are shown.

**Status:** NOT TESTED

## TC-INV-PROC-007: Deactivating a Supplier/Rate Contract/Tax Rule doesn't delete it

**Preconditions:**
- A Supplier, a Rate Contract, and a Tax Rule each exist and are Active.

**Steps:**
1. Deactivate each one from its list screen.

**Expected Result:**
- Each item's Status badge changes to "Inactive"; the row remains visible in the unfiltered list (not deleted), and can be reactivated the same way.

**Status:** NOT TESTED
