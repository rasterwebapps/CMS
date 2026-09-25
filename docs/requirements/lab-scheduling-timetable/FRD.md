# Functional Requirements Document — Lab Scheduling & Timetable

**Module:** Lab Scheduling & Timetable (Timetable Builder / Class Schedule engine)
**App:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Companion documents:** `SRS.md`, `BRD.md` (same folder) — this FRD assumes their terminology and does not repeat their narrative; read them first.

This document is reverse-engineered from the shipped codebase as of this writing (backend `backend/src/main/java/com/cms/{controller,service,model,dto}/`, frontend `frontend/src/app/features/timetable/`, `frontend/src/app/features/lab-schedule/`, `frontend/src/app/features/faculty-availability/`, `frontend/src/app/shared/{week-grid,week-navigator,day-agenda}/`). Every permission code, endpoint, DTO field, and data-model column below was confirmed against the actual source (controllers, entity classes, and — for tables — the Flyway migration that created/altered them). Where the shipped code goes beyond what SRS.md/BRD.md describe (e.g. Clinical Shift Groups, Day Mapping Overrides), it is documented here and flagged as such, since code is the source of truth for this pass.

---

## 1. Overview

The Lab Scheduling & Timetable module is OneCMS's engine for scheduling every recurring Theory, Lab, Clinical, Library, and Sports session a nursing college runs, plus the ad-hoc/remedial classes, faculty-absence substitutions, room relocations, and staff swaps that come up after a term's timetable is published. It began (R1-M3.2) as a single-screen Lab Scheduling CRUD and has since grown into a two-layer engine:

- **`ClassSchedule`** (table `class_schedules`, originally `lab_schedules`, generalized to THEORY/LAB in V293 and CLINICAL in V334) — the recurring **weekly template**: day-of-week + period + term, no calendar date.
- **`SessionOccurrence`** (table `session_occurrences`, V322) — the **per-date actual**, created lazily only when something is logged, requested, or overridden for a specific date. Since V374/V529 this table also carries ad-hoc special/remedial classes, whole-day-repeat batches, weekly-recurring special classes, and Clinical Shift blocks that have **no** backing `ClassSchedule` row at all (see §6).

