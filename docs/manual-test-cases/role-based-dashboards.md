# Role-Based Dashboard — Manual Test Cases

> **Note:** The Admin dashboard was refactored in 2026-04 to match `docs/college-management-v2.html`. TCs `TC-DASH-001`/`002`/`004`/`005`/`006`/`008` reference the previous layout (gradient hero, Recent Activity, Breakdown Analytics, 6-Month Trends, Academic Calendar) and are **superseded** by `TC-DASH-009` through `TC-DASH-014` below. The Front-Office (`TC-DASH-003`) and Faculty (`TC-DASH-007`) tests are unaffected.

## TC-DASH-001: Admin Dashboard — KPI cards load correctly

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running (backend + frontend)

**Steps:**
1. Navigate to `/dashboard`
2. Wait for the page to load (skeleton loaders should appear, then real data)
3. Verify the hero banner appears with an indigo-to-violet gradient and the username displayed
4. Verify 5 KPI cards are visible: Total Students, Active Faculty, Labs, Fee Collected, Fee Outstanding
5. Verify each card shows a numeric value (not `—`) once loaded

**Expected Result:**
- Dashboard shows the Admin layout (no tabs)
- KPI cards animate in with staggered delay
- All values reflect current database counts

**Status:** NOT TESTED

---

## TC-DASH-002: Front Office Dashboard — KPI cards load correctly

**Preconditions:**
- User is logged in with `ROLE_FRONT_OFFICE`

**Steps:**
1. Navigate to `/dashboard`
2. Wait for the page to load
3. Verify the hero banner shows sky-blue gradient with "Admissions Desk" text
4. Verify 3 prominent CTA buttons appear: "New Enquiry", "New Admission", "Collect Payment"
5. Verify 5 KPI cards: Today's Enquiries, Pending Admissions, Fees Collected, Conversions, Conversion Rate
6. Verify "Today's Enquiries" panel shows up to 10 enquiries from today
7. Verify the Enquiry Funnel panel appears at the bottom

**Expected Result:**
- Front Office layout is shown with sky-blue theme
- CTA buttons are large (min-height 48px) and prominent
- Enquiry funnel shows all stages with percentage labels

**Status:** NOT TESTED

---

## TC-DASH-003: Dual-role user — Tab switching works

**Preconditions:**
- User is logged in with BOTH `ROLE_ADMIN` AND `ROLE_FRONT_OFFICE`

**Steps:**
1. Navigate to `/dashboard`
2. Verify two tabs appear at the top: "Admin" and "Front Office"
3. Verify "Admin" tab is active by default (higher priority)
4. Click the "Front Office" tab
5. Verify the Front Office dashboard is now displayed
6. Refresh the page
7. Verify the Front Office dashboard is still selected (persisted in sessionStorage)
8. Click the "Admin" tab
9. Verify the Admin dashboard is displayed again

**Expected Result:**
- Tab switching is smooth and instant
- Active tab is persisted across page reloads
- Keyboard navigation (arrow keys) cycles through tabs

**Status:** NOT TESTED

---

## TC-DASH-004: Backend — GET /api/v1/dashboard/front-office

**Preconditions:**
- Application is running
- User has `ROLE_FRONT_OFFICE` or `ROLE_ADMIN` JWT token

**Steps:**
1. Send `GET /api/v1/dashboard/front-office` with a valid `ROLE_FRONT_OFFICE` token
2. Verify response status is `200 OK`
3. Verify response body contains: `todayEnquiryCount`, `totalEnquiryCount`, `pendingAdmissionsCount`, `feeCollectedToday`, `conversionsThisWeek`, `conversionRate`, `enquiryFunnel`, `todaysEnquiries`, `pendingActionItems`
4. Send the same request with a valid `ROLE_ADMIN` token
5. Verify response status is `200 OK`
6. Send the same request with a `ROLE_STUDENT` token
7. Verify response status is `403 Forbidden`

**Expected Result:**
- Front office and admin users can access the endpoint
- Students are forbidden
- Response JSON matches `FrontOfficeDashboardResponse` structure

**Status:** NOT TESTED

---

