# Functional Requirements Document — Attendance Management

**App:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing

## 1. Overview

Covers `Attendance`/`LabAttendance` marking and reporting, and `AttendanceThreshold` configuration. Backend `com.cms.{model,dto,controller,service,repository}` (Attendance*, LabAttendance), frontend `frontend/src/app/features/attendance/`.

## 2. Actors & Permissions

| Permission | Purpose |
|---|---|
| `ATTENDANCE_MANAGE` | Mark (single/bulk), view available-subjects/roster, update/delete records, view alerts |
| `ATTENDANCE_VIEW` | Read-only list/report access (alongside `ATTENDANCE_MANAGE` holders) |
| `MY_ATTENDANCE_VIEW` | Student self-service — own attendance only (V514) |
| `MY_WARD_ATTENDANCE_VIEW` | Guardian self-service — ward's attendance only, server-validated (V520, parent portal) |
| `ATTENDANCE_THRESHOLD_VIEW` | View per-curriculum-mapping thresholds |
| `ATTENDANCE_THRESHOLD_MANAGE` | Create/update/delete threshold overrides (V270, BR-49) |

Roles are assigned to these permissions via the DB-driven Role Management module.

## 3. Screens & UI Behavior

### 3.1 Attendance Mark (`attendance-mark.component`)
- Batch-wise marking view: faculty picks a date, sees eligible subjects for that date (from their own timetable), picks a subject, and marks the full roster in one bulk submission (or edits individual entries).
- Model fields: `studentId`, `subjectId`, `date`, `status`, `type` per `AttendanceRequest`/`BulkAttendanceRequest`.

### 3.2 Attendance List (`attendance-list.component`)
- Lists/filters attendance records by student, subject, and/or date (at least one filter required — the backend rejects a request with none of `studentId`/`subjectId`/`date`).

### 3.3 Threshold configuration
- Not a standalone screen — inline-edited on the Curriculum Map screen's mapping row per curriculum-term-course + component type (BR-49), backed by `AttendanceThresholdController`.

## 4. Functional Workflows

**Mark attendance:**
1. Faculty selects date → `GET /attendance/available-subjects?date=` returns eligible subjects from their timetable.
2. Faculty selects subject → `GET /attendance/subject-roster?subjectId=` returns the roster.
3. Faculty submits `POST /attendance/bulk` (whole roster) or `POST /attendance` (single student), status per student: `PRESENT`/`ABSENT`/`LATE`/`EXCUSED`.

**Report / alert:**
1. `GET /attendance/reports?studentId=&subjectId=` — for each `AttendanceType` (THEORY/LAB/CLINICAL) with at least one recorded class, computes `classesAttended / totalClasses * 100` (2-decimal, `HALF_UP` rounding), resolves the threshold via `AttendanceThresholdService.resolveThreshold`, and flags `lowAttendance = attendancePercentage < threshold`.
2. `GET /attendance/alerts?subjectId=` — iterates every student registered for the subject (`CourseRegistrationRepository.findRegisteredStudentsBySubjectId`), runs the same per-component report for each, and returns only the entries flagged `lowAttendance`.

**Self-service:**
1. Student: `GET /attendance/my` — resolves identity from JWT `preferred_username`, returns own records.
2. Guardian: `GET /attendance/my-wards?studentId=` — resolves identity from JWT, validates `studentId` is an actual ward via `GuardianService` before returning records.

## 5. API Endpoints

See SRS §4.2 for the full table. Request DTOs: `AttendanceRequest` (studentId, subjectId, date, status, type), `BulkAttendanceRequest` (subjectId, date, type, list of {studentId, status}), `AttendanceThresholdRequest`. Response DTOs: `AttendanceResponse`, `AttendanceReportResponse` (studentId, studentName, rollNumber, subjectId, subjectName, subjectCode, type, totalClasses, classesAttended, attendancePercentage, threshold, lowAttendance), `AvailableSubjectResponse`, `StudentRosterResponse`, `AttendanceThresholdDto`.

## 6. Data Model

**`attendances`** (V18): `id`, `student_id` (FK, NOT NULL), `course_id`/`subject_id` (FK, NOT NULL), `date`, `status` (VARCHAR, unconstrained), `type` (VARCHAR, unconstrained), `remarks`, `marked_by` (FK → faculty), `created_at`, `updated_at`.

**`lab_attendances`** (V19): `id`, `student_id` (FK), `subject_id` (FK), `experiment_id` (FK, nullable), `lab_id` (FK, NOT NULL), `lab_schedule_id` (FK → `ClassSchedule`, nullable), `lab_batch` (NOT NULL), `date`, `status`, `remarks`, `marked_by`, `created_at`, `updated_at`.

**`attendance_thresholds`** (V268, BR-49): keyed on `(curriculum_term_course_id, attendance_type)`, carries the override percentage; absence of a row for a given key falls back to the 75% default.

**Enums:** `AttendanceStatus` (PRESENT, ABSENT, LATE, EXCUSED); `AttendanceType` (THEORY, LAB, CLINICAL — CLINICAL added later with no migration, since `attendances.type` is unconstrained `VARCHAR`).

## 7. Edge Cases & Validation Rules

- `GET /attendance` throws `IllegalArgumentException` if none of `studentId`/`subjectId`/`date` is supplied.
- A component (THEORY/LAB/CLINICAL) with zero recorded classes for a student+subject is silently omitted from the report — not shown as 0% — since `getAttendanceReport` only emits an entry when `totalClasses > 0`.
- Guardian access (`/attendance/my-wards`) always re-validates `studentId` server-side against the caller's actual wards, regardless of what is passed in the query parameter — a direct application of the codebase-wide rule (OC-236) that self-service `studentId`s are never trusted from the client.
- Percentage rounding is `HALF_UP` to 2 decimal places (`BigDecimal`), applied consistently for both individual reports and alert scanning.

## 8. Known Gaps / Deferred

See BRD §7 — no standalone charted Attendance Report screen found in `frontend/src/app/features/attendance/` at time of writing, despite original milestone scope (R1-3.3.10) calling for one.
