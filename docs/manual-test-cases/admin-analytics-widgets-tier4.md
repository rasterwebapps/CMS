# Admin Analytics Widgets — Tier 4 Manual Test Cases

Module: Dashboard (Tier-4 exception / passive alert widgets, May 2026)

Widgets added in Tier 4 (configurable per role via **Role Management → Dashboard Widgets**):

- `anomaly-banner` — Collection Anomaly Banner
- `capacity-alert` — Capacity Alert
- `compliance-alerts` — Compliance Alerts
- `audit-mini-feed` — Audit Log Mini-feed

Backend endpoints (under `/api/v1/dashboard/data/{key}`):

- `GET /anomaly-banner`
- `GET /capacity-alert`
- `GET /compliance-alerts`
- `GET /audit-mini-feed`

DB/schema support: Flyway `V148__tier4_alert_schema_and_widgets.sql`:
- Adds nullable `programs.seat_capacity`
- Creates `compliance_documents`
- Seeds alert widgets into ADMIN-class dashboards
- Seeds collection anomaly for CASHIER and capacity alert for FRONT_OFFICE

---

## TC-DASH-T4-001: Tier-4 widgets appear in Role Management picker

**Preconditions:**
- Logged in as `DEV_ADMIN`
- V148 migration applied

**Steps:**
1. Open **Role Management** → ADMIN role → Dashboard Widgets picker
2. Browse the Operational category

**Expected Result:**
- Four new widgets are selectable:
  - Collection Anomaly Banner
  - Capacity Alert
  - Compliance Alerts
  - Audit Log Mini-feed

**Status:** NOT TESTED

---

## TC-DASH-T4-002: Collection Anomaly Banner — drop alert

**Preconditions:**
- `payment_receipts` has collections today and on the same weekday last week
- Today's amount is at least 20% lower than last week

**Steps:**
1. Open `/dashboard` as ADMIN or CASHIER
2. Locate **Collection Anomaly Banner**

**Expected Result:**
- Banner message says `Collections down {x}% vs same day last week`.
- Severity colour is red for ≥20% drop, amber for smaller drop, green for flat/up.
- Today and last-week amounts display in INR.

**Status:** NOT TESTED

---

## TC-DASH-T4-003: Capacity Alert — high occupancy

**Preconditions:**
- At least one active program has `seat_capacity` configured
- Active student count for the program is ≥ 85% of capacity

**Steps:**
1. Open `/dashboard` as ADMIN or FRONT_OFFICE
2. Locate **Capacity Alert**

**Expected Result:**
- Program rows show code, name, filled/capacity, seats left, occupancy %.
- Amber is used from 85% to 94%.
- Red is used from 95% upward.
- If no program has capacity configured or no program is ≥85%, an all-clear message is shown.

**Status:** NOT TESTED

---

## TC-DASH-T4-004: Compliance Alerts — expiring statutory documents

**Preconditions:**
- `compliance_documents` has ACTIVE rows for UGC/NAAC/AICTE documents expiring within 90 days

**Steps:**
1. Open `/dashboard` as ADMIN
2. Locate **Compliance Alerts**

**Expected Result:**
- Up to 6 documents are listed sorted by nearest expiry.
- Expired or ≤30 days are red.
- 31–60 days are amber.
- 61–90 days are accent blue.
- If no documents expire within 90 days, all-clear message is shown.

**Status:** NOT TESTED

---

## TC-DASH-T4-005: Audit Log Mini-feed — high privilege activity

**Preconditions:**
- `audit_log` contains recent role, permission, user, fee, waiver, or override actions

**Steps:**
1. Open `/dashboard` as ADMIN or CASHIER
2. Locate **Audit Log Mini-feed**

**Expected Result:**
- Last 5 high-privilege audit events are shown newest first.
- Each row shows action, actor, entity and timestamp using `AppDatePipe` dateTime format.
- Delete/deactivate/revoke actions are red; update/change/grant actions amber; others accent.
- If no matching audit rows exist, all-clear message is shown.

**Status:** NOT TESTED

---

## TC-DASH-T4-006: Default role seed applies

**Preconditions:**
- Fresh database with V148 applied
- User has no personal dashboard override

**Steps:**
1. Login as ADMIN and open `/dashboard`
2. Login as CASHIER and open `/dashboard`
3. Login as FRONT_OFFICE and open `/dashboard`

**Expected Result:**
- ADMIN-class roles see all four Tier-4 widgets.
- CASHIER sees `anomaly-banner`.
- FRONT_OFFICE sees `capacity-alert`.

**Status:** NOT TESTED

---

## TC-DASH-T4-007: Mobile and theme parity

**Preconditions:**
- Tier-4 widgets visible on dashboard

**Steps:**
1. Test at 360 × 740 viewport
2. Toggle dark mode and light mode

**Expected Result:**
- No horizontal page scroll.
- Alert copy and badges remain readable.
- Red/amber/green severity colours remain visible in both themes.

**Status:** NOT TESTED

---

## TC-DASH-T4-008: Permission gating and resilience

**Preconditions:**
- A test role lacks the required permissions for one or more alert endpoints

**Steps:**
1. Add Tier-4 widgets to the test role dashboard config
2. Login as that role
3. Open `/dashboard`

**Expected Result:**
- Unauthorized widgets show `widget-error` and do not crash the dashboard.
- Other widgets continue loading independently.

**Status:** NOT TESTED

