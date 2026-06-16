# Enquiry Document File Upload / Download

These test cases cover browsing scanned documents from the local computer,
saving them in the database against an enquiry's document type, and later
viewing or downloading them from the enquiry view screen.

## TC-DOCFILE-001: Browse and upload a scanned document

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
- An enquiry exists in `FEES_PAID` or `PARTIALLY_PAID` status.

**Steps:**
1. Navigate to **Admission Management → Submit Documents**.
2. Click **Collect Documents** for the target enquiry.
3. In the **Document Checklist** table, locate the desired document row (e.g. *Tenth Marksheet*).
4. Click the **Browse** button in the **File** column.
5. In the native file picker, choose a PDF or image file from your local computer and confirm.

**Expected Result:**
- A loading spinner appears briefly on the row.
- A success snackbar is shown: `Tenth Marksheet: <file name> uploaded`.
- The row's **File** column now shows the file name and size.
- The status badge changes to **UPLOADED**.
- A page reload preserves the file metadata (proves it was persisted in the database).

**Status:** NOT TESTED

---

## TC-DOCFILE-002: Replace an already-uploaded file

**Preconditions:**
- A document row already has an uploaded file (see TC-DOCFILE-001).

**Steps:**
1. In the same row, click the **Replace** (refresh) icon next to the file name.
2. Choose a different file from the picker.

**Expected Result:**
- The file name and size in the row update to the new file.
- The previously stored file is no longer downloadable; downloading now returns the new file.

**Status:** NOT TESTED

---

## TC-DOCFILE-003: Reject oversize file

**Preconditions:**
- Same as TC-DOCFILE-001.

**Steps:**
1. Click **Browse** and select a file larger than 10 MB.

**Expected Result:**
- A snackbar warning appears: *"Only PDF, JPG, PNG files are allowed (max 10 MB)"*.
- No upload request is sent and the row's state is unchanged.

**Status:** NOT TESTED

---

## TC-DOCFILE-004: View document inline from collection screen

**Preconditions:**
- A document row has an uploaded file.

**Steps:**
1. In the **File** column, click the **View** (eye) icon.

**Expected Result:**
- A new browser tab opens displaying the document inline (PDF/image).
- If the browser blocks the pop-up, the file is downloaded instead and a snackbar may indicate the fallback.

**Status:** NOT TESTED

---

## TC-DOCFILE-005: Download document from collection screen

**Preconditions:**
- A document row has an uploaded file.

**Steps:**
1. Click the **Download** icon in the **File** column.

**Expected Result:**
- The browser downloads the file using its original file name.

**Status:** NOT TESTED

---

## TC-DOCFILE-006: View / download from the enquiry detail (view) screen

**Preconditions:**
- The same enquiry has at least one document with an uploaded file.

**Steps:**
1. Navigate to **Enquiries** and open the enquiry detail page.
2. Switch to the **Documents** tab.
3. For a document row that has a file, click the **View** icon, then click the **Download** icon.

**Expected Result:**
- The View action opens the file inline in a new tab.
- The Download action saves the file with its original name.
- For documents that have no file, neither action button is rendered.

**Status:** NOT TESTED

---

## TC-DOCFILE-007: Backend persists binary across restarts (PostgreSQL profile)

**Preconditions:**
- Backend running with the `prod` profile against PostgreSQL (or any
  non-`local` profile where Flyway is enabled).
- Migration `V57__add_file_data_to_enquiry_documents.sql` has been applied.

**Steps:**
1. Upload a document file via the UI.
2. Restart the backend.
3. Reopen the enquiry detail screen and click **Download** for that document.

**Expected Result:**
- The downloaded file matches the original byte-for-byte, proving the
  binary is stored in the `enquiry_documents.file_data` BLOB column.

**Status:** NOT TESTED

---

## TC-DOCFILE-008: Document Verification rejects unsupported replacement format

**Preconditions:**
- User is logged in with document verification permissions.
- Enquiry is in `DOCUMENTS_SUBMITTED` status.
- At least one document is `REJECTED` so **Replace File** is visible.

**Steps:**
1. Open **Admission Management → Verify Documents** and open the enquiry.
2. Click **Replace File** on a rejected document.
3. Choose an unsupported file format (e.g., `.webp`, `.webm`, `.docx`).

**Expected Result:**
- Upload is blocked.
- A warning toast is shown: *"Only PDF, JPG, PNG files are allowed (max 10 MB)"*.
- The row remains unchanged and no new file is attached.

**Status:** NOT TESTED

---

## TC-DOCFILE-009: Enquiry Convert consent upload rejects unsupported format

**Preconditions:**
- User can open **Create Admission** for an enquiry.
- Optional consent document section is visible.

**Steps:**
1. In **Consent Documents**, choose a non-supported file for either parent/applicant consent input.
2. Try selecting an unsupported format (e.g., `.webp`, `.webm`, `.docx`).

**Expected Result:**
- Selection is rejected and file name is not retained.
- A warning toast is shown: *"Only PDF, JPG, PNG files are allowed (max 10 MB)"*.
- Submission can proceed without the invalid file (uploads are optional).

**Status:** NOT TESTED

