# Functional Requirements Document — Term Lifecycle Confirmation & Overdue Alerting

**Module slug:** `term-lifecycle-management`
**Source BR:** BR-53 · **Milestone:** R1-M6.3

## 1. Overview

Adds an itemized, system-verified confirmation checklist before a `TermInstance` status transition
and a daily job that proactively alerts admins when a term is still `PLANNED` too close to its
start date — plus the general-purpose in-app notification feed (`notifications`/
`notification_dismissals`) this feature is the first real end-to-end slice of.

## 2. Actors & Permissions

| Permission | Gates | Notes |
|---|---|---|
| `ACADEMIC_YEAR_MANAGE` | Visibility of `academicTermAlerts` notifications in the feed; the underlying `PUT` term-instance status-update endpoint | Reused, not new. |
| `isAuthenticated()` only | `GET /notifications/feed`, `POST /notifications/{id}/dismiss`, `GET/PUT /notifications/preferences`, `GET /term-instances/{id}/advance-checklist` (gated identically to the existing `PUT`) | Self-service by design — matches the pre-existing `/notifications/preferences` precedent; no dedicated RBAC permission for the feed/dismiss/checklist-read itself. |

## 3. Screens & UI Behavior

### 3.1 `TermAdvanceChecklistDialogComponent`
- Opened by `academic-year-form.component.ts`'s `advanceTermStatus()` in place of the old shared
  `ConfirmDialogComponent`.
- Title: "Open Term" / "Lock Term" based on `targetStatus`. Intro lines describe the concrete
  consequence (offerings generated and made available for registration/fees on OPEN; offerings
  deactivated and timetable frozen — placement/staffing/approve/discard/revert-to-draft all stop
  working — on LOCKED) and state plainly that the action cannot be undone.
- Items split into two groups: **attention** (`warn: true` or `warn: null`/self-attested) shown
  expanded, and **clear** (`warn: false`) collapsed by default under a toggle, pre-checked
  automatically since there's nothing to review.
- Each attention item requires its own explicit checkbox click; a separate final "I acknowledge
  this is permanent" checkbox is always required regardless of item count.
- Confirm button (`canConfirm()`) enables only when: not loading, acknowledged is checked, and
  every item (including the pre-checked clear ones) is checked.
- On confirm, the dialog simply closes with `true`; the caller then proceeds with the existing
  `PUT` status-update call — the dialog does not itself call `advance-checklist`'s underlying
  transition endpoint beyond the initial GET.

