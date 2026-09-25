# Functional Requirements Document — Examination Management

**Product:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Overview
Two independently-built subsystems both live under the "examination" umbrella. Only Subsystem 1 (Examination/ExamResult/LabContinuousEvaluation) has a frontend. Subsystem 2 (ExamSession/ExamEvent/StudentMark/SemesterResult/ResultReport) is fully implemented and tested server-side but unreachable from the UI — documented here for completeness since it is real, shipped backend code.

## 2. Actors & Permissions

Permission strings, as seeded/used in code (`backend/src/main/resources/db/migration/V88__seed_roles_and_permissions.sql`, `V514`, `V520`) and enforced via `@PreAuthorize("@perm.has(...)")`/`@perm.hasAny(...)`:

| Permission | Purpose | Enforced on |
|---|---|---|
| `EXAMINATION_VIEW` | View examinations / exam sessions/events | `ExamSessionController`, `ExamEventController` GETs (not `ExaminationController`, see §7) |
| `EXAMINATION_MANAGE` | Create/update/delete examinations, sessions, events; publish/lock a session | `ExaminationController`, `ExamSessionController`, `ExamEventController` mutating ops |
| `EXAM_RESULT_VIEW` | View exam results / semester result by enrollment / result sheet | `ExamResultController`, `SemesterResultController.getByEnrollment`, `ResultReportController.getResultSheet` |
| `EXAM_RESULT_MANAGE` | Create/update/delete exam results, lab evaluations, student marks; compute/lock semester results; view result summaries/course stats | `ExamResultController`, `LabContinuousEvaluationController`, `StudentMarkController`, `SemesterResultController`, `ResultReportController.getSummary/getCourseStats` |
| `MY_EXAM_RESULT_VIEW` | Student self-service: view own results | `ExamResultController.myResults` |
| `MY_WARD_EXAM_RESULT_VIEW` | Guardian self-service: view a ward's results | `ExamResultController.myWardResults` |

Seeded (V242) and, as of 2026-09-24, wired as `hasAny(granular, MANAGE)` alternatives on their matching mutation: `EXAMINATION_CREATE`/`EDIT`/`DELETE` on `ExaminationController`, `EXAM_RESULT_CREATE`/`EDIT`/`DELETE` on `ExamResultController`. `EXAM_RESULT_EXPORT` remains unused — no export endpoint exists to gate.

Role assignment is DB-only via the Role Management module; DEV_ADMIN/SUPPORT_ADMIN receive all permissions via the standard catch-all sync block in `V88`.

## 3. Screens & UI Behavior

### 3.1 Examinations List (`/examinations`)
- Columns: Name, Course *(bound to `row.courseName`, but the API never returns that field — see §7)*, Type, Date, Duration, Max Marks, Actions.
- Search (client-side filter), sort, column picker, resizable columns (`cmsResizableColumns`), guided tour button.
- Row actions: Edit, Delete (confirm dialog).
- Route guard: `EXAMINATION_VIEW` or `EXAMINATION_MANAGE`.

### 3.2 Examination Form (`/examinations/new`, `/examinations/:id/edit`)
- Fields: Name (required, max 255, min trimmed length 2, no consecutive spaces), Course dropdown (required — populated from `/courses`, not `/subjects`), Exam Type (required: THEORY/PRACTICAL/VIVA), Date (optional), Duration minutes (optional, ≥1), Max Marks (optional, ≥0).
- Live preview card mirrors form values as typed.
- No uniqueness check on Name (no `uniqueFieldValidator`/`name-exists` call), unlike other master screens in the app.
- Route guard: `EXAMINATION_MANAGE`.

### 3.3 Exam Results List (`/exam-results`)
- An infinite-scroll examination picker drives which examination's results are shown (no results load until one is selected).
- Columns: Roll No., Student, Marks, Grade, Status (rendered with `cms-status-badge`; PENDING/PUBLISHED/WITHHELD all map to defined badge classes).
- Search, sort, column picker, resizable columns.
- Route guard: `EXAM_RESULT_VIEW` or `EXAM_RESULT_MANAGE`.
- No create/edit UI form is wired into this list component — `ExaminationService.createResult`/`updateResult` exist in the Angular service but no component in the examination feature calls them, so the actual data-entry screen for exam results (if any) was not found in this codebase.

## 4. Functional Workflows

### 4.1 Create & publish a result (Subsystem 1)
1. Staff creates an `Examination` (subject, type, max marks, etc.).
2. Staff (via API — no discovered UI form) creates an `ExamResult` with status `PENDING` and marks.
3. Staff updates status to `PUBLISHED`; backend recomputes `outcome` = PASS if `marksObtained*100 >= maxMarks*50`, else FAIL.
4. Student/guardian can now see the result via `/exam-results/my` or `/exam-results/my-wards`.
5. Student Promotion reads the latest PUBLISHED result per subject when computing arrears.

