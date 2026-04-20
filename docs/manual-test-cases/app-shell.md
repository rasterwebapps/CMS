# Manual Test Cases — Application Shell (R1-M1.3)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Keycloak running at `http://localhost:8180` with `cms` realm configured
- User logged in (e.g., `admin` / `admin123`)

---

## TC-SHELL-001: Sidenav Layout Displayed After Login

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Log in as any user                               |
| **Expected**| Application displays: left sidebar with navigation items, top toolbar with app title, menu toggle, theme toggle, and user menu |

---

## TC-SHELL-002: Sidenav Toggle

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the hamburger menu icon (☰) in the toolbar |
| **Expected**| Side navigation panel collapses/expands          |

---

## TC-SHELL-003: Dashboard Landing Page

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to `/` or `/dashboard`                  |
| **Expected**| Dashboard loads with: 4 metric cards (Total Students, Total Faculty, Departments, Active Courses) showing placeholder "—" values, Lab Utilization placeholder widget, Recent Activity placeholder widget |

---

## TC-SHELL-004: Dark Theme Toggle

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the theme toggle button (sun/moon icon) in the toolbar |
| **Expected**| Application switches to dark theme: dark background, light text, Material components update accordingly; icon changes from `dark_mode` to `light_mode` |

---

## TC-SHELL-005: Light Theme Toggle

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | While in dark theme, click the theme toggle button again |
| **Expected**| Application switches back to light theme          |

---

## TC-SHELL-006: User Menu — Username Display

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the user icon (account_circle) in the toolbar |
| **Expected**| Dropdown menu shows the logged-in username (e.g., "admin") and a Logout option |

---

## TC-SHELL-007: User Menu — Logout

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open user menu and click "Logout"                |
| **Expected**| User is logged out and redirected to Keycloak login page |

---

## TC-SHELL-008: Navigation — Dashboard Link Active State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to `/dashboard`                         |
| **Expected**| The "Dashboard" link in the sidenav has an active/highlighted appearance |

---

## TC-SHELL-009: Wildcard Route Redirect

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to a non-existent route (e.g., `/nonexistent`) |
| **Expected**| User is redirected to `/dashboard`               |

---

## TC-SHELL-010: Responsive Layout

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Resize browser window to mobile width (< 768px)  |
| **Expected**| Layout adjusts; sidenav can be toggled, content area fills available width |

---

## TC-SHELL-011: Menu Search — Filter by Keyword

**Preconditions:**
- User is logged in and sidebar is in expanded state

**Steps:**
1. Locate the search box at the top of the sidebar (below the logo, above navigation items)
2. Type `"fee"` into the search box

**Expected Result:**
- Navigation groups and items that do not match are hidden in real-time
- Only items containing "fee" (e.g., "Fee Structures", "Student Fees", "Fee Payments", "Fee Finalization") remain visible
- Matching items under a parent group still show their group header

**Status:** NOT TESTED

---

## TC-SHELL-012: Menu Search — Clear Search

**Preconditions:**
- User has typed a search term in the menu search box

**Steps:**
1. Type `"student"` in the menu search box — menu filters are applied
2. Click the "×" (close) button that appears at the right of the search box

**Expected Result:**
- Search input is cleared
- Full navigation menu is restored

**Status:** NOT TESTED

---

## TC-SHELL-013: Menu Search — No Results

**Preconditions:**
- User is on any page with the sidebar expanded

**Steps:**
1. Type `"zzz"` in the menu search box

**Expected Result:**
- All navigation entries are hidden (no match)
- No error or crash occurs

**Status:** NOT TESTED

---

## TC-SHELL-014: Menu Search Hidden When Sidebar Collapsed

**Preconditions:**
- Sidebar is in expanded state

**Steps:**
1. Collapse the sidebar using the hamburger toggle button
2. Observe the sidebar state

**Expected Result:**
- The menu search box is not shown when the sidebar is in collapsed (icon-rail) mode

**Status:** NOT TESTED

---

## TC-SHELL-015: Focus Mode — New Enquiry Form

**Preconditions:**
- User is logged in and on any standard page (e.g., Dashboard)

