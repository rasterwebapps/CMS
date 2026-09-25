# Business Requirements Document — Term Lifecycle Confirmation & Overdue Alerting

**Module slug:** `term-lifecycle-management`
**Source BR:** BR-53 · **Milestone:** R1-M6.3

## 1. Executive Summary / Business Objective

Prevent a term from silently stalling at `PLANNED` past its start date — the exact failure found
during a live debugging session, where `2025-2026`'s terms sat unopened while `2026-2027`'s were
already `LOCKED`, with nothing in the system surfacing it. Two changes close this gap: an informed
consequence-confirmation checklist before an admin advances a term's status, and a daily overdue
alert. This is also the first real, end-to-end slice of BR-28's long-pending notification-sending
backend.

## 2. Stakeholders

- **Academic admin** — the only actor who advances term status and the primary audience for
  overdue alerts (via `ACADEMIC_YEAR_MANAGE`).
- **Fee office** — indirectly surfaced via the `OPEN → LOCKED` checklist's outstanding fee-demand
  count.
- **Timetable/Academic coordinators** — indirectly surfaced via the draft-timetable-session count
  shown before locking.
- **Exam cell** — indirectly referenced via the self-attested "exam results published" checklist
  item (no system integration exists yet).

## 3. Business Rules

| ID | Rule | Rationale |
|---|---|---|
| BR-TERM-1 | Advancing a term's status (`PLANNED→OPEN` / `OPEN→LOCKED`) requires confirming an itemized checklist, not a single warning paragraph. | Upgraded 2026-08-11 because how much now depends on term status (course offering generation, fee eligibility, timetable freezing on LOCKED) outgrew one paragraph. |
| BR-TERM-2 | Checklist data is system-verified wherever a reliable signal exists (curriculum coverage, outstanding fees, draft timetable sessions); items with no reliable signal (exam-result publication) are plainly labeled self-attestation, never disguised as verified. | Honesty about what the system actually knows vs. what it's trusting the admin to confirm. |
| BR-TERM-3 | The hard block is on ticking every item + a separate final acknowledgment — never on the underlying checks themselves passing. | Two of the real dependencies have no reliable system signal; a lockout tied to incomplete logic would be a worse failure mode than trusting the admin's judgment once shown the real numbers, especially since both transitions are irreversible either way. |
| BR-TERM-4 | Academic Year *creation* is not itself gated by any confirmation. | Nothing depends on year creation until a term inside it is actually advanced. |
| BR-TERM-5 | A daily job (06:00 cron) raises an in-app alert for any term still `PLANNED` within 14 days of its start date. | Directly closes the discovered gap — the earlier failure mode is now proactively surfaced instead of found by accident. |
| BR-TERM-6 | Alerts are idempotent per term (DB-level partial unique index on active rows) and auto-resolve once the term is advanced, regardless of dismissal state. | Prevents the daily job from spamming duplicate alerts, and ensures the alert always reflects current reality rather than a stale dismissal. |
| BR-TERM-7 | Notifications are broadcast-style (one row per alert instance); per-user dismissal is tracked separately so each admin can independently dismiss their own copy without hiding it from others. | An alert relevant to the whole admin team shouldn't disappear for everyone because one person dismissed it. |

## 4. Business Process / Workflow

1. **Daily job:** `AcademicTermAlertService.checkOverdueTerms()` runs at 06:00. It (a) raises a new
   `academicTermAlerts` notification for any `PLANNED` term whose start date is within 14 days and
   has no existing active alert, and (b) resolves any active alert whose term has since moved past
   `PLANNED`.
2. **Admin sees the alert:** the toolbar bell shows an unread count; opening it lists visible
   notifications (filtered by the admin's own category preference and, for this category,
   `ACADEMIC_YEAR_MANAGE`). The admin can dismiss their own copy or click through to the linked
   Academic Year edit screen.
3. **Admin advances the term:** clicking the status-advance action opens
   `TermAdvanceChecklistDialogComponent`, which fetches live checklist data
   (`GET /term-instances/{id}/advance-checklist`) scoped to the target status.
   - Opening `PLANNED → OPEN`: shows any active cohort with no curriculum version mapped (would
     silently generate zero offerings), or a single "all clear" line.
   - Locking `OPEN → LOCKED`: shows outstanding fee-demand count/amount, unapproved draft
     timetable-session count, and a self-attested "exam results published and finalized" line.
4. Admin ticks every relevant item and the final acknowledgment checkbox; Confirm becomes enabled
   only then. Confirming calls the existing `PUT` term-instance status-update endpoint (no new
   permission — same gate as before).
5. The transition applies its existing downstream effects (course offering generation on `OPEN`,
   offering deactivation on `LOCKED`) unchanged by this module — the checklist only adds an
   informed-confirmation gate in front of the same transition.

## 5. Success Criteria

Not formally defined with a KPI — inferred from feature completeness: a term can no longer be
advanced without the admin being shown live, real numbers about the consequence, and a term
silently overdue for advancement now proactively surfaces an alert instead of requiring someone to
notice by accident (the original discovered failure mode).

## 6. Assumptions & Constraints

- Both `PLANNED → OPEN` and `OPEN → LOCKED` remain fully irreversible in the backend — this module
  adds visibility and confirmation, not reversibility.
- `academicTermAlerts` reuses the existing `ACADEMIC_YEAR_MANAGE` permission; no new RBAC surface
  was introduced.
- The feed/dismiss/preferences endpoints are self-service (any authenticated user), matching the
  pre-existing `/notifications/preferences` precedent — not specific to academic admins, since any
  authenticated user could in principle hold a category preference.

## 7. Known Gaps / Deferred

- **Email/other delivery channels** — in-app only for now.
- **Role-filtered visibility for other BR-28 notification categories** — unimplemented; only
  `academicTermAlerts` is wired end-to-end.
- **Real-time push** — feed loads once on app init, no live refresh.
- Notification infrastructure (`Notification`/`NotificationDismissal`) has since been extended with
  a `recipientFacultyId` column for targeted (non-broadcast) alerts used by a later, unrelated
  feature (holiday disruption notices) — confirms this module's tables are now general
  infrastructure rather than single-purpose, but that consumer is out of this module's scope.
