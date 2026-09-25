# Functional Requirements Document — INC Nursing Curriculum Compliance

**Module slug:** `inc-curriculum-compliance`
**Source BR:** BR-49 · **Milestone:** R1-M6.1

## 1. Overview

Adds INC-compliant per-mapping component hours/type, choice/institution-decided electives,
per-component attendance thresholds, and course-offering-scoped roster batches to the curriculum
subsystem.

## 2. Actors & Permissions

Exact permission strings (grepped from controllers/migrations):

| Permission | Gates |
|---|---|
| `CURRICULUM_ELECTIVE_GROUP_VIEW` | `GET /curriculum-elective-groups` |
| `CURRICULUM_ELECTIVE_GROUP_MANAGE` | create/update-selection-mode/delete elective group |
| `ATTENDANCE_THRESHOLD_VIEW` | `GET /attendance-thresholds` |
| `ATTENDANCE_THRESHOLD_MANAGE` | upsert/delete threshold |
| `BATCH_VIEW` | list batches, roster, lifecycle-impact |
| `BATCH_MANAGE` | update/delete batch, roster add/remove, name-exists check |
| `CURRICULUM_MANAGE` | `PUT /curriculum-semester-courses/{id}` (reused, pre-existing) |
| `COURSE_REGISTRATION_GENERATE` | `POST /course-registrations/generate` (dedicated since V525; superseded reuse of `ADMISSION_CREATE`) |
| `ADMISSION_CREATE` | `PUT /course-registrations/{id}/drop` (reused, pre-existing) |
| `COURSE_REGISTRATION_ELECTIVE_ASSIGN` | single + bulk elective assignment |
| `ADMISSION_VIEW` or `COURSE_REGISTRATION_ELECTIVE_ASSIGN` | read registrations, elective group summaries |

Role→permission assignment is DB-only (Role Management module). New permissions in this BR were
auto-granted at migration time to holders of the closest existing permission (e.g.
`CURRICULUM_ELECTIVE_GROUP_*` → existing `CURRICULUM_*` holders), then synced to DEV_ADMIN/
SUPPORT_ADMIN per the standard catch-all block.

## 3. Screens & UI Behavior

### 3.1 Curriculum Map (`curriculum-map.component`)
- Per-term subject list; "Add Subject" form: Subject autocomplete (search-filtered, excludes
  subjects already mapped to that term), Theory/Lab/Clinical checkboxes each enabling/disabling its
  hour input (unchecked → forced to 0), Subject Type select (`CORE`/`FOUNDATIONAL`/`ELECTIVE`/
  `CO_CURRICULAR`), an "is elective" checkbox two-way-synced with Subject Type (`ELECTIVE` ⇄
  checked, no contradictory state possible), and — once editing an existing row — inline
  Theory/Lab/Clinical attendance threshold number inputs.
- Regular (non-elective) subjects render sorted CORE → FOUNDATIONAL → CO_CURRICULAR → alphabetical;
  electives render bucketed by elective group under a group heading.
- Term hour summary sums regular subjects' hours plus, per elective group, only one representative
  offering's hours (since a student only ever takes one option).
- Validation: `subjectId` required; hour fields NOT NULL at the API (client always sends 0 when a
  component is unchecked, never null).
