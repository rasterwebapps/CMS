# Admin Analytics Widgets — Tier 3 Manual Test Cases

Module: Dashboard (Tier-3 strategic/monthly analytics widgets, May 2026)

Widgets added in Tier 3 (configurable per role via **Role Management → Dashboard Widgets**):

- `geographic-admissions` — Geographic Admissions Heatmap
- `yoy-admissions` — Year-over-Year Admission Comparison
- `refund-cancellation-rate` — Refund & Cancellation Rate
- `payment-mode-breakdown` — Fee Payment Mode Breakdown
- `student-faculty-ratio` — Student : Faculty Ratio
- `lab-utilization-heatmap` — Lab Utilization Heatmap
- `cohort-retention` — Cohort Retention
- `top-line-kpis` — Top-line KPI Strip

Backend endpoints (under `/api/v1/dashboard/data/{key}`):

- `GET /geographic-admissions`
- `GET /yoy-admissions`
- `GET /refund-cancellation-rate`
- `GET /payment-mode-breakdown`
- `GET /student-faculty-ratio`
- `GET /lab-utilization-heatmap`
- `GET /cohort-retention`
- `GET /top-line-kpis`

DB seed: Flyway `V147__seed_tier3_strategic_dashboard_widgets.sql`:
- ADMIN-class roles — all eight widgets
- CASHIER — top-line KPIs, refund/cancellation, payment mode breakdown
- FRONT_OFFICE — top-line KPIs, geography, YoY admissions

> Note: `geographic-admissions` uses a dependency-free heatmap list/grid instead of an India map because no map/chart package is declared in `package.json`. `lab-utilization-heatmap` uses active `LabSchedule` day × slot density because booking density is not available until the Lab Safety/booking schema exists.

---

## TC-DASH-T3-001: Tier-3 widgets appear in Role Management picker

**Preconditions:**
- Logged in as `DEV_ADMIN`
- V147 migration applied

**Steps:**
1. Open **Role Management** → ADMIN role → Dashboard Widgets picker
2. Browse Charts and Stats categories

**Expected Result:**
- All eight Tier-3 widget labels appear and are selectable.
- `Top-line KPI Strip` appears in Stats.
- Other Tier-3 widgets appear in Charts.

**Status:** NOT TESTED

---

## TC-DASH-T3-002: Geographic Admissions Heatmap

**Preconditions:**
- Students have `address.state` and `address.district` or `address.city`

**Steps:**
1. Open `/dashboard` as ADMIN
2. Locate **Geographic Admissions Heatmap**

**Expected Result:**
- A dependency-free India outline map is shown, with state/UT markers coloured and sized by admission volume.
- Hovering a marker shows state name and admission count.
- A compact Top Districts list is shown beside/below the map, sorted by count desc.
- Missing district/state data is grouped under `Unknown District` / `Unknown State`.
- The map outline and marker positions are stylized and dependency-free, not an official GIS boundary map.

**Status:** NOT TESTED

---

## TC-DASH-T3-003: Year-over-Year Admission Comparison

**Preconditions:**
- Students exist with `admissionDate` in current year and previous two years

**Steps:**
1. Open `/dashboard` as ADMIN or FRONT_OFFICE
2. Locate **YoY Admission Comparison**

**Expected Result:**
- 12 month groups render from JAN to DEC.
- Each month shows three bars: current year, last year, two years ago.
- Bar heights are normalized to the maximum monthly value.

**Status:** NOT TESTED

---

## TC-DASH-T3-004: Refund & Cancellation Rate

**Preconditions:**
- Some `FeePayment` rows have status `REFUNDED`
- Some students have status `WITHDRAWN`

**Steps:**
1. Open `/dashboard` as ADMIN or CASHIER
2. Locate **Refund & Cancellation Rate**

**Expected Result:**
- Refund rate = refunded payments / total payments.
- Cancellation/withdrawal rate = withdrawn students / total students.
- 12-month mini trend renders with refund and withdrawal bars.

**Status:** NOT TESTED

---

## TC-DASH-T3-005: Fee Payment Mode Breakdown

**Preconditions:**
- Term fee payments exist with payment modes: UPI, CASH, CHEQUE, CARD, BANK_TRANSFER, etc.

**Steps:**
1. Open `/dashboard` as ADMIN or CASHIER
2. Locate **Fee Payment Mode Breakdown**

**Expected Result:**
- Donut displays payment-mode share by amount.
- Center shows total collected amount using INR formatting.
- Legend shows each mode and share %.

**Status:** NOT TESTED

---

## TC-DASH-T3-006: Student : Faculty Ratio

**Preconditions:**
- Specialities have active faculty and active students assigned via specialization speciality

**Steps:**
1. Open `/dashboard` as ADMIN
2. Locate **Student : Faculty Ratio**

**Expected Result:**
- Speciality rows show students, faculty, and ratio.
- Green ≤ 20:1, amber ≤ 30:1, red > 30:1.
- Threshold marker is visible on each bar.

**Status:** NOT TESTED

---

## TC-DASH-T3-007: Lab Utilization Heatmap

**Preconditions:**
- Active `LabSchedule` rows exist across days and lab slots

**Steps:**
1. Open `/dashboard` as ADMIN
2. Locate **Lab Utilization Heatmap**

**Expected Result:**
- Heatmap grid displays days as rows and lab slots as columns.
- Each cell shows schedule count and intensity based on max scheduled cell.
- If no active schedules exist, empty state is shown.

**Status:** NOT TESTED

---

## TC-DASH-T3-008: Cohort Retention

**Preconditions:**
- `StudentTermEnrollment` rows exist across multiple term numbers for at least one cohort

**Steps:**
1. Open `/dashboard` as ADMIN
2. Locate **Cohort Retention**

**Expected Result:**
- Up to 5 cohorts are listed.
- Each cohort shows baseline count and term pills (`T1`, `T2`, etc.).
- Term pill % = active/enrolled students relative to baseline.
- Retention below 70% is highlighted red.

**Status:** NOT TESTED

---

## TC-DASH-T3-009: Top-line KPI Strip

**Preconditions:**
- Dashboard user has required student/fee/enquiry permissions

**Steps:**
1. Open `/dashboard` as ADMIN, CASHIER, or FRONT_OFFICE
2. Locate **Top-line KPI Strip**

**Expected Result:**
- Six compact cards render:
  - Today's Collection
  - Today's Admissions
  - Pending Verifications
  - Overdue Fees
  - Active Enquiries
  - Conversion 30d
- Currency values use INR formatting from backend short format.
- Severity colors match risk/health state.

**Status:** NOT TESTED

---

## TC-DASH-T3-010: Mobile and theme parity

**Preconditions:**
- All Tier-3 widgets visible on ADMIN dashboard

**Steps:**
1. Test at 360 × 740 viewport
2. Toggle dark mode and light mode

**Expected Result:**
- No horizontal page scroll.
- Donut/heatmap/ratio/cohort layouts remain readable.
- Text and chart colors are legible in both themes.

**Status:** NOT TESTED

---

## TC-DASH-T3-011: Permission gating

**Preconditions:**
- A test role lacks the required permissions for one or more Tier-3 endpoints

**Steps:**
1. Add Tier-3 widgets to the test role dashboard config
2. Login as that role
3. Open `/dashboard`

**Expected Result:**
- Unauthorized widgets show `widget-error` and do not crash the dashboard.
- Authorized widgets continue loading independently.

**Status:** NOT TESTED