**Steps:**
1. Navigate to `/enquiries/new`
2. Observe the top area of the content region

**Expected Result:**
- The global toolbar (with app title, notifications, theme, profile) is replaced by a slim focus header
- The focus header shows: a back arrow button on the left and the title "New Enquiry"
- The sidebar remains visible and functional
- An indigo accent underline appears at the bottom of the focus header instead of the regular toolbar border

**Status:** NOT TESTED

---

## TC-SHELL-016: Focus Mode — Back Button Navigation

**Preconditions:**
- User has navigated to a focus mode page (e.g., `/enquiries/new`)

**Steps:**
1. Observe the focus header showing the back button (←)
2. Click the back button

**Expected Result:**
- Browser navigates back to the previous page (e.g., Enquiries list)
- Standard toolbar is restored

**Status:** NOT TESTED

---

## TC-SHELL-017: Focus Mode — Student Registration

**Preconditions:**
- User is on the Students list page

**Steps:**
1. Navigate to `/students/new`

**Expected Result:**
- Focus header shows with title "New Student Registration"
- Global toolbar is hidden, providing extra vertical space for the registration form
- Sidebar remains accessible

**Status:** NOT TESTED

---

## TC-SHELL-018: Focus Mode — Edit Routes

**Preconditions:**
- At least one enquiry exists in the system

**Steps:**
1. Navigate to `/enquiries/{id}/edit` for an existing enquiry

**Expected Result:**
- Focus header appears with title "Edit Enquiry"
- Standard toolbar is hidden

**Status:** NOT TESTED

---

## TC-SHELL-019: Standard Toolbar Visible on List Pages

**Preconditions:**
- User is logged in

**Steps:**
1. Navigate to `/enquiries` (list page)
2. Navigate to `/students` (list page)
3. Navigate to `/dashboard`

**Expected Result:**
- On all list and dashboard pages, the global toolbar (with app title, notifications, theme picker, and user avatar) is visible
- No focus header is shown on these pages

**Status:** NOT TESTED

---

## TC-SHELL-020: Breadcrumbs — Single-Segment Route

**Preconditions:**
- User is logged in

**Steps:**
1. Navigate to `/students`

**Expected Result:**
- Breadcrumb strip appears below the toolbar showing: `Home > Students`
- "Home" is a clickable link; "Students" is the current page (non-clickable, bold)

**Status:** NOT TESTED

---

## TC-SHELL-021: Breadcrumbs — Multi-Segment Route

**Preconditions:**
- User is logged in

**Steps:**
1. Navigate to `/enquiries/new`

**Expected Result:**
- Breadcrumb strip shows: `Home > Enquiries > New`
- Numeric ID segments (e.g., `/enquiries/42/edit` → `Home > Enquiries > Edit`) are omitted from breadcrumbs

**Status:** NOT TESTED

---

## TC-SHELL-022: Breadcrumbs — Click Parent Segment

**Preconditions:**
- User is on `/enquiries/new`

**Steps:**
1. Click "Enquiries" in the breadcrumb strip

**Expected Result:**
- Browser navigates to `/enquiries` (the enquiry list page)

**Status:** NOT TESTED

---

## TC-SHELL-023: Thin Scrollbar — Sidebar

**Preconditions:**
- Sufficient menu items are expanded so the sidebar is scrollable

**Steps:**
1. Expand multiple navigation groups until the sidebar overflows vertically
2. Hover the mouse cursor over the sidebar

**Expected Result:**
- A 4px-wide scrollbar appears only when hovering over the sidebar
- When not hovering, the scrollbar track and thumb are invisible (transparent)

**Status:** NOT TESTED

---

## TC-SHELL-024: Thin Scrollbar — Main Content

**Preconditions:**
- User is on a page with content that overflows vertically

**Steps:**
1. Navigate to a page with a long list (e.g., `/students`)
2. Hover over the main content area

**Expected Result:**
- A 4px-wide scrollbar appears only on hover over the content area
- Scrollbar is invisible (transparent) when not hovering

**Status:** NOT TESTED
