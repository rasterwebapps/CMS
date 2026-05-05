# Manual Test Cases — Community & Blood Group Masters + Expanded Family Information

Module: Community Master, Blood Group Master, Family Information enhancements
Related: V92 migration, new `/communities` and `/blood-groups` endpoints, Complete Admission screen, Student form, Admission form.

---

## TC-COMM-001: Create a new community

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.
- Application is running.

**Steps:**
1. Navigate to **Preferences → Communities**.
2. Click **Add Community**.
3. Fill in: Name = `EWS Category`, Code = `ewscat` (auto-uppercase to `EWSCAT`), Description = `Economically weaker section`.
4. Toggle **Active** on (default).
5. Click **Create Community**.

**Expected Result:**
- Toast "Community created" appears.
- Navigation returns to the Community list.
- The new "EWS Category (EWSCAT)" appears in the list with Active status.
- `GET /api/v1/communities` returns the new entry.

**Status:** NOT TESTED

---

## TC-COMM-002: Reject duplicate community code

**Preconditions:**
- A community with code `BC` already exists (seeded by default).

**Steps:**
1. Navigate to **Communities → Add Community**.
2. Enter Name = `Backward Class Duplicate`, Code = `BC`.
3. Click **Create Community**.

**Expected Result:**
- Backend returns HTTP 400 with message containing "already exists".
- Toast displays the error message.
- The form remains, no record is created.

**Status:** NOT TESTED

---

## TC-COMM-003: Edit and update a community

**Preconditions:**
- At least one community exists.

**Steps:**
1. Navigate to **Communities**.
2. Click the edit (pencil) icon on any community row.
3. Change the description.
4. Click **Update Community**.

**Expected Result:**
- Toast "Community updated".
- List shows the updated values.
- Code is preserved.

**Status:** NOT TESTED

---

## TC-COMM-004: Delete a community

**Preconditions:**
- A community that is NOT in use exists.

**Steps:**
1. Navigate to **Communities**.
2. Click the delete (trash) icon on the row.
3. Confirm in the dialog.

**Expected Result:**
- Toast "Community deleted successfully".
- The row disappears from the list.
- `GET /api/v1/communities` no longer includes the deleted record.

**Status:** NOT TESTED

---

## TC-BG-001: Create a new blood group

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.

**Steps:**
1. Navigate to **Preferences → Blood Groups**.
2. Click **Add Blood Group**.
3. Fill in: Name = `Bombay Blood`, Code = `hh` (auto-uppercase to `HH`).
4. Click **Create Blood Group**.

**Expected Result:**
- Toast "Blood group created".
- The new entry appears in the list.

**Status:** NOT TESTED

---

## TC-BG-002: Reject duplicate blood group code

**Preconditions:**
- The blood group `O+` already exists (seeded).

**Steps:**
1. Navigate to **Blood Groups → Add Blood Group**.
2. Enter Name = `O Pos Duplicate`, Code = `O+`.
3. Click **Create**.

**Expected Result:**
- Backend returns HTTP 400 with "already exists" message.
- Toast displays the error; no record is created.

**Status:** NOT TESTED

---

## TC-ADM-COMM-001: Complete Admission shows API-loaded communities

**Preconditions:**
- At least 3 active communities exist in the master.
- An enquiry in `DOCUMENTS_SUBMITTED` status exists.

**Steps:**
1. Navigate to **Admission Management → Complete Admission**.
2. Click "Complete Admission" on an enquiry row.
3. Scroll to the **Demographics** section.
4. Click the **Community** dropdown.

**Expected Result:**
- The dropdown shows all active communities from the master (e.g., `Backward Class (BC)`, `Other Caste (OC)`, etc.) — NOT a hardcoded list.
- Inactive communities do NOT appear.
- A user-created community is selectable.

**Status:** NOT TESTED

---

## TC-ADM-BG-001: Complete Admission shows API-loaded blood groups

**Preconditions:**
- At least 3 active blood groups exist in the master.
- An enquiry in `DOCUMENTS_SUBMITTED` status exists.

**Steps:**
1. Open the Complete Admission screen.
2. Scroll to **Demographics**.
3. Click the **Blood Group** dropdown.

**Expected Result:**
- The dropdown shows blood groups from the master in `<code> — <name>` format (e.g., `O+ — O Positive`).
- Custom user-created blood groups appear.

**Status:** NOT TESTED

