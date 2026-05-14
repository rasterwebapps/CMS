# Profile / Dashboard Separation — Manual Test Cases

These cases verify the refactor that moved all operational/aggregate widgets
out of the Profile screen and into the role-specific Dashboards, leaving Profile
as a focused identity + personal-document-vault page.

## TC-PDS-001: Profile (Faculty) shows only identity + documents

**Preconditions:**
- Logged in as a Faculty user with at least one required document slot.

**Steps:**
1. Navigate to `/profile`.
2. Inspect the page top-to-bottom.

**Expected Result:**
- Theme dropdown (top right) is visible and works.
- Hero strip shows avatar (initials), name, designation chip, department,
  qualification chip, employee code, email, phone, joining date, and an
  **Edit Profile** button.
- Bento grid shows exactly two cards: **About** and **Personal Info**.
- **My Documents** section appears below with the document vault.
- The page does **NOT** show: avatar progress %, document stats row,
  completion ring, recent activity timeline, colleagues card, system health,
  admin quick-access grid, floating dock.

**Status:** NOT TESTED

---

## TC-PDS-002: Profile (Student) shows only identity + documents

**Preconditions:**
- Logged in as a Student user with an admission record and document checklist.

**Steps:**
1. Navigate to `/profile`.

**Expected Result:**
- Hero shows program, year of study, roll number chip, email, phone, admission date.
- Bento shows **About** + **Personal Info** only.
- **My Documents** section is present.
- Same operational widgets are absent as in TC-PDS-001.

**Status:** NOT TESTED

---

## TC-PDS-003: Profile (Admin) shows only identity

**Preconditions:**
- Logged in as a Developer Administrator (`devadmin`).

**Steps:**
1. Navigate to `/profile`.

**Expected Result:**
- Hero shows Administrator chip + email + Full System Access pill.
- Bento shows **About** + **Account Details** cards.
- The page does **NOT** show: stats row (Students/Faculty/Departments/...),
  System Health card, Recent Activity, Quick Access grid, floating dock.

**Status:** NOT TESTED

---

## TC-PDS-004: Faculty Dashboard receives the moved widgets

**Preconditions:**
- Logged in as a Faculty user.

**Steps:**
1. Navigate to `/dashboard`.

**Expected Result:**
- Faculty hero strip with greeting + course/student counts.
- **Document Stats Row** (5 tiles: Total / Verified / Pending / To Upload / Complete %).
- Two-column row: **Document Completion** ring card + **Colleagues** card.
- Today's Classes card (existing).
- Side column: Pending Attendance, Lab Schedule (existing) + **Recent Activity** card.
- Numbers in stat tiles use tabular figures.

**Status:** NOT TESTED

---

## TC-PDS-005: Student Dashboard exists and shows document widgets

**Preconditions:**
- Logged in as a Student user.

**Steps:**
1. Navigate to `/dashboard`.

**Expected Result:**
- A new role-specific Student Dashboard renders (no longer falls through to
  the "Account Not Configured" page).
- Hero with welcome + verified/total document count.
- **Document Stats Row** with the same 5 tiles.
- Two-column row: **Document Completion** ring + **Recent Activity**.
- Quick Links card with shortcuts to **My Documents** and **My Profile**.

**Status:** NOT TESTED

---

## TC-PDS-006: Admin Dashboard receives the moved widgets

**Preconditions:**
- Logged in as `devadmin` / `supportadmin` / `collegeadmin`.

**Steps:**
1. Navigate to `/dashboard`.

**Expected Result:**
- All existing admin widgets render (hero, stats grid, quick actions,
  Trend + Approvals row, Equipment + Fee row).
- Below those, a new **System Health + Recent Activity** row.
- Below that, a new **Team (Colleagues) + Quick Access** row showing 10 admin
  shortcut tiles (Programs, Departments, Faculty, Students, Enquiries, Admissions,
  Documents, Fees, Users, Reports).

**Status:** NOT TESTED

---

## TC-PDS-007: Theme picker still works on Profile

**Preconditions:**
- Logged in as any user.

**Steps:**
1. Navigate to `/profile`.
2. Click the theme dropdown (top right) and pick a different colour.
3. Observe Profile, Dashboard, and any other page.

**Expected Result:**
- The chosen colour drives the `--primary-theme` CSS variable across the entire
  app (buttons, ring fills, accent borders, dashboard widgets).

**Status:** NOT TESTED

---

## TC-PDS-008: Document upload on Profile updates Dashboard widgets

**Preconditions:**
- Logged in as Faculty or Student.
- At least one required document slot is empty.

**Steps:**
1. Navigate to `/profile`.
2. In **My Documents**, upload a file for one slot.
3. Wait for the upload to finish (toast confirmation).
4. Navigate to `/dashboard`.

**Expected Result:**
- The Document Stats Row on the dashboard reflects the new state — `To Upload`
  decreases by 1 and `Pending` (uploaded, awaiting verification) increases by 1.
- The Document Completion ring percentage updates accordingly.
- This works because Profile and Dashboard share the same `DocumentSlotsService`
  signal.

**Status:** NOT TESTED

---

## TC-PDS-009: Build & navigation smoke

**Preconditions:**
- Repository checked out cleanly.

**Steps:**
1. Run `cd frontend && npx ng build --configuration development`.
2. Run the app and log in once with each role (admin, faculty, student).
3. Open browser dev console.

**Expected Result:**
- `ng build` completes successfully with no errors.
- Each `/dashboard` page renders without console errors and without 404s on
  internal `routerLink` targets.
- `/profile` page renders without console errors.

**Status:** NOT TESTED

