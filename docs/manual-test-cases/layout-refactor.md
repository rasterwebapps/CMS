# Layout Refactor & UI Enhancements — Manual Test Cases

## TC-LAYOUT-001: Fixed sidebar remains stationary while content scrolls

**Preconditions:**
- Application is running and user is logged in
- Navigate to Dashboard or Enquiries list (a page with enough content to scroll)

**Steps:**
1. Open the application in a browser
2. Navigate to a page with scrollable content (e.g., Enquiries list with several records)
3. Scroll down in the main content area

**Expected Result:**
- The left sidebar (navigation) stays fixed and does not scroll with the content
- The top toolbar stays fixed and does not scroll with the content
- Only the centre content panel scrolls

**Status:** NOT TESTED

---

## TC-LAYOUT-002: Sidebar collapse/expand does not cause content to jump

**Preconditions:**
- Application is running and user is logged in

**Steps:**
1. Open the application
2. Click the application icon in the top-left of the toolbar to collapse the sidebar
3. Click it again to expand the sidebar
4. Scroll the content area partially and then toggle the sidebar

**Expected Result:**
- The sidebar smoothly transitions between expanded (272 px) and collapsed (68 px) icon-rail states
- The main content area shifts its left margin accordingly without any flash or jump
- Scroll position in the content area is preserved

**Status:** NOT TESTED

---

## TC-LAYOUT-003: Application icon replaces hamburger menu in toolbar

**Preconditions:**
- Application is running and user is logged in

**Steps:**
1. Open the application
2. Observe the far-left element of the top toolbar

**Expected Result:**
- The application logo image (SKS College icon) is displayed at the far left of the toolbar
- No hamburger (`menu` / `menu_open`) material icon is visible
- Clicking the application icon toggles the sidebar collapse/expand (same as the old hamburger)

**Status:** NOT TESTED

---

## TC-LAYOUT-004: Slim scrollbars displayed in Webkit and Firefox

**Preconditions:**
- Application is running

**Steps:**
1. Open the application in Chrome/Edge (Webkit)
2. Navigate to a page with scrollable content and verify the scrollbar width is slim (~6 px)
3. Open the application in Firefox
4. Navigate to the same page and verify the scrollbar appears thin

**Expected Result:**
- Chrome/Edge: scrollbar width ≤ 6 px, semi-transparent thumb, transparent track
- Firefox: scrollbar uses `scrollbar-width: thin` rendering (narrow scrollbar)
- Both browsers: scrollbar thumb becomes more opaque on hover

**Status:** NOT TESTED

---

## TC-LAYOUT-005: Add Enquiry screen shows full toolbar and sidebar

**Preconditions:**
- Application is running and user is logged in with ROLE_ADMIN or ROLE_FACULTY

**Steps:**
1. Navigate to Enquiries list (`/enquiries`)
2. Click "New Enquiry" or navigate to `/enquiries/new`

**Expected Result:**
- The full top toolbar is visible (not replaced by a minimal focus-mode header)
- The left sidebar is visible and collapsible
- The breadcrumb bar shows: Home › Enquiries › New
- The "Add Enquiry" page title and description appear at the **top** of the content area, directly below the toolbar and breadcrumbs

**Status:** NOT TESTED

---

## TC-LAYOUT-006: Edit Enquiry screen shows full toolbar and sidebar

**Preconditions:**
- At least one enquiry exists in the system

**Steps:**
1. Navigate to the Enquiries list
2. Click Edit on any existing enquiry

**Expected Result:**
- The full top toolbar is visible
- The left sidebar remains visible and collapsible
- The "Edit Enquiry" page title appears at the top of the content area

**Status:** NOT TESTED

---

## TC-LAYOUT-007: Convert Enquiry screen shows full toolbar and sidebar

**Preconditions:**
- At least one enquiry exists with status allowing conversion (DOCUMENTS_SUBMITTED)

**Steps:**
1. Navigate to the Enquiry detail page for a convertible enquiry
2. Initiate the "Convert to Student" action

**Expected Result:**
- The full top toolbar is visible
- The left sidebar remains visible and collapsible

**Status:** NOT TESTED

---

## TC-LAYOUT-008: Independent scroll — content area scrolls, toolbar and sidebar do not

**Preconditions:**
- Application is running and enough records exist to cause scrolling on a list page

**Steps:**
1. Navigate to the Students list or Enquiries list
2. Scroll to the bottom of the content area
3. Observe the toolbar and sidebar positions

**Expected Result:**
- The top toolbar sticks to the top of the viewport (does not scroll away)
- The sidebar remains stationary
- Only the content area between the toolbar and the bottom of the viewport scrolls

**Status:** NOT TESTED

---

## TC-LAYOUT-009: Responsive layout on smaller screen sizes

**Preconditions:**
- Browser is available

**Steps:**
1. Open the application
2. Resize the browser window to a narrower width (e.g., 900 px, 768 px)
3. Navigate between pages

**Expected Result:**
- The fixed layout does not break or cause horizontal overflow
- Content remains accessible and usable at reduced widths

**Status:** NOT TESTED
