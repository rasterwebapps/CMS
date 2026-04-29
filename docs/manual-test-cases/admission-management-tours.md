# Admission Management Tours — Manual Test Cases

## TC-ADM-TOUR-001: Enquiry list tour launches and highlights core areas

**Preconditions:**
- User is logged in with `ROLE_ADMIN`, `ROLE_COLLEGE_ADMIN`, or `ROLE_FRONT_OFFICE`.
- Application is running and the frontend build has completed successfully.

**Steps:**
1. Navigate to `/enquiries`.
2. Click the info/tour icon beside the page title.
3. Step through the tour.
4. Verify the header, Add Enquiry button, toolbar, and enquiry table/card content are highlighted.

**Expected Result:**
- The tour opens and all steps display without missing-element errors.
- The final step completes and closes the tour.

**Status:** NOT TESTED

## TC-ADM-TOUR-002: Enquiry form and enquiry detail tours launch

**Preconditions:**
- User is logged in with admission management access.
- At least one enquiry exists for detail screen testing.

**Steps:**
1. Navigate to `/enquiries/new` and click the tour icon.
2. Verify the form tour highlights candidate basics, programme/course, and referral fields.
3. Navigate to an existing enquiry detail page.
4. Click the tour icon near the candidate name.
5. Verify the tour highlights the hero, quick actions, and detail tabs.

**Expected Result:**
- Both tours launch and progress through all steps successfully.

**Status:** NOT TESTED

## TC-ADM-TOUR-003: Document submission and collection tours launch

**Preconditions:**
- User is logged in with admission management access.
- At least one enquiry is eligible for document submission/collection.

**Steps:**
1. Navigate to `/enquiries/document-submission`.
2. Click the tour icon and verify the header, toolbar, and candidate queue steps.
3. Open a candidate document collection screen.
4. Click the tour icon and verify candidate banner and document checklist steps.

**Expected Result:**
- Both document workflow tours run without errors and highlight the expected UI areas.

**Status:** NOT TESTED

## TC-ADM-TOUR-004: Admission completion and conversion tours launch

**Preconditions:**
- User is logged in with `ROLE_ADMIN`, `ROLE_COLLEGE_ADMIN`, or `ROLE_FRONT_OFFICE`.
- At least one enquiry is in `DOCUMENTS_SUBMITTED` status for completion testing.

**Steps:**
1. Navigate to `/enquiries/admission-completion`.
2. Click the tour icon and verify the stats and table steps are highlighted.
3. Open an enquiry conversion screen from an eligible enquiry.
4. Click the tour icon and verify the enquiry summary and admission form steps.

**Expected Result:**
- Admission completion and conversion tours launch and complete successfully.

**Status:** NOT TESTED

## TC-ADM-TOUR-005: Admission list, form, and detail tours launch

**Preconditions:**
- User is logged in with admission management access.
- At least one admission exists for detail screen testing.

**Steps:**
1. Navigate to `/admissions` and click the tour icon.
2. Verify the New Admission button, toolbar, and records table are highlighted.
3. Navigate to `/admissions/new` and click the tour icon.
4. Verify admission form mode/body steps are shown.
5. Open an admission detail page and click the tour icon.
6. Verify the header/actions and tabs are highlighted.

**Expected Result:**
- All admission tours launch, target visible elements, and finish cleanly.

**Status:** NOT TESTED

## TC-ADM-TOUR-006: Student list, form, detail, and roll number tours launch

**Preconditions:**
- User is logged in with admission management access.
- At least one student exists for detail testing.

**Steps:**
1. Navigate to `/students` and click the tour icon.
2. Verify the Add Student button, toolbar, and student table are highlighted.
3. Navigate to `/students/new` and click the tour icon.
4. Verify basic, personal, family, and submit sections are highlighted.
5. Open a student detail page and click the tour icon.
6. Verify hero and tabbed content are highlighted.
7. Navigate to `/students/roll-numbers` and click the tour icon.
8. Verify filters, roll number table, and Save All button are highlighted.

**Expected Result:**
- Student-related tours under Admission Management launch and complete successfully.

**Status:** NOT TESTED

