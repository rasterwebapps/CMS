# OneBook Integration — Manual Test Cases

Covers the full OneBook payment cycle: configuration, outbound pushes (scholarship disbursement, commission, refund), inbound webhook callbacks, and the UI surface on the student detail page.

**Permission required:** `SCHOLARSHIP_DISBURSE` (push), `SCHOLARSHIP_VIEW` (read), Admin access (config).  
**OneBook config keys (System Configuration):** `onebook.enabled`, `onebook.api_url`, `onebook.username`, `onebook.password`, `onebook.org_id`, `onebook.branch_id`, `onebook.webhook_secret`.

---

## Configuration

### TC-OB-CFG-001: Enable OneBook integration via System Configuration

**Preconditions:**
- Logged in as DEV_ADMIN or SUPPORT_ADMIN.

**Steps:**
1. Navigate to **Settings → System Configuration**.
2. Locate the key `onebook.enabled` and set value to `true`.
3. Set `onebook.api_url` to the OneBook base URL (e.g. `https://onebook.example.com/`).
4. Set `onebook.username` and `onebook.password`.
5. Set `onebook.org_id`, `onebook.branch_id`, `onebook.zone_name`.
6. Set `onebook.webhook_secret` to a random secret string (same one registered in OneBook's callback config).
7. Save each key.

**Expected Result:**
- All keys saved with a success toast.
- No restart required — values are read from DB on every push.

**Status:** NOT TESTED

---

### TC-OB-CFG-002: Push blocked when integration is disabled

**Preconditions:**
- `onebook.enabled` is set to `false` (or missing).
- An APPROVED scholarship application exists.

**Steps:**
1. Navigate to the student's scholarship tab.
2. Click **Disburse via OneBook** on any APPROVED application.

**Expected Result:**
- Error toast or dialog: "OneBook integration is not enabled."
- No `onebook_payment_requests` row is created.

**Status:** NOT TESTED

---

## Scholarship Disbursement

### TC-OB-SCH-001: Push APPROVED scholarship to OneBook — TRANSMITTED

**Preconditions:**
- OneBook integration is enabled (TC-OB-CFG-001 complete).
- A student has an APPROVED scholarship application.
- Student has bank details: Account Number, IFSC Code, Bank Name populated on their profile.

**Steps:**
1. Navigate to **Students → [Student] → Detail → Scholarships tab**.
2. Find the APPROVED scholarship row.
3. Click **Disburse via OneBook**.
4. Fill in: Amount, Disbursement Date, Disbursement Mode = `DIRECT_CREDIT`.
5. Confirm the action.

**Expected Result:**
- Success toast: "Disbursement submitted to OneBook."
- Response includes a `referenceId` and `status: TRANSMITTED`.
- A row appears in **OneBook Payments** panel on the Scholarships tab with status chip `TRANSMITTED`.
- `onebook_payment_requests` row exists in DB with `payment_type = SCHOLARSHIP`, `status = TRANSMITTED`, `transmitted_at` populated.

**Status:** NOT TESTED

---

### TC-OB-SCH-002: Push fails when student has no bank details

**Preconditions:**
- An APPROVED scholarship application exists.
- Student's bank account number is blank.

**Steps:**
1. Attempt **Disburse via OneBook** on the APPROVED application.

**Expected Result:**
- Error: "Missing bank details for Student for scholarship #[id] — bank account, bank name, and IFSC code are required."
- A `onebook_payment_requests` row is **not** created.

**Status:** NOT TESTED

---

### TC-OB-SCH-003: Push blocked when application is not APPROVED

**Preconditions:**
- A scholarship application exists with status `PENDING` or `SANCTIONED`.

**Steps:**
1. Attempt **Disburse via OneBook** on that application.

**Expected Result:**
- Error: "Only APPROVED scholarship applications can be disbursed via OneBook."
- No DB row created.

**Status:** NOT TESTED

---

### TC-OB-SCH-004: OneBook API unreachable — stored as FAILED

**Preconditions:**
- `onebook.api_url` is set to a non-reachable URL (e.g. `http://127.0.0.1:9999/`).
- An APPROVED scholarship with bank details exists.

**Steps:**
1. Click **Disburse via OneBook**.

**Expected Result:**
- Response returns `status: FAILED`.
- A `onebook_payment_requests` row exists with `status = FAILED`, `error_message` contains "OneBook API call failed:".
- **OneBook Payments** panel on the student profile shows a FAILED chip with the error message (truncated to 60 chars with `…`).

**Status:** NOT TESTED

---

### TC-OB-SCH-005: Multiple push attempts accumulate rows

**Preconditions:**
- A prior push resulted in FAILED.
- Student now has correct bank details and OneBook is reachable.

**Steps:**
1. Click **Disburse via OneBook** a second time on the same application.

**Expected Result:**
- A **second** `onebook_payment_requests` row is created (TRANSMITTED).
- **OneBook Payments** panel shows both rows, sorted newest-first.
- Most recent row is TRANSMITTED; earlier row is FAILED.

**Status:** NOT TESTED

---

### TC-OB-SCH-006: OneBook Payments panel hidden when no payments exist

**Preconditions:**
- A student with scholarship applications exists but no OneBook push has been attempted.

**Steps:**
1. Navigate to **Students → [Student] → Detail → Scholarships tab**.

**Expected Result:**
- **OneBook Payments** panel is **not rendered** (hidden when list is empty).
- Only the Eligibility Profile, Scholarships list, and Disbursements sections are visible.

**Status:** NOT TESTED

---

## Commission Payment

### TC-OB-COM-001: Push commission payment to OneBook — TRANSMITTED

**Preconditions:**
- OneBook integration is enabled.
- An enquiry with `commission_payment_status = PENDING` and a referral source (agent/staff/faculty) that has bank details.

**Steps:**
1. Navigate to the relevant enquiry.
2. Click **Push Commission to OneBook** (or equivalent action).
3. Confirm.

**Expected Result:**
- Commission status updates to `TRANSMITTED`.
- `onebook_payment_requests` row: `payment_type = COMMISSION`, `status = TRANSMITTED`, invoice number in `COMM-YYYY-NNNNN` format.
- Enquiry commission field reflects TRANSMITTED state.

**Status:** NOT TESTED

---

### TC-OB-COM-002: Commission push blocked — wrong status

**Preconditions:**
- An enquiry with `commission_payment_status = PAID`.

**Steps:**
1. Attempt to push commission to OneBook.

**Expected Result:**
- Error: "Cannot transmit to OneBook from status: PAID."
- No DB row created, no status change.

**Status:** NOT TESTED

---

## Refund Payment

### TC-OB-RFD-001: Push student fee refund to OneBook — TRANSMITTED

**Preconditions:**
- OneBook integration is enabled.
- A `fee_refunds` row with `entity_type = STUDENT`, `status = PENDING`, and the student has bank details.

**Steps:**
1. Navigate to **Finance → Fee Refunds → [Refund]**.
2. Click **Push to OneBook**.
3. Confirm.

**Expected Result:**
- Refund row status updates to the OneBook flow state.
- `onebook_payment_requests` row: `payment_type = REFUND`, `status = TRANSMITTED`, invoice in `RFD-YYYY-NNNNN` format.

**Status:** NOT TESTED

---

### TC-OB-RFD-002: Enquiry-type refund blocked

**Preconditions:**
- A `fee_refunds` row with `entity_type = ENQUIRY`.

**Steps:**
1. Attempt to push this refund to OneBook.

**Expected Result:**
- Error: "Enquiry refunds cannot be pushed to OneBook — bank details are not stored for enquiry records. Process this refund manually."
- No DB row created.

**Status:** NOT TESTED

---

### TC-OB-RFD-003: Refund push fails when student has no bank details

**Preconditions:**
- A student refund (`entity_type = STUDENT`) exists.
- The student's bank account number is blank.

**Steps:**
1. Attempt to push to OneBook.

**Expected Result:**
- Error: "Missing bank details for Student for refund #[id]…"
- Refund row status unchanged, no `onebook_payment_requests` row created.

**Status:** NOT TESTED

---

## Webhook Callbacks

> Simulate with `curl -X PUT` or Postman. The webhook URL is `POST /webhooks/onebook/posting-track-update` and `PUT /webhooks/onebook/posting-track-completion`. Header: `X-OneBook-Secret: <configured secret>`.

### TC-OB-WH-001: posting-track-update — updates OneBook register ID

**Preconditions:**
- A `onebook_payment_requests` row exists with `status = TRANSMITTED` and a known `invoice_number`.

**Steps:**
Send to `PUT /webhooks/onebook/posting-track-update`:
```json
[{
  "invoiceNumber": "<invoice_number>",
  "oneBookPaymentRegisterId": "REG-12345",
  "status": "CREATED",
  "remarks": "Queued for processing"
}]
```
Header: `X-OneBook-Secret: <secret>`

**Expected Result:**
- HTTP 200: `{"message": "true"}`
- `onebook_payment_requests` row: `onebook_txn_id = REG-12345`, `onebook_status = CREATED`, `onebook_remarks = Queued for processing`.
- UI **OneBook Payments** panel: Details cell shows `CREATED — Queued for processing`.

**Status:** NOT TESTED

---

### TC-OB-WH-002: posting-track-update — rejected when secret is wrong

**Preconditions:**
- OneBook integration is configured with a webhook secret.

**Steps:**
Send `PUT /webhooks/onebook/posting-track-update` with header `X-OneBook-Secret: wrong-secret` and any valid JSON body.

**Expected Result:**
- HTTP 401 (no body).
- No DB changes.

**Status:** NOT TESTED

---

### TC-OB-WH-003: posting-track-update — rejected when secret header is absent

**Steps:**
Send `PUT /webhooks/onebook/posting-track-update` with **no** `X-OneBook-Secret` header.

**Expected Result:**
- HTTP 401.

**Status:** NOT TESTED

---

### TC-OB-WH-004: posting-track-completion — PAID creates ScholarshipDisbursement

**Preconditions:**
- A `onebook_payment_requests` row with `payment_type = SCHOLARSHIP`, `status = TRANSMITTED`, and a known `invoice_number`.
- The linked scholarship application is APPROVED.

**Steps:**
Send to `PUT /webhooks/onebook/posting-track-completion`:
```json
[{
  "invoiceNumber": "<invoice_number>",
  "status": "PAID",
  "transactionNumber": "TXN-2026-999",
  "paymentMode": "NEFT",
  "bankName": "State Bank of India",
  "transactionDate": "2026-07-03"
}]
```
Header: `X-OneBook-Secret: <secret>`

**Expected Result:**
- HTTP 200: `{"message": "true"}`
- `onebook_payment_requests` row: `status = PAID`.
- A `scholarship_disbursements` row is created for the application.
- **Disbursements** panel on the student profile now shows the new disbursement entry.
- **OneBook Payments** panel chip: `PAID` (green).

**Status:** NOT TESTED

---

### TC-OB-WH-005: posting-track-completion — FAILED marks row as FAILED

**Preconditions:**
- A `onebook_payment_requests` row with `payment_type = SCHOLARSHIP`, `status = TRANSMITTED`.

**Steps:**
Send `PUT /webhooks/onebook/posting-track-completion`:
```json
[{
  "invoiceNumber": "<invoice_number>",
  "status": "REJECTED",
  "remarks": "Invalid IFSC code"
}]
```

**Expected Result:**
- HTTP 200: `{"message": "true"}`
- `onebook_payment_requests` row: `status = FAILED`.
- No `scholarship_disbursements` row is created.
- **OneBook Payments** panel chip: `FAILED` (red) with error icon and truncated error message in the Details cell.
- No toast/alert in UI — panel auto-refreshes on next page load.

**Status:** NOT TESTED

---

### TC-OB-WH-006: posting-track-completion — unknown invoice returns 200 (OneBook always gets ACK)

**Preconditions:**
- No `onebook_payment_requests` row for invoice `GHOST-001`.

**Steps:**
Send `PUT /webhooks/onebook/posting-track-completion` with `"invoiceNumber": "GHOST-001"`.

**Expected Result:**
- HTTP 200: `{"message": "true"}` — OneBook always receives an ACK regardless of lookup result.
- Backend logs a `NOT_FOUND` warning.
- No DB changes.

**Status:** NOT TESTED

---

### TC-OB-WH-007: posting-track-completion — body can be a single object (not array)

**Preconditions:**
- A known `invoice_number` in TRANSMITTED state.

**Steps:**
Send `PUT /webhooks/onebook/posting-track-completion` with a **plain JSON object** (not wrapped in `[…]`):
```json
{
  "invoiceNumber": "<invoice_number>",
  "status": "PAID"
}
```

**Expected Result:**
- HTTP 200: `{"message": "true"}`
- Row updated to PAID — single-object body is normalized to a 1-element array internally.

**Status:** NOT TESTED

---

## UI Surface — OneBook Payments Panel

### TC-OB-UI-001: All four status chips render correctly

**Preconditions:**
- Manually insert (or trigger via pushes + webhooks) four `onebook_payment_requests` rows linked to the same scholarship application: one each with `status = PENDING`, `TRANSMITTED`, `PAID`, `FAILED`.

**Steps:**
1. Open the student's Scholarships tab.

**Expected Result:**

| Status      | Chip color    |
|-------------|--------------|
| PENDING     | Amber/warning |
| TRANSMITTED | Blue/primary  |
| PAID        | Green/success |
| FAILED      | Red/danger    |

- Text is uppercase on the chip.
- No missing/unstyled chip (white text on white background).

**Status:** NOT TESTED

---

### TC-OB-UI-002: FAILED row shows error message in Details column

**Preconditions:**
- A FAILED `onebook_payment_requests` row exists with `error_message = "OneBook API call failed: Connection refused to http://onebook.example.com/api/paymentregister"`.

**Steps:**
1. Open the student's Scholarships tab, locate the OneBook Payments panel.

**Expected Result:**
- Details cell shows a red error icon + truncated message: `OneBook API call failed: Connection refused to h…`
- Full message visible on hover via `title` attribute (tooltip).

**Status:** NOT TESTED

---

### TC-OB-UI-003: TRANSMITTED row without onebook status shows "Awaiting callback" in Details

**Preconditions:**
- A TRANSMITTED row with no `onebook_status` (i.e. posting-track-update webhook not yet received).

**Steps:**
1. Open the student's Scholarships tab.

**Expected Result:**
- Details cell: italic muted text "Awaiting callback from OneBook".

**Status:** NOT TESTED

---

### TC-OB-UI-004: Payments sorted newest-first

**Preconditions:**
- Two `onebook_payment_requests` rows exist for the same application, created at different times.

**Steps:**
1. Open the student's Scholarships tab.

**Expected Result:**
- Most recently created payment appears at the top of the OneBook Payments table.
- Invoice number, amount, transmitted date, and status all match the corresponding row.

**Status:** NOT TESTED

---

## End-to-End Flow

### TC-OB-E2E-001: Full scholarship cycle — push → track-update → completion → disbursement visible

**Preconditions:**
- OneBook integration enabled with real or sandbox credentials.
- Student has an APPROVED scholarship, valid bank details.

**Steps:**
1. Push via **Disburse via OneBook** — verify TRANSMITTED in UI.
2. Simulate or wait for OneBook to call `PUT /webhooks/onebook/posting-track-update` with `status = CREATED`.
   - Verify Details cell updates to `CREATED — <remarks>`.
3. Simulate or wait for `PUT /webhooks/onebook/posting-track-completion` with `status = PAID`.
   - Verify OneBook Payments chip: PAID.
   - Verify **Disbursements** panel gains a new entry with the correct amount.
   - Verify `scholarship_disbursements` row created in DB.

**Expected Result:**
- End state: OneBook Payments chip = PAID, Disbursements panel shows 1 new entry, student profile is consistent.
- No duplicate disbursement rows even if the completion webhook fires twice (idempotency: depends on service-layer guard; verify in DB).

**Status:** NOT TESTED

---

*Document created: 2026-07-03 — R1-4.4.10*