### 3.2 Toolbar notification bell (`app.ts`/`app.html`)
- `notificationCount` = `notificationService.feed().length` (computed signal).
- Feed loaded once on app init (`loadFeed()`), silently ignoring fetch errors.
- Per-item dismiss button calls `dismiss(id)`, silently ignoring errors, and (implicitly via the
  service's signal update) removes the item from the visible feed.

## 4. Functional Workflows

**Daily overdue-alert job** (`AcademicTermAlertService.checkOverdueTerms`, cron `0 0 6 * * *`):
1. `raiseAlertsForPlannedTermsStartingSoon()` — for every `TermInstance` with `status = PLANNED`
   and `startDate <= today + 14 days`, if no active (`resolved_at IS NULL`) alert already exists for
   `(source_type='TERM_INSTANCE', source_id=term.id, category_key='academicTermAlerts')`, insert a
   new `Notification` with a link to `/academic-years/{academicYearId}/edit`.
2. `resolveAlertsForTermsNoLongerPlanned()` — for every currently-active `academicTermAlerts`
   notification, if its source term's status is no longer `PLANNED`, set `resolvedAt = now()`.

**Checklist fetch** (`GET /term-instances/{id}/advance-checklist?targetStatus=`):
1. Verify the term instance exists.
2. If `targetStatus == OPEN`: `cohortsWithoutCurriculum` =
   `courseOfferingService.findActiveCohortsWithoutCurriculumVersion()` display names; fee/timetable
   fields left at zero/empty.
3. If `targetStatus == LOCKED`: `outstandingFeeDemandCount`/`outstandingFeeDemandAmount` computed
   from `feeDemandService.getOutstandingDemands(termInstanceId)` (summed);
   `draftTimetableSessionCount` = count of `ClassSchedule` rows for the term with
   `status = DRAFT` and `isActive = true`; `cohortsWithoutCurriculum` left empty.

**Feed computation** (`NotificationService.getFeed`):
1. Resolve current user id; return empty list if unauthenticated (defensive — endpoint itself
   requires `isAuthenticated()`).
2. Load all `resolvedAtIsNull` notifications, newest first.
3. Filter out `academicTermAlerts` rows unless the caller holds `ACADEMIC_YEAR_MANAGE`
   (`PermSecurityBean.has`).
4. Filter out rows with a non-null `recipientFacultyId` that doesn't match the caller's own
   faculty id (per-faculty-targeted rows, e.g. holiday-disruption alerts — not used by this BR but
   present in the same table/service).
5. Filter by the caller's own category preference (`isCategoryEnabled`).
6. Filter out rows the caller has already dismissed.
7. Map to `NotificationResponse`.

**Dismiss** (`POST /notifications/{id}/dismiss`): resolve current user, no-op if the notification
doesn't exist or is already dismissed by this user, else insert a `NotificationDismissal` row.

## 5. API Endpoints

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| GET | `/term-instances/{id}/advance-checklist?targetStatus=` | — | `TermAdvanceChecklistResponse {targetStatus, cohortsWithoutCurriculum: [string...], outstandingFeeDemandCount, outstandingFeeDemandAmount, draftTimetableSessionCount}` | same gate as the existing `PUT` (no new permission) |
| GET | `/notifications/feed` | — | `List<NotificationResponse> {id, categoryKey, title, message, link, createdAt}` | `isAuthenticated()` |
| POST | `/notifications/{id}/dismiss` | — | 204 | `isAuthenticated()` |
| GET | `/notifications/preferences` | — | `List<NotificationPreferenceResponse>` | `isAuthenticated()` |
| PUT | `/notifications/preferences` | `NotificationPreferenceRequest` | `List<NotificationPreferenceResponse>` | `isAuthenticated()` |

## 6. Data Model

- **`notifications`** (V287): `id`, `category_key` (VARCHAR(50) NOT NULL), `title`
  (VARCHAR(200)), `message` (TEXT), `link` (VARCHAR(255)), `source_type`/`source_id` (nullable,
  identifies the underlying entity an alert is about), `recipient_faculty_id` (nullable — null
  means broadcast-to-category; non-null scopes to one faculty member, added for a later,
  out-of-scope consumer), `created_at`, `resolved_at` (nullable — null = active).
  - Partial unique index `uq_notifications_active_source` on `(source_type, source_id,
    category_key) WHERE resolved_at IS NULL` — idempotency guarantee.
  - Partial index `idx_notifications_category_unresolved` on `(category_key) WHERE resolved_at IS
    NULL`.
- **`notification_dismissals`** (V287): `id`, `notification_id` FK, `user_id` (VARCHAR(255)),
  `dismissed_at`. Unique on `(notification_id, user_id)`.
- No schema change to `term_instances` for the checklist — all checklist fields are computed live
  from `Cohort`, `FeeDemand`, and `ClassSchedule` data at request time.
- `academicTermAlerts` is registered in `NotificationPreferenceService.CATEGORY_DEFAULTS` (default
  enabled) alongside the pre-existing `systemAnnouncements`, `documentReminders`,
  `admissionUpdates`, `feeAlerts`, `examSchedule`, `attendanceAlerts` categories.

## 7. Edge Cases & Validation Rules

- The daily job never creates a duplicate active alert for the same term — enforced at the DB
  level via the partial unique index, not just application-side deduplication logic.
- An alert auto-resolves the moment its source term moves past `PLANNED`, **even if no one ever
  saw or dismissed it** — resolution is driven by term state, not by dismissal state.
- A `resolved_at` alert never reappears in anyone's feed even if a new `PLANNED` period were to
  somehow recur for the same term (status transitions are one-directional/no-skip, so this can't
  actually happen for the same term instance in practice).
- Checklist items with `warn: null` (self-attested) are visually distinct from `warn: true`
  (system-detected issue) in the dialog but both still require an explicit tick — only `warn:
  false` items are auto-checked.
- `getAdvanceChecklist` returns empty/zero for fields irrelevant to the requested `targetStatus`
  rather than `null`, so the frontend never has to null-check.
- Feed filtering order matters for correctness but not idempotency: permission gate → per-faculty
  scoping → category preference → per-user dismissal, each an independent `filter`, so any one
  false result hides the notification regardless of the others.

## 8. Known Gaps / Deferred

See SRS.md §6 and BRD.md §7: no email/other delivery channels; other BR-28 categories remain
unimplemented for role-filtered visibility; feed is load-once (no real-time push); the
`Notification` table has since grown a `recipientFacultyId` targeting mechanism for a later,
unrelated feature (holiday disruption notices), confirming it is now shared infrastructure beyond
this BR's original scope.
