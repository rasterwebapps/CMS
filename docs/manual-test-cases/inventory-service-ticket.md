# Inventory Gate Pass, Vendor-Owned Stock & Service Requests — Service Ticket Manual Test Cases

Covers Phase 7's ("Gate Pass, Vendor-Owned Stock & Service Requests") third and final slice of
the Inventory Management module (`docs/inventory-management/`): general internal service/
complaint ticketing.

## TC-INV-TICKET-001: Category is a configurable lookup, not a fixed list

**Preconditions:**
- Logged in as a user holding `INVENTORY_SERVICE_TICKET_CATEGORY_MANAGE`.

**Steps:**
1. Go to Gate Pass & Service Requests → Ticket Categories → Add Category, enter a Name, Save.
2. Attempt to add a second category with the same name (any case).
3. Deactivate the category, then attempt to select it when raising a new ticket.

**Expected Result:**
- Step 1: category created, active by default.
- Step 2: rejected with a clear message that the name already exists — same real-time
  uniqueness check pattern as every other master in this module.
- Step 3: the deactivated category no longer appears in the New Ticket category dropdown, but
  existing tickets already using it still display its name correctly.

**Status:** NOT TESTED

## TC-INV-TICKET-002: Raise a ticket

**Preconditions:**
- An active category and an Inventory Location exist.
- Logged in as a user holding `INVENTORY_SERVICE_TICKET_MANAGE`.

**Steps:**
1. Go to Service Tickets → New Ticket, fill Location, Category, Requested By, Priority,
   Description, Save.

**Expected Result:**
- The ticket is created with status "Open" and the chosen priority (defaults to Medium if left
  unchanged).

**Status:** NOT TESTED

## TC-INV-TICKET-003: Full lifecycle — Open → Assign → Resolve → Close, each a separate permission

**Preconditions:**
- An open ticket exists. User A holds only `INVENTORY_SERVICE_TICKET_ASSIGN`; User B holds only
  `INVENTORY_SERVICE_TICKET_RESOLVE`; User C holds only `INVENTORY_SERVICE_TICKET_CLOSE`.

**Steps:**
1. As User A, open the ticket — confirm only the Assign control is shown — and assign it.
2. As User B, reopen the ticket — confirm only the Resolve control is shown — and resolve it
   with resolution notes.
3. As User C, reopen the ticket — confirm only the Close control is shown, with an optional
   1–5 feedback rating — and close it.

**Expected Result:**
- Step 1: status becomes "In Progress"; Assigned To/At stamped.
- Step 2: status becomes "Resolved"; Resolution Notes/Date/By stamped.
- Step 3: status becomes "Closed"; Closed By/At stamped, and the feedback rating (if given) is
  shown. No further action control is offered once Closed.
- Each user only ever sees the one control matching their own permission — never another
  stage's control.

**Status:** NOT TESTED

## TC-INV-TICKET-004: Stage gating is enforced server-side, not just hidden in the UI

**Preconditions:**
- An open (not yet assigned) ticket exists.
- Logged in as a user holding `INVENTORY_SERVICE_TICKET_RESOLVE` and `_CLOSE` but not `_ASSIGN`.

**Steps:**
1. Attempt to call the resolve endpoint directly against the still-Open ticket.
2. Attempt to call the close endpoint directly against the same ticket.

**Expected Result:**
- Both rejected with a clear message — a ticket can only be resolved once it's In Progress, and
  closed once it's Resolved; skipping a stage is never possible regardless of which permissions
  are held.

**Status:** NOT TESTED

## TC-INV-TICKET-005: Cancel is only reachable from Open or In Progress

**Preconditions:**
- One ticket at Open, one at Resolved.
- Logged in as a user holding `INVENTORY_SERVICE_TICKET_MANAGE`.

**Steps:**
1. Cancel the Open ticket with a reason.
2. Attempt to cancel the Resolved ticket.

**Expected Result:**
- Step 1: status becomes "Cancelled"; Cancelled By/At/Reason stamped; no further action offered.
- Step 2: rejected with a clear message — a resolved (or closed) ticket cannot be cancelled.

**Status:** NOT TESTED

## TC-INV-TICKET-006: Filters narrow the list correctly

**Preconditions:**
- Tickets exist across at least two locations, two categories, two statuses, and two priorities.

**Steps:**
1. Apply each filter (Location, Category, Status, Priority) one at a time on the Service Tickets
   list.

**Expected Result:**
- Each filter narrows the list to exactly the matching tickets; combining filters narrows
  further (AND, not OR).

**Status:** NOT TESTED
