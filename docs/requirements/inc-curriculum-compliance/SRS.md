# Software Requirements Specification — INC Nursing Curriculum Compliance

**Module slug:** `inc-curriculum-compliance`
**Product:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Source BR:** BR-49 (docs/BUSINESS_REQUIREMENTS.md), Milestone R1-M6.1 (docs/RELEASE_1_MILESTONES.md, lines 632–652)
**Basis:** Reverse-engineered from shipped code as of 2026-09-24.

## 1. Introduction

### 1.1 Purpose
Documents the requirements actually implemented for expressing Indian Nursing Council (INC)
curriculum structure — per-semester/per-mapping Theory/Lab/Clinical contact hours, subject
category (Core/Foundational/Elective/Co-Curricular), choice-based and institution-decided
electives, per-component attendance thresholds, and roster-splitting batches — none of which the
pre-existing curriculum/attendance model could express.

### 1.2 Scope
Covers: `curriculum_term_courses` hour/type/elective columns; `CurriculumElectiveGroup` and its
selection mode; `attendance_thresholds`; the `Batch`/`batch_students` roster-split entities as they
exist today (creation path now owned by Timetable/Capacity Auto-Plan, not this module — see §6);
`CourseRegistration`'s elective-assignment endpoints; and the Curriculum Map / Elective Assignment
screens.

### 1.3 References
- `docs/BUSINESS_REQUIREMENTS.md` — BR-49 (primary), BR-56 (Batch's later ownership transfer to
  Capacity Auto-Plan), BR-52 (Student Promotion, downstream consumer of curriculum hours/arrears)
- `docs/RELEASE_1_MILESTONES.md` — R1-M6.1
- Project CLAUDE.md — "Batches are created by Capacity Auto-Plan alone" hard gate

## 2. Overall Description

### 2.1 Product Perspective
An extension of the existing Curriculum Version / Curriculum Map subsystem. It does not replace
`Subject` (the reusable subject master) — hours and INC category live on the curriculum-mapping
row (`curriculum_term_courses`) because the same subject can have different hours/category across
different curricula or terms (e.g. Nursing Foundations is Theory/Lab-heavy in Term I, Clinical-heavy
in Term II).

### 2.2 Actors / User Classes
- **Curriculum admin** (holds `CURRICULUM_MANAGE`, `CURRICULUM_ELECTIVE_GROUP_MANAGE`,
  `ATTENDANCE_THRESHOLD_MANAGE`) — maintains Curriculum Map, elective groups, thresholds.
- **Admissions/Academic admin** (holds `COURSE_REGISTRATION_ELECTIVE_ASSIGN`) — assigns/bulk-applies
  student elective choices.
- **Batch/Course Offering admin** (holds `BATCH_MANAGE`) — edits, deletes, and manages roster of a
  `Batch` (not create — see §6).
- Exact role→permission mapping is DB-only (Role Management module); not hardcoded.

### 2.3 Operating Environment
Angular frontend (`frontend/src/app/features/curriculum`, `.../elective-assignment`,
`.../batch`), Spring Boot backend (`com.cms.controller/service/model/repository`), PostgreSQL via
Flyway (V265–V274, V384, V530).

### 2.4 Constraints / Assumptions
- Faculty-role scoping deliberately reuses existing single-value fields (`CourseOffering.facultyId`
  for theory instructor, `LabSchedule.faculty` for lab instructor) rather than a new multi-role join
  table.
