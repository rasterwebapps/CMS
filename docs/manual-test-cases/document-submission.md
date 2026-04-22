# Document Submission — Manual Test Cases

These test cases verify the document submission workflow for enquiries that have completed payment (`FEES_PAID` or `PARTIALLY_PAID`).

The workflow is:

1. **List screen** — `/enquiries/document-submission` shows every enquiry whose status is `FEES_PAID` or `PARTIALLY_PAID`.
2. **Entry screen** — `/enquiries/document-submission/:id` lets the user collect each required document for the selected enquiry, marking each as `UPLOADED`, `VERIFIED`, or `REJECTED` and optionally adding remarks.
3. **Submit** — once all 5 mandatory documents (`TENTH_MARKSHEET`, `TWELFTH_MARKSHEET`, `TRANSFER_CERTIFICATE`, `AADHAR_CARD`, `PASSPORT_PHOTO`) are `UPLOADED` or `VERIFIED`, the user clicks **Submit Documents** and the enquiry transitions to `DOCUMENTS_SUBMITTED`.

---

## TC-DOCSUB-001: List screen shows enquiries in FEES_PAID and PARTIALLY_PAID

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
- At least one enquiry exists in `FEES_PAID` status and one in `PARTIALLY_PAID` status.

**Steps:**
1. Navigate to **Admission Management → Document Submission** in the side navigation.
2. Wait for the list to load.

**Expected Result:**
- The page header reads **"Document Submission"**.
- Both the `FEES_PAID` and `PARTIALLY_PAID` enquiries appear in the table.
- The status badge shows the correct status with the appropriate colour.
- Enquiries in any other status (e.g. `INTERESTED`, `FEES_FINALIZED`, `DOCUMENTS_SUBMITTED`, `ADMITTED`) are **not** shown.

**Status:** NOT TESTED

---

## TC-DOCSUB-002: Open the document collection entry screen

**Preconditions:**
- TC-DOCSUB-001 passed.
- At least one enquiry is visible in the document submission list.

**Steps:**
1. From the document submission list, click the upload-icon action on a row.

**Expected Result:**
- The browser navigates to `/enquiries/document-submission/<id>`.
- The toolbar enters focus mode and shows the title **"Collect Documents"**.
- An enquiry summary card displays the candidate's name, phone, program, course, status, and net fee.
- A "Document Checklist" table lists all 15 document types defined in `DocumentType.java`.
- Mandatory documents (`Tenth Marksheet`, `Twelfth Marksheet`, `Transfer Certificate`, `Aadhar Card`, `Passport Photo`) are marked with a red asterisk.
- A progress banner reads **"0 of 5 mandatory documents collected"**.

**Status:** NOT TESTED

---

## TC-DOCSUB-003: Mark a document as uploaded

**Preconditions:**
- TC-DOCSUB-002 passed and the entry screen is open.

**Steps:**
1. Click the **upload_file** icon on the `Tenth Marksheet` row.
2. Wait for the inline spinner to disappear.

**Expected Result:**
- The status badge for `Tenth Marksheet` updates to **"Uploaded"**.
- The progress banner updates to **"1 of 5 mandatory documents collected"**.
- A snackbar reads **"Tenth Marksheet marked as Uploaded"**.
- The button now shows the active style and is disabled (cannot re-mark uploaded).

**Status:** NOT TESTED

---

## TC-DOCSUB-004: Mark a document as verified

**Preconditions:**
- TC-DOCSUB-003 passed.

**Steps:**
1. Click the **verified** icon on the `Tenth Marksheet` row.

**Expected Result:**
- The status badge updates to **"Verified"**.
- A snackbar reads **"Tenth Marksheet marked as Verified"**.
- The progress banner still reads **"1 of 5 mandatory documents collected"** (verified counts toward the requirement).

**Status:** NOT TESTED

---

## TC-DOCSUB-005: Add remarks to a document

**Preconditions:**
- A document row has been previously saved (TC-DOCSUB-003 passed).

