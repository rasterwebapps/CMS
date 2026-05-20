# Admin Analytics Widgets — Tier 2 Manual Test Cases

Module: Dashboard (Tier-2 admin analytics widgets, May 2026)

Widgets added in Tier 2 (configurable per role via **Role Management → Dashboard Widgets**):

- `agent-performance`          — Agent Performance leaderboard
- `program-revenue-mix`        — Program Revenue Mix (donut)
- `scholarship-burn`           — Scholarship & Concession Burn
- `doc-verification-backlog`   — Document Verification Backlog

Backend endpoints (under `/api/v1/dashboard/data/{key}`):

- `GET /agent-performance`         — `@perm.hasAny('ENQUIRY_VIEW','STUDENT_VIEW','REPORT_VIEW')`
- `GET /program-revenue-mix`       — `@perm.hasAny('STUDENT_FEE_VIEW','REPORT_VIEW')`
- `GET /scholarship-burn`          — `@perm.hasAny('STUDENT_FEE_VIEW','REPORT_VIEW')`
- `GET /doc-verification-backlog`  — `@perm.hasAny('DOCUMENT_SUBMISSION_VIEW','DOCUMENT_SUBMISSION_MANAGE','ENQUIRY_VIEW')`

DB seed: Flyway `V146__seed_tier2_analytics_widgets.sql`:
- ADMIN-class roles (DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN) — all four (orders 19–22)
- CASHIER — `scholarship-burn`
- FRONT_OFFICE — `agent-performance`, `doc-verification-backlog`

---

## TC-DASH-T2-001: Tier-2 widgets appear in Role Management picker

**Preconditions:**
- Logged in as `DEV_ADMIN`
- V146 migration applied

**Steps:**
1. Open **Role Management** → ADMIN role → Dashboard Widgets picker
2. Browse the Charts, Lists and Operational categories

**Expected Result:**
- Four new widgets are selectable:
  - Agent Performance (Lists)
  - Program Revenue Mix (Charts)
  - Scholarship & Concession Burn (Charts)
  - Document Verification Backlog (Operational)

**Status:** NOT TESTED

---

## TC-DASH-T2-002: Agent Performance — leaderboard ordering

**Preconditions:**
- At least 4 agents exist
- Each agent referred different numbers of enquiries, some converted to ADMITTED/CONVERTED

**Steps:**
1. Open `/dashboard` as ADMIN
2. Locate **Agent Performance** widget

**Expected Result:**
- Top 6 agents listed.
- Sorted by conversions DESC, then leads DESC.
- Rank 1/2/3 show gold/silver/bronze medals; ranks 4–6 plain badge.
- Avatar shows two-letter initials, name truncates with ellipsis.
- Conversion % chips: green ≥ 50%, red < 20% (when leads > 0), default otherwise.
- "View all →" link routes to `/agents`.

**Status:** NOT TESTED

---

## TC-DASH-T2-003: Program Revenue Mix — donut + legend

**Preconditions:**
- At least 3 active programs each have `StudentFeeAllocation` rows with non-zero `netFee`

**Steps:**
1. Open `/dashboard` as ADMIN
2. Locate **Program Revenue Mix** widget

**Expected Result:**
- Donut chart renders with up to 6 coloured slices; remaining slice shows muted grey.
- Center hole displays total net revenue (₹ via `InrPipe`) with "Net" caption.
- Legend lists each slice with colour dot, program code chip, name, and % share.
- Slices are sorted by netRevenue DESC, max 6.
- On narrow widgets (≤ 380 px) layout stacks (donut above legend).

**Status:** NOT TESTED

---

## TC-DASH-T2-004: Scholarship & Concession Burn — stacked bar math

**Preconditions:**
- Several `StudentFeeAllocation` rows with non-zero `discountAmount` and/or `scholarshipDiscountAmount`

**Steps:**
1. Open `/dashboard` as ADMIN or CASHIER
2. Locate **Scholarship & Concession Burn** widget