On top of this spine sit: a manual **Timetable Builder** (placement) → **Staffing** (faculty/room assignment) workflow with **Auto-place/Auto-staff** and a full-term **Global Auto-Schedule**; **Capacity Auto-Plan** (term-scoped exclusive room claims + batch creation); **Batch rotation** (N-way cyclical roster rotation for shared Lab/Clinical venues); a **draft → publish → lock** lifecycle with a per-cohort **Conflict Inspector**; post-publish **Faculty Absence/Substitute**, **Staff Session Swap**, and **Room Relocation** tools; the **Special/Remedial Class Scheduler**; **Portion-Completion** tracking (frozen Blueprint vs. live-recomputed Projected date); and **Holiday auto-block**/**Holiday Templates**. The legacy **Lab Schedules** CRUD screen still edits the same `class_schedules` rows directly.

Two capabilities exist in shipped code but are **not** covered by SRS.md/BRD.md's original scope statement and are documented here as additions: **Clinical Shift Groups** (`clinical_shift_groups` — off-campus shift-based Clinical rostering with real clock times instead of the Period grid) and **Day Mapping Overrides** (`/day-mappings`, e.g. "today runs Monday's timetable") — both directly consumed by Resource Timetable Grid's Date-mode view and the draft/published grids.

---

## 2. Actors & Permissions

### 2.1 Actors

| Actor | Primary interactions |
|---|---|
| **Timetable/Academic Coordinator** | Capacity Auto-Plan, Timetable Builder, Staffing, Publish/Discard/Revert, Conflict Inspector, Rotation Groups, post-publish adjustments (Faculty Absence, Staff Session Swap, Room Relocation), Special Class approval, Faculty Workload Rules, Holiday Templates |
| **Faculty** | My Timetable (Staff), Log Progress, Special/Remedial Class request (self), Recurring Unavailability (own standing weekly blocks) |
| **Student** | My Timetable (Student) — read-only |
| **HOD / Progress reviewer** | Progress Report (portion-completion), Special Class Approval Queue (if granted) |
| **Lab Schedule maintainer (legacy)** | Lab Schedules CRUD screen |

### 2.2 Permission codes (exact strings, grep-confirmed against controllers and migrations)

All permissions below are enforced via `@PreAuthorize("@perm.has('...')")`/`@perm.hasAny(...)` on the controller methods cited in §5. Per the project's operation-wise permission mapping rule, each distinct operation has its own code (no shared "Manage" catch-all across unrelated actions).

| Code | Gates |
|---|---|
| `TIMETABLE_VIEW` | Browse published timetable (`GET /timetables`), cohort-status summary, skeleton read, staffing read, elective schedule read |
| `TIMETABLE_MANAGE` | Draft review reads (`/timetables/draft`, `/draft/clinical-shift-summary`) |
| `TIMETABLE_PUBLISH` | Approve/publish a term's draft (combined with `TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE` when overriding a coverage gap) |
| `TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE` | Publish despite unplaced curriculum hours (`TimetableCoverageGapException` override) |
| `TIMETABLE_DISCARD_DRAFT` | Discard a cohort's draft |
| `TIMETABLE_DISCARD_PUBLISHED` | Revert a published cohort back to draft |
| ~~`TIMETABLE_CONFLICT_INSPECTOR_VIEW`~~ | **Removed 2026-09-24, corrected in this doc.** An earlier pass here said this was merely unused; on investigation it had already been deleted from the `permissions` table entirely by migration `V550__remove_conflict_inspector_permissions.sql` — this codebase's own established cleanup precedent for a retired screen's leftover permission, applied before this documentation set was even written. There was nothing to fix; only this doc was wrong. |
| `TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE` | Per-cohort "acknowledge conflicts" before publish (restored by uncommitted `V553`, see §8) |
| `TIMETABLE_SWAP` | Draft-stage `TimetableSwapService` swap (day/period-mutating) |
| `TIMETABLE_FACULTY_GRID_VIEW` / `TIMETABLE_CLASSROOM_GRID_VIEW` | Resource Timetable Grid (Faculty / Classroom-Lab-Clinical resource type) |
| `TIMETABLE_SKELETON_MANAGE` | Place / remove a Timetable Builder cell |
| `TIMETABLE_SKELETON_PIN` | Pin/unpin a draft cell |
| `TIMETABLE_SKELETON_REPLACE` | Replace a placed Theory cell's subject |
| `TIMETABLE_SKELETON_MOVE` | Move/swap/relocate a cell (drag gestures), including previews |
| `TIMETABLE_SKELETON_DUTY_DAY_MOVE` | Move a Clinical Shift group's duty day |
| `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` | Global Auto-Schedule (prerequisites/precheck/run) |
| `TIMETABLE_SKELETON_CLINICAL_DUTY_FIT` | Apply the minimum clinical-shift duration fix |
| `TIMETABLE_SKELETON_ELECTIVE_PLACE` | Place an elective group |
| ~~`TIMETABLE_SKELETON_AUTO_PLACE`~~ | **Removed 2026-09-24** by new migration `V558`, following the exact `V550` precedent above — its planned consuming service (`TimetableSkeletonAutoPlaceService`, named only in a javadoc comment) was never built, superseded before shipping by the broader `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` feature. `V558` also removed `TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE` in the same pass — see §7/§8. |
| `TIMETABLE_STAFFING_MANAGE` | Staff a single cell (assign faculty/room) |
| `TIMETABLE_STAFFING_AUTO_STAFF` | Auto-staff remaining unstaffed cells |
| `TIMETABLE_ROTATION_VIEW` / `TIMETABLE_ROTATION_MANAGE` | Rotation Groups read / create+delete |
| `TIMETABLE_COHORT_ROOM_ALLOCATION_VIEW` / `_MANAGE` / `_REVERT` | Capacity Auto-Plan: view current allocation / commit / revert |
| `TIMETABLE_CAPACITY_PLANNER_VIEW` | Capacity Planner reads (plan, faculty workload, term overview, venue capacity, rebalance preview) |
| `TIMETABLE_CAPACITY_PLANNER_REBALANCE` | Apply a venue rebalance |
| ~~`TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE`~~ | **Removed 2026-09-24** by `V558` — leftover from the manual "create batch" UI action and its `POST /batches`/`BatchService.createBatch` backend path, both already intentionally deleted as dead code per this project's "Batches are created by Capacity Auto-Plan alone" hard gate (OC-191). See §8. |
| `TIMETABLE_STAFF_SWAP` | Post-publish Staff Session Swap (candidates + apply) |
| `TIMETABLE_ROOM_RELOCATE` | Post-publish Room Relocation (candidates, relocate, revert) |
| `TIMETABLE_SPECIAL_CLASS_REQUEST` | Faculty submits a special/remedial/day-repeat/recurring class request |
| `TIMETABLE_SPECIAL_CLASS_VIEW` | Faculty's My Special Classes list |
| `TIMETABLE_SPECIAL_CLASS_APPROVE` | Approval queue, approve/reject (single or batch) |
| `TIMETABLE_SPECIAL_CLASS_CANCEL` | Cancel an approved special-class request |
| `TIMETABLE_WORKLOAD_RULES_VIEW` / `_MANAGE` | Faculty Workload Rules screen (read / edit daily-weekly-continuous caps) |
| `TIMETABLE_CLINICAL_SHIFT_VIEW` / `_MANAGE` | Clinical Shift Groups (read / create-update-delete-generate) |
| `TIMETABLE_DAY_MAPPING_VIEW` / `_MANAGE` | Day Mapping Overrides |
| `TIMETABLE_WORKING_SATURDAYS_MANAGE` | Configure which Saturdays a term treats as working days (on `TermInstanceController`, a dependency of this module) |
| `TIMETABLE_ESCORT_ROTATION_VIEW` / `_MANAGE` | Escort Rotation — adjacent feature, out of scope per SRS §1.2 |
| `TIMETABLE_GENERATE` | Orphaned — no controller reference. Left as-is (not removed alongside the three above): unlike those, no confirmed "superseded by X" or "intentionally removed per Y" story was found for this one — see §8. |
| `MY_TIMETABLE_VIEW` | Legacy unsplit "My Timetable" code, backfilled onto both split variants (V541) |
| `MY_TIMETABLE_VIEW_STUDENT` / `MY_TIMETABLE_VIEW_STAFF` | `/my-timetable/student` / `/my-timetable/staff` |
| `LAB_SCHEDULE_VIEW` / `_CREATE` / `_EDIT` / `_DELETE` / `_EXPORT` / `_MANAGE` | Legacy Lab Schedules CRUD screen (nav gate uses all six; write endpoints gate on `_MANAGE`; read endpoints now gate on `_VIEW` or `_MANAGE`, fixed 2026-09-24 — see §7) |
| `FACULTY_ABSENCE_MARK` | Mark a faculty absent for a date |
| `FACULTY_ABSENCE_SUBSTITUTE_APPLY` | Find/apply a substitute for an absence-affected session |
| `FACULTY_AVAILABILITY_VIEW` / `_MANAGE` | Recurring Unavailability screen (read / add-remove blocks) |
| `HOLIDAY_TEMPLATE_VIEW` / `_MANAGE` | Holiday Templates — all `GET` endpoints on `HolidayTemplateController` now gate on `_VIEW` or `_MANAGE` (fixed 2026-09-24; previously unguarded — see §7), `/name-exists` requires `_MANAGE` |
| `PORTION_BLUEPRINT_MANAGE` | Generate a course offering's portion-completion Blueprint |
| `PROGRESS_REPORT_VIEW` | Progress Report screen, shortfall check, blueprint/projection read (shared with `PROGRESS_LOG_CREATE`) |
| `PROGRESS_LOG_CREATE` | Log Progress dialog (staff, per-occurrence unit coverage) |
| `PORTION_PLAN_VISIBLE_TO_FACULTY` | A `SystemConfiguration` toggle, not a permission — exists (V354) but **gates nothing in shipped code**, frontend or backend (see §7/§8) |

---

## 3. Screens & UI Behavior

### 3.1 My Timetable — `features/timetable/my-timetable/`
One component serves both `/my-timetable/student` (`MY_TIMETABLE_VIEW_STUDENT`) and `/my-timetable/staff` (`MY_TIMETABLE_VIEW_STAFF`), distinguished by `route.data['audience']`; Angular recreates the component on navigation between the two routes.
- **Filters:** Academic Year → Term cascade; view-mode toggle **Generic / Date-wise / Day** (`TimetableViewMode = 'week' | 'dateWise' | 'day'`); "Week of" date (Generic), `cms-week-navigator` (Date-wise), a date picker (Day) — all clamped to the selected term's `startDate`/`endDate`, with a mid-week term start skipping to the next Monday rather than showing a misleading partial week.
- **Validation:** none (read-only browse screen); no `uniqueFieldValidator` use (not applicable to a browse screen).
- **Staff-only Log Progress:** shown when `audience === 'STAFF' && permission('PROGRESS_LOG_CREATE')`; opens `LogProgressDialogComponent` (640px) seeded with `classScheduleId`, subject, term start date, and a suggested (non-enforced) period-hours value.
- **Rendering:** shared `cms-week-grid` (Generic/Date-wise) or `cms-day-agenda` (Day); `mat-spinner` loading per view mode; empty states via `cms-empty-state` (icon `calendar_today`).

### 3.2 Shared grid components (`frontend/src/app/shared/`)
- **`week-grid` (`cms-week-grid`):** day-columns × period-rows session chips (`.session-chip`); a mini term-number badge (`.session-chip__term`, "T{n}"); holiday/"Not in Term" badges; swap-mode candidate/dimmed cell states. Also drives a `mode="review"` toolbar (Generate/Regenerate, Discard, Publish, Revert to Draft) used by the draft-review flow. All referenced modifier classes (`--draft`, `--unstaffed`, `--swap-source`, `--readonly`, `--cancelled`, `--substituted`, `--holiday`, `--candidate`, `--dimmed`, `--shift`) are defined in the component's own `.scss` — no undefined-class defect found.
- **`day-agenda` (`cms-day-agenda`):** period-row agenda list with cancelled/substituted badges; an optional Relocate button (`allowRoomRelocate`) emitting `relocateRoomClick`.
- **`week-navigator` (`cms-week-navigator`):** Prev/Next/Today control; `canGoPrevious` checks the *previous week's Saturday* against `min` (not Monday), so paging back to a term's real partial first week still works when the term starts mid-week.

### 3.3 Resource Timetable Grid — `features/timetable/resource-timetable-grid/` (+ `resource-week-modal/`)
Cross-cohort, per-resource comparison grid — one full day at a time, all active resources of the chosen type as rows, Periods as columns.
- **Filters:** Academic Year, Term, Faculty/Classroom-Lab-Clinical resource-type toggle (`TIMETABLE_FACULTY_GRID_VIEW`/`TIMETABLE_CLASSROOM_GRID_VIEW`), single-resource filter, and a **Date vs. Weekday (planning)** view-mode toggle — Date mode resolves through Day Mapping Overrides for the real calendar date; Weekday mode is the pure recurring template. Date picker clamped to the selected term.
- **Off-grid entries** (e.g. Clinical Shift duty windows with no `periodId`) render as a spanning segment across the periods they overlap.
- **Row click → "Full Week" drill-in:** opens `ResourceWeekModalComponent` (1200px), that resource's full Mon–Sat week across every cohort via `cms-week-grid`. Confirmed in current code: a term-number chip (`resource-grid__chip-term`, styled with `var(--cms-text-secondary)` text on a color-mixed background — the low-contrast fix is present) and the modal's default week start is clamped to `termStartDate`/`termEndDate` (not "today"), preventing a term's recurring sessions from appearing under real dates before the term starts.
- No uniqueness validators (browse screen). No undefined badge/style classes found.

### 3.4 Timetable (general browse) — `features/timetable/timetable-view/`
- **Filters:** Academic Year → Term (query-param-seedable), **Cohort** selector (loaded college-wide, since the grid itself has no cohort dimension), Faculty/Room/Batch filters (options derived from both `sessions` and `occurrences` so a substituted occurrence's stand-in faculty/room still appears after a view-mode switch).
- **View-mode toggle:** labeled exactly "Generic" / "Date-wise" / "Day" in the UI (SRS/BRD's "Date-wise-weekly" appears only in code comments, not user-facing text).
- Guards against an out-of-order response (an unfiltered all-cohorts load racing a subsequent cohort-scoped load) via a captured `requestedCohortId` comparison.
- **Day view only:** a Relocate action (`TIMETABLE_ROOM_RELOCATE`) opening `RoomRelocationModalComponent` (440px); Week view has no relocate (no date concept to relocate against).
- Renders through the same shared `cms-week-grid`/`cms-day-agenda` as My Timetable — same badge/status classes, all defined.

### 3.5 Timetable Builder — `features/timetable/timetable-builder/` (structural summary; workflow in §4)
Uses Angular CDK drag-drop (`CdkDragDrop`) for cell placement/move/swap. Sub-dialogs: `hours-progress-card`, `timetable-cell-reassign-faculty-dialog`, `timetable-cell-replace-dialog`, `timetable-cell-swap-dialog`, `coverage-override-dialog`, `global-auto-schedule-report-flyout`, `working-saturdays-flyout`, plus a shared `ConfirmDialogComponent` for destructive/override confirmations. Uses `CmsStatusBadgeComponent` for cohort status and `CmsRowActionButtonComponent` for row actions. Consumes the `StaffingService` (see §3.6) directly — Staffing is not a separate routed screen.

### 3.6 Staffing — `features/timetable/staffing/`
**No standalone routed screen exists.** The folder contains only `staffing.model.ts` and `staffing.service.ts`, consumed directly by Timetable Builder (and by the Global Auto-Schedule report flyout and the cell-reassign-faculty dialog). Staffing is a capability embedded in Timetable Builder, not an independent nav entry or route — consistent with nav-config's comment that "no standalone Staffing nav entry" exists because Approve auto-staffs the draft and any leftover unstaffed cell is fixed in-grid.

### 3.7 Lab Schedules (legacy CRUD) — `features/lab-schedule/lab-schedule-list/`, `lab-schedule-form/`
- **Form validation:** plain `Validators.required`/`Validators.maxLength(100)` plus conditional requiredness of `classroomId`/`clinicalVenueId`/`labId`/`batchName` depending on `sessionType`. **No `uniqueFieldValidator`/name-exists check** — confirmed by grep across the whole `features/` tree (used in ~19 other master screens, e.g. inventory, referral-type, clinical-venue, program, period, but not here). This is a deliberate finding, not an oversight to silently fix: a `ClassSchedule` row has no name/code field for uniqueness to apply to — its own de-duplication is the `check-conflicts` endpoint (day/period/room/faculty clash detection), which is a different validation shape than the master-screen uniqueness pattern.
- **List screen:** `MatTableDataSource` with client-side search, `MatSort`, `MatPaginator`, a persisted column picker, pinnable/resizable columns. Delete via `ConfirmDialogComponent`. Columns: Day, Type, Room, Subject, Code, Faculty, Batch, Start, End, Actions. Write actions gated on `LAB_SCHEDULE_MANAGE`.

### 3.8 Special Classes — `features/timetable/special-classes/`
- **My Requests** (`my-requests-list`): faculty's own requests, `approvalStatus` rendered via `<cms-status-badge [status]="...">` (`PENDING/APPROVED/REJECTED/CANCELLED` all map to defined global classes: `APPROVED → status-active`, `PENDING → status-pending`, `REJECTED|CANCELLED → status-inactive`). "New Request" opens `SpecialClassRequestFlyoutComponent`.
- **Approval Queue** (`approval-queue-list`): a `DAY_REPEAT`/`RECURRING_SPECIAL_CLASS` row's approve/reject acts on its whole `requestBatchId` (batch endpoints); a single-subject row approves/rejects individually. Reject uses a reason-entry side panel (`cms-flyout-panel` pattern).
- **Request Flyout:** two modes, `SINGLE_SUBJECT` and `DAY_REPEAT`, one Academic Year → Term → Cohort cascade; a consecutive-period range picker (backend requires an unbroken period block); deep-link prefill from Global Auto-Schedule's "last remaining subject" alert; an "Approve now" checkbox shown only when the requester also holds `TIMETABLE_SPECIAL_CLASS_APPROVE` (still issues a separate, audited approve call).
- **Log Special Class Progress Dialog:** separate dialog for logging progress against an approved special-class occurrence.

### 3.9 Faculty Absence, Room Relocation, Staff Session Swap (post-publish tools)
- **Faculty Absence** (`features/timetable/faculty-absence/`): single-page flow — pick faculty → mark absent (date + optional reason) → affected sessions for that date auto-load → per-session "Find Substitute" fetches candidates → Apply reassigns. No dialogs; sequential signal-driven loading states.
- **Room Relocation** (`room-relocation-modal`, 440px dialog): single-date-only; lists only conflict-free venue candidates (backend pre-filters); "Reset to recurring room" is a harmless no-op if the occurrence's SUBSTITUTED status actually came from a faculty swap rather than a room override.
- **Staff Session Swap** (`features/timetable/staff-session-swap/`): Academic Year → Term → Date → that day's occurrences → "Find swap partners" per session → apply swaps faculty between two sessions for that single date only.

### 3.10 Faculty Workload Rules — `features/timetable/faculty-workload-rules/`
Edits exactly three global fields (`maxDailyHours`, `maxWeeklyHours`, `maxContinuousHours`, all nullable = "no cap"), backed by `timetable.faculty_max_*_hours` config keys. Client validation blocks save on any negative value. Deliberately does not duplicate per-designation/per-faculty override editing — links out to Designation Master / Faculty Master instead.

### 3.11 Progress Report — `features/timetable/progress-report/`
Portion-completion "blueprint" (Planned vs. Projected/Actual) variance view; Blueprint generation gated by `PORTION_BLUEPRINT_MANAGE`. The `PORTION_PLAN_VISIBLE_TO_FACULTY` config flag is referenced only in a code comment here, explaining that this screen is admin/HOD-only via `PROGRESS_REPORT_VIEW` so the flag has nothing to gate on this surface — it is not wired to any conditional logic anywhere in the current frontend (see §8).

### 3.12 Capacity Auto-Plan / Capacity Planner / Venue Rebalance Panel
- **Capacity Auto-Plan** (`features/timetable/capacity-auto-plan/`): cohort auto-plan proposal screen — a grouped Room Inventory chip grid (Classrooms/Labs/Clinical Venues), editable draft section cards (server-suggested section, room/size swappable), an "Adjust manually" link out to the full Capacity Planner.
- **Capacity Planner** (`features/timetable/capacity-planner/`): allocation cards using a local `.cap-fw-badge` system (`--ok`/`--warn`/`--danger`, defined locally) for hours-fit indicators, plus the global `.cms-badge--green/--blue/--violet/--gray` classes for session-type chips (THEORY/LAB/CLINICAL/Inactive) — all confirmed defined.
- **Venue Rebalance Panel** (`cms-venue-rebalance-panel`): expandable inline panel gated by rebalance permission; preview → confirm → apply flow (`TIMETABLE_CAPACITY_PLANNER_REBALANCE` for apply, `_VIEW` for preview).

### 3.13 Recurring Unavailability — `features/faculty-availability/` (route `/faculty-availability`)
Grid: Faculty picker × (Day-of-week columns × Period rows), cells toggle blocked/available (`FACULTY_AVAILABILITY_MANAGE` gates writes). Clicking a free cell opens `BlockAvailabilityDialogComponent` (420px) collecting reason + optional date range; clicking a blocked cell opens a `ConfirmDialogComponent` "Unblock This Period" showing the original reason/range. Hard-block conflicts (every affected class) are shown via a non-auto-dismissing toast (`durationMs: 0`). A tooltip per cell shows the block's reason and date range.

### 3.14 Badge/status audit result (module-wide)
Every template across the screens above was checked against its own `.scss` and the global `frontend/src/styles.scss` (`.cms-badge`/`.status-badge` blocks, with light/dark theme variables). **No undefined or orphaned badge/status class was found anywhere in this module as of the current code state** — every `cms-badge--*`, `status-*`, `session-chip--*`, `week-grid__*`, `resource-grid__*`, and `cap-fw-badge--*` class referenced has a matching definition. This suggests the module's known "missing-class" defect pattern (documented elsewhere in this project's standards) does not currently affect Timetable — consistent with recent commits specifically fixing low-contrast/undefined badge styling in Resource Timetable Grid.

---

## 4. Functional Workflows

### 4.1 Capacity Auto-Plan → commit a cohort's term room allocation
1. Coordinator opens Capacity Auto-Plan (or Capacity Planner) for a Cohort + Term Instance, choosing `planningBasis` (`ENROLLED` live headcount or `SANCTIONED` university intake).
2. Reviews the system-suggested Theory section split (`CohortSectionRequest[]`: label, classroom, planned size) and Lab/Clinical venue splits (`VentureSplitRequest[]`: course offering, session type, venue, batch name, planned size, owning section label).
3. `POST /timetables/cohort-room-allocations/commit` (`TIMETABLE_COHORT_ROOM_ALLOCATION_MANAGE`) creates one `COMMITTED` `CohortRoomAllocation` row (DB-enforced: exactly one active `COMMITTED` row per cohort+term, and one cohort per Theory classroom per term — see §6) plus one `CohortSection` per section and one `Batch` per venture split via `CohortRoomAllocationService.createVentureBatch` — **the only legitimate path that creates a `Batch`** (no manual create-batch UI/endpoint exists; see §7).
4. Revert (`POST /{id}/revert`, `TIMETABLE_COHORT_ROOM_ALLOCATION_REVERT`) soft-deactivates the allocation (status → `REVERTED`); it never deletes, so roster history survives.

### 4.2 Timetable Builder — placement
1. Coordinator selects Term + Cohort; `GET /timetables/skeleton` loads the cohort's current draft grid (`SkeletonBuilderResponse`).
2. **Manual placement:** `POST /timetables/skeleton/cells` (`TIMETABLE_SKELETON_MANAGE`) places a `SkeletonCellPlacementRequest` — course offering, session type, day, period, optional batch (required for LAB/CLINICAL), cohort, optional cohort section (Theory), optional `spanPeriodIds` for a multi-period session. No faculty/room at this stage — that's Staffing. A manual placement is always pinned, so the next Global Auto-Schedule rebuild works around it instead of clearing it.
3. **Move/swap/relocate:** drag gestures share `TIMETABLE_SKELETON_MOVE` across move/swap/relocate/their previews — a move into an empty slot, a swap with an occupied one, or a whole-block relocate (parallel Lab batches, rotation partner, idle-batch fallback included) that finds every same-length legal window and swaps a same-length occupied window into the vacated slot.
4. **Auto-place:** bounded single-attempt backtracking greedy scan per cohort for remaining shortfall.
5. **Global Auto-Schedule:** `GET .../global-auto-place/prerequisites` (missing-faculty/over-capacity report) → `GET .../precheck` (term-wide capacity sanity check the frontend must pass before running) → `POST /timetables/skeleton/global-auto-place` (optionally scoped to one `cohortId`, else every cohort in the term) — plans Theory/Lab/Clinical against curriculum hours, then Library (1×2 periods/week, a bonus 2nd session only when ≥8 free periods remain), Sports (1×2 periods/week, PE-eligible faculty, a Sports & Recreation room), then Self-Study exactly to its curriculum hours. Placement order is Monday–Friday first, then Saturday only once the term has a working-Saturday pattern and curriculum has genuinely run out of weekday room; every subject budget is counted against the term's real total working-period occurrences (§ BRD BR-TT-14), not a flat weekly multiplier. Best-effort: commits everything placeable and reports the rest per-cohort as `unplaced`.
6. Institution-selected electives are placed as one common whole-cohort session (`POST /elective-groups/place`) in the section's own classroom — every offering in the same elective group is forced onto the same day+period (BR-TT-3).

### 4.3 Staffing
1. `GET /timetables/staffing?termInstanceId=` lists unstaffed cells.
2. `PUT /timetables/staffing/cells/{id}` (`TIMETABLE_STAFFING_MANAGE`) assigns `facultyId` (+ `classroomId`, read only for an elective Theory session — every other case resolves its room server-side from the venue already committed in Capacity Auto-Plan). Hard-blocked by: venue seating fit (whole-cohort for Theory, batch/largest-rotating-batch roster for Lab/Clinical), and the faculty's daily/weekly/continuous-run workload caps, each resolved **per-faculty override → designation default → flat institution-wide config → no cap** (identical precedence for all three tiers; a configured value of 0 or blank means "no cap"). Also blocks assigning a room already occupied by a *different* venue that resolves to the same underlying physical `Room`.
3. `POST /timetables/staffing/auto-staff?termInstanceId=` (`TIMETABLE_STAFFING_AUTO_STAFF`) staffs every remaining unstaffed cell for the term, no backtracking, ranked by subject-continuity (same faculty across a subject's sessions where possible).

### 4.4 Batch rotation (shared Lab/Clinical venue)
1. Coordinator creates a `RotationGroup` (`POST /timetables/rotation-groups`, `TIMETABLE_ROTATION_MANAGE`): a label, an `anchorOccurrenceDate`, N `slots` (each an already-placed `ClassSchedule` cell) and N `members` (each with `assignments` mapping a slot's `classScheduleId` to a `Batch`). Creating the group nulls out `ClassSchedule.batch` on every linked cell — the occupant becomes rotation-resolved, not fixed.
2. `RotationResolverService` resolves, for any date, which member/batch occupies a given slot by counting **whole weeks elapsed since `anchorOccurrenceDate`** (not raw ISO week number, so it stays predictable across year boundaries) and cyclically shifting `slotOrder` by that count modulo the cycle length (a Latin-square rotation, generalized from a simple 2-way swap to any N-way).
3. `GET /{id}/effective?classScheduleId=&date=` returns the resolved assignment for display; `GET /candidate-slots` helps the coordinator pick eligible day/period slots when building a group.
4. Delete (`TIMETABLE_ROTATION_MANAGE`) removes the group; the underlying `ClassSchedule` cells are unaffected.

### 4.5 Lifecycle: draft → publish → lock
1. `GET /timetables/draft?termInstanceId=` (`TIMETABLE_MANAGE`) reviews every `DRAFT` `ClassSchedule` row for the term, merged with Clinical Shift Group duty-roster entries (which never produce a real `ClassSchedule` row) via `/draft/clinical-shift-summary`.
2. Per-cohort **Conflict Inspector**: `GET /{termInstanceId}/cohorts/{cohortId}/conflict-status` then `POST .../acknowledge-conflicts` (`TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE`) — Timetable Builder's own "Check & Resolve Conflicts" row action (the earlier standalone term-wide Conflict Inspector screen was retired, see §8).
3. `POST /timetables/{termInstanceId}/approve` (`TIMETABLE_PUBLISH`, body `cohortIds` — required, no whole-term shortcut) converts `DRAFT` → `PUBLISHED` for the named cohorts. If curriculum-required hours remain unplaced for any cohort/session-type, `TimetableCoverageGapException` is thrown; resubmitting with `overrideIncompleteCoverage=true` + `overrideReason` requires the caller to also hold `TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE` (enforced in the `@PreAuthorize` SpEL itself, before business logic runs).
4. `POST /{termInstanceId}/discard-draft` (`TIMETABLE_DISCARD_DRAFT`) and `POST /{termInstanceId}/revert-to-draft` (`TIMETABLE_DISCARD_PUBLISHED`) are both cohort-scoped (body `TimetableCohortActionRequest.cohortIds`).
5. Every write anywhere in the module is blocked once the owning `TermInstance` is `LOCKED`, independent of each row's own DRAFT/PUBLISHED status (BR-TT-8).

### 4.6 Post-publish: Faculty Absence & Substitute
1. `POST /faculty-absences` (`FACULTY_ABSENCE_MARK`) records a `FacultyAbsence` (faculty + date + optional reason; unique per faculty/date).
2. `GET /{absenceId}/affected-sessions` lists that faculty's sessions on that date.
3. `GET /sessions/{classScheduleId}/substitute-candidates?date=` (`FACULTY_ABSENCE_SUBSTITUTE_APPLY`) finds eligible substitutes.
4. `POST /{absenceId}/sessions/{classScheduleId}/apply-substitute` writes `effectiveFaculty`/`facultyAbsence` onto that date's `SessionOccurrence` row only — the recurring `ClassSchedule.faculty` is never mutated (BR-TT-10), so every other date of the same weekly schedule still shows the original faculty.

### 4.7 Post-publish: Staff Session Swap
`GET /timetables/staff-swap/sessions/{classScheduleId}/candidates?date=` then `POST .../apply` (both `TIMETABLE_STAFF_SWAP`) mutually swaps faculty between two published sessions for one specific date, writing both sides as per-date `SessionOccurrence` overrides (`swapPartnerOccurrence` linking the two rows) — never touching either recurring `ClassSchedule` row.

### 4.8 Post-publish: Room Relocation
`GET /timetables/room-relocation/sessions/{classScheduleId}/candidates?date=` → `POST .../relocate` (venue + date) → `DELETE .../relocate?date=` to revert (all `TIMETABLE_ROOM_RELOCATE`). Day view only (per SRS FR-TT-17); single-date override on `SessionOccurrence`.

### 4.9 Special/Remedial Class Scheduler (BR-55)
1. Faculty submits one of three shapes, all requiring `TIMETABLE_SPECIAL_CLASS_REQUEST` and a resolved `FACULTY` identity:
   - **Single-subject** (`POST /single-subject`): one date, a consecutive block of `periodIds`, subject/course-offering/session-type/venue/requested-faculty.
   - **Day-repeat** (`POST /day-repeat`): copies an entire source weekday's recurring timetable onto a different target date for one cohort section — one `SessionOccurrence` per resolvable source `ClassSchedule` row, faculty inherited per-row from each source row (not one faculty for the whole day); rows whose cohort ownership can't be unambiguously resolved are skipped and reported via a skipped count.
   - **Recurring** (`POST /recurring`): the single-subject shape repeated weekly from `startDate` through `endDate`, skipping any week whose date isn't itself a non-instruction day.
2. All three create `SessionOccurrence` rows with `occurrenceSource ∈ {SPECIAL_CLASS, DAY_REPEAT, RECURRING_SPECIAL_CLASS}`, `approvalStatus = PENDING`, sharing one `requestBatchId` when the request produces multiple rows.
3. Approver: `GET /approval-queue` (`TIMETABLE_SPECIAL_CLASS_APPROVE`) → `PUT /{id}/approve` or `/batches/{requestBatchId}/approve` (and the `/reject` mirror, requiring a rejection reason). `PUT /{id}/cancel` (`TIMETABLE_SPECIAL_CLASS_CANCEL`) withdraws an approved-but-not-yet-occurred request.
4. Conflict-checked against **both** the recurring template and other special classes (a special class has no `ClassSchedule` row for either check alone to see). Period-grid slots only — never free-form times. Does not count against the faculty weekly workload cap and does not feed portion-completion aggregation (both deliberate v1 exclusions per BR-TT-11).

### 4.10 Portion-Completion tracking
1. `POST /portion-blueprint/course-offerings/{id}/generate` (`PORTION_BLUEPRINT_MANAGE`) freezes a `SyllabusUnitPlan` row per syllabus unit: planned completion date, planned cumulative hours, sequence index — generated once, only moved again by an explicit regenerate.
2. `GET .../projection` recomputes the Projected/Actual date live on every read against the current timetable (so a holiday added later cascades the projection forward automatically) via `UnitVarianceDto`.
3. `GET /portion-blueprint/shortfall?termInstanceId=&cohortId=` (`PROGRESS_REPORT_VIEW`) checks whether a cohort is falling behind its planned pace.
4. Faculty log per-occurrence unit coverage against `SessionOccurrence.unitCoverages` (`session_occurrence_units`) via the Log Progress dialog (`PROGRESS_LOG_CREATE`).

### 4.11 Holiday auto-block & Holiday Templates
1. A `HOLIDAY`-type `CalendarEvent` auto-generates one or more `BlockedPeriod` rows (`ONE_OFF`, `sourceCalendarEvent` set) covering the whole day or a specified half-day subset, kept in sync on the event's create/update/delete.
2. `RECURRING` blocked periods (a day-of-week within a date range) are always a hard placement conflict in Timetable Builder; a holiday-derived `ONE_OFF` block is hard-enforced there too; a manually-created `ONE_OFF` block (no source event) only affects Capacity Planner buffer-hours math and calendar display, since it has no recurring weekly-template placement to conflict with.
3. An admin may delete just the auto-generated `BlockedPeriod` row (not the source event) to run a special class during an otherwise-blocked slot.
4. **Holiday Templates** (`/holiday-templates`, `HOLIDAY_TEMPLATE_MANAGE` for writes) define a recurring-holiday master (YEARLY/MONTHLY/WEEKLY/DAILY/CUSTOM recurrence, nth-weekday patterns via `weekOfMonth`+`dayOfWeek`) that auto-seeds `CalendarEvent`s into new academic years. `GET /name-exists` backs a real-time uniqueness check on the template name.

### 4.12 My Timetable consumption
Students and staff view their own schedule via the audience-split `/my-timetable/{student|staff}` route (§3.1); `GET /timetables/me` and `GET /timetables/occurrences?scope=personal` (both self-scoped through `PersonalTimetableService`) back it, deliberately unable to widen into the broader `scope=browse` used by the admin-facing Timetable screen.

---

## 5. API Endpoints

Every endpoint below is grep-confirmed against its controller (method, path, `@PreAuthorize` expression). Request/response DTO field lists are in §4 (workflow narrative) and §6 (data model) where not repeated here; only the endpoint surface is tabulated.

### 5.1 `ClassScheduleController` — base `/lab-schedules`
| Method & Path | Permission | Notes |
|---|---|---|
| `POST /` | `LAB_SCHEDULE_MANAGE` | Create (`ClassScheduleRequest` → `ClassScheduleResponse`) |
| `GET /` | — | Filterable by `labId`/`facultyId`/`termInstanceId`/`batchName`/`dayOfWeek` |
| `GET /by-term/{termInstanceId}` | — | |
| `GET /{id}` | — | |
| `POST /check-conflicts` | `LAB_SCHEDULE_MANAGE` | `ClassScheduleRequest` → `ScheduleConflictResponse` |
| `PUT /{id}` | `LAB_SCHEDULE_MANAGE` | |
| `DELETE /{id}` | `LAB_SCHEDULE_MANAGE` | |

### 5.2 `TimetableSkeletonController` — base `/timetables/skeleton`
| Method & Path | Permission |
|---|---|
| `GET /?termInstanceId=&cohortId=` | `TIMETABLE_VIEW` |
| `GET /suggest?courseOfferingId=&sessionType=&batchId=&cohortSectionId=` | `TIMETABLE_SKELETON_MANAGE` |
| `POST /cells` | `TIMETABLE_SKELETON_MANAGE` |
| `PUT /cells/{id}/pin?pinned=` | `TIMETABLE_SKELETON_PIN` |
| `PUT /cells/{id}/replace` | `TIMETABLE_SKELETON_REPLACE` |
| `DELETE /cells/{id}` | `TIMETABLE_SKELETON_MANAGE` |
| `PUT /cells/{id}/move` | `TIMETABLE_SKELETON_MOVE` |
| `GET /cells/{id}/move-preview?cohortId=` | `TIMETABLE_SKELETON_MOVE` |
| `PUT /cells/{id}/swap` | `TIMETABLE_SKELETON_MOVE` |
| `GET /cells/{id}/relocate-preview?cohortId=` | `TIMETABLE_SKELETON_MOVE` |
| `PUT /cells/{id}/relocate` | `TIMETABLE_SKELETON_MOVE` |
| `GET /clinical-shift-groups/{id}/day-preview?cohortId=` | `TIMETABLE_SKELETON_DUTY_DAY_MOVE` |
| `PUT /clinical-shift-groups/{id}/day` | `TIMETABLE_SKELETON_DUTY_DAY_MOVE` |
| `GET /global-auto-place/prerequisites?termInstanceId=&cohortId=` | `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` |
| `GET /global-auto-place/has-existing-draft?termInstanceId=` | `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` |
| `GET /global-auto-place/precheck?termInstanceId=` | `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` |
| `POST /global-auto-place?termInstanceId=&cohortId=` | `TIMETABLE_SKELETON_GLOBAL_AUTO_PLACE` |
| `POST /clinical-duty-fit/{courseOfferingId}` | `TIMETABLE_SKELETON_CLINICAL_DUTY_FIT` |
| `POST /elective-groups/place` | `TIMETABLE_SKELETON_ELECTIVE_PLACE` |
| `GET /elective-groups/{electiveGroupId}/schedule?termInstanceId=` | `TIMETABLE_VIEW` or `COURSE_REGISTRATION_ELECTIVE_ASSIGN` |

### 5.3 `TimetableStaffingController` — base `/timetables/staffing`
| Method & Path | Permission |
|---|---|
| `GET /?termInstanceId=` | `TIMETABLE_VIEW` |
| `PUT /cells/{id}` | `TIMETABLE_STAFFING_MANAGE` |
| `POST /auto-staff?termInstanceId=` | `TIMETABLE_STAFFING_AUTO_STAFF` |

### 5.4 `TimetableController` — base `/timetables`
| Method & Path | Permission |
|---|---|
| `GET /resource-grid/faculty?termInstanceId=&dayOfWeek=&date=` | `TIMETABLE_FACULTY_GRID_VIEW` |
| `GET /resource-grid/classroom?...` | `TIMETABLE_CLASSROOM_GRID_VIEW` |
| `GET /resource-grid/faculty/week?facultyId=&termInstanceId=&weekStart=` | `TIMETABLE_FACULTY_GRID_VIEW` |
| `GET /resource-grid/classroom/week?resourceId=&termInstanceId=&weekStart=` | `TIMETABLE_CLASSROOM_GRID_VIEW` |
| `GET /me?termInstanceId=&weekStart=` | `TIMETABLE_VIEW` or `MY_TIMETABLE_VIEW*` |
| `GET /occurrences?termInstanceId=&from=&to=&scope=&cohortId=` | `TIMETABLE_VIEW`, or (`scope=personal`) `MY_TIMETABLE_VIEW*` |
| `GET /draft?termInstanceId=` | `TIMETABLE_MANAGE` |
| `GET /draft/clinical-shift-summary?termInstanceId=` | `TIMETABLE_MANAGE` |
| `GET /draft/cohort-status-summary?termInstanceId=&cohortId=&page=&size=` | `TIMETABLE_VIEW` (paginated `Page<CohortTermStatusSummary>`) |
| `GET /?termInstanceId=&cohortId=` | `TIMETABLE_VIEW` (published) |
| `POST /{termInstanceId}/approve` | `TIMETABLE_PUBLISH` (+ `TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE` if overriding) |
| `POST /{termInstanceId}/discard-draft` | `TIMETABLE_DISCARD_DRAFT` |
| `POST /{termInstanceId}/revert-to-draft` | `TIMETABLE_DISCARD_PUBLISHED` |
| `GET /{termInstanceId}/cohorts/{cohortId}/conflict-status` | `TIMETABLE_VIEW` |
| `POST /{termInstanceId}/cohorts/{cohortId}/acknowledge-conflicts` | `TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE` |
| `GET /{termInstanceId}/sessions/{sessionId}/swap-candidates` | `TIMETABLE_SWAP` |
| `POST /{termInstanceId}/sessions/{sessionId}/swap` | `TIMETABLE_SWAP` |

### 5.5 `TimetableCapacityPlanningController` — base `/timetables/capacity-plan`
| Method & Path | Permission |
|---|---|
| `GET /?termInstanceId=&cohortId=&planningBasis=` | `TIMETABLE_CAPACITY_PLANNER_VIEW` |
| `GET /faculty-workload?termInstanceId=` | `TIMETABLE_CAPACITY_PLANNER_VIEW` |
| `GET /faculty-workload-overview?termInstanceId=` | `TIMETABLE_CAPACITY_PLANNER_VIEW` |
| `GET /term-overview?termInstanceId=&planningBasis=` | `TIMETABLE_CAPACITY_PLANNER_VIEW` |
| `GET /venue-capacity?termInstanceId=&planningBasis=` | `TIMETABLE_CAPACITY_PLANNER_VIEW` |
| `GET /rebalance-preview?termInstanceId=&sessionType=&venueId=&planningBasis=` | `TIMETABLE_CAPACITY_PLANNER_VIEW` |
| `POST /rebalance?termInstanceId=` | `TIMETABLE_CAPACITY_PLANNER_REBALANCE` |

### 5.6 `CohortRoomAllocationController` — base `/timetables/cohort-room-allocations`
| Method & Path | Permission |
|---|---|
| `GET /?cohortId=&termInstanceId=` | `TIMETABLE_COHORT_ROOM_ALLOCATION_VIEW` |
| `POST /commit` | `TIMETABLE_COHORT_ROOM_ALLOCATION_MANAGE` |
| `POST /{id}/revert` | `TIMETABLE_COHORT_ROOM_ALLOCATION_REVERT` |

### 5.7 `RotationGroupController` — base `/timetables/rotation-groups`
| Method & Path | Permission |
|---|---|
| `GET /?termInstanceId=` | `TIMETABLE_ROTATION_VIEW` |
| `GET /candidate-slots?termInstanceId=&dayOfWeek=&periodId=` | `TIMETABLE_ROTATION_VIEW` |
| `GET /{id}/effective?classScheduleId=&date=` | `TIMETABLE_ROTATION_VIEW` |
| `POST /` | `TIMETABLE_ROTATION_MANAGE` |
| `DELETE /{id}` | `TIMETABLE_ROTATION_MANAGE` |

### 5.8 `SpecialClassController` — base `/timetables/special-classes`
| Method & Path | Permission |
|---|---|
| `POST /single-subject` | `TIMETABLE_SPECIAL_CLASS_REQUEST` |
| `POST /day-repeat` | `TIMETABLE_SPECIAL_CLASS_REQUEST` |
| `POST /recurring` | `TIMETABLE_SPECIAL_CLASS_REQUEST` |
| `GET /my-requests` | `TIMETABLE_SPECIAL_CLASS_VIEW` |
| `GET /approval-queue` | `TIMETABLE_SPECIAL_CLASS_APPROVE` |
| `PUT /{id}/approve` / `PUT /batches/{requestBatchId}/approve` | `TIMETABLE_SPECIAL_CLASS_APPROVE` |
| `PUT /{id}/reject` / `PUT /batches/{requestBatchId}/reject` | `TIMETABLE_SPECIAL_CLASS_APPROVE` |
| `PUT /{id}/cancel` | `TIMETABLE_SPECIAL_CLASS_CANCEL` |

### 5.9 `FacultyAbsenceController` — base `/faculty-absences`
| Method & Path | Permission |
|---|---|
| `POST /` | `FACULTY_ABSENCE_MARK` |
| `GET /{absenceId}` | `FACULTY_ABSENCE_MARK` or `FACULTY_ABSENCE_SUBSTITUTE_APPLY` |
| `GET /{absenceId}/affected-sessions` | `FACULTY_ABSENCE_MARK` or `FACULTY_ABSENCE_SUBSTITUTE_APPLY` |
| `GET /sessions/{classScheduleId}/substitute-candidates?date=` | `FACULTY_ABSENCE_SUBSTITUTE_APPLY` |
| `POST /{absenceId}/sessions/{classScheduleId}/apply-substitute` | `FACULTY_ABSENCE_SUBSTITUTE_APPLY` |

### 5.10 `FacultySessionSwapController` — base `/timetables/staff-swap`
| Method & Path | Permission |
|---|---|
| `GET /sessions/{classScheduleId}/candidates?date=` | `TIMETABLE_STAFF_SWAP` |
| `POST /sessions/{classScheduleId}/apply` | `TIMETABLE_STAFF_SWAP` |

### 5.11 `RoomRelocationController` — base `/timetables/room-relocation`
| Method & Path | Permission |
|---|---|
| `GET /sessions/{classScheduleId}/candidates?date=` | `TIMETABLE_ROOM_RELOCATE` |
| `POST /sessions/{classScheduleId}/relocate` | `TIMETABLE_ROOM_RELOCATE` |
| `DELETE /sessions/{classScheduleId}/relocate?date=` | `TIMETABLE_ROOM_RELOCATE` |

### 5.12 `HolidayTemplateController` — base `/holiday-templates`
| Method & Path | Permission |
|---|---|
| `POST /` | `HOLIDAY_TEMPLATE_MANAGE` |
| `GET /`, `GET /page?search=`, `GET /{id}` | none (`@PreAuthorize` absent — see §7) |
| `PUT /{id}` | `HOLIDAY_TEMPLATE_MANAGE` |
| `DELETE /{id}` | `HOLIDAY_TEMPLATE_MANAGE` |
| `GET /name-exists?value=&excludeId=` | `HOLIDAY_TEMPLATE_MANAGE` |

### 5.13 `PortionBlueprintController` — base `/portion-blueprint`
| Method & Path | Permission |
|---|---|
| `POST /course-offerings/{id}/generate` | `PORTION_BLUEPRINT_MANAGE` |
| `GET /course-offerings/{id}` | `PROGRESS_REPORT_VIEW` or `PROGRESS_LOG_CREATE` |
| `GET /course-offerings/{id}/projection` | `PROGRESS_REPORT_VIEW` or `PROGRESS_LOG_CREATE` |
| `GET /shortfall?termInstanceId=&cohortId=` | `PROGRESS_REPORT_VIEW` |

### 5.14 `FacultyAvailabilityController` — base `/faculty-availability`
| Method & Path | Permission |
|---|---|
| `GET /` | `FACULTY_AVAILABILITY_VIEW` or `_MANAGE` |
| `POST /` | `FACULTY_AVAILABILITY_MANAGE` |
| `DELETE /{id}` | `FACULTY_AVAILABILITY_MANAGE` |

### 5.15 `FacultyWorkloadRulesController` — base `/timetables/workload-rules`
| Method & Path | Permission |
|---|---|
| `GET /` | `TIMETABLE_WORKLOAD_RULES_VIEW` or `_MANAGE` |
| `PUT /` | `TIMETABLE_WORKLOAD_RULES_MANAGE` |

### 5.16 Beyond SRS/BRD scope — `ClinicalShiftGroupController` (base `/clinical-shift-groups`) and `DayMappingOverrideController` (base `/day-mappings`)
| Method & Path | Permission |
|---|---|
| `POST /clinical-shift-groups`, `PUT /{id}`, `DELETE /{id}`, `PUT /{id}/batches/{batchId}`, `DELETE /{id}/batches/{batchId}`, `PUT /{id}/theory-blocks`, `POST /{id}/generate` | `TIMETABLE_CLINICAL_SHIFT_MANAGE` |
| `GET /clinical-shift-groups`, `GET /{id}` | `TIMETABLE_CLINICAL_SHIFT_VIEW` |
| `POST /day-mappings`, `PUT /{id}`, `DELETE /{id}` | `TIMETABLE_DAY_MAPPING_MANAGE` |
| `GET /day-mappings`, `GET /{id}` | `TIMETABLE_DAY_MAPPING_VIEW` or `_MANAGE` (fixed 2026-09-24; previously unguarded) |

---

## 6. Data Model

All tables below are current per their creating/latest-altering migration and the live JPA entity. Standard `created_at`/`updated_at` audit columns are omitted from the column lists except where independently meaningful.

### 6.1 `class_schedules` — `ClassSchedule` (V293, generalized from `lab_schedules`; V334 added CLINICAL)
The recurring weekly template, no calendar date.
- **Columns:** `id`; `lab_id` (FK `labs`, LAB only); `subject_id` (FK, NOT NULL); `faculty_id` (FK, nullable since V335 — null only while `status != PUBLISHED`); `batch_name` (free text); `batch_id` (FK `batches`, optional real roster link); `day_of_week` (enum, NOT NULL); `term_instance_id` (FK, NOT NULL); `is_active`; `is_pinned` (boolean, default false); `session_type` (enum `THEORY|LAB|CLINICAL|LIBRARY|SPORTS`, NOT NULL); `status` (enum `DRAFT|PUBLISHED`, NOT NULL); `classroom_id` (FK, THEORY); `period_id` (FK); `clinical_venue_id` (FK, CLINICAL); `course_offering_id` (FK, optional); `cohort_section_id` (FK, THEORY section-scoping); `session_group_id` (UUID, links N consecutive-period rows of one multi-period session).
- **Constraint:** `chk_class_schedule_session_shape` (CHECK, redefined V334) enforces the correct venue/batch combination per `sessionType`.

### 6.2 `session_occurrences` — `SessionOccurrence` (V322; extended V374, V409, V529)
The per-date actual/override/ad-hoc anchor. Unique on `(class_schedule_id, occurrence_date)`.
- **REGULAR-row columns:** `class_schedule_id` (FK, non-null for REGULAR); `occurrence_date`; `recorded_by_faculty_id`; `remarks`; `effective_faculty_id` (per-date substitute override); `faculty_absence_id` (FK); `occurrence_status` (enum `HELD|SUBSTITUTED|CANCELLED`); `swap_partner_occurrence_id` (self-FK, staff-swap partner row).
- **Special-class-shape columns** (V374, populated only when `occurrence_source != REGULAR`; `class_schedule_id` NULL): `occurrence_source` (enum `REGULAR|SPECIAL_CLASS|DAY_REPEAT|CLINICAL_SHIFT|RECURRING_SPECIAL_CLASS`); `subject_id`, `course_offering_id`, `cohort_section_id`, `period_id`, `session_type`, `classroom_id`/`lab_id`/`clinical_venue_id`; `requested_faculty_id`; `approval_status` (enum `PENDING|APPROVED|REJECTED|CANCELLED`); `requested_by_faculty_id`, `requested_at`, `request_reason`; `source_day_of_week` (DAY_REPEAT only); `request_batch_id` (UUID, groups a multi-row batch for atomic approve/reject); `approved_by`, `approved_at`, `rejection_reason`.
- **CLINICAL_SHIFT columns** (V409): `block_start_time`/`block_end_time` (real clock times, bypassing `period` entirely); `batch_id` (one row per linked Batch for a CLINICAL block; null for a shared THEORY block, which uses `cohort_section` instead).
- **Constraint:** `chk_session_occurrences_special_shape` (V374) enforces the REGULAR-vs-special-class column shape.
- **Child table `session_occurrence_units`:** composite PK (`session_occurrence_id`, `syllabus_unit_id`) — many-to-many, one occurrence can cover several units and a unit can span several occurrences.

### 6.3 `cohort_room_allocations` — `CohortRoomAllocation` (V359)
- **Columns:** `cohort_id`, `term_instance_id` (both FK, NOT NULL); `theory_classroom_id` (FK, NOT NULL); `status` (`COMMITTED|REVERTED`, CHECK-enforced); `planning_basis` (enum `ENROLLED|SANCTIONED`); `planned_strength`; `committed_by`/`committed_at`; `reverted_by`/`reverted_at`.
- **Unique indexes (BR-TT-7's "DB-unique-index enforced" claim, confirmed exact):**
  - `ux_cohort_room_alloc_active` on `(cohort_id, term_instance_id) WHERE status='COMMITTED'` — one active allocation per cohort/term.
  - `ux_theory_classroom_per_term` on `(theory_classroom_id, term_instance_id) WHERE status='COMMITTED'` — one cohort's home room can never be double-claimed by another cohort in the same term.

### 6.4 `cohort_sections` — `CohortSection` (V364)
`cohort_room_allocation_id` (FK), `term_instance_id` (FK), `section_label`, `classroom_id` (FK), `class_incharge_faculty_id` (FK), `planned_size`, `is_active`.

### 6.5 `batches` — `Batch`
`course_offering_id` (FK, NOT NULL), `name`, `capacity`, `term_instance_id` (FK), `coordinator_faculty_id` (FK), `lab_id`/`clinical_venue_id` (FK, venue split), `cohort_room_allocation_id` (FK — **the non-null signal that this batch came from Capacity Auto-Plan**, per the mandatory creation-path gate), `cohort_section_id` (FK), `clinical_shift_group_id` (FK), `is_active`, `version` (optimistic lock), `students` (M:M `batch_students`). **Unique:** `(course_offering_id, name)`.

### 6.6 `rotation_groups` / `rotation_slots` / `rotation_members` / `rotation_member_assignments`
- `rotation_groups`: `term_instance_id` (FK), `label`, `cycle_length` (int, server-derived = `slots.size()`), `anchor_occurrence_date`, `is_active`, `created_by`.
- `rotation_slots`: `rotation_group_id` (FK), `class_schedule_id` (FK), `slot_order`.
- `rotation_members`: `rotation_group_id` (FK), `member_order`, `label`.
- `rotation_member_assignments`: `rotation_member_id` (FK), `rotation_slot_id` (FK), `batch_id` (FK).

### 6.7 `blocked_periods` — `BlockedPeriod`
`period_id` (FK, NOT NULL); `block_type` (`ONE_OFF|RECURRING`); `specific_date` (ONE_OFF); `day_of_week`/`range_start_date`/`range_end_date` (RECURRING); `reason` (NOT NULL); `source_calendar_event_id` (FK, nullable — non-null is the "auto-generated by a HOLIDAY event" discriminator; deleting just this row without touching the source event is the "unblock for a special class" override). `RECURRING` blocks and holiday-derived `ONE_OFF` blocks are hard placement conflicts; a manually created `ONE_OFF` block only affects Capacity Planner's buffer-hours math and calendar display.

### 6.8 `faculty_absences` — `FacultyAbsence`
`faculty_id` (FK, NOT NULL), `absence_date` (NOT NULL), `reason`, `recorded_by`. **Unique:** `(faculty_id, absence_date)`.

### 6.9 `holiday_templates` — `HolidayTemplate`
`name` (unique, NOT NULL), `recurrence_type` (enum), `event_type` (enum, default `HOLIDAY`), `holiday_category` (enum, HOLIDAY-only), `description`, `duration_days` (default 1), `interval_count` (default 1), `anchor_date`, `end_date` (null = repeats forever), `month`/`day_of_month` (YEARLY/fixed-day-MONTHLY), `week_of_month`/`day_of_week` (nth-weekday MONTHLY, and WEEKLY), `is_active`.

### 6.10 `curriculum_elective_groups` — `CurriculumElectiveGroup`
`curriculum_version_id` (FK), `term_number`, `group_name`, `group_code`, `selection_mode` (enum, default `STUDENT_CHOICE` — though per BRD BR-TT-3/management-selected-electives memory, current timetabling behavior treats the chosen option as a common whole-cohort subject regardless of this field's literal value for "institution decided" groups).

### 6.11 `clinical_venues` — `ClinicalVenue`
`name`, `hospital_name`, `department`, `capacity`, `room_id` (FK `rooms` — physical-location binding), `is_active`.

### 6.12 `syllabus_unit_plan` — `SyllabusUnitPlan` (V353) — the frozen portion-completion Blueprint
`course_offering_id` (FK), `syllabus_unit_id` (FK), `planned_completion_date` (NOT NULL, frozen at generation), `planned_cumulative_hours` (NOT NULL), `sequence_index` (NOT NULL).

### 6.13 `clinical_shift_groups` — `ClinicalShiftGroup` (beyond SRS/BRD scope, documented here)
A recurring off-campus clinical shift window. `course_offering_id` (FK, NOT NULL), `cohort_section_id` (FK), `term_instance_id` (FK, NOT NULL), `label`, `day_of_week` (NOT NULL). Several `Batch` rows (each with its own `lab`/`clinical_venue`) link to one group when they run clinical in parallel at different venues under the same shift; the group's students reconvene into one shared theory block captured separately (`ClinicalShiftTheoryBlock`, not independently detailed here). The block's end time is derived at read time from `CourseOffering.clinicalShiftDurationMinutes`, not stored redundantly.

### 6.14 Key enums (exact values, grep-confirmed)
- `ClassSessionType`: `THEORY, LAB, CLINICAL, LIBRARY, SPORTS`
- `ClassScheduleStatus`: `DRAFT, PUBLISHED`
- `OccurrenceStatus`: `HELD, SUBSTITUTED, CANCELLED`
- `OccurrenceSource`: `REGULAR, SPECIAL_CLASS, DAY_REPEAT, CLINICAL_SHIFT, RECURRING_SPECIAL_CLASS`
- `SpecialClassApprovalStatus`: `PENDING, APPROVED, REJECTED, CANCELLED`
- `PlanningBasis`: `ENROLLED, SANCTIONED`
- `CohortRoomAllocationStatus`: `COMMITTED, REVERTED`
- `BlockType`: `ONE_OFF, RECURRING`

### 6.15 Relationship summary
`TermInstance` 1—* `ClassSchedule` 1—* `SessionOccurrence`. `ClassSchedule` *—1 `Subject`/`Faculty`(nullable)/`Period`/`CourseOffering`(optional)/`CohortSection`(optional, Theory)/`Classroom`|`Lab`|`ClinicalVenue` (one per `sessionType`)/`Batch`(optional). `CohortRoomAllocation` 1—* `CohortSection`, 1—* `Batch` (via `cohort_room_allocation_id`). `RotationGroup` 1—* `RotationSlot`(*—1 `ClassSchedule`) and 1—* `RotationMember` 1—* `RotationMemberAssignment` (*—1 `RotationSlot`, *—1 `Batch`). `FacultyAbsence` 1—* `SessionOccurrence` (via `faculty_absence_id`). `BlockedPeriod` *—1 `Period`, optionally *—1 `CalendarEvent` (source).

---

## 7. Edge Cases & Validation Rules

- **Workload cap resolution** is identically three-tier for all of daily/weekly/continuous-run caps: per-faculty override → designation default (both via `FacultyWorkloadCapacityService`) → flat institution-wide `timetable.faculty_max_{daily,weekly,continuous}_hours` config → no cap. A configured value of 0 or blank is treated as "unset" at every tier, not as a zero-hour cap. The weekly/daily/continuous reports and the hard staffing gate are guaranteed to resolve a faculty's capacity identically because both call the same resolver — never duplicated.
- **Physical-room double-booking detection** at staffing time compares the *resolved physical `Room`*, not just the virtual venue id — two different `Classroom`/`Lab`/`ClinicalVenue` rows that happen to share the same underlying `Room` are correctly treated as the same physical space and block each other.
- **Elective-group placement** forces every `CourseOffering` in the same elective group onto the identical day+period within a term (BR-TT-3) — students choose one option, but every option must run simultaneously so no student's choice collides with another subject.
- **Venue seating fit** is hard-blocked at staffing: whole-cohort headcount for Theory, batch roster count (or the largest rotating batch, for a rotation-shared venue) for Lab/Clinical.
- **Coverage-gap override** (`TimetableCoverageGapException`) is the one non-structural, overridable violation type in the module — every other placement/staffing violation (`TimetableConstraintViolationException`) is a hard structural defect with no override path. The override requires a distinct permission (`TIMETABLE_APPROVE_INCOMPLETE_OVERRIDE`) checked in the `@PreAuthorize` SpEL itself, so an unauthorized override attempt never reaches business logic.
- **Rotation anchor arithmetic** counts *whole weeks elapsed* since `RotationGroup.anchorOccurrenceDate`, not the ISO week number — deliberately, to stay correct across a year boundary where ISO week numbering resets.
- **Special-class two-part conflict check:** because a special/remedial class has no backing `ClassSchedule` row, `SpecialClassRequestService` must check conflicts against both the recurring template *and* other special-class occurrences separately — neither check alone can see the other source.
- **Day-repeat skip-not-guess rule:** a Day-Repeat request's source-weekday rows whose cohort ownership can't be unambiguously resolved are silently skipped (never guessed) and reported back via a skipped-row count.
- **Batch creation gate:** the only legitimate `Batch`-creating code path is `CohortRoomAllocationService.createVentureBatch` inside a Capacity Auto-Plan commit. `Batch.cohortRoomAllocation` non-null is the sole signal a batch is "real" (came from this path); null means legacy/stale data predating the gate — every batch picker (Manage Batches, Clinical Shift Group, Escort Rotation, Lab Schedule) must only surface non-null-allocation batches.
- ~~**`HolidayTemplateController` read endpoints have no `@PreAuthorize`**~~ **Fixed 2026-09-24.** `GET /`, `GET /page`, `GET /{id}` now require `HOLIDAY_TEMPLATE_VIEW` or `HOLIDAY_TEMPLATE_MANAGE`, consistent with the write-shaped endpoints (`POST`/`PUT`/`DELETE`/`GET /name-exists`, all `_MANAGE`) and with `ClassScheduleController`'s `GET /`, `GET /by-term/{termInstanceId}`, `GET /{id}` (now `LAB_SCHEDULE_VIEW` or `_MANAGE`) and `DayMappingOverrideController`'s `GET /`, `GET /{id}` (now `TIMETABLE_DAY_MAPPING_VIEW` or `_MANAGE`, see §5.16). No new migrations needed — all three `_VIEW` permissions were already seeded (V88, V352, V381), just unused.
- **`V553__restore_cohort_conflict_acknowledge_permission.sql` is uncommitted** in the working tree at the time of this pass (confirmed present on disk; git status shows it untracked). It restores `TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE`, which an earlier migration (`V550`) incorrectly deleted while `TimetableController.acknowledgeCohortConflicts` still required it — meaning, until V553 is committed and applied, **every user including DEV_ADMIN is locked out of "Check & Resolve Conflicts" with an Access Denied error** in any environment that has already run V550. Verify V553 has been committed and applied before relying on this action in any environment.
- **`PORTION_PLAN_VISIBLE_TO_FACULTY` (V354)** exists as a `SystemConfiguration` row but gates no logic anywhere in the current codebase — not in `PortionBlueprintController`'s `@PreAuthorize` (which is a flat `PROGRESS_REPORT_VIEW`/`PROGRESS_LOG_CREATE` OR), and not in the frontend Progress Report screen (confirmed: referenced only in an explanatory code comment). A faculty member cannot reach the Progress Report route at all today (`PROGRESS_REPORT_VIEW` is not granted to the Faculty role by default), so the flag is currently inert regardless of its stored value.
- **`sessionGroupId`/`spanPeriodIds`** (OC-127, multi-period sessions) — placed/staffed/removed atomically as one unit when present, but every conflict/constraint check still operates per-row against each row's own `Period`; there is no separate "block-level" constraint check distinct from the per-period ones.

---

## 8. Known Gaps / Deferred

Carried forward from SRS §6/BRD §7 (code-confirmed, not fixed as part of this documentation pass), plus new findings from this FRD pass:

- No single centralized constraint-validation orchestrator — `placeCell`/`moveCell`/`staffCell` each assemble their own check list independently; `TimetableSwapService.evaluateSlot` and `FacultySessionSwapService.isFacultyFreeAt` each reimplement conflict logic independently and neither rechecks workload caps.
- ~~`TIMETABLE_GENERATE`, `TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE`, and `TIMETABLE_SKELETON_AUTO_PLACE` are seeded permission codes with no remaining controller reference~~ — **`TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE` and `TIMETABLE_SKELETON_AUTO_PLACE` removed 2026-09-24** via migration `V558`, each with a confirmed "superseded by X" or "intentionally removed per Y" rationale (see the permission table above). `TIMETABLE_GENERATE` remains orphaned — left alone, no equally solid rationale was found for it in this pass.
- ~~`TIMETABLE_CONFLICT_INSPECTOR_VIEW` is likewise a leftover from the retired standalone Conflict Inspector screen... still present in the permission seed data~~ — **corrected 2026-09-24:** it was already removed by `V550`, before this documentation set was written; this doc's earlier claim that it was "still present" was itself wrong.
- **SRS.md §6/BRD.md BR-TT-5 are stale on workload-cap scope — confirmed by direct code read, not just this pass's grep.** Both companion documents state "daily and continuous-run caps remain institution-wide only... only the weekly tier resolves per-faculty/per-designation overrides." That is no longer true of the shipped code: `FacultyWorkloadCapacityService` defines `resolveEffectiveDailyCapacity(Faculty)` and `resolveEffectiveContinuousCapacity(Faculty)` (lines ~144–151) alongside the pre-existing `resolveEffectiveCapacity` (weekly), and `TimetableStaffingService.resolveDailyCap`/`resolveContinuousCap` (lines ~850–865) call them with the identical per-faculty-override → designation-default → flat-institution-config → no-cap precedence used for the weekly tier (`resolveWeeklyCap`, lines ~841–847) — all three tiers are symmetric today. This FRD documents the **current, code-verified** behavior (§7); SRS.md/BRD.md were not updated as part of this pass (out of scope per the task) but should be corrected in a follow-up edit to those files, since this is a factual drift, not a deliberate v1 scope boundary.
- No global multi-cohort CSP/backtracking solver; no alternative-generation/reseed; no pin-lock-then-regenerate beyond "pinned cells survive a rebuild"; no arbitrary day-mapping override at the placement level (Day Mapping Overrides affect display/resource-grid resolution, not placement itself).
- Cross-term double-booking is not runtime-checked (relies on the design assumption that `TermInstance` date ranges never overlap institution-wide).
- No notification firing wired for special-class approve/reject or absence-substitute/staff-swap events.
- No conflict-inspector/pre-publish violation dashboard beyond the per-cohort conflict-status/acknowledge endpoints (the standalone term-wide screen was deliberately retired, not a gap).
- `CourseOffering.secondaryFacultyId` is informational-only, never eligible for staffing/substitution (by design, not a gap).
- Special classes do not count against the weekly workload cap and do not yet feed portion-completion aggregation (both explicit, documented v1 exclusions).
- `PORTION_PLAN_VISIBLE_TO_FACULTY` config toggle exists (V354) but gates nothing anywhere in shipped code, backend or frontend (confirmed independently by both the backend controller read and the frontend screen read in this pass — see §7).
- `V553__restore_cohort_conflict_acknowledge_permission.sql` is uncommitted in the working tree as of this pass — verify it has been committed and applied in every environment before relying on Conflict-Acknowledge access (see §7).
- Staffing (§3.6) has no standalone routed screen/component — confirmed by this pass's frontend read; it is embedded entirely in Timetable Builder. If BR-56/SRS wording elsewhere implies a separate "Staffing screen," that should be read as the Timetable Builder's staffing capability, not a distinct route.
