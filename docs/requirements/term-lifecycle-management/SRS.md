# Software Requirements Specification — Term Lifecycle Confirmation & Overdue Alerting

**Module slug:** `term-lifecycle-management`
**Product:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Source BR:** BR-53 (docs/BUSINESS_REQUIREMENTS.md), Milestone R1-M6.3 (docs/RELEASE_1_MILESTONES.md, lines 669–685)
**Basis:** Reverse-engineered from shipped code as of 2026-09-24.

## 1. Introduction

### 1.1 Purpose
Documents two related additions to the `TermInstance` lifecycle (`PLANNED → OPEN → LOCKED`,
one-directional, no skipping, no reverse transitions): (1) a consequence-confirmation checklist
before advancing a term's status, and (2) a daily overdue-alert job plus the in-app notification
feed it is the first real slice of.

### 1.2 Scope
Covers `TermInstanceService.getAdvanceChecklist`, `AcademicTermAlertService`,
`Notification`/`NotificationDismissal`, `NotificationService` (feed/dismiss),
`TermAdvanceChecklistDialogComponent`, and the toolbar notification bell.

### 1.3 References
- `docs/BUSINESS_REQUIREMENTS.md` BR-53 (primary), BR-28 (the notification-sending backend this is
  the first shipped slice of)
- `docs/RELEASE_1_MILESTONES.md` R1-M6.3
- Change Log entries 2026-07-17 (original ship) and 2026-08-11 (checklist upgrade)

## 2. Overall Description

### 2.1 Product Perspective
Triggered by a real production incident: `2025-2026`'s term instances sat at `PLANNED` while
`2026-2027`'s were already fully `LOCKED` — an admin never advanced the earlier year on schedule
and nothing in the system surfaced that. This module closes that visibility gap without making the
underlying transitions themselves any less irreversible.

### 2.2 Actors / User Classes
- **Academic admin** (`ACADEMIC_YEAR_MANAGE`) — advances term status, sees `academicTermAlerts`
  notifications.
- **Any authenticated user** — can view/dismiss their own notification feed (self-service,
  `isAuthenticated()` only; category visibility is filtered server-side by permission +
  preference).

### 2.3 Operating Environment
Angular (`frontend/src/app/app.ts` toolbar bell,
`features/academic-year/term-advance-checklist-dialog`), Spring Boot
(`com.cms.service.AcademicTermAlertService`, `NotificationService`,
`com.cms.controller.NotificationPreferenceController`, `TermInstanceController`), PostgreSQL via
Flyway (V287).

### 2.4 Constraints / Assumptions
- Neither `PLANNED → OPEN` nor `OPEN → LOCKED` has a reverse path anywhere in the backend —
  `validateStatusTransition` only permits forward moves, never backward, never skipping a stage.
- The checklist is **deliberately not a hard block** on its underlying checks passing — only on
  ticking every item plus a separate final acknowledgment. Two of the real dependencies (exam
  results published, fees "finalized") have no reliable system signal anywhere in the codebase; a
  lockout tied to incomplete logic was judged a worse failure mode than trusting the admin's
  judgment once shown the real numbers.
- Notifications are broadcast-style (one row per alert instance, not per-user); per-user state is
  tracked only via a separate dismissal table.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-TERM-1 | Advancing a term's status requires opening a checklist dialog first — the action is disabled until every item is ticked and a final "I acknowledge this is permanent" checkbox is checked. | Must | — |
