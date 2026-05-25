# Admission Printable Form Manual Test Cases

These tests cover the official admission form output used by **View Form**, **Print Form**, and **Download Form** from the admission detail screen.

## TC-ADMPRINT-001: Admission print preview fits A4 and excludes academic qualifications

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
- At least one admission exists with student, parent/guardian, declaration, document checklist, and academic qualification records.

**Steps:**
1. Open **Admission Management → Admissions**.
2. Open an admission detail page.
3. Click **Print Form**.
4. In the browser print dialog, select paper size **A4** and orientation **Portrait**.
5. Review the print preview.

**Expected Result:**
- The official admission form preview is optimized for a single A4 portrait page for normal admission data.
- The text is readable in print preview.
- The **Academic Qualifications** section is not shown anywhere in the printed form.
- Admission, personal, contact, parent/guardian, document checklist, declaration, signature, seal, and footer sections remain visible.
- The visual style remains consistent with the SKS College of Nursing admission form.

**Status:** NOT TESTED

---

## TC-ADMPRINT-002: View, print, and download use the same admission form layout

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
- At least one admission exists.

**Steps:**
1. Open an admission detail page.
2. Click **View Form** and review the opened form.
3. Return to the admission detail page.
4. Click **Print Form** and review the print preview.
5. Return to the admission detail page.
6. Click **Download Form** and review the opened save/print output.

**Expected Result:**
- All three actions use the same admission form template and layout.
- Academic qualifications are absent from all three outputs.
- The document checklist appears in the same two-column layout in all outputs.
- No separate or outdated admission form layout is used.

**Status:** NOT TESTED

---

## TC-ADMPRINT-003: Submitted passport photo appears in the admission form

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
- An admission has a submitted admission document with document type `PASSPORT_PHOTO` and an uploaded image file.
- The passport photo document is available from the admission document list.

**Steps:**
1. Open the admission detail page for the admission.
2. Wait for the page and documents to finish loading.
3. Click **View Form**.
4. Review the photo box in the Personal Details section.
5. Repeat with **Print Form** and **Download Form**.

**Expected Result:**
- The uploaded `PASSPORT_PHOTO` document image appears in the photo box of the admission form.
- The image is cropped proportionally within the passport-photo frame and does not distort the layout.
- If no `PASSPORT_PHOTO` document file exists, the form shows the existing "Paste Photo Here" placeholder.

**Status:** NOT TESTED
No
---

## TC-ADMPRINT-004: Physical Disability appears in admission form output

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
- At least one admission exists for a student with `physicalDisability = true`.

**Steps:**
1. Open the admission detail page for the admission.
2. Confirm **Student Snapshot** shows **Physical Disability = Yes**.
3. Click **View Form** and review the Personal Details section.
4. Repeat with **Print Form** and **Download Form**.

**Expected Result:**
- View, Print, and Download outputs all show **Physical Disability = Yes** in Personal Details.
- The additional field does not break the A4 portrait layout or hide the photo/document/declaration sections.
- For a student with `physicalDisability = false`, the form shows **Physical Disability = No**.

**Status:** NOT TESTED

