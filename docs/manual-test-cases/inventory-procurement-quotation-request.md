# Inventory Procurement — Quotation Request (RFQ) Manual Test Cases

No manual-test-case doc existed for the Quotation Request module before the document-numbering
feature (2026-09-22) — the module's other flows (invite suppliers, record responses, award,
convert to Purchase Order) are not yet covered here and should be backfilled separately, per the
project's BR-doc backfill queue. This file currently covers only the numbering feature.

## TC-INV-QUOTE-001: Quotation number is auto-generated and immutable

**Preconditions:**
- A `QUOTATION_REQUEST_NUMBER` series is configured under Settings → Number Sequences (seeded by
  default: prefix "QR", scope Financial Year, e.g. `QR-2526-00001`).

**Steps:**
1. Create a new quotation request and note its number in the list/detail header.
2. Create a second quotation request for the same financial year.

**Expected Result:**
- Step 1: the number matches the configured series format and is shown as the request's primary
  identifier (list column "Number", detail page header) — never `#{id}` unless the row predates
  this feature.
- Step 2: its number is the next sequence value, never a repeat.

**Status:** NOT TESTED

## TC-INV-QUOTE-002: Regenerate Quotation Numbers (admin action)

**Preconditions:**
- Logged in as a user holding `INVENTORY_QUOTATION_REGENERATE_NUMBERS`.
- At least one quotation request predates this feature (no number).
- **This mutates production data — take a database backup first** outside a local/throwaway
  environment, per the project's production-data-safety policy.

**Steps:**
1. From the Quotation Requests list, click "Regenerate Numbers", review the confirmation summary, confirm.

**Expected Result:**
- Every affected request's number updates to a fresh, gap-free per-scope-period sequence (oldest
  `request_date` first); a user without the permission never sees the button.

**Status:** NOT TESTED