## TC-DASH-005: Frontend — Skeleton loaders appear during data fetch

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`
- Network throttling enabled (simulate slow connection in browser DevTools)

**Steps:**
1. Navigate to `/dashboard`
2. Verify skeleton loaders appear immediately while data is loading
3. Verify skeletons match the layout (5 skeleton KPI cards, 2 skeleton widget boxes)
4. Wait for data to load
5. Verify skeletons are replaced by real content

**Expected Result:**
- Skeleton loaders appear and animate with shimmer effect
- Layout shift is minimal when real content replaces skeletons

**Status:** NOT TESTED

---

## TC-DASH-006: Frontend — Enquiry funnel percentages display correctly

**Preconditions:**
- User is logged in with `ROLE_FRONT_OFFICE`
- At least some enquiries exist in the database

**Steps:**
1. Navigate to `/dashboard`
2. Scroll to the "Admission Funnel" section
3. Verify each status row shows a label, a progress bar, count, and percentage

**Expected Result:**
- Percentage labels are visible next to each funnel entry
- Bars scale proportionally to the highest value
- All funnel statuses present in the data are shown

**Status:** NOT TESTED

---

## TC-DASH-007: Admin Dashboard — Pending Approvals panel

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- At least some students with `PENDING` status exist, or outstanding fee > 0

**Steps:**
1. Navigate to `/dashboard`
2. Scroll to the "Pending Approvals" panel (right column in the two-column section)
3. If pending items exist, verify colored severity dots and "Review →" links are displayed
4. If nothing is pending, verify the "✅ All clear" message is shown

**Expected Result:**
- Panel shows relevant pending action items with correct severity colors
- Links navigate to the appropriate section when clicked

**Status:** NOT TESTED

---

## TC-DASH-008: Academic Calendar strip renders

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to `/dashboard`
2. Scroll to the bottom of the Admin dashboard
3. Verify the Academic Calendar strip is visible with 5 static pills
4. Verify the strip is horizontally scrollable

**Expected Result:**
- 5 color-coded calendar pills displayed in a horizontal row
- Pills are scrollable if they overflow the container
- Static placeholder text is displayed (not live data)

**Status:** NOT TESTED

---

## TC-DASH-009: Admin Dashboard — Hero matches v2 spec

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to `/dashboard`
2. Inspect the hero section at the top of the page

**Expected Result:**
- Hero is a single rounded card on the surface-2 background (NOT a primary-colour gradient)
- A 1-pixel accent gradient line runs across the very top edge of the hero
- Three concentric translucent accent rings are visible on the right side
- Eyebrow row contains: `ACADEMIC YEAR YYYY–YY` chip (uppercase, accent-tinted) and a "N students enrolled" live badge with a pulsing green dot
- Title reads `Welcome back, <username>` in serif italic with the username highlighted in the accent colour
- Sub-line reads: "Here's your college overview for today — all systems nominal."
- Two action buttons on the right: "View Reports" (outlined ghost) and "Generate Report" (primary, filled, accent-coloured)

**Status:** NOT TESTED

---

## TC-DASH-010: Admin Dashboard — Five stat cards render with correct accents

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to `/dashboard`
2. Inspect the row of statistic cards directly below the hero

**Expected Result:**
- Exactly 5 cards in a single grid row (3 cards per row at <1200 px, 2 per row at <720 px)
- Each card has a 3-pixel coloured top edge: card 1 = accent (primary), card 2 = green, card 3 = purple, card 4 = amber, card 5 = red
- Each card displays, top to bottom: a corner pill badge in the matching colour, a tinted icon square, a large mono-font numeric value, a label, and a small meta line
- Card titles in order: "Total Students", "Active Faculty", "Labs", "Fee Collected", "Outstanding"
- Money values are prefixed with `₹` and use Indian-locale grouping
- Cards rise into view with staggered animation (`dash-rise` 0.5s)
- Hovering a card lifts it 3 px and adds a soft shadow

**Status:** NOT TESTED

---

## TC-DASH-011: Admin Dashboard — Quick-actions row

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to `/dashboard`
2. Inspect the horizontal row of buttons below the stat cards
3. Click each button in turn

**Expected Result:**
- Row contains exactly 5 ghost-style pill buttons in this order, each with a leading SVG icon: "Enrol Student", "View Reports", "Fee Waiver", "Settings", "Agents"
- Hover state tints background, text, and border using the active accent colour and lifts the button by 1 px
- Buttons route to: `/students/new`, `/reports`, `/student-fees`, `/settings`, `/agents` respectively

**Status:** NOT TESTED

---

## TC-DASH-012: Admin Dashboard — Admission Trend + Pending Approvals row

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Backend has at least one trend datapoint and one approvable item (outstanding fees, open maintenance, or new enquiry)

**Steps:**
1. Navigate to `/dashboard`
2. Inspect the row containing the Admission Trend and Pending Approvals cards

**Expected Result:**
- Two cards side by side, the left (Admission Trend) wider than the right (Pending Approvals) at a 1.65 : 1 ratio
- Admission Trend card: header has accent-tinted icon and "View all →" link; body shows a 120 px tall bar chart with one bar per month; bars at or above the median value are accent-coloured ("hi" with glow), the rest are dimmed surface-3 ("lo"); month abbreviations are listed underneath
- Pending Approvals card: header has red-tinted icon and "Review →" link; body shows up to three rows; each row has a coloured glowing dot, title, sub-line, and a right-aligned mono-font amount in the matching severity colour
- When there are no approvals, the card shows "All clear — nothing pending."

**Status:** NOT TESTED

---

## TC-DASH-013: Admin Dashboard — Equipment Status + Fee Overview row

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to `/dashboard`
2. Scroll to the bottom row containing Equipment Status and Fee Overview

**Expected Result:**
- Two cards side by side (1.65 : 1 ratio)
- Equipment Status: header has purple-tinted icon; body shows one row per equipment status, each with name and "N units" on the same line, and a thin progress bar underneath whose fill colour reflects the status (green = OPERATIONAL/AVAILABLE, accent = IN_USE/ASSIGNED, amber = IN_REPAIR/MAINTENANCE, red = FAULTY/DECOMMISSIONED)
- Fee Overview: header has green-tinted icon; body shows exactly four rows in this order: "Collected this month" (₹ value, green), "Total outstanding" (₹ value, red), "Labs fee" (`—`, muted), "Equipment fee" (`—`, muted); hovering a row tints the background to surface-3

**Status:** NOT TESTED

---

## TC-DASH-014: Admin Dashboard — Removed legacy sections

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to `/dashboard`
2. Scroll the entire admin dashboard from top to bottom

**Expected Result:**
- The page contains **only** the five sections defined in `docs/college-management-v2.html`: Hero, Stats Grid, Quick Actions, Trend + Approvals, Equipment + Fee
- The following sections are **no longer present**: "Recent Activity" feed, "Breakdown Analytics" (Equipment / Maintenance / Students / Attendance by Status), "6-Month Trends", "Enquiry Funnel", "Academic Calendar" strip
- No requests are made to `/api/v1/dashboard/activity` or `/api/v1/academic-years/current/events` from the admin dashboard

**Status:** NOT TESTED
