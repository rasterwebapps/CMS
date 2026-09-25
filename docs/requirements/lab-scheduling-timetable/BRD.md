# Business Requirements Document — Lab Scheduling & Timetable

**App:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Company:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective

A nursing college must schedule Theory, Lab, and Clinical sessions across cohorts, faculty, classrooms, labs, and clinical venues while respecting curriculum-mandated hours, faculty workload limits, room capacity, and INC compliance needs (electives, batch rotation for lab/clinical roster splits). What began as a single "Lab Scheduling" screen (R1-M3.2) has grown into a full timetable engine: a two-phase manual Timetable Builder → Staffing workflow, augmented by automation (auto-place, auto-staff, Global Auto-Schedule), a full lifecycle (draft → publish → lock, with post-publish adjustment tools for real-world disruptions like faculty absence and room unavailability), and downstream portion-completion tracking that ties the timetable to actual syllabus progress.

## 2. Stakeholders

- **Timetable/Academic Coordinator** — owns building, staffing, publishing, and adjusting the institution's timetable.
- **Faculty** — consumes their own schedule, requests special classes, logs progress, records personal unavailability.
- **Students** — consume their own schedule.
- **HOD / Academic Admin** — reviews capacity/workload reports, approves special-class requests, monitors portion-completion shortfall.
- **SKSCON compliance office** — relies on the engine correctly enforcing INC curriculum hour/elective/clinical-duty rules (BR-49) so audits pass.

## 3. Business Rules

- **BR-TT-1 (mapped from BR-56).** A `ClassSchedule` is a recurring weekly master row with no calendar date; a `SessionOccurrence` is the per-date actual instance, created lazily only when something is logged or overridden — never pre-populated for every date up front.
- **BR-TT-2 (mapped from BR-56).** Timetable Builder is the sole manual placement path; the old one-shot randomized generator was retired entirely because it could destructively overwrite in-progress draft work.
- **BR-TT-3 (mapped from BR-56).** Every `CourseOffering` in the same elective group must be placed at the exact same day+period within a term — students choose one elective, so all options must run simultaneously.
- **BR-TT-4 (mapped from BR-56).** A venue is hard-blocked from staffing if it cannot seat the resolved required strength (whole-cohort count for Theory; batch roster count, or the largest rotating batch, for Lab/Clinical).
- **BR-TT-5 (mapped from BR-56).** Faculty weekly workload cap resolves in priority order: per-faculty override → designation default → flat institution-wide config → no cap. Daily and continuous-run caps remain institution-wide only.
- **BR-TT-6 (mapped from BR-56).** Batch rotation (which `Batch` occupies a shared Lab/Clinical cell) is computed from actual elapsed weeks since a fixed anchor date, not raw ISO week number, so it stays predictable across year boundaries.
- **BR-TT-7 (mapped from BR-56).** Cohort Room Allocation "commit" is a hard, term-scoped exclusive room claim (DB-unique-index enforced) — a claim on *ownership*, not a day/period lock; actual slot conflicts are still resolved later at Staffing time. Revert never deletes — it soft-deactivates so roster history survives.
- **BR-TT-8 (mapped from BR-57).** Any timetable write is blocked once a `TermInstance` is `LOCKED`, independent of each row's own DRAFT/PUBLISHED status.
- **BR-TT-9 (mapped from BR-57).** Faculty Absence recording and substitute application are two separate, explicit steps with no auto-apply — a coordinator must deliberately choose and apply one substitute per affected session, re-validated at apply time.
- **BR-TT-10 (mapped from BR-57).** A substitute or a relocated room is always a **per-date override** on `SessionOccurrence` — the recurring `ClassSchedule` row is never mutated, so every other date of the same weekly schedule keeps showing the original faculty/room.
- **BR-TT-11 (mapped from BR-55).** Special/remedial classes go through a faculty-request → admin-approval workflow, must use existing Period-grid slots (never free-form times), and do not count against the faculty weekly workload cap (a deliberate v1 exclusion, not an oversight).
- **BR-TT-12 (mapped from BR-58).** A holiday `CalendarEvent` automatically generates and re-syncs `BlockedPeriod` rows on create/update/delete; an admin may still delete one specific auto-generated block to run a special class during an otherwise-blocked slot.
- **BR-TT-13 (mapped from BR-58).** A portion-completion Blueprint's Planned date is frozen once generated and only moves on explicit regenerate; the Projected/Actual date recomputes live on every read against the current timetable, so holidays cascade the projection forward automatically.
- **BR-TT-14 (mapped from Monday–Friday Scheduling Rules, OC-227, current code).** Once a term opts into any working-Saturday pattern, Saturday is a regular working day for Theory/Lab/Clinical/Library, not a fallback; curriculum hours are planned against the term's total working-period occurrences, crediting each placed session by how often it actually runs.

