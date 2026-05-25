# Manual Test Cases — Document Types Refactor & Program-level Mapping

Module: Document Type Catalogue, Program → Required Documents Mapping
Related: V91 migration, `DocumentType` enum refactor, new `/document-types` and `/programs/{id}/document-types` endpoints.

---

## TC-DOCTYPES-001: Display labels are humanised in Submit Documents screen

**Preconditions:**
- User is logged in as `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
- An enquiry exists in `FEES_PAID` or `PARTIALLY_PAID` status.

**Steps:**
1. Navigate to **Front Office → Document Submission**.
2. Open the document collection screen for the enquiry.
3. Inspect the document checklist labels.

**Expected Result:**
- Labels show humanised text such as "10th Marksheet", "11th Marksheet", "12th Marksheet" (NOT `TENTH_MARKSHEET`, `ELEVENTH_MARKSHEET`, `TWELFTH_MARKSHEET`).
- "Provisional Certificate" appears (NOT "Undertaking Document").
- New types appear: "Genuineness Certificate", "Semester 1–8 Marksheet", "College TC / School TC", "Transcript", "Degree Certificate", "Scanned Signature".

**Status:** NOT TESTED

---

## TC-DOCTYPES-002: GET /document-types returns full catalogue with labels & categories

**Preconditions:**
- Backend is running.
- Any authenticated user.

**Steps:**
1. `GET /api/v1/document-types` with a valid bearer token.
2. Inspect the JSON response.

**Expected Result:**
- HTTP 200.
- Array of objects each with `code`, `label`, and `category` fields.
- 26 entries total. `code: "PROVISIONAL_CERTIFICATE"` is present; `UNDERTAKING_DOCUMENT` is NOT.
- `category` values are one of: `Academic`, `Administrative`, `Identity`, `Statutory`, `Other`.

**Status:** NOT TESTED

---

## TC-DOCTYPES-003: Admin maps required documents to a Program

**Preconditions:**
- User logged in as `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.
- At least one program exists.

**Steps:**
1. Navigate to **Programs**.
2. Click the edit pencil for an existing program.
3. Scroll to the new **Required Documents** card.
4. Type "marksheet" into the search box — list filters to marksheet entries.
5. Tick checkboxes for: `10th Marksheet`, `12th Marksheet`, `Aadhar Card`, `Passport Photo`.
6. Click **Save Required Documents**.

**Expected Result:**
- A success toast "Required documents updated" appears.
- Reloading the screen shows the same four checkboxes still selected.
- `GET /api/v1/programs/{id}/document-types` returns those four codes.

**Status:** NOT TESTED

---

## TC-DOCTYPES-004: Submit Documents screen uses program-mapped mandatory list

**Preconditions:**
- A program has explicit required documents configured (TC-DOCTYPES-003).
- An enquiry tied to that program exists in `FEES_PAID` status.

**Steps:**
1. Open the Submit Documents screen for that enquiry.
2. Inspect the **Required Documents** progress KPI.
3. Inspect which rows are flagged as mandatory.

**Expected Result:**
- The mandatory KPI total equals the count configured for the program (e.g., 4).
- Only the configured types appear in the mandatory section (10th Marksheet, 12th Marksheet, Aadhar Card, Passport Photo).
- All other catalogue entries appear in the optional section.

**Status:** NOT TESTED

---

## TC-DOCTYPES-005: Submit Documents falls back to defaults when program has no mapping

**Preconditions:**
- A program with NO required documents configured (`requiredDocumentTypes` empty).
- An enquiry tied to that program exists in `FEES_PAID` status.

**Steps:**
1. Open the Submit Documents screen for that enquiry.
2. Inspect the mandatory KPI total.

**Expected Result:**
- Mandatory KPI total = 5 (the legacy default set: 10th, 12th, TC, Aadhar, Passport Photo).
- Behaviour matches the previous release for backward compatibility.

**Status:** NOT TESTED

---

## TC-DOCTYPES-006: Backend submit endpoint validates against program-mapped mandatory list

**Preconditions:**
- Same as TC-DOCTYPES-004.
- Only some mandatory documents have been uploaded.

**Steps:**
1. Try to submit (`POST /api/v1/enquiries/{id}/documents/submit`) without all program-required types uploaded.