- `lab_schedules.batch_name` (free text) is kept indefinitely alongside `batch_id` — no hard cutover.
- `CLINICAL` was added to the `AttendanceType` enum with no migration (`attendances.type` is an
  unconstrained `VARCHAR`).

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-INC-1 | Curriculum Map lets an admin toggle Theory/Lab/Clinical per mapping row; unchecking a component zeroes and disables its hour field. | Must | Curriculum Version exists |
| FR-INC-2 | Each mapping row carries a `SubjectType`: `CORE`, `FOUNDATIONAL`, `ELECTIVE`, `CO_CURRICULAR`. Picking `ELECTIVE` auto-sets the elective flag and vice versa (two-way sync, no contradiction possible). | Must | FR-INC-1 |
| FR-INC-3 | `PUT /curriculum-semester-courses/{id}` allows in-place edit of hours/type/elective/sort-order — previously add/remove only. | Must | — |
| FR-INC-4 | A `CurriculumElectiveGroup` scopes mutually-exclusive elective offerings to one curriculum version + term. A term has at most one group in the UI (auto-resolved/auto-created, no manual picker). | Must | FR-INC-2 |
| FR-INC-5 | Each elective group has a `selectionMode`: `STUDENT_CHOICE` (default) or `INSTITUTION_DECIDED`. | Should | FR-INC-4 |
| FR-INC-6 | Bulk course-registration generation (`generateRegistrationsForTermInstance`) skips any offering whose mapping is marked elective; an offering with no resolved curriculum mapping is treated as non-elective (preserves legacy behavior). | Must | FR-INC-4 |
| FR-INC-7 | `POST /course-registrations/elective-assignment` lets an admin single-pick a student's elective offering, idempotent, rejects a second pick within the same group unless explicitly changed (frontend confirms and drops the prior registration). | Must | FR-INC-4 |
| FR-INC-8 | `POST /course-registrations/elective-assignment/bulk` ("Apply to All") assigns every eligible student in a group to one offering in one call, reporting assigned/blocked counts. | Should | FR-INC-7 |
| FR-INC-9 | Per-component attendance thresholds (`attendance_thresholds`, keyed on curriculum mapping + `AttendanceType`) override a 75% default at any missing step in the student→registration→offering→mapping resolution chain. | Must | FR-INC-1 |
| FR-INC-10 | `AttendanceService.getAttendanceReport()` returns one entry per component type instead of one blended percentage. | Must | FR-INC-9 |
| FR-INC-11 | `Batch` (course-offering-scoped roster split) supports update, delete (hard delete, blocked if it has students or timetable data), roster add/remove, and a lifecycle-impact check. Capacity is a service-layer check, not a DB constraint. | Must | — |
| FR-INC-12 | Batch **creation** is exclusively performed by `CohortRoomAllocationService.createVentureBatch` (Capacity Auto-Plan commit flow) — no manual/API create path exists (removed, see §6). | Must | Timetable/Capacity module |
| FR-INC-13 | `LabSchedule`/`ClassSchedule` can reference a `Batch` via nullable `batch_id`, populated alongside the pre-existing free-text `batch_name`. | Should | FR-INC-11 |

## 4. External Interface Requirements

### 4.1 Screens
- **Curriculum Map** (`curriculum-map.component`) — per-term subject list with add/edit form
  (Theory/Lab/Clinical checkboxes + hours, Subject Type, elective toggle, inline attendance
  threshold inputs), grouped display (regular subjects by CORE→FOUNDATIONAL→CO_CURRICULAR rank,
  then elective groups bucketed together), term hour summary (elective groups counted once via a
  representative offering).
- **Elective Assignment** (`elective-assignment.component`) — academic year → term → elective
  group cascade; group-launcher cards with progress (%); STUDENT_CHOICE/INSTITUTION_DECIDED mode
  toggle; per-student assign dropdown + "Apply to All" bulk action; integrates with Timetable
  Builder's elective-group schedule status.
- Batch management is a dialog/panel reached from a Course Offering row (edit/roster only, no
  create action in the current UI).