**Steps:**
1. Type **"Original copy received"** into the remarks input on the `Tenth Marksheet` row.
2. Click anywhere outside the input (blur).

**Expected Result:**
- A spinner appears briefly while the change is persisted.
- A snackbar confirms the update.
- Refreshing the page preserves the remark text in the input.

**Status:** NOT TESTED

---

## TC-DOCSUB-006: Mark a document as rejected

**Preconditions:**
- The entry screen is open.

**Steps:**
1. Click the **close** icon on the `Aadhar Card` row.

**Expected Result:**
- The status badge updates to **"Rejected"**.
- The progress banner does **not** count this row toward the 5 mandatory documents.

**Status:** NOT TESTED

---

## TC-DOCSUB-007: Clear a document record

**Preconditions:**
- A document row has been previously saved.

**Steps:**
1. Click the **delete_outline** icon on a row that already has a status.

**Expected Result:**
- The status reverts to **"Not Uploaded"**.
- The remarks input is cleared.
- The progress banner decrements if the row was a mandatory document in `UPLOADED`/`VERIFIED` state.

**Status:** NOT TESTED

---

## TC-DOCSUB-008: Submit button disabled when mandatory documents missing

**Preconditions:**
- The entry screen is open and fewer than 5 mandatory documents are `UPLOADED`/`VERIFIED`.

**Steps:**
1. Hover over the **Submit Documents** button at the bottom of the page.

**Expected Result:**
- The button is disabled.
- The tooltip reads **"All mandatory documents must be uploaded or verified"**.

**Status:** NOT TESTED

---

## TC-DOCSUB-009: Submit documents successfully

**Preconditions:**
- All 5 mandatory documents on an enquiry are marked `UPLOADED` or `VERIFIED` via TC-DOCSUB-003 / TC-DOCSUB-004.

**Steps:**
1. Click the **Submit Documents** button.

**Expected Result:**
- A snackbar reads **"Documents submitted successfully"**.
- The browser navigates back to `/enquiries/document-submission`.
- The enquiry no longer appears in the list (its status is now `DOCUMENTS_SUBMITTED`).
- Visiting the enquiry detail page shows status **"Documents Submitted"** and a new entry in the status history.

**Status:** NOT TESTED

---

## TC-DOCSUB-010: Cannot collect documents for an enquiry in the wrong status

**Preconditions:**
- An enquiry exists with status `INTERESTED` (or any status other than `FEES_PAID` / `PARTIALLY_PAID`).

**Steps:**
1. Manually navigate to `/enquiries/document-submission/<that enquiry id>` in the URL bar.

**Expected Result:**
- A snackbar reads **"Documents can only be collected for enquiries in FEES_PAID or PARTIALLY_PAID status"**.
- The user is redirected back to `/enquiries/document-submission`.

**Status:** NOT TESTED

---

## TC-DOCSUB-011: Read-only access for non-privileged roles

**Preconditions:**
- A user logged in with neither `ROLE_ADMIN` nor `ROLE_FRONT_OFFICE` (e.g. `ROLE_FACULTY`).

**Steps:**
1. Try to navigate to `/enquiries/document-submission` and `/enquiries/document-submission/<id>`.

**Expected Result:**
- The list returns 403 from the backend; the page shows the empty state or the navigation item is hidden.
- On the entry screen the per-row action buttons (mark uploaded / verified / rejected / clear) are not rendered, and remark inputs are disabled.
- The **Submit Documents** button is disabled.

**Status:** NOT TESTED

---

## TC-DOCSUB-012: Entry screen launched from main enquiry list

**Preconditions:**
- An enquiry exists with status `FEES_PAID` or `PARTIALLY_PAID`.
- User is `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.

**Steps:**
1. Open **Admission Management → Enquiries**.
2. Locate the enquiry and click the **upload** icon ("Collect & Submit Documents") in the row actions.

**Expected Result:**
- The browser navigates to `/enquiries/document-submission/<id>` (the same entry screen as TC-DOCSUB-002).

**Status:** NOT TESTED
