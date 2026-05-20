# Admin Analytics Widgets — Manual Test Cases

Module: Dashboard (Admin analytics widgets, May 2026)

Widgets added (each is a standalone, configurable dashboard widget — added/removed
per role via **Role Management → Dashboard Widgets**):

- `admission-funnel`        — Admission Funnel
- `fee-collection-target`   — Fee Collection vs Target
- `dues-aging`              — Outstanding Dues Aging
- `program-admissions`      — Admissions by Program

Backend endpoints (all under `/api/v1/dashboard/data/{key}`):

- `GET /admission-funnel`
- `GET /fee-collection-target`
- `GET /dues-aging`
- `GET /program-admissions`

DB seed: Flyway `V145__seed_admin_analytics_widgets.sql` adds these to:
- ADMIN-class roles (DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN) — all four
- CASHIER — `fee-collection-target`, `dues-aging`
- FRONT_OFFICE — `admission-funnel`

---

## TC-DASH-WIDGETS-001: Widgets appear in the Role Management widget picker

**Preconditions:**
- Logged in as `DEV_ADMIN` (or any user with `ROLE_MANAGE` permission)
- Database has been migrated (Flyway V145 applied)

**Steps:**
1. Navigate to **Role Management** → pick the **ADMIN** role
2. Click the dashboard-widgets icon (⊞ `dashboard_customize`)
3. In the widget picker, scroll the **Charts** category

**Expected Result:**
- The four new widgets appear in the Charts category with these labels:
  - Admission Funnel
  - Fee Collection vs Target
  - Outstanding Dues Aging
  - Admissions by Program
- Each widget is selectable and shows its description on hover.

**Status:** NOT TESTED

---

## TC-DASH-WIDGETS-002: Default seeded widgets render for ADMIN

**Preconditions:**
- Logged in as `ADMIN`
- V145 migration applied (or installed environment seeded `role_dashboard_widget_configs` for these keys)

**Steps:**
1. Navigate to `/dashboard`
2. Scroll to the bottom of the page

**Expected Result:**
- All four new widgets render in this order at orders 15–18:
  Admission Funnel | Fee Collection vs Target | Outstanding Dues Aging | Admissions by Program
- Each widget half-width (`col_span = 2`), arranged 2-per-row.
- Each widget shows a shimmer skeleton briefly, then real data.

**Status:** NOT TESTED

---

## TC-DASH-WIDGETS-003: Admission Funnel — pipeline counts and conversion %

**Preconditions:**
- At least one enquiry exists in each status (ENQUIRED, INTERESTED, FEES_FINALIZED, FEES_PAID, ADMITTED)

**Steps:**
1. Open `/dashboard` as ADMIN
2. Locate the **Admission Funnel** widget
3. Verify the five stages render: Enquiry, Interested, Fee Finalized, Fee Paid, Admitted
4. Check the conversion % pill in the top-right header

**Expected Result:**
- The first stage (Enquiry) has no conversion % beside its bar.
- Each subsequent stage shows a conversion % representing `count / previous_stage_count`.
- The header pill shows overall conversion (`admitted / enquired`).
- Bars decrease in width left-to-right, normalised to the top-of-funnel count.

**Status:** NOT TESTED

---

## TC-DASH-WIDGETS-004: Fee Collection vs Target — gauge math

**Preconditions:**
- At least one `FeeDemand` with a due-date in the current month
- At least one `TermFeePayment` in the current month and one in the previous month

**Steps:**
1. Open `/dashboard` as ADMIN or CASHIER
2. Locate the **Fee Collection · This Month** widget
3. Observe the ring percentage, collected amount, target amount, and delta pill

**Expected Result:**
- Ring fill matches `collected / target × 100` (rounded).
- Collected and Target values are in INR (₹) using the standard `InrPipe`.
- The delta pill is green with ↑ if current > last month, red with ↓ if less, neutral if equal.
- If there is no target (no demands due this month), achievedPct = 0% (gauge empty).

**Status:** NOT TESTED

---

## TC-DASH-WIDGETS-005: Outstanding Dues Aging — bucketing

**Preconditions:**
- At least one unpaid `FeeDemand` whose `due_date` is in each of the four buckets:
  0–30, 31–60, 61–90, 90+ days before today

**Steps:**
1. Open `/dashboard` as ADMIN or CASHIER
2. Locate the **Outstanding Dues · Aging** widget
3. Verify each bucket shows demand count, amount, and a bar
4. Total pill in header equals sum of all four buckets

**Expected Result:**
- 0–30 and 31–60 buckets are amber-coloured.
- 61–90 and 90+ buckets are red-coloured.
- Bars are sized proportionally to bucket amount / total outstanding.
- When no dues exist, the widget shows the "All caught up" empty state.

**Status:** NOT TESTED

---

## TC-DASH-WIDGETS-006: Admissions by Program — counts and bars

**Preconditions:**
- At least 2 active programs with different student counts

**Steps:**
1. Open `/dashboard` as ADMIN
2. Locate the **Admissions by Program** widget
3. Observe the list of programs

**Expected Result:**
- Programs are listed in descending order of admitted student count.
- Maximum 8 programs shown (limited).
- Each row shows the program code chip, program name, count, and a bar.
- Bar widths are normalised to the largest program (top program = 100%).
- Header pill shows total students across all listed programs.

**Status:** NOT TESTED

---

## TC-DASH-WIDGETS-007: Personal dashboard customisation persists

**Preconditions:**
- Logged in as a user with `DASHBOARD_CUSTOMIZE` permission

**Steps:**
1. Open `/dashboard`
2. Click **Configure**
3. Remove one of the four new widgets (e.g. Admission Funnel)
4. Save and reload `/dashboard`
5. Click Configure again and re-add the widget; save and reload

**Expected Result:**
- After removal, the widget is no longer rendered.
- After re-adding, the widget appears and loads its data.
- Personal config persists across reloads (via `PUT /dashboard/config`).

**Status:** NOT TESTED

---

## TC-DASH-WIDGETS-008: Light + dark mode visual parity

**Preconditions:**
- All four widgets visible on the admin dashboard

**Steps:**
1. Open `/dashboard` in dark mode — inspect each new widget
2. Toggle to light mode (theme switcher) — inspect again

**Expected Result:**
- Every widget renders correctly in both modes: text legible, bars/gauge visible,
  borders consistent with neighbouring widgets (`equipment-status`, `fee-overview`).
- No white-on-white or black-on-black text in either mode.
- Headers/footers match the standard `.widget-shell` chrome.

**Status:** NOT TESTED

---

## TC-DASH-WIDGETS-009: Mobile responsiveness (360 × 740)

**Preconditions:**
- Chrome DevTools device emulation set to 360 × 740 (typical Android)

**Steps:**
1. Open `/dashboard` as ADMIN
2. Scroll through each of the four new widgets

**Expected Result:**
- No horizontal scroll on the page.
- Each widget is full-width (single column).
- The Fee Collection gauge widget switches to vertical layout (gauge above KPIs)
  on very narrow widgets (≤ 380 px).
- All tap targets (labels, pills) are readable; no clipped numbers.

**Status:** NOT TESTED

---

## TC-DASH-WIDGETS-010: Error / empty states

**Preconditions:**
- Backend stopped (or endpoints returning 5xx)

**Steps:**
1. Reload `/dashboard`
2. Observe each new widget

**Expected Result:**
- Each widget shows its skeleton briefly, then transitions to the
  `widget-error` state with an icon and "No data" message — it does **not**
  crash the dashboard or block sibling widgets from loading.

**Status:** NOT TESTED

