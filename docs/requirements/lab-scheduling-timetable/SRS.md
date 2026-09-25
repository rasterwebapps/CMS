# Software Requirements Specification — Lab Scheduling & Timetable

**Module:** Lab Scheduling & Timetable (Timetable Builder / Class Schedule engine)
**App:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Source milestone:** R1-M3.2 (`docs/RELEASE_1_MILESTONES.md` lines 317–337) — **historical only**. The shipped module has evolved far past its original scope (a single lab-scheduling screen); this document describes the system as it exists in code today.

---

## 1. Introduction

### 1.1 Purpose
Defines requirements for the full timetable/class-scheduling engine: the recurring weekly `ClassSchedule` master and its per-date `SessionOccurrence` actuals, the Timetable Builder (placement) and Staffing (faculty/room assignment) workflows, Capacity Planning, batch rotation, lifecycle (draft→publish→lock) gating, post-publish adjustments (faculty absence/substitute, staff swap, room relocation), the Special/Remedial Class Scheduler, portion-completion tracking, and holiday-driven scheduling. Also covers the legacy manual Lab Schedules CRUD screen that still reads/writes the same underlying table.

### 1.2 Scope
In scope: everything under the "Timetable"/"Lab Schedules" nav entries and their backing controllers/services (`Timetable*`, `ClassSchedule*`, `FacultyAbsence*`, `FacultySessionSwap*`, `RoomRelocation*`, `RotationGroup*`, `SpecialClassRequest*`, `CohortRoomAllocation*`, `PortionBlueprint*`, `HolidayTemplate*`). Out of scope: Attendance marking itself (separate module, though it depends on `ClassSchedule`), the base Academic Calendar CRUD (`CalendarEvent`, predates this engine), Curriculum Map / Course Registration / Elective Assignment screens (separate modules this engine consumes), Escort Rotation (a related but functionally distinct transport/duty-rotation feature sharing the Academics nav group — not detailed here).

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md` lines 317–337 (historical)
- `docs/BUSINESS_REQUIREMENTS.md`: BR-49 (INC compliance, batches, attendance thresholds — the module's origin point), BR-54 (Room Purpose, infrastructure dependency), BR-55 (Special/Remedial Class Scheduler), **BR-56** (Core Engine — data model, placement, staffing, rotation, capacity), **BR-57** (Lifecycle, Audit Trail, Post-Publish Adjustments), **BR-58** (Portion-Completion & Holiday-Driven Scheduling)
- Backend: `backend/src/main/java/com/cms/{model,controller,service,dto}/{ClassSchedule,Timetable,SessionOccurrence,RotationGroup,FacultyAbsence,FacultySessionSwap,RoomRelocation,SpecialClass,PortionBlueprint,HolidayTemplate}*.java`
- Frontend: `frontend/src/app/features/timetable/*`, `frontend/src/app/features/lab-schedule/*`, `frontend/src/app/shared/{week-grid,week-navigator,day-agenda}/*`
- Nav config: `frontend/src/app/core/nav/nav-config.ts` (Academics group)

## 2. Overall Description

### 2.1 Product Perspective
Two layers underpin the entire module: `ClassSchedule` (table `class_schedules`, originally `lab_schedules`, renamed/generalized V293 to cover THEORY as well as LAB, then CLINICAL added V334) is the **recurring weekly master** row (day-of-week + period + term, no calendar date); `SessionOccurrence` (V322) is the **per-date actual instance**, created lazily only when something is logged or overridden for a specific date. Everything else in the module — placement, staffing, capacity planning, rotation, lifecycle, post-publish adjustments, portion tracking — operates on this two-layer model.

The module replaced an earlier one-shot randomized generator (`TimetableGenerationService.generate()`, deleted OC-111) with a manual two-phase **Timetable Builder** (placement) → **Staffing** (faculty/room assignment) workflow, augmented by **Auto-place**/**Auto-staff** and, most recently, a **Global Auto-Schedule** report that plans an entire cohort's term at once against curriculum hours.

### 2.2 Actors / User Classes
- **Timetable Admin / Academic Coordinator** — builds and staffs the timetable, runs capacity planning, approves/publishes, handles post-publish adjustments (absence, swap, room relocation), approves special-class requests.
- **Faculty** — views own timetable (`/my-timetable/staff`), logs session progress, requests special/remedial classes, records own recurring unavailability.
- **Student** — views own timetable (`/my-timetable/student`).
- **HOD / Progress reviewer** — views Progress Report (portion-completion tracking).
- **Lab Schedule maintainer (legacy)** — uses the plain CRUD `/lab-schedules` screen for direct row-level edits over the same `class_schedules` table.

### 2.3 Operating Environment
Angular SPA (`frontend/src/app/features/timetable/`, `features/lab-schedule/`) using shared components `CmsWeekGridComponent`, `CmsWeekNavigatorComponent`, `CmsDayAgendaComponent`. Spring Boot REST API under `/timetables/*` and `/lab-schedules`, PostgreSQL via Flyway (`class_schedules`, `session_occurrences`, and ~20 supporting tables). Keycloak JWT auth; DB-driven granular permission system (`@perm.has`/`@perm.hasAny`).

