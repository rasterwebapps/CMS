# Mobile Compatibility Matrix

_Last reviewed: **18 May 2026** (after P0 + P1 mobile responsiveness pass.)_

This document categorizes every screen in OneCMS by **mobile compatibility**.
A screen is considered **mobile-compatible** when, at a 360 × 740 viewport:

1. There is **no horizontal page scroll**.
2. All **primary actions are reachable** (no controls clipped off-screen).
3. **Tap targets are ≥ 40 × 40 CSS px**.
4. **Text remains readable** (≥ 14 px effective body size).
5. **Tables scroll horizontally** inside their container — never overflow the page.

> Going forward (per the project's [responsive-by-default rule](../.github/copilot-instructions.md#responsive-by-default-mandatory-for-all-new-ui)), **every new screen MUST be mobile-compatible at creation time**. This matrix exists only to track the legacy backlog.

---

## Legend

| Status | Meaning |
|--------|---------|
| 🟢 **Compatible** | Works correctly on phones with the global safety nets and component-level rules in place. |
| 🟡 **Acceptable** | Functional on phones but with minor friction (e.g. horizontal table scroll, dense filters). Listed here so they can be polished individually. |
| 🔴 **Not compatible** | Known to break on phones — requires component-level work before being declared safe. |
| ⚪ **Desktop-only by design** | Workflow is inherently large-screen (e.g. high-density data grids, multi-column dashboards). A simplified mobile variant should be planned but is not blocking. |

---

## Authentication & Shell

| Screen | Status | Notes |
|---|---|---|
| Login / Keycloak redirect | 🟢 | Handled by Keycloak's responsive theme. |
| App shell (toolbar, sidenav drawer) | 🟢 | Hamburger button added in P0; sidenav opens in `over` mode on phones. |
| Breadcrumb bar | 🟢 | Now horizontally scrollable on phones (safety net). |
| Global search | 🟡 | Pill shrinks fine; results panel could use full-width on small phones. |

## Dashboards

| Screen | Status | Notes |
|---|---|---|
| Admin Dashboard | 🟡 | Hero KPI grid stacks via existing `@media` rules. Some chart cards need 320 px polish. |
| Front Office Dashboard | 🟡 | Same as above; quick-action tiles wrap well. |
| Cashier Dashboard | 🟡 | Receipt list table requires horizontal scroll (now provided by safety net). |
| Faculty Dashboard | 🟡 | Compact layout already; minor tweaks recommended. |
| Student Dashboard | 🟢 | Already follows a single-column phone layout. |
| Parent Dashboard | 🟢 | Same as student dashboard. |

## Master Data — List Screens (MLP pattern)

All MLP list screens benefit from the global safety nets: toolbar stacks, headers stack, table scrolls horizontally, card actions stay visible.

| Screen | Status | Notes |
|---|---|---|
| Departments | 🟢 | Card view defaults; table view scrolls. |
| Programs | 🟢 | |
| Courses | 🟢 | |
| Academic Years | 🟢 | |
| Subjects | 🟢 | |
| Fee Heads | 🟢 | |
| Fee Structures | 🟡 | Wide fee matrix → horizontal scroll on phones. Consider card view on phone. |
| Faculty | 🟢 | |
| Agents | 🟢 | |
| Referral Types | 🟢 | |
| Scholarships | 🟢 | |
| Users (Admin) | 🟢 | |

## Master Data — Add / Edit Forms

| Screen | Status | Notes |
|---|---|---|
| Department form | 🟢 | Two-column `entry-form-layout` collapses ≤ 900 px. |
| Program form | 🟢 | |
| Course form | 🟢 | |
| Academic Year form | 🟢 | |
| Semester form | 🟢 | |
| Subject form | 🟢 | |
| Fee Head form | 🟢 | |
| Fee Structure form | 🟡 | Wide matrix of fee components — usable on phones but designed for desktop. |
| Faculty form | 🟢 | |
| Agent form | 🟢 | |
| Referral Type form | 🟢 | |
| Scholarship form | 🟢 | |

## Admission Management

| Screen | Status | Notes |
|---|---|---|
| Enquiry list | 🟢 | |
| Enquiry form (new / edit) | 🟢 | `field-row` collapses ≤ 560 px (component) + ≤ 640 px (global). |
| Enquiry detail | 🟡 | Split-pane (`.detail-split`) collapses to single column ≤ 900 px. |
| Enquiry → convert | 🟢 | `field-row` collapses ≤ 640 px. |
| Admission list | 🟢 | |
| Admission form | 🟢 | `field-row` mobile breakpoint added in P1. |
| Admission detail | 🟡 | Document checklist and receipts stack well; long descriptions may wrap aggressively. |
| Document upload | 🟢 | File picker is native — works on phones. |

## Finance / Cashier

| Screen | Status | Notes |
|---|---|---|
| Fee collection (search + collect) | 🟡 | Two-step flow works; receipt preview is wide. |
| Receipt list | 🟢 | Table scrolls horizontally on phones. |
| Receipt detail | 🟢 | |
| Refunds | 🟡 | Multi-column form — usable, room for polish. |
| Daily collection report | ⚪ | Designed for desktop print/export; mobile is read-only. |
| Outstanding fees report | ⚪ | High-density data grid — desktop-first by design. |

## Academics

| Screen | Status | Notes |
|---|---|---|
| Faculty subject mapping | 🟡 | Drag/drop list works on touch but is dense. |
| Timetable | ⚪ | Grid is inherently wide; mobile users see a horizontally scrollable week. |
| Attendance entry | 🟡 | Roll-call list works on phones; long rosters require scrolling. |
| Marks entry | 🟡 | Same as above. |
| Result publication | 🟢 | Read-only summary cards. |

## Lab / Resource Management

| Screen | Status | Notes |
|---|---|---|
| Lab list | 🟢 | |
| Lab detail | 🟡 | Equipment list table scrolls horizontally. |
| Equipment list | 🟢 | |
| Equipment form | 🟢 | |
| Lab schedule | ⚪ | Calendar view is wide; planned simplified mobile view. |
| Safety / compliance logs | 🟡 | Forms are long — sticky footer ensures Save is reachable. |

## Reports

| Screen | Status | Notes |
|---|---|---|
| All tabular reports | ⚪ | High-density grids — designed for desktop / export. Mobile shows scrollable table. |
| Chart-only reports | 🟢 | Cards stack naturally. |

## Settings

| Screen | Status | Notes |
|---|---|---|
| Profile | 🟢 | |
| Theme picker | 🟢 | |
| Notification preferences | 🟢 | |
| Audit log viewer | ⚪ | Desktop-first; mobile shows scrollable table. |

---

## How to upgrade a "🟡" or "🔴" screen

1. Read the [Responsive-by-Default rule](../.github/copilot-instructions.md#responsive-by-default-mandatory-for-all-new-ui).
2. Test the screen at a 360 × 740 viewport in Chrome DevTools.
3. Apply the relevant patterns from the global safety nets (`styles.scss`, section "GLOBAL RESPONSIVE / MOBILE SAFETY NETS").
4. If the screen has a card alternative to its table view, default to **card view on mobile** via `ResponsiveService.isMobile()`.
5. Add a manual test case under `docs/manual-test-cases/` validating the mobile layout.
6. Move the screen to 🟢 in this matrix.

## How to handle a "⚪" desktop-first screen

These screens are intentionally large-screen workflows (timetables, dense reports, audit logs). The expectation is:

- On phones, content remains **viewable** (horizontal scroll allowed).
- A **summary card view** or **export to PDF** option may be added as a phone-friendly alternative.
- The full power user workflow stays on tablet+ devices.