**Expected Result:**
- Response includes `missingDocumentTypes` listing the program-required codes that are missing — not the legacy default set.

**Status:** NOT TESTED

---

## TC-DOCTYPES-007: Admission document checklist is program-aware

**Preconditions:**
- Student's program has a configured required document set (e.g., 5 types).
- An admission exists for that student.

**Steps:**
1. Call `GET /api/v1/admissions/{id}/documents/checklist` (or open the admission's documents view).

**Expected Result:**
- Checklist contains exactly the 5 program-required types (verification status `NOT_UPLOADED` for those not yet supplied).
- Types outside the program mapping are NOT in the checklist.
- If the program has no mapping, the checklist contains all 26 catalogue types (legacy fallback).

**Status:** NOT TESTED

---

## TC-DOCTYPES-008: Existing UNDERTAKING_DOCUMENT rows are migrated to PROVISIONAL_CERTIFICATE

**Preconditions:**
- Database had at least one `enquiry_documents` and/or `admission_documents` row with `document_type = 'UNDERTAKING_DOCUMENT'` before the V91 migration ran.

**Steps:**
1. Apply V91 migration (PostgreSQL profile).
2. Query: `SELECT DISTINCT document_type FROM enquiry_documents;`
3. Query: `SELECT DISTINCT document_type FROM admission_documents;`

**Expected Result:**
- No rows have `document_type = 'UNDERTAKING_DOCUMENT'`; they now read `PROVISIONAL_CERTIFICATE`.
- A new `program_document_types` table exists with columns `program_id`, `document_type` and a composite primary key.

**Status:** NOT TESTED

---

## TC-DOCTYPES-009: Authorization — only PROGRAM_MANAGE can set required documents

**Preconditions:**
- A user with `ROLE_FRONT_OFFICE` (no `PROGRAM_MANAGE` permission) is logged in.

**Steps:**
1. `PUT /api/v1/programs/1/document-types` with body `["AADHAR_CARD"]`.

**Expected Result:**
- HTTP 403 Forbidden.
- The same call by an `ROLE_ADMIN` returns HTTP 200 with the saved set.

**Status:** NOT TESTED

---

## TC-DOCTYPES-010: Programs API response includes requiredDocumentTypes

**Preconditions:**
- A program has 3 document types configured.

**Steps:**
1. `GET /api/v1/programs/{id}`.

**Expected Result:**
- Response JSON includes a `requiredDocumentTypes` array with the 3 configured codes.
- `GET /api/v1/programs` (list endpoint) also returns the field on each program.

**Status:** NOT TESTED

---
## TC-DOCTYPES-011: Newly added program document appears as missing for existing admissions
**Preconditions:**
- An admission already exists for a student in a program.
- The program's required document list does not currently include `COMMUNITY_CERTIFICATE`.
- User is logged in with `DOCUMENT_SUBMISSION_MANAGE` permission.
**Steps:**
1. Open the program edit page and add `Community Certificate` to the required documents.
2. Save the program required documents.
3. Open the existing admission detail page and select the **Documents** tab.
4. Upload a file for `Community Certificate` from the missing required documents section.
**Expected Result:**
- The existing admission remains admitted and is not reverted or cancelled.
- `Community Certificate` appears under **Missing Required Documents** with status `NOT_UPLOADED`.
- The Upload action is available for the missing document.
- After upload, the document moves to **Uploaded / Pending Verification** with status `UPLOADED`.
**Status:** NOT TESTED
---
## TC-DOCTYPES-012: Removed program document remains preserved for existing admissions
**Preconditions:**
- An admission already exists for a student in a program.
- `10th Marksheet` is currently required for the program and has already been uploaded or verified for the admission.
- User is logged in with `PROGRAM_MANAGE` and document-view permission.
**Steps:**
1. Open the program edit page and remove `10th Marksheet` from the required documents.
2. Save the program required documents.
3. Open the existing admission detail page and select the **Documents** tab.
**Expected Result:**
- The already collected `10th Marksheet` is not deleted.
- Its uploaded file, status, and verification history remain available.
- It is not shown as a missing required document.
- It appears under **Collected Documents Not Currently Required** with a "No longer required" indicator.
- The admission remains valid and admitted.
**Status:** NOT TESTED