- No explicit uniqueness-check directive call visible on this form (subject-per-term uniqueness is
  enforced by simply excluding already-used subjects from the picker, not a live `/name-exists`
  check — this screen doesn't follow the master-list uniqueFieldValidator pattern since a curriculum
  mapping isn't a named master).

### 3.2 Elective Assignment (`elective-assignment.component`)
- Cascade: Academic Year → Term Instance → Elective Group (group-launcher cards show
  assigned/eligible progress %, click to select).
- Selection-mode toggle (`STUDENT_CHOICE` / `INSTITUTION_DECIDED`) visible to holders of
  `CURRICULUM_ELECTIVE_GROUP_MANAGE`.
- Per-student table (roll number, name, cohort, current choice, action): pick an offering from a
  dropdown, "Assign"; if the student already has a different choice, a confirm dialog warns the
  prior registration will be dropped.
- "Apply to All": picks one offering and bulk-assigns every eligible student in the group,
  confirmed via dialog (irreversible warning), reports assigned/blocked counts (blocked = already
  scheduled/attendance recorded).
- Edit-mode toggle: a fully-assigned group opens read-only by default (safety default against
  accidental overwrite of a completed group); one click flips it editable.
- Integrates with Timetable Builder's `ElectiveGroupScheduleResponse` to show whether the group's
  offerings are already placed on the timetable (informational, not a hard gate here).

### 3.3 Batch management
- No standalone Batch screen and no create UI — batches are viewed/edited/rostered from wherever a
  Course Offering exposes them; `batch.service.ts` has no `create()` method (see Known Gaps in
  SRS/BRD).

## 4. Functional Workflows

**Assign a single elective choice:**
1. Admin selects a group and student row, picks an offering.
2. `assign()` checks `isChange()`; if the student has a different current choice, shows a confirm
   dialog.
3. On confirm (or directly if no prior choice), `POST /course-registrations/elective-assignment`
   with `{enrollmentId, courseOfferingId}`.
4. `CourseRegistrationServiceImpl.assignElectiveChoice` — idempotent if resubmitting the same
   offering; drops the prior elective registration in the same group before creating the new one.
5. UI reloads enrollments + group summaries.

**Bulk-assign ("Apply to All"):**
1. Admin picks one offering for the group, confirms the overwrite warning.
2. `POST /course-registrations/elective-assignment/bulk {termInstanceId, electiveGroupId,
   courseOfferingId}`.
3. `bulkAssignElectiveChoice` iterates eligible students, skipping any already scheduled/with
   attendance recorded (reported as `blockedCount`), returns `assignedCount`/`eligibleStudentCount`.

**Resolve an attendance threshold:** student → their `CourseRegistration` → its `CourseOffering` →
`curriculum_term_course_id` → `attendance_thresholds` row for the relevant `AttendanceType`; falls
back to 75% the instant any link in that chain is missing.

## 5. API Endpoints

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/curriculum-elective-groups` | `CurriculumElectiveGroupRequest` | `CurriculumElectiveGroupDto` (201) | `CURRICULUM_ELECTIVE_GROUP_MANAGE` |
| GET | `/curriculum-elective-groups?curriculumVersionId=&termNumber=` | — | `List<CurriculumElectiveGroupDto>` | `CURRICULUM_ELECTIVE_GROUP_VIEW` |
| PUT | `/curriculum-elective-groups/{id}/selection-mode` | `UpdateElectiveSelectionModeRequest` | `CurriculumElectiveGroupDto` | `CURRICULUM_ELECTIVE_GROUP_MANAGE` |
| DELETE | `/curriculum-elective-groups/{id}` | — | 204 | `CURRICULUM_ELECTIVE_GROUP_MANAGE` |
| GET | `/attendance-thresholds?curriculumTermCourseId=` | — | `List<AttendanceThresholdDto>` | `ATTENDANCE_THRESHOLD_VIEW` |
| PUT | `/attendance-thresholds` | `AttendanceThresholdRequest` | `AttendanceThresholdDto` | `ATTENDANCE_THRESHOLD_MANAGE` |
| DELETE | `/attendance-thresholds/{id}` | — | 204 | `ATTENDANCE_THRESHOLD_MANAGE` |
| PUT | `/batches/{id}` | `BatchRequest` (incl. `version`) | `BatchDto` | `BATCH_MANAGE` |
| GET | `/batches/name-exists?value=&courseOfferingId=&excludeId=` | — | `boolean` | `BATCH_MANAGE` |
| DELETE | `/batches/{id}` | — | 204 (blocked if students/timetable data) | `BATCH_MANAGE` |
| GET | `/batches/{id}/lifecycle-impact` | — | `BatchLifecycleImpactDto` | `BATCH_VIEW` |
| GET | `/batches?courseOfferingId=` or `?subjectId=&termInstanceId=` | — | `List<BatchDto>` | `BATCH_VIEW` |
| GET | `/batches/{id}/roster` | — | `List<BatchStudentDto>` | `BATCH_VIEW` |
| POST/DELETE | `/batches/{id}/students/{studentId}` | — | 204 | `BATCH_MANAGE` |
| PUT | `/curriculum-semester-courses/{id}` | `CurriculumSemesterCourseRequest` | `CurriculumSemesterCourse` | `CURRICULUM_MANAGE` |
| POST | `/course-registrations/generate?termInstanceId=` | — | `{registrationsCreated: int}` | `COURSE_REGISTRATION_GENERATE` |
| PUT | `/course-registrations/{id}/drop` | — | `CourseRegistrationDto` | `ADMISSION_CREATE` |
| POST | `/course-registrations/elective-assignment` | `ElectiveAssignmentRequest` | `CourseRegistrationDto` | `COURSE_REGISTRATION_ELECTIVE_ASSIGN` |
| POST | `/course-registrations/elective-assignment/bulk` | `ElectiveBulkAssignmentRequest` | `ElectiveBulkAssignmentResponse` | `COURSE_REGISTRATION_ELECTIVE_ASSIGN` |
| GET | `/course-registrations/elective-assignment/summary?termInstanceId=` | — | `List<ElectiveGroupSummaryResponse>` | `ADMISSION_VIEW` or `COURSE_REGISTRATION_ELECTIVE_ASSIGN` |

## 6. Data Model

- **`curriculum_term_courses`** (+V266): `theory_hours`, `lab_hours`, `clinical_hours` (NOT NULL,
  default 0), `subject_type` (`SubjectType` enum), `is_elective` (bool), `elective_group_id` (FK).
- **`curriculum_elective_groups`** (V265, +V384): `curriculum_version_id` FK, `term_number`,
  `group_name`, `group_code`, `selection_mode` (`STUDENT_CHOICE`/`INSTITUTION_DECIDED`, default
  `STUDENT_CHOICE`). Unique on `(curriculum_version_id, term_number, group_code)`.
- **`attendance_thresholds`** (V268): keyed on `(curriculum_term_course_id, attendance_type)`.
- **`course_offerings`** (+V269): `curriculum_term_course_id` nullable FK (backfilled via
  `(curriculum_version_id, term_number, subject_id)` match).
- **`batches`** (V271): `course_offering_id` FK NOT NULL, `name`, `capacity`, `term_instance_id` FK
  NOT NULL, `coordinator_faculty_id` FK nullable, `is_active`, `version` (optimistic lock), plus
  later-added `lab_id`/`clinical_venue_id`/`cohort_room_allocation_id`/`cohort_section_id`/
  `clinical_shift_group_id` (added by the later Capacity Auto-Plan work, outside BR-49's original
  scope but present on the live entity). Unique on `(course_offering_id, name)`.
- **`batch_students`** (V271): composite PK `(batch_id, student_id)`.
- **`lab_schedules`** (+V272): `batch_id` nullable FK, additive alongside free-text `batch_name`.

## 7. Edge Cases & Validation Rules

- Unchecking a component field forces its hours to 0 (never null) before submit — `theory_hours`/
  `lab_hours`/`clinical_hours` are NOT NULL columns.
- Picking `subjectType = ELECTIVE` forces the elective checkbox on; unpicking elective resets
  Subject Type to `CORE` (never left contradictory).
- A subject already mapped into a term is excluded from that term's Subject autocomplete (a
  subject may repeat across *different* terms).
- Elective-group resolution on submit always re-fetches from the server (never a cached signal) to
  avoid a race creating a duplicate group when two admins submit near-simultaneously.
- Re-submitting the same elective offering for a student is idempotent (no duplicate registration).
- Changing a student's elective choice always drops the prior registration in that group first.
- Bulk "Apply to All" silently skips (reports as blocked) students already scheduled or with
  attendance recorded against their current choice — it does not force-overwrite those.
- Batch delete hard-blocks (409-shaped) if the batch has students or timetable data attached.
- Batch update carries an optimistic-lock `version` field; a stale update is rejected as a
  conflict rather than silently last-write-wins.
- `CLINICAL` `AttendanceType` required no schema change since `attendances.type` is an
  unconstrained `VARCHAR`.

## 8. Known Gaps / Deferred

See SRS.md §6 and BRD.md §7 (batch creation ownership, `batch_name` cutover, student self-service
electives, generic multi-faculty-role table, `COURSE_REGISTRATION_GENERATE` permission drift from
BR-49's original text).
