# Business Requirements Document — INC Nursing Curriculum Compliance

**Module slug:** `inc-curriculum-compliance`
**Source BR:** BR-49 · **Milestone:** R1-M6.1

## 1. Executive Summary / Business Objective

SKSCON's nursing programs follow Indian Nursing Council (INC) curriculum norms that the original
curriculum model could not express: a subject's Theory/Lab/Clinical contact hours and its INC
category (Core/Foundational/Elective) vary **by the semester/curriculum mapping**, not just by the
subject itself. This module adds that expressiveness plus the operational machinery it implies —
choice-based electives, per-component attendance thresholds, and lab/clinical roster batching —
so the college's curriculum data and day-to-day attendance/registration workflows match how INC
programs are actually run and accredited.

## 2. Stakeholders

- **Academic/Curriculum office** — defines curriculum versions, hours, and elective structure.
- **Admissions/Academic admin** — assigns students to elective offerings each term.
- **Faculty** — batch coordinators (nudged, not required, to be assigned per batch).
- **Students** — attendance thresholds and elective outcomes affect their eligibility for
  promotion (see BR-52).
- **INC/university accreditation bodies** — indirect stakeholders; per-component hours and
  attendance feed compliance reporting.

## 3. Business Rules

| ID | Rule | Rationale |
|---|---|---|
| BR-INC-1 | Theory/Lab/Clinical hours and Subject Type live on the curriculum-mapping row (`curriculum_term_courses`), not on `Subject`. | The same subject's component mix legitimately differs by term/curriculum (e.g. Nursing Foundations: Theory/Lab-heavy Term I, Clinical-heavy Term II). |
| BR-INC-2 | Unchecking a component (Theory/Lab/Clinical) zeroes and disables its hour field rather than requiring all three filled. | Most subjects only use a subset of components; forcing all three is needless friction. |
| BR-INC-3 | Electives are grouped by `CurriculumElectiveGroup`; bulk registration generation skips elective offerings entirely, leaving them for explicit admin assignment. | Bulk auto-registration can't know which elective a student picked — an explicit admin step is required. |
| BR-INC-4 | A second elective pick within the same group for the same student is rejected unless the admin explicitly confirms a change (frontend), and the assignment endpoint is idempotent on re-submit of the same offering. | Prevents silent double-registration into mutually exclusive options while still allowing deliberate correction. |
| BR-INC-5 | An elective group may be `STUDENT_CHOICE` (each student picks) or `INSTITUTION_DECIDED` (one offering bulk-applied to the whole eligible group). | Reflects how many colleges centrally decide the elective rather than truly offering student choice. |
| BR-INC-6 | Attendance minimums resolve per curriculum mapping + component type, walking student → registration → offering → mapping → threshold row, falling back to 75% at any missing step. | INC requires different minimums per component (e.g. 80% Theory / 100% Clinical); a single blended number would hide a student failing just one component. |
| BR-INC-7 | A `Batch` (roster split for lab/clinical, e.g. 60 → 3×20) is scoped to a term's `CourseOffering`, not the curriculum mapping, and enforces capacity at the service layer. | A batch is a per-term roster split (operational), not curriculum-design metadata; capacity enforcement follows this codebase's existing convention (e.g. `Cohort` seat limits). |
| BR-INC-8 | Batch creation is exclusively owned by the Capacity Auto-Plan commit flow — there is no manual "create batch" affordance anywhere in the app. | Settled project-wide hard gate (post-BR-49): prevents batches existing outside the room/capacity allocation the rest of the timetable engine depends on. See Known Gaps. |
| BR-INC-9 | `lab_schedules.batch_name` (free text) is kept and kept in sync alongside `batch_id` indefinitely. | A hard cutover would require making `batchId` mandatory and rewriting conflict-detection — judged not worth pursuing without a concrete pain point. |

## 4. Business Process / Workflow Narrative

1. **Curriculum design:** Curriculum admin opens Curriculum Map for a version, adds a subject to a
   term, ticks the components that apply (Theory/Lab/Clinical), sets hours, picks Subject Type, and
   optionally marks it elective — which auto-resolves or creates that term's single elective group.
   Inline, the admin sets per-component attendance thresholds if they differ from the 75% default.
2. **Term opens / offerings generate:** `CourseOfferingService` generates offerings from the
   curriculum. Bulk course-registration generation registers every student for non-elective
   offerings automatically, skipping elective ones.
3. **Elective assignment:** Academic admin opens Elective Assignment, picks academic year → term →
   elective group, and either assigns students individually or (if the group is set to
   `INSTITUTION_DECIDED` or just for speed) uses "Apply to All" to bulk-assign every eligible
   student to one offering.
4. **Attendance tracking:** As attendance is recorded, `AttendanceService.getAttendanceReport()`
   returns one line per component type per student, each checked against its own resolved
   threshold — feeding downstream consumers like Student Promotion's arrear detection (BR-52).
5. **Batch/roster splitting:** For LAB/CLINICAL sessions needing sub-groups, the timetable's
   Capacity Auto-Plan creates `Batch` rows scoped to the course offering, with roster membership
   and a nudged (not mandatory) coordinator faculty. A Course-Offering-scoped admin can then edit
   capacity/coordinator, manage roster, or delete a batch (blocked if it still has students/
   timetable data).

## 5. Success Criteria

Not formally defined with a KPI — inferred from feature completeness: curriculum data can express
per-mapping component hours/type, electives route through a controlled assignment step instead of
blind bulk-registration, attendance is evaluated per component against a configurable threshold,
and lab/clinical rosters are enforced batches rather than free text. All of the above are live and
enforced in the shipped code as of this review.

## 6. Assumptions & Constraints

- Faculty-role scoping (theory instructor, lab instructor, batch coordinator) deliberately reuses
  existing single-value fields rather than introducing a multi-role join table — acceptable unless
  a future need arises for one faculty holding multiple concurrent roles on one offering in a
  reportable way.
- `CLINICAL` attendance type required no migration since `attendances.type` was already an
  unconstrained `VARCHAR`.
- Bulk generate/drop endpoints on `CourseRegistrationController` were left on pre-existing
  permissions (`ADMISSION_VIEW`/`ADMISSION_CREATE`) at BR-49 ship time to avoid silently changing
  access for roles holding those permissions for unrelated reasons — though the `/generate`
  endpoint has since (V525) moved to its own dedicated permission (see Known Gaps).

## 7. Known Gaps / Deferred

- **Manual batch creation removed.** What BR-49 originally shipped (a Batch Manage create dialog
  under Course Offering) no longer exists in the current app — Batch creation is now exclusively
  the Capacity Auto-Plan commit flow's responsibility, per a later, settled project-wide decision.
  This BRD documents BR-49's original intent for context but the *current* creation path is
  outside this module.
- **`lab_schedules.batch_name` hard cutover** — deferred; `batchName` remains load-bearing
  server-side (reconfirmed 2026-07-22).
- **Student self-service elective selection** — not built; every assignment is admin-entered.
- **A generic multi-faculty-role join table** — not built; each faculty role on an offering reuses
  an existing single-value field.