---

## TC-FAM-001: Capture per-parent contact details on Complete Admission

**Preconditions:**
- An enquiry in `DOCUMENTS_SUBMITTED` status exists.

**Steps:**
1. Open Complete Admission for that enquiry.
2. Scroll to **Family Information**.
3. Verify that the section now contains:
   - Father's Name, Father's Phone, Father's Email
   - Mother's Name, Mother's Phone, Mother's Email
   - Primary Parent Mobile
4. Fill in all six per-parent fields plus the primary mobile.
5. Submit the form to convert the enquiry.

**Expected Result:**
- The form submits successfully.
- The created `Student` row in the database contains all 7 family contact values.
- Subsequent edits via Student Form preserve and display the same values.
- Student detail screen displays Father Phone/Email and Mother Phone/Email rows.

**Status:** NOT TESTED

---

## TC-FAM-002: Email validation on parent emails

**Preconditions:**
- Same as TC-FAM-001.

**Steps:**
1. Open Complete Admission.
2. In Family Information, enter `not-an-email` in Father's Email.
3. Click outside the field (blur).

**Expected Result:**
- Inline error "Enter a valid email" appears below the field.
- Submit button is blocked / form stays invalid until the email is corrected or cleared.

**Status:** NOT TESTED

---

## TC-FAM-003: Student Edit form shows expanded family fields

**Preconditions:**
- A student with all 7 family contact fields populated exists.

**Steps:**
1. Navigate to **Students**.
2. Open Edit on the student.
3. Scroll to **Family Details**.

**Expected Result:**
- All six per-parent fields and the primary parent mobile are pre-filled with the saved values.
- The Community and Blood Group dropdowns are pre-selected based on the student's stored `code`.

**Status:** NOT TESTED

---

## TC-FAM-004: V92 migration converts blood-group enum names to human-readable codes

**Preconditions:**
- Database had at least one student row with `blood_group = 'A_POSITIVE'` (legacy enum format) before V92 ran.

**Steps:**
1. Apply V92 migration (PostgreSQL profile).
2. Query: `SELECT DISTINCT blood_group FROM students WHERE blood_group IS NOT NULL;`

**Expected Result:**
- No rows have `A_POSITIVE`, `B_NEGATIVE`, etc. They have been converted to `A+`, `B-`, etc.
- New tables `communities` and `blood_groups` exist and are populated with default seed values.
- Columns `father_phone`, `father_email`, `mother_phone`, `mother_email` exist on the `students` table.

**Status:** NOT TESTED

---

## TC-AUTH-001: Non-admin users cannot manage masters

**Preconditions:**
- A user logged in with `ROLE_FRONT_OFFICE` (no `ROLE_ADMIN` / `ROLE_COLLEGE_ADMIN`).

**Steps:**
1. Send `POST /api/v1/communities` with a valid body.
2. Send `PUT /api/v1/communities/1` with a valid body.
3. Send `DELETE /api/v1/communities/1`.

**Expected Result:**
- All three calls return HTTP 403 Forbidden.
- The same calls performed as `ROLE_ADMIN` succeed (200/201/204).
- `GET /api/v1/communities` and `GET /api/v1/blood-groups` are accessible to all authenticated users (so dropdowns work in admission/enquiry forms).

**Status:** NOT TESTED

---

## TC-MASTER-NAV-001: Users with VIEW-only permission can access master lists (without manage actions)

**Preconditions:**
- A test user exists with `COMMUNITY_VIEW` and `BLOOD_GROUP_VIEW` permissions.
- The same user does NOT have `COMMUNITY_MANAGE` / `BLOOD_GROUP_MANAGE`.
- Application is running.

**Steps:**
1. Log in as the VIEW-only user.
2. In the sidebar, open **Preferences**.
3. Click **Communities**.
4. Verify the list loads.
5. Verify the **Add Community** button is not visible.
6. Verify edit/delete actions are not visible (card view and table view).
7. Navigate directly to `/communities/new`.
8. Repeat steps 3–7 for **Blood Groups**.

**Expected Result:**
- Communities and Blood Groups appear in the **Preferences** navigation for users with VIEW permission.
- List pages are accessible.
- Create/edit/delete UI controls are hidden for VIEW-only users.
- Direct navigation to the create/edit routes redirects to Dashboard / is blocked by the permission guard.

**Status:** NOT TESTED

