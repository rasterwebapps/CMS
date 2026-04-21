# Role-Based Dashboard — Manual Test Cases

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