### 4.2 Term-based exam cycle (Subsystem 2, backend-only)
1. Create an `ExamSession` (term instance + session type INTERNAL1/INTERNAL2/PRACTICAL/FINAL) — starts `DRAFT`.
2. Create `ExamEvent`s under it, one per course offering (max marks, pass marks).
3. Enter `StudentMark`s per course registration via upsert (blocked if session `LOCKED`); mark status PRESENT/ABSENT/MALPRACTICE; ABSENT/MALPRACTICE force marks to 0; PRESENT requires marks in `[0, maxMarks]`.
4. Publish the session (`DRAFT`→`PUBLISHED`), then lock it (`PUBLISHED`→`LOCKED`) — status transitions are strictly one-directional and validated.
5. Once **all** exam sessions in a term are `LOCKED`, compute semester results for the whole term (`POST /semester-results/compute-term`) or for a single enrollment; yearly-pattern programs aggregate ODD+EVEN term marks onto the EVEN term's result.
6. Lock an individual semester result to prevent recomputation.
7. Reporting: per-student result sheet, per-cohort pass/fail summary, per-course-offering stats (present/absent/malpractice counts, average marks).

## 5. API Endpoints

All paths relative to `/api/v1`.

| Method | Path | Request → Response | Permission |
|---|---|---|---|
| POST | `/examinations` | `ExaminationRequest` → `ExaminationResponse` | `EXAMINATION_CREATE` or `EXAMINATION_MANAGE` (fixed 2026-09-24) |
| GET | `/examinations` | — → `ExaminationResponse[]` | `EXAMINATION_VIEW` or `EXAMINATION_MANAGE` |
| GET | `/examinations/{id}` | — → `ExaminationResponse` | `EXAMINATION_VIEW` or `EXAMINATION_MANAGE` |
| GET | `/examinations/subject/{subjectId}` | — → `ExaminationResponse[]` | `EXAMINATION_VIEW` or `EXAMINATION_MANAGE` |
| PUT | `/examinations/{id}` | `ExaminationRequest` → `ExaminationResponse` | `EXAMINATION_EDIT` or `EXAMINATION_MANAGE` (fixed 2026-09-24) |
| DELETE | `/examinations/{id}` | — → 204 | `EXAMINATION_DELETE` or `EXAMINATION_MANAGE` (fixed 2026-09-24) |
| POST | `/exam-results` | `ExamResultRequest` → `ExamResultResponse` | `EXAM_RESULT_CREATE` or `EXAM_RESULT_MANAGE` (fixed 2026-09-24) |
| GET | `/exam-results/examination/{id}`, `/exam-results/student/{id}`, `/exam-results/{id}` | → `ExamResultResponse[]`/`ExamResultResponse` | `EXAM_RESULT_VIEW` or `_MANAGE` |
| GET | `/exam-results/my` | JWT identity → own `ExamResultResponse[]` | `MY_EXAM_RESULT_VIEW` |
| GET | `/exam-results/my-wards?studentId=` | JWT identity + ward id → `ExamResultResponse[]` | `MY_WARD_EXAM_RESULT_VIEW` |
| PUT | `/exam-results/{id}` | `ExamResultRequest` → `ExamResultResponse` | `EXAM_RESULT_EDIT` or `EXAM_RESULT_MANAGE` (fixed 2026-09-24) |
| DELETE | `/exam-results/{id}` | — → 204 | `EXAM_RESULT_DELETE` or `EXAM_RESULT_MANAGE` (fixed 2026-09-24) |
| POST/GET/PUT/DELETE | `/lab-evaluations[...]` | `LabContinuousEvaluationRequest` → `Response` | `EXAM_RESULT_MANAGE` (create/update/delete); GETs unguarded |
| POST | `/exam-sessions`, `/exam-sessions/{id}/publish`, `/exam-sessions/{id}/lock` | `ExamSessionRequest` → `ExamSessionDto` | `EXAMINATION_MANAGE` |
| GET | `/exam-sessions/{id}`, `/exam-sessions?termInstanceId=` | → `ExamSessionDto`(`[]`) | `EXAMINATION_VIEW` |
| POST/GET/PUT/DELETE | `/exam-events[...]` | `ExamEventRequest` → `ExamEventDto` | `EXAMINATION_MANAGE` (write) / `EXAMINATION_VIEW` (read) |
| POST | `/student-marks` (upsert) | `StudentMarkRequest` → `StudentMarkDto` | `EXAM_RESULT_MANAGE` |
| GET | `/student-marks`, `/student-marks/{id}` | → `StudentMarkDto`(`[]`) | `EXAM_RESULT_MANAGE` |
| POST | `/semester-results/compute?enrollmentId=`, `/semester-results/compute-term?termInstanceId=`, `/semester-results/{id}/lock` | → `SemesterResultDto` / void | `EXAM_RESULT_MANAGE` |
| GET | `/semester-results?termInstanceId=\|studentId=` | → `SemesterResultDto[]` | `EXAM_RESULT_MANAGE` |
| GET | `/semester-results/enrollment/{id}` | → `SemesterResultDto` | `EXAM_RESULT_VIEW` |
| GET | `/result-reports/result-sheet/{enrollmentId}` | → `StudentResultSheetDto` | `EXAM_RESULT_VIEW` |
| GET | `/result-reports/summary?termInstanceId=`, `/result-reports/course-stats?termInstanceId=` | → `SemesterSummaryDto[]`/`CourseStatsDto[]` | `EXAM_RESULT_MANAGE` |