### 4.2 API Endpoints (high level)
| Method & Path | Permission |
|---|---|
| `POST /curriculum-elective-groups` | `CURRICULUM_ELECTIVE_GROUP_MANAGE` |
| `GET /curriculum-elective-groups` | `CURRICULUM_ELECTIVE_GROUP_VIEW` |
| `PUT /curriculum-elective-groups/{id}/selection-mode` | `CURRICULUM_ELECTIVE_GROUP_MANAGE` |
| `DELETE /curriculum-elective-groups/{id}` | `CURRICULUM_ELECTIVE_GROUP_MANAGE` |
| `GET/PUT/DELETE /attendance-thresholds` | `ATTENDANCE_THRESHOLD_VIEW`/`MANAGE` |
| `GET/PUT/DELETE /batches[/...]` | `BATCH_VIEW`/`BATCH_MANAGE` |
| `PUT /curriculum-semester-courses/{id}` | `CURRICULUM_MANAGE` |
| `POST /course-registrations/generate` | `COURSE_REGISTRATION_GENERATE` |
| `POST /course-registrations/elective-assignment[/bulk]` | `COURSE_REGISTRATION_ELECTIVE_ASSIGN` |
| `GET /course-registrations/elective-assignment/summary` | `ADMISSION_VIEW` or `COURSE_REGISTRATION_ELECTIVE_ASSIGN` |

### 4.3 Key DB Entities
`curriculum_term_courses` (+ hours/type/elective/group columns), `curriculum_elective_groups` (+
`selection_mode`), `attendance_thresholds`, `batches`, `batch_students`, `course_offerings` (+
`curriculum_term_course_id`), `lab_schedules`/`class_schedules` (+ `batch_id`).

## 5. Non-Functional Requirements

- **Performance:** Threshold resolution walks a fixed-depth chain (student → registration →
  offering → mapping → threshold row) per attendance-report call; no batch/N+1 concerns documented.
- **Security/RBAC:** Every endpoint gated by a dedicated DB-driven permission (see §4.2); no
  endpoint here is open to unauthenticated callers.
- **Auditability:** No dedicated audit trail for curriculum/elective/batch changes beyond standard
  `created_at`/`updated_at`; `Batch` has optimistic-lock `version` for concurrent-edit safety.
- **Data integrity:** `batches` has a `UNIQUE(course_offering_id, name)` constraint; elective
  groups are unique per `(curriculum_version_id, term_number, group_code)`.

## 6. Known Gaps / Not Yet Implemented

- **Batch creation has moved out of this module's UI entirely.** BR-49 originally shipped a "Batch
  Manage" create dialog nested under Course Offerings; the project's later hard gate ("Batches are
  created by Capacity Auto-Plan alone") removed the manual create endpoint (`POST /batches`) and
  the frontend create affordance as dead code (OC-191). `BatchController`/`BatchService` today only
  expose update/delete/roster/lifecycle-impact — confirmed by reading the live controller and
  `batch.service.ts` (no `create()` method). Any request to reintroduce manual batch creation is a
  deviation requiring full specialist review, not a fix to this module.
- **`lab_schedules.batch_name` hard cutover** — explicitly deferred (see BR-49 Explicitly Out of
  Scope); `batchName` remains authoritative server-side for LAB conflict-detection, `batchId` is
  still a soft convenience FK. Reconfirmed deferred as of 2026-07-22.
- **Student self-service elective selection** — not built; assignment is always admin-entered.
- **`COURSE_REGISTRATION_GENERATE` permission** — BR-49's text says the bulk generate endpoint
  "deliberately stays on `ADMISSION_VIEW`/`ADMISSION_CREATE`"; this has since changed (V525) to a
  dedicated permission (labeled "Backfill Course Registrations" in the UI) — BR-49's text on this
  point is stale; the code is current.
- **`SubjectType.CO_CURRICULAR`** and `ElectiveSelectionMode.INSTITUTION_DECIDED` were added after
  BR-49 shipped (see `V530__reclassify_self_study_subjects_as_co_curricular.sql`,
  `V384__add_selection_mode_to_elective_groups.sql`) — real, shipped extensions not described in
  BR-49's original text but present in the live schema/enum.
- **Retrofitting `CourseRegistrationController`/`StudentTermEnrollmentController` onto dedicated
  permission codes** beyond the generate/elective-assignment endpoints — remains pre-existing debt,
  not addressed here.
