# Profile Enhancements — Manual Test Cases

These cases verify the Profile enhancements added in May 2026: user profile photo upload, inline self-edit for safe personal fields, Account & Security card, Contact & Emergency card, and role-specific Quick Links.

---

## TC-PRO-001: Upload profile photo as Faculty

**Preconditions:**
- Logged in as a Faculty user.

**Steps:**
1. Navigate to `/profile`.
2. Hover over the avatar circle.
3. Click the avatar.
4. Select a JPEG or PNG image under 2 MB.
5. Wait for the toast confirmation.
6. Refresh the page.

**Expected Result:**
- Camera overlay appears on avatar hover.
- Toast says the profile photo was updated.
- Avatar displays the uploaded photo instead of initials.
- Photo persists after refresh.

**Status:** NOT TESTED

---

## TC-PRO-002: Upload profile photo as Admin

**Preconditions:**
- Logged in as `devadmin` or another admin user.

**Steps:**
1. Navigate to `/profile`.
2. Click the avatar.
3. Upload a valid JPEG/PNG image under 2 MB.

**Expected Result:**
- Admin profile accepts the photo upload.
- Avatar displays the uploaded photo.
- No Faculty/Student record is required for photo upload because the photo is stored on `app_users`.

**Status:** NOT TESTED

---

## TC-PRO-003: Reject invalid profile photo file type

**Preconditions:**
- Logged in as any user.

**Steps:**
1. Navigate to `/profile`.
2. Click the avatar.
3. Select a PDF, SVG, or any non-JPEG/PNG file.

**Expected Result:**
- The upload is blocked.
- User sees an error toast: only JPEG or PNG images are allowed.
- Avatar remains unchanged.

**Status:** NOT TESTED

---

## TC-PRO-004: Reject oversized profile photo

**Preconditions:**
- Logged in as any user.

**Steps:**
1. Navigate to `/profile`.
2. Click the avatar.
3. Select a JPEG/PNG larger than 2 MB.

**Expected Result:**
- The upload is blocked.
- User sees an error toast that the photo must be under 2 MB.
- Avatar remains unchanged.

**Status:** NOT TESTED

---

## TC-PRO-005: Remove profile photo

**Preconditions:**
- Logged in as any user with an uploaded profile photo.

**Steps:**
1. Navigate to `/profile`.
2. Click **Remove Photo** in the hero actions.
3. Refresh the page.

**Expected Result:**
- Toast confirms the photo was removed.
- Avatar returns to initials.
- Photo remains absent after refresh.

**Status:** NOT TESTED

---

## TC-PRO-006: Faculty inline self-edit updates whitelisted fields only

**Preconditions:**
- Logged in as a Faculty user.

**Steps:**
1. Navigate to `/profile`.
2. In **Personal Info**, click the pencil icon.
3. Update Phone, Blood Group, City, District, State, and Pincode.
4. Click **Save Changes**.
5. Refresh the page.

**Expected Result:**
- Toast says the profile was updated.
- Updated fields are visible after save and after refresh.
- Employee Code, Speciality, Designation, Joining Date, Status, and Email are not editable in the self-edit form.

**Status:** NOT TESTED

---

## TC-PRO-007: Student inline self-edit updates whitelisted fields only

**Preconditions:**
- Logged in as a Student user.

**Steps:**
1. Navigate to `/profile`.
2. In **Personal Info**, click the pencil icon.
3. Update Phone, Blood Group, and Address fields.
4. Click **Save Changes**.
5. Refresh the page.

**Expected Result:**
- Updated values persist.
- Roll Number, Program, Year of Study, Admission Date, and Email are not editable in the self-edit form.

**Status:** NOT TESTED

---

## TC-PRO-008: Cancel self-edit does not save changes

**Preconditions:**
- Logged in as Faculty or Student.

**Steps:**
1. Navigate to `/profile`.
2. Click the pencil icon in **Personal Info**.
3. Change the Phone value.
4. Click **Cancel**.

**Expected Result:**
- Edit mode closes.
- Original value is still shown.
- No success toast is shown.

**Status:** NOT TESTED

---

## TC-PRO-009: Quick Links point to valid routes

**Preconditions:**
- Logged in as Faculty, Student, and Admin in separate sessions.

**Steps:**
1. Navigate to `/profile` for each role.
2. Click each link in the **Quick Links** card.

**Expected Result:**
- Faculty links navigate to Academic Calendar, Attendance, Lab Schedule, and Profile/Documents.
- Student links navigate to Academic Calendar, Student Fees, Attendance, and Exam Results.
- Admin links navigate to Programs, Faculty, Students, Reports, and User Management.
- No link causes a 404.

**Status:** NOT TESTED

---

## TC-PRO-010: Self-edit is scoped to the authenticated user

**Preconditions:**
- Backend is running.
- Logged in as any Faculty or Student user.

**Steps:**
1. Call `PUT /api/v1/profile/me/self-info` with valid self-edit fields.
2. Confirm there is no path or body field for another user ID.
3. Try changing admin-only fields such as `designation`, `specialityId`, `rollNumber`, or `email` in the request payload.

**Expected Result:**
- Backend resolves the user from the JWT only.
- Request cannot target another user ID.
- Unsupported/admin-only fields are ignored because the endpoint accepts only the self-edit whitelist.

**Status:** NOT TESTED

