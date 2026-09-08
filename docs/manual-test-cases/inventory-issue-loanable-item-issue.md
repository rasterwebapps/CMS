# Inventory Requests, Issues & Returns — Loanable Item Issue Manual Test Cases

Covers the fourth and final Phase 4 ("Requests, Issues & Returns") slice of the Inventory
Management module (`docs/inventory-management/`): "Requests, Issues & Returns" → Loanable Item
Issues.

## TC-INV-ISSUE-009: Issue a loanable product

**Preconditions:**
- A Product exists with "Loanable" checked; an Inventory Location exists.
- Logged in as a user holding `INVENTORY_LOAN_ISSUE_MANAGE`.

**Steps:**
1. Go to Requests, Issues & Returns → Loanable Item Issues → Issue Item.
2. Select the loanable product and location, enter a Borrower Name, an Expected Return Date on or
   after today's Issue Date, Save.

**Expected Result:**
- The record is created with Status "Issued".
- Attempting to issue a product that is **not** flagged Loanable is rejected with a clear message.
- Attempting an Expected Return Date before the Issue Date is rejected with a clear message.

**Status:** NOT TESTED

## TC-INV-ISSUE-010: Overdue surfaces without any background job

**Preconditions:**
- An Issued record exists whose Expected Return Date is in the past.

**Steps:**
1. View the Loanable Item Issues list and the record's own detail screen.
2. Toggle the "Overdue only" filter on the list.

**Expected Result:**
- Both the list row and the detail screen show an "Overdue" indicator — computed live from
  today's date vs. the expected return date, not a stored/stale flag (create a second record
  with a future expected return date and confirm it is never shown as overdue).
- The "Overdue only" filter shows only the overdue, still-Issued record.

**Status:** NOT TESTED

## TC-INV-ISSUE-011: Mark returned records condition and closes the loan

**Preconditions:**
- An Issued record exists.
- Logged in as a user holding `INVENTORY_LOAN_ISSUE_RETURN`.

**Steps:**
1. On the record's detail screen, optionally enter a Condition on Return, then click Mark
   Returned and confirm.
2. Attempt to mark the same record returned again.

**Expected Result:**
- Step 1: Status changes to "Returned"; Actual Return Date, returning user, and the entered
  condition are shown; the Mark Returned control disappears.
- Step 2: rejected with a clear message that the item has already been returned; the control is
  not offered again once Returned.

**Status:** NOT TESTED

## TC-INV-ISSUE-012: No stock ledger effect

**Preconditions:**
- A loanable product with an existing Stock Balance entry at the issuing location.

**Steps:**
1. Note the product's on-hand quantity at that location via the Stock Balance report.
2. Issue the item, then mark it returned.
3. Re-check the Stock Balance report.

**Expected Result:**
- The on-hand quantity is unchanged throughout — Loanable Item Issue deliberately does not post
  to the stock ledger (see the design note in the module's decision log); it is a standalone
  borrow/return record only.

**Status:** NOT TESTED