## 4. Business Process / Workflow

1. **Capacity Auto-Plan.** Coordinator commits a term-scoped room allocation per cohort (home Theory classroom + Lab/Clinical batch-venue splits), optionally splitting into `CohortSection`s and draft `Batch`es.
2. **Timetable Builder.** Coordinator places every non-elective `CourseOffering` for a cohort/term into a day×period grid — manually, via drag-move, or via Auto-place for remaining shortfall (or Global Auto-Schedule for a full-term automated plan against curriculum hours, including Library/Sports/Self-Study filler and elective placement).
3. **Staffing.** Coordinator assigns faculty/room to each placed cell — manually, or via Auto-staff for remaining unstaffed cells — hard-blocked by capacity fit and workload caps.
4. **Conflict check.** Coordinator reviews per-cohort conflict status and acknowledges any flagged issues before publishing.
5. **Publish.** Coordinator approves the draft, converting it to `PUBLISHED` (optionally overriding an incomplete-coverage warning if permitted).
6. **Post-publish adjustments (ongoing).** As real-world disruptions occur: mark a Faculty Absence and apply a per-date substitute; run a Staff Session Swap between two published sessions; relocate a room for a single date; or submit/approve a Special/Remedial Class request.
7. **Consumption.** Students and staff view their own schedule via My Timetable (split by audience); coordinators cross-check resource utilization via the Resource Timetable Grid; faculty log per-occurrence progress, which rolls up into the Progress Report against the frozen Blueprint.
8. **Term close.** Once the term is `LOCKED`, no further timetable writes are accepted anywhere in the module (including special-class requests).

## 5. Success Criteria

Not formally defined with quantitative KPIs. Inferred from feature completeness: every cohort's term can be placed, staffed, published, and adjusted without double-booking a faculty member or room; INC-mandated curriculum hours are demonstrably planned and trackable to completion; post-publish disruptions (absence, room unavailability) can be handled without editing the recurring schedule.

## 6. Assumptions & Constraints

- The module assumes `TermInstance` date ranges never overlap institution-wide (no runtime cross-term double-booking check).
- Theory instructor, Lab instructor, and Batch/Clinical coordinator each reuse a single existing field (`CourseOffering.facultyId`, `LabSchedule.faculty`, `Batch.coordinatorFacultyId`) rather than a generic multi-role join table — a deliberate scope boundary, not an oversight.
- `Batch` creation is exclusively a byproduct of the Capacity Auto-Plan commit flow (`CohortRoomAllocationService.createVentureBatch`) — there is no manual "create batch" UI or endpoint (removed as dead code per OC-191); any future request to add one back is a deviation requiring full specialist review.
- No print/export capability existed as of the last full BR pass (BR-56); a 4th "Consolidated" export-capable view was decided via specialist review but export status per-view should be re-verified against current code before being assumed complete.

## 7. Known Gaps / Deferred

See SRS §6 for the complete, code-confirmed list (orphaned permissions, no centralized constraint orchestrator, no cross-term conflict check, no notification wiring on several adjustment flows, `PORTION_PLAN_VISIBLE_TO_FACULTY` unwired, etc.). Also note: an uncommitted migration (`V553`) exists in the working tree restoring a permission an earlier migration incorrectly deleted — flagged for verification, not resolved by this documentation task.
