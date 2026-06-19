## TC-MFS-P1-001: Designation form uses entry-form standard layout

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or a role that can access designation master screens
- Frontend is running

**Steps:**
1. Navigate to `/designations/new`.
2. Verify the page uses the standard header with back button, title, and subtitle.
3. Verify the form is rendered inside `entry-form` style card sections.
4. Enter valid values in Name and Code, then click Save.
5. Verify navigation returns to `/designations` and success toast is shown.
6. Open an existing designation in edit mode and verify Update works.

**Expected Result:**
- Designation add/edit screens follow the default `entry-form-*` pattern.
- Validation and save/update behavior remain unchanged.

**Status:** NOT TESTED

## TC-MFS-P2-001: Speciality form follows entry-form layout with shared side cards

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or a role that can manage specialities
- Frontend is running

**Steps:**
1. Navigate to `/specialities/new`.
2. Verify the page header uses the standard `entry-form` back button and title layout.
3. Verify form actions render in `entry-form-card__footer` and submit button text is Create Speciality.
4. Verify right panel renders a live preview card and guidance tips card.
5. Type Name/Code/Description and select Head of Speciality; verify preview updates immediately.
6. Verify async uniqueness checks still show `Checking…` and duplicate errors for Name/Code.
7. Save with valid values and verify success toast and redirect to `/specialities`.

**Expected Result:**
- Speciality add/edit uses the shared `entry-form-*` structure.
- Existing validation, uniqueness checks, submit flow, and tour anchors remain intact.

**Status:** NOT TESTED

## TC-MFS-P2-002: Legacy department template matches speciality entry-form shell

**Preconditions:**
- Frontend source is available for template inspection

**Steps:**
1. Open `frontend/src/app/features/department/department-form/department-form.component.html`.
2. Verify top-level wrapper is `entry-form-page`.
3. Verify header and layout use `entry-form-header` and `entry-form-layout`.
4. Verify action row uses `entry-form-card__footer`.
5. Verify labels/routes still represent speciality behavior.

**Expected Result:**
- Legacy department template is aligned to the speciality/entry-form shell for consistency.

**Status:** NOT TESTED

## TC-MFS-P3-001: Library book form uses entry-form shell and sticky footer

**Preconditions:**
- User is logged in with a role that can access library book management
- Frontend is running

**Steps:**
1. Navigate to `/library/books/new`.
2. Verify page uses `entry-form-page` and `entry-form-header` layout with standard back button.
3. Verify form section order remains unchanged (Identification, Book Details, Classification, Acquisition, Remarks).
4. Verify action row is sticky and uses `entry-form-sticky-footer`.
5. Validate required field and accession uniqueness behavior still works.
6. Save valid data and verify success toast and navigation to `/library/books`.

**Expected Result:**
- Layout is standardized to entry-form shell.
- Existing validation and save behavior remain unchanged.

**Status:** NOT TESTED

## TC-MFS-P3-002: Library periodical form uses entry-form shell and sticky footer

**Preconditions:**
- User is logged in with a role that can access periodical management
- Frontend is running

**Steps:**
1. Navigate to `/library/periodicals/new`.
2. Verify header uses standard `entry-form` shell.
3. Verify section order remains unchanged (Journal Identity, Volume & Issue, Receipt).
4. Verify footer uses `entry-form-sticky-footer`.
5. Save valid values and verify success toast and redirect to `/library/periodicals`.

**Expected Result:**
- Periodical add/edit layout is standardized.
- Existing form behavior is preserved.

**Status:** NOT TESTED

## TC-MFS-P3-003: Library issue form uses entry-form shell and sticky footer

**Preconditions:**
- User is logged in with a role that can access issue register
- Frontend is running
- At least one available book exists

**Steps:**
1. Navigate to `/library/issues/new`.
2. Verify header uses standard `entry-form` shell.
3. Look up an accession number and verify book preview still appears.
4. Verify borrower controls and issue-date flow remain unchanged.
5. Verify footer uses `entry-form-sticky-footer` and Issue button enable/disable logic still works.
6. Issue a valid book and verify success toast and redirect to `/library/issues`.

**Expected Result:**
- Issue form follows entry-form shell while preserving lookup and issue workflow behavior.

**Status:** NOT TESTED

## TC-MFS-P4-001: Fee structure form uses entry-form header and sticky action footer

**Preconditions:**
- User is logged in with a role that can manage fee structures
- Frontend is running

**Steps:**
1. Navigate to `/fee-structures/new`.
2. Verify page uses `entry-form-header` with standard back button to `/fee-structures`.
3. Verify criteria panel, fee grids, replication panel, and finalized warning sections still render and behave as before.
4. Verify action area uses `entry-form-sticky-footer` with Cancel and Save/Update buttons.
5. Save a valid form and verify success toast and redirect.

**Expected Result:**
- Fee structure form shell is aligned to entry-form standard.
- Existing fee computation and save flows are unchanged.

**Status:** NOT TESTED

## TC-MFS-P4-002: Academic year form uses shared sticky footer class

**Preconditions:**
- User is logged in with a role that can manage academic years
- Frontend is running

**Steps:**
1. Navigate to `/academic-years/new` and `/academic-years/{id}/edit`.
2. Verify existing section order and term/seat/billing behaviors remain unchanged.
3. Verify footer action area uses `entry-form-sticky-footer` in create/edit and still shows Back/Edit buttons in view mode.
4. Save a valid create/edit and verify success toast and redirect to `/academic-years`.

**Expected Result:**
- Academic year form uses shared sticky footer styling.
- Existing create/edit/view behavior is preserved.

**Status:** NOT TESTED

## TC-MFS-P1-002: CO-PO mapping form uses entry-form standard card and footer

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or a role that can access curriculum mapping screens
- At least one experiment exists
- Frontend is running

**Steps:**
1. Navigate to `/curriculum-mappings/new`.
2. Verify the page uses `entry-form` header and back button.
3. Verify the form card has section header, body, and `entry-form-card__footer` actions.
4. Submit with empty required fields and verify required field errors appear.
5. Fill required fields and click Create Mapping.
6. Verify navigation returns to `/curriculum-mappings` and success toast is shown.
7. Open an existing mapping in edit mode and verify Update works.

**Expected Result:**
- CO-PO mapping add/edit screen follows the standard `entry-form-*` layout.
- Form validation and submit behavior are unchanged.

**Status:** NOT TESTED