**Expected Result:**
- Header pill shows `−{discountPct}%` (combined discount + scholarship as % of gross).
- Top row: Gross → arrow → Net Collectable (Net value in green).
- Stacked bar segments sum to 100% in this order:
  green (net) → amber (discount) → pink (scholarship).
- Rows beneath show Discount amount, Scholarship amount, and Students impacted count.
- All currency values use `InrPipe`.

**Status:** NOT TESTED

---

## TC-DASH-T2-005: Document Verification Backlog — severity colouring

**Preconditions:**
- `EnquiryDocument` rows in all states: UPLOADED (pending), VERIFIED (some within last 24h), REJECTED

**Steps:**
1. Open `/dashboard` as ADMIN or FRONT_OFFICE
2. Locate **Document Verification** widget
3. Vary the underlying data and reload to verify severity transitions

**Expected Result:**
- Big number = pending count.
- Severity: green when 0 pending; amber 1–24 pending and oldest < 5 days; red when oldest ≥ 5 days OR pending ≥ 25.
- The widget's accent colour (top bar, icon) follows the severity.
- "Oldest N days" subtitle shown only if pending > 0.
- Mini-stats show: verified in last 24h (green), total rejected (red).
- CTA button "Open verification queue" routes to `/enquiries/document-submission`.

**Status:** NOT TESTED

---

## TC-DASH-T2-006: Default seed applies to seeded role

**Preconditions:**
- Fresh database with V146 applied

**Steps:**
1. Log in as a user with the **ADMIN** role (no personal dashboard override)
2. Open `/dashboard` and scroll to the bottom

**Expected Result:**
- The four Tier-2 widgets appear at the end of the layout (orders 19–22).
- CASHIER login shows `scholarship-burn` added to their dashboard.
- FRONT_OFFICE login shows `agent-performance` and `doc-verification-backlog` added.

**Status:** NOT TESTED

---

## TC-DASH-T2-007: Light + dark mode parity

**Preconditions:** All Tier-2 widgets visible

**Steps:**
1. Toggle dark mode → light mode → dark mode
2. Inspect each widget in both modes

**Expected Result:**
- Donut chart center hole switches between dark and white backgrounds.
- Severity colours (green/amber/red) remain identical in both modes.
- All text remains legible; no white-on-white in light mode.

**Status:** NOT TESTED

---

## TC-DASH-T2-008: Mobile responsiveness (360 × 740)

**Steps:**
1. Set DevTools viewport to 360 × 740
2. Open `/dashboard` as ADMIN

**Expected Result:**
- All four widgets render full-width with no horizontal scroll.
- Agent Performance leaderboard table fits with `lb-rank` collapsed sensibly.
- Program Revenue Mix donut and legend stack vertically.
- Doc Verification CTA button is full-width tap target (≥ 40px tall).

**Status:** NOT TESTED

---

## TC-DASH-T2-009: Error / empty states

**Preconditions:** Backend stopped or returning 5xx

**Steps:**
1. Reload `/dashboard`
2. Observe each Tier-2 widget

**Expected Result:**
- Each widget shows its skeleton briefly, then a `widget-error` placeholder
  with icon + "No ... data" message. Dashboard does not crash; other widgets
  load independently.

**Status:** NOT TESTED

---

## TC-DASH-T2-010: Permission gating

**Preconditions:**
- A test role that lacks `STUDENT_FEE_VIEW`, `REPORT_VIEW`,
  `DOCUMENT_SUBMISSION_VIEW`, `DOCUMENT_SUBMISSION_MANAGE`

**Steps:**
1. Add all four Tier-2 widgets to that role's dashboard config
2. Log in as a user holding only that role
3. Open `/dashboard`

**Expected Result:**
- `scholarship-burn`, `program-revenue-mix` and `doc-verification-backlog`
  show the `widget-error` placeholder (HTTP 403) but do not crash the dashboard.
- `agent-performance` is similarly gated and shows the error placeholder unless
  the role has `ENQUIRY_VIEW`/`STUDENT_VIEW`/`REPORT_VIEW`.

**Status:** NOT TESTED