## 6. Data Model

- **`examinations`** (`Examination`): id, name, subject_id → `subjects`, exam_type (THEORY/PRACTICAL/VIVA), date, duration, max_marks, created_at, updated_at. (Column was `course_id`→`courses` until V37 renamed it to `subject_id`→`subjects`.)
- **`exam_results`** (`ExamResult`): id, examination_id → `examinations`, student_id → `students`, marks_obtained (numeric), grade (text), status (PENDING/PUBLISHED/WITHHELD), outcome (PASS/FAIL, added by V284), created_at, updated_at.
- **`lab_continuous_evaluations`** (`LabContinuousEvaluation`): id, experiment_id → `experiments`, student_id → `students`, record_marks, viva_marks, performance_marks, total_marks (all client-supplied ints), evaluation_date, evaluated_by, created_at, updated_at.
- **`exam_sessions`** (`ExamSession`): id, term_instance_id → `term_instances`, session_type (INTERNAL1/INTERNAL2/PRACTICAL/FINAL), status (DRAFT/PUBLISHED/LOCKED), start_date, end_date. Unique (term_instance_id, session_type).
- **`exam_events`** (`ExamEvent`): id, exam_session_id → `exam_sessions`, course_offering_id → `course_offerings`, exam_date, max_marks, pass_marks. Unique (exam_session_id, course_offering_id).
- **`student_marks`** (`StudentMark`): id, exam_event_id → `exam_events`, course_registration_id → `course_registrations`, mark_status (PRESENT/ABSENT/MALPRACTICE), marks_obtained, remarks. Unique (exam_event_id, course_registration_id).
- **`term_results`** (`SemesterResult`): id, student_term_enrollment_id → `student_term_enrollments` (1:1, unique), total_max_marks, total_marks_obtained, percentage, result_status (PASS/FAIL/WITHHELD/NOT_PUBLISHED), is_locked.

## 7. Edge Cases & Validation Rules
- **`marksObtained` bounds (Subsystem 2 only):** `StudentMarkServiceImpl.resolveMarks` rejects negative marks and marks exceeding the exam event's `maxMarks`; ABSENT/MALPRACTICE are auto-forced to 0 server-side. Subsystem 1 (`ExamResultService`) has **no such bound check** — marks above `maxMarks` or negative are accepted as-is.
- **Locked-session guard:** upserting a `StudentMark` throws `IllegalStateException` if the owning session is `LOCKED`.
- **Session status transitions:** `publish()` requires current status `DRAFT`; `lock()` requires current status `PUBLISHED` — any other current state throws.
- **Semester result recompute guard:** `computeForEnrollment` throws if the existing result `isLocked`; yearly-pattern ODD-term computation attempts throw (`AssessmentPattern.YEARLY` results are only computed on the EVEN term).
- ~~**Frontend/backend field-name mismatch**~~ — **fixed 2026-09-24.** `examination.model.ts`, `ExaminationFormComponent`/its template, `ExaminationListComponent`/its template, `ExamResultListComponent`'s picker label, `ExaminationService.getByCourse` (renamed `getBySubject`, now calling `GET /examinations/subject/{subjectId}` instead of a non-existent `?courseId=` query param), and the module's tour copy were all renamed from `courseId`/`courseName` to `subjectId`/`subjectName`. The form's dropdown now sources options from `GET /subjects` instead of the unrelated `/courses` endpoint. The Subject column now renders correctly and Create/Edit submits a valid `subjectId`.
- ~~**Unauthenticated-permission GETs**~~ — **fixed 2026-09-24.** `ExaminationController`'s three GET endpoints (`findAll`, `findById`, `findBySubjectId`) now require `EXAMINATION_VIEW` or `EXAMINATION_MANAGE`, consistent with every other read in this module (no new permission needed — `EXAMINATION_VIEW` was already seeded in V88 but unused by this controller).

## 8. Known Gaps / Deferred
See SRS.md §6 and BRD.md §7 — not duplicated here. Additionally: no PDF/transcript export was found for either subsystem (no export endpoint, no export button in the two list components).