| FR-TERM-2 | `GET /term-instances/{id}/advance-checklist?targetStatus=` returns live, system-verified data specific to the target status (empty/zero for irrelevant fields). | Must | — |
| FR-TERM-3 | For `PLANNED → OPEN`: lists every active cohort with no curriculum version mapped (would silently generate zero offerings); a clean pass shows one "all clear" confirmation row instead. | Must | `CourseOfferingService.findActiveCohortsWithoutCurriculumVersion` |
| FR-TERM-4 | For `OPEN → LOCKED`: shows live outstanding fee-demand count/amount for the term, live unapproved (DRAFT) timetable session count, plus one self-attested "exam results published and finalized" row (no system signal exists for this). | Must | `FeeDemandService.getOutstandingDemands`, `ClassScheduleRepository` |
| FR-TERM-5 | Clean (non-warning) checklist items are pre-checked and collapsed into an "All clear" group by default; warning and self-attested items require the admin's own explicit click. | Should | FR-TERM-1 |
| FR-TERM-6 | None of the checklist items block the transition on their own — only "every item ticked + final acknowledgment" gates the Confirm button. | Must | FR-TERM-1 |
| FR-TERM-7 | A daily job (`0 0 6 * * *`) raises an in-app `academicTermAlerts` notification for any `TermInstance` still `PLANNED` within 14 days of its `startDate`. | Must | `Notification` |
| FR-TERM-8 | Alert raising is idempotent per term — a partial unique index (`source_type, source_id, category_key WHERE resolved_at IS NULL`) guarantees at most one active alert per term. | Must | FR-TERM-7 |
| FR-TERM-9 | The alert auto-resolves once the term is advanced past `PLANNED`, regardless of whether anyone dismissed it first. | Must | FR-TERM-7 |
| FR-TERM-10 | `GET /notifications/feed` returns the caller's visible, undismissed notifications (category preference + permission-gated + not-yet-dismissed-by-this-user). | Must | — |
| FR-TERM-11 | `POST /notifications/{id}/dismiss` records a per-user dismissal without affecting other users' visibility of the same broadcast alert. | Must | FR-TERM-10 |
| FR-TERM-12 | Toolbar notification bell shows a real unread count and dropdown feed with per-item dismiss, replacing a previously hardcoded, always-zero badge. | Must | FR-TERM-10 |
| FR-TERM-13 | Academic Year *creation* itself is not gated by any confirmation — nothing depends on it until a term inside it is advanced. | Must | — |

## 4. External Interface Requirements

### 4.1 Screens
- `TermAdvanceChecklistDialogComponent` — opened from Academic Year form's `advanceTermStatus()`;
  title/intro text varies by target status ("Open Term" / "Lock Term"), grouped attention items
  (warnings + self-attested) vs. collapsible "all clear" items, per-item checkbox + final
  acknowledgment checkbox, Confirm disabled until `canConfirm()` (loaded, acknowledged, every item
  checked).
- Toolbar notification bell (`app.html`/`app.ts`) — unread count badge, dropdown feed, per-item
  dismiss.

### 4.2 API Endpoints (high level)
See FRD.md §5.

### 4.3 Key DB Entities
`notifications`, `notification_dismissals`; no schema change to `term_instances` for the checklist
itself (all checklist data is computed live from existing tables).

## 5. Non-Functional Requirements

- **Performance:** Checklist computation is scoped to one term instance per call (active cohorts /
  outstanding demands / draft sessions for that term only) — not an institution-wide scan.
- **Security/RBAC:** `academicTermAlerts` visibility reuses the existing `ACADEMIC_YEAR_MANAGE`
  permission (no new permission introduced); feed/dismiss/checklist endpoints are self-service
  (`isAuthenticated()`) or reuse the existing term-instance update permission — no new RBAC surface.
- **Auditability:** Notifications carry `created_at`/`resolved_at`; dismissals carry
  `dismissed_at` per user. No separate audit log beyond these timestamps.
- **Reliability:** Alert idempotency is enforced at the DB level (partial unique index), not just
  in application logic, so a job re-run or race cannot create duplicate active alerts.

## 6. Known Gaps / Not Yet Implemented

- **Email/other delivery channels** — in-app only; BR-28's `EMAIL`/`BOTH` channel option exists in
  the preference schema but nothing sends mail yet for any category, including this one.
- **Role-filtered category visibility for other BR-28 categories** (`feeAlerts`,
  `admissionUpdates`, `examSchedule`, `attendanceAlerts`, `systemAnnouncements`,
  `documentReminders`/`profileReminders`) — still unimplemented; this module only wires up
  `academicTermAlerts` end-to-end.
- **Real-time push** — the feed loads once on app init; no polling or websocket refresh.
- **Notification infrastructure has since been reused beyond this module** — `Notification` gained
  a `recipientFacultyId` column (per-faculty targeted alerts, e.g.
  `HolidayDisruptionNotificationService`) after BR-53 shipped; that consumer is out of this
  module's scope but confirms the table is now shared, general-purpose infrastructure, not
  single-purpose to term alerts.