### 2.4 Constraints / Assumptions
- Cross-term double-booking is not runtime-checked — relies on the design assumption that `TermInstance` date ranges never overlap institution-wide.
- There is no single centralized constraint-validation orchestrator: `placeCell`, `moveCell`, and `staffCell` each independently assemble their own list of checks to run, even though the individual check implementations are shared (`TimetableBlockedPeriodChecker`, `TimetableConstraintViolationException`).
- No print/export capability exists anywhere in the module as of the last documented state (BR-56) — though a "Consolidated" export-capable 4th view was decided via specialist review 2026-09-22 (OC-262); current repo state shows active frontend work on `timetable-view`/`my-timetable`/`week-grid`/`day-agenda` (see git status) consistent with this still being built out.
- View-mode terminology in code: `week` (labelled "Generic" in UI per BR-56's 2026-09-22 rename), `dateWise` ("Date-wise-weekly"), `day` ("Day") — `TimetableViewMode` type in `timetable-view.component.ts`/`my-timetable.component.ts`.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-TT-1 | Maintain recurring weekly class schedules (legacy CRUD: create/list/update/delete/conflict-check) over `class_schedules` | Must | — |
| FR-TT-2 | Timetable Builder: place a subject into a cohort/term grid cell (`placeCell`), respecting elective-slot, blocked-period, and cohort-exclusivity rules | Must | FR-TT-1 |
| FR-TT-3 | Move a placed cell to a new day/period, re-validated against the same placement (and, if staffed, staffing) rules | Must | FR-TT-2 |
| FR-TT-4 | Pin / replace / swap / relocate a placed cell | Should | FR-TT-2 |
| FR-TT-5 | Auto-place remaining unplaced sessions (bounded single-attempt backtracking greedy scan) per cohort | Should | FR-TT-2 |
| FR-TT-6 | Global Auto-Schedule: plan an entire term's curriculum hours (Theory/Lab/Clinical/Library/Sports/Self-Study) across working days including chosen working Saturdays | Should | FR-TT-2, Curriculum Map |
| FR-TT-7 | Staffing: assign faculty/room to a placed cell, hard-blocked by capacity fit and configurable workload caps (daily/weekly/continuous-run) | Must | FR-TT-2 |
| FR-TT-8 | Auto-staff remaining unstaffed cells for a term (no backtracking; ranks by subject-continuity) | Should | FR-TT-7 |
| FR-TT-9 | Batch rotation: N-way cyclical rotation of which `Batch` occupies a shared Lab/Clinical cell, computed from elapsed weeks since an anchor date | Should | FR-TT-2 |
| FR-TT-10 | Capacity Planning: commit a term-scoped exclusive room claim (Theory classroom + Lab/Clinical batch-venue splits) per cohort, with revert (soft-deactivate, not delete) | Must | FR-TT-2 |
| FR-TT-11 | Advisory capacity/workload reports (term overview, faculty workload, venue capacity) — read-only, block nothing | Should | FR-TT-10 |
| FR-TT-12 | Timetable lifecycle: draft → approve/publish → discard-draft / revert-to-published-to-draft, all gated by term-lock status | Must | FR-TT-2, FR-TT-7 |
| FR-TT-13 | Conflict Inspector: per-cohort conflict-status check and acknowledge action before publish | Must | FR-TT-12 |
| FR-TT-14 | Draft-stage Staff Session Swap (day/period-mutating, `TimetableSwapService`) | Should | FR-TT-12 |
| FR-TT-15 | Post-publish Faculty Absence recording, affected-session lookup, eligible-substitute lookup, and explicit per-session substitute apply (per-date override, never mutates the recurring row) | Must | FR-TT-12 |
| FR-TT-16 | Post-publish Staff Session Swap (mutual, both-direction, per-date override) between two published sessions | Should | FR-TT-12 |
| FR-TT-17 | Post-publish single-date Room Relocation (Day view only), with per-date conflict-checking and revert | Should | FR-TT-12 |
| FR-TT-18 | Resource Timetable Grid: cross-cohort, per-resource (Faculty or Room) one-day view | Should | FR-TT-12 |
| FR-TT-19 | Recurring Unavailability (formerly "Faculty Availability"): standing weekly faculty block, consumed as a hard constraint everywhere faculty availability is checked | Must | FR-TT-7 |
| FR-TT-20 | Special/Remedial Class Scheduler: faculty-requested, admin-approved single-subject or whole-day-repeat ad-hoc sessions on Period-grid slots | Should | FR-TT-12, BR-55 |
| FR-TT-21 | Portion-Completion tracking: base per-occurrence unit-completion logging, plus a frozen Planned-date Blueprint and a live-recomputed Projected/Actual date | Should | FR-TT-1 |
| FR-TT-22 | Holiday auto-block: a `HOLIDAY` calendar event auto-generates/syncs `BlockedPeriod` rows (whole-day or half-day subset), kept in sync on create/update/delete | Must | Academic Calendar |
| FR-TT-23 | Holiday Templates: recurring-holiday master (yearly/monthly/custom, nth-weekday patterns) auto-seeding events into new academic years | Should | FR-TT-22 |
| FR-TT-24 | My Timetable: separate Student and Staff self-service timetable views, staff variant retains a Log Progress action | Must | FR-TT-12 |
| FR-TT-25 | Faculty Workload Rules screen: scoped editor over the institution-wide daily/weekly/continuous-run cap config | Should | FR-TT-7 |

## 4. External Interface Requirements

### 4.1 Screens (nav-config.ts, Academics group)
Capacity Auto-Plan · Timetable Builder · Timetable (browse/My Timetable split: Student/Staff) · Resource Timetable · Lab Schedules (legacy CRUD) · Staff Session Swap · My Special Classes / Special Class Approvals · Faculty Workload Rules · (adjacent, not detailed: My Escort Duties)

### 4.2 API Endpoints (representative — see FRD for full tables)
Base paths: `/timetables`, `/timetables/skeleton`, `/timetables/staffing`, `/timetables/capacity-plan`, `/lab-schedules`, `/faculty-absences`, `/faculty-session-swaps`, `/room-relocations`, `/rotation-groups`, `/special-classes`, `/portion-blueprints`, `/holiday-templates`, `/timetables/workload-rules`, `/cohort-room-allocations`.

### 4.3 Key DB Entities
`class_schedules`, `session_occurrences`, `curriculum_elective_groups`, `clinical_venues`, `blocked_periods`, `rotation_groups`/`rotation_slots`/`rotation_members`/`rotation_member_assignments`, `cohort_sections`, `cohort_room_allocations`, `batches`/`batch_students`, `faculty_absences`, `syllabus_unit_plan`, `holiday_templates`, `attendance_thresholds` (shared with Attendance module).

## 5. Non-Functional Requirements

- **Performance:** Auto-place/Auto-staff are bounded (single-attempt backtrack for placement, none for staffing) to keep response times predictable rather than running an unbounded search; Global Auto-Schedule plans against total term working-period occurrences rather than per-week iteration.
- **Security / RBAC:** Highly granular — over 25 distinct `TIMETABLE_*` permission codes gate individual operations (view/manage/move/pin/replace/auto-place/auto-staff/rebalance/discard/publish/approve-incomplete-override/conflict-acknowledge/swap/room-relocate/rotation/workload-rules/etc.), following the operation-wise permission mapping rule. `TIMETABLE_GENERATE` and `TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE` are confirmed orphaned (no controller references).
- **Auditability:** Every timetable-mutating action that changes committed state (discard, approve, revert-to-draft, staff swap, absence substitute apply, room relocation/revert, special-class request actions) is recorded via `AuditLogService.record()`, running in its own `REQUIRES_NEW` transaction so the audit row survives even if the caller's transaction rolls back. Actor resolution uses the JWT `preferred_username` claim with a `"system"` fallback in test contexts.

## 6. Known Gaps / Not Yet Implemented

(Carried forward from BR-56/57/58, confirmed still applicable at time of writing; not fixed as part of this documentation task.)

- No single centralized constraint-validation orchestrator — `placeCell`/`moveCell`/`staffCell` each assemble their own check list independently.
- `TIMETABLE_GENERATE` and `TIMETABLE_CAPACITY_PLANNER_BATCH_CREATE` permissions are orphaned (no remaining controller references).
- Daily and continuous-run workload caps remain institution-wide only; only the weekly tier resolves per-faculty/per-designation overrides.
- No global multi-cohort CSP/backtracking solver, no alternative-generation/reseed, no pin-lock-then-regenerate, no arbitrary day-mapping override, no multi-period (`periodSpan`) spanning session.
- Cross-term double-booking is not runtime-checked.
- `TimetableSwapService.evaluateSlot` and `FacultySessionSwapService.isFacultyFreeAt` each reimplement conflict logic independently of the shared checks, and neither rechecks workload caps.
- No notification firing wired for special-class approve/reject or absence-substitute/staff-swap events (BR-28's notification backend not yet extended to these categories).
- No conflict-inspector / pre-publish violation dashboard beyond the per-cohort conflict-status/acknowledge endpoints.
- `CourseOffering.secondaryFacultyId` is informational-only, never eligible for staffing/substitution (by design).
- Special classes do not count against the weekly workload cap, and do not yet feed portion-completion aggregation (both explicit, documented v1 exclusions).
- `PORTION_PLAN_VISIBLE_TO_FACULTY` config toggle exists (V354) but gates nothing — Progress Report remains admin/HOD-only regardless of its value.
- **A migration in this repo's working tree (`V553__restore_cohort_conflict_acknowledge_permission.sql`) is uncommitted at the time of this documentation pass** — it restores `TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE` after an earlier migration (V550) incorrectly deleted it while the endpoint still required it. Documented here as current intended state; confirm it has been applied/committed before relying on conflict-acknowledge access in any environment.
