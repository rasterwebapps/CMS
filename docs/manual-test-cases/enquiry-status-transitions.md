# Enquiry Status Transition Enforcement — Manual Test Cases

## Overview

These test cases verify the enforced status transition rules for enquiries. Only the transitions
listed below are permitted via the manual status dropdown. All other transitions are system-driven
and must occur only through their dedicated screens.

**Allowed manual transitions:**

| From             | Allowed targets           |
|------------------|---------------------------|
| ENQUIRED         | INTERESTED, NOT_INTERESTED |
| NOT_INTERESTED   | INTERESTED                |
| FEES_FINALIZED   | NOT_INTERESTED            |
| CLOSED           | ENQUIRED (re-open)        |
| INTERESTED / FEES_PAID / PARTIALLY_PAID / DOCUMENTS_SUBMITTED / CONVERTED | *(none — no manual options)* |

---

## TC-ST-001: ENQUIRED → INTERESTED (valid manual transition)

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_FRONT_OFFICE
- An enquiry exists in ENQUIRED status

**Steps:**
1. Open the Enquiry List page
2. Locate the enquiry with ENQUIRED status
3. Click the status change dropdown
4. Verify the options shown are **INTERESTED** and **NOT_INTERESTED** only (CLOSED must not appear)
5. Select **INTERESTED**
6. Confirm the update

**Expected Result:**
- Enquiry status updates to INTERESTED
- Status history records the transition

**Status:** NOT TESTED

---

## TC-ST-002: ENQUIRED → NOT_INTERESTED (valid manual transition)

**Preconditions:**
- An enquiry exists in ENQUIRED status

**Steps:**
1. Open the Enquiry List page
2. Locate the enquiry in ENQUIRED status
3. Click the status dropdown and select **NOT_INTERESTED**

**Expected Result:**
- Enquiry status updates to NOT_INTERESTED

**Status:** NOT TESTED

---

## TC-ST-003: INTERESTED — no manual status options

**Preconditions:**
- An enquiry exists in INTERESTED status

**Steps:**
1. Open the Enquiry List page
2. Locate the enquiry in INTERESTED status
3. Inspect the status dropdown / actions column

**Expected Result:**
- No manual status change dropdown is shown for INTERESTED enquiries (or it is disabled/empty)

**Status:** NOT TESTED

---

## TC-ST-004: NOT_INTERESTED → INTERESTED (re-engagement)

**Preconditions:**
- An enquiry exists in NOT_INTERESTED status

**Steps:**
1. Open the Enquiry List page
2. Locate the enquiry in NOT_INTERESTED status
3. Click the status dropdown — verify only **INTERESTED** is offered (no CLOSED)
4. Select **INTERESTED**

**Expected Result:**
- Enquiry status updates to INTERESTED

**Status:** NOT TESTED

---

## TC-ST-005: FEES_FINALIZED → NOT_INTERESTED (student changes mind)

**Preconditions:**
- An enquiry exists in FEES_FINALIZED status

**Steps:**
1. Open the Enquiry List page
2. Locate the enquiry in FEES_FINALIZED status
3. Click the status dropdown — verify only **NOT_INTERESTED** is offered (no FEES_PAID, PARTIALLY_PAID, or CLOSED)
4. Select **NOT_INTERESTED**

**Expected Result:**
- Enquiry status updates to NOT_INTERESTED

**Status:** NOT TESTED

---

## TC-ST-006: CLOSED → ENQUIRED (re-open)

**Preconditions:**
- An enquiry exists in CLOSED status

**Steps:**
1. Open the Enquiry List page
2. Locate the enquiry in CLOSED status
3. Click the status dropdown — verify only **ENQUIRED** is offered
4. Select **ENQUIRED**

**Expected Result:**
- Enquiry status updates to ENQUIRED

**Status:** NOT TESTED

---

## TC-ST-007: Backend rejects invalid manual transition via PATCH /{id}/status

**Preconditions:**
- An enquiry exists in INTERESTED status
- API tool (e.g., Postman) or curl available

**Steps:**
1. Send `PATCH /api/v1/enquiries/{id}/status?status=FEES_FINALIZED`
2. Observe the response

**Expected Result:**
- HTTP 409 Conflict
- Response body contains message: `Cannot manually transition from INTERESTED to FEES_FINALIZED`

**Status:** NOT TESTED

---

## TC-ST-008: Backend rejects CLOSED as a manual target via PATCH

**Preconditions:**
- An enquiry exists in ENQUIRED status

**Steps:**
1. Send `PATCH /api/v1/enquiries/{id}/status?status=CLOSED`

**Expected Result:**
- HTTP 409 Conflict (CLOSED is not a valid manual target from ENQUIRED)

**Status:** NOT TESTED

---

## TC-ST-009: Fee finalization only allowed from INTERESTED status

**Preconditions:**
- An enquiry exists in ENQUIRED status

**Steps:**
1. Navigate to the Fee Finalization screen for that enquiry, or send `POST /api/v1/enquiries/{id}/finalize-fees` directly

**Expected Result:**
- HTTP 409 Conflict
- Response body contains: `Enquiry must be in INTERESTED status to finalize fees`

**Status:** NOT TESTED

---

## TC-ST-010: Submit Documents only allowed from FEES_PAID or PARTIALLY_PAID

**Preconditions:**
- An enquiry exists in INTERESTED status

**Steps:**
1. Send `POST /api/v1/enquiries/{id}/submit-documents`

**Expected Result:**
- HTTP 409 Conflict
- Response body contains: `Enquiry must be in FEES_PAID or PARTIALLY_PAID status to submit documents`

**Status:** NOT TESTED

---

## TC-ST-011: Submit Documents succeeds from FEES_PAID

**Preconditions:**
- An enquiry exists in FEES_PAID status
- All mandatory documents have been uploaded

**Steps:**
1. Send `POST /api/v1/enquiries/{id}/submit-documents`

**Expected Result:**
- HTTP 200 OK
- Enquiry status transitions to DOCUMENTS_SUBMITTED

**Status:** NOT TESTED

---

## TC-ST-012: Convert to student only allowed from DOCUMENTS_SUBMITTED

**Preconditions:**
- An enquiry exists in FEES_PAID status

**Steps:**
1. Send `PUT /api/v1/enquiries/{id}/convert?studentId=1` or navigate to Convert screen

**Expected Result:**
- HTTP 409 Conflict
- Response body contains: `Enquiry must be in DOCUMENTS_SUBMITTED status to convert`

**Status:** NOT TESTED

---

## TC-ST-013: canConvert() button only shown for DOCUMENTS_SUBMITTED

**Preconditions:**
- Enquiries exist in INTERESTED, FEES_FINALIZED, FEES_PAID, PARTIALLY_PAID, and DOCUMENTS_SUBMITTED statuses

**Steps:**
1. Open the Enquiry List page
2. Examine the actions column for each enquiry

**Expected Result:**
- Convert button is visible **only** for the enquiry in DOCUMENTS_SUBMITTED status
- All other statuses do not show the Convert button

**Status:** NOT TESTED

---

## TC-ST-014: Optimistic locking — concurrent edit returns 409

**Preconditions:**
- Two browser sessions logged in as admin
- An enquiry in ENQUIRED status

**Steps:**
1. In Session A: Open the enquiry status change dropdown (do not submit yet)
2. In Session B: Change the enquiry status to INTERESTED and save
3. In Session A: Select NOT_INTERESTED and submit

**Expected Result:**
- Session A receives HTTP 409 Conflict with message: `This record was modified by another user. Please refresh and try again.`

**Status:** NOT TESTED
