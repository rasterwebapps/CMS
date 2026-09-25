# Software Requirements Specification — Attendance Management

**Module:** Attendance Management
**App:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Source milestone:** R1-M3.3 (`docs/RELEASE_1_MILESTONES.md` lines 338–362, historical scope only)

---

## 1. Introduction

### 1.1 Purpose
Defines requirements for recording, reporting, and alerting on student class attendance (Theory/Lab/Clinical), including per-component INC-compliant attendance thresholds introduced by BR-49.

### 1.2 Scope
In scope: marking attendance (individual + bulk), the `Attendance`/`LabAttendance` records, attendance percentage/report computation, low-attendance alerts, per-component threshold configuration, and student/guardian self-service attendance views. Out of scope: the Timetable/ClassSchedule engine that determines *which* subjects are available to mark attendance against on a given date (documented under lab-scheduling-timetable).

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md` lines 338–362
- `docs/BUSINESS_REQUIREMENTS.md` BR-49 (INC Nursing Curriculum Compliance — per-component attendance thresholds)
- `backend/src/main/java/com/cms/model/Attendance.java`, `LabAttendance.java`, `AttendanceThreshold.java`
- `backend/src/main/java/com/cms/controller/AttendanceController.java`, `AttendanceThresholdController.java`
- `backend/src/main/java/com/cms/service/AttendanceService.java`
- `frontend/src/app/features/attendance/*`

## 2. Overall Description

### 2.1 Product Perspective
Attendance is recorded per `Student` + `Subject` + `date`, with a `type` (`THEORY`/`LAB`/`CLINICAL`) distinguishing which curriculum component the session belongs to. `LabAttendance` is a parallel, lab-specific record carrying additional `Experiment`/`Lab`/`labBatch`/`ClassSchedule` context. Attendance thresholds are resolved per curriculum mapping (`curriculum_term_courses`) + `AttendanceType`, walking student → course registration → offering → curriculum mapping → an override row, falling back to a 75% institutional default.

### 2.2 Actors / User Classes
- **Faculty / Lab Incharge** — marks attendance (individual/bulk), views reports/alerts.
- **Admin / HOD** — configures per-curriculum-mapping attendance thresholds.
- **Student (self-service)** — views own attendance (`MY_ATTENDANCE_VIEW`).
- **Guardian (parent portal)** — views ward's attendance (`MY_WARD_ATTENDANCE_VIEW`), server-validated against actual guardian-student linkage.

### 2.3 Operating Environment
Angular SPA (`frontend/src/app/features/attendance/`) calling Spring Boot REST (`/attendance`, `/attendance-thresholds`) over PostgreSQL (`attendances`, `lab_attendances`, `attendance_thresholds`). Keycloak JWT auth; DB-driven permission checks.

### 2.4 Constraints / Assumptions
- `attendances.type` is an unconstrained `VARCHAR` (confirmed from `V18__create_attendances_table.sql`) — the `CLINICAL` value was added to the Java `AttendanceType` enum with no migration needed, since the column has no DB-level check constraint.
- Attendance percentage and low-attendance flags are computed live on every report read (not pre-aggregated/cached).
- A student can meet one component's threshold (e.g. Theory) while failing another (e.g. Clinical) — reports are per-component, never blended into one number.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-ATTENDANCE-1 | Mark attendance for a single student/subject/date/type | Must | — |
| FR-ATTENDANCE-2 | Mark attendance in bulk for a roster (one subject/date/type, many students) | Must | FR-ATTENDANCE-6 |
| FR-ATTENDANCE-3 | List/filter attendance records by student, subject, and/or date | Must | — |
| FR-ATTENDANCE-4 | Student self-service: view own attendance records | Should | `MY_ATTENDANCE_VIEW` |
| FR-ATTENDANCE-5 | Guardian self-service: view a ward's attendance, server-validated against actual guardianship | Should | `MY_WARD_ATTENDANCE_VIEW` |
| FR-ATTENDANCE-6 | Resolve which subjects are available to mark for the current faculty on a given date, and the roster for a chosen subject | Must | Timetable/ClassSchedule |
| FR-ATTENDANCE-7 | Per-component (Theory/Lab/Clinical) attendance report for a student+subject, each checked against its own resolved threshold | Must | BR-49 |
| FR-ATTENDANCE-8 | Low-attendance alert list for a subject (all registered students, any component below threshold) | Must | FR-ATTENDANCE-7 |
| FR-ATTENDANCE-9 | Update or delete an existing attendance record | Should | — |
| FR-ATTENDANCE-10 | Configure/view per-curriculum-mapping attendance thresholds by component type, with a 75% institutional default when unconfigured | Should | BR-49 |

## 4. External Interface Requirements

### 4.1 Screens
- **Attendance Mark** (`attendance-mark.component`) — batch-wise marking view (per subject/date roster).
- **Attendance List** (`attendance-list.component`) — record listing/filtering.
- Attendance report/alert consumption is embedded in these screens and in the Timetable module's Progress Report context rather than a standalone reports screen (no dedicated `attendance-report` component folder found under `frontend/src/app/features/attendance/`).

### 4.2 API Endpoints (base `/attendance`)
| Method | Path | Purpose | Permission |
|---|---|---|---|
| POST | `/attendance` | Mark single attendance | `ATTENDANCE_MANAGE` |
| POST | `/attendance/bulk` | Mark bulk attendance | `ATTENDANCE_MANAGE` |
| GET | `/attendance` | List by student/subject/date filters | `ATTENDANCE_VIEW` or `ATTENDANCE_MANAGE` |
| GET | `/attendance/my` | Current student's own records | `MY_ATTENDANCE_VIEW` |
| GET | `/attendance/my-wards?studentId=` | Guardian's ward's records | `MY_WARD_ATTENDANCE_VIEW` |
| GET | `/attendance/available-subjects?date=` | Subjects the current faculty can mark on a date | `ATTENDANCE_MANAGE` |
| GET | `/attendance/subject-roster?subjectId=` | Roster for a subject | `ATTENDANCE_MANAGE` |
| GET | `/attendance/reports?studentId=&subjectId=` | Per-component report | `ATTENDANCE_VIEW` or `ATTENDANCE_MANAGE` |
| GET | `/attendance/alerts?subjectId=` | Low-attendance alert list | `ATTENDANCE_MANAGE` |
| PUT | `/attendance/{id}` | Update record | `ATTENDANCE_MANAGE` |
| DELETE | `/attendance/{id}` | Delete record | `ATTENDANCE_MANAGE` |

Base `/attendance-thresholds`:
| Method | Path | Purpose | Permission |
|---|---|---|---|
| GET | `/attendance-thresholds?curriculumTermCourseId=` | Get thresholds for a curriculum mapping | `ATTENDANCE_THRESHOLD_VIEW` |
| PUT | `/attendance-thresholds` | Upsert a threshold | `ATTENDANCE_THRESHOLD_MANAGE` |
| DELETE | `/attendance-thresholds/{id}` | Delete a threshold override | `ATTENDANCE_THRESHOLD_MANAGE` |

### 4.3 Key DB Entities
`attendances` (V18), `lab_attendances` (V19), `attendance_thresholds` (V268, BR-49).

## 5. Non-Functional Requirements

- **Performance:** low-attendance alerts iterate every registered student for a subject and recompute a full per-component report per student on each call — acceptable at current class-roster scale; no caching/pre-aggregation layer exists.
- **Security / RBAC:** all mutate endpoints require `ATTENDANCE_MANAGE`; view endpoints accept either `ATTENDANCE_VIEW` or `ATTENDANCE_MANAGE`. Self-service endpoints (`/my`, `/my-wards`) resolve identity from the JWT `preferred_username` claim server-side — `studentId` passed for `/my-wards` is validated against the caller's actual wards, never trusted directly from the request.
- **Auditability:** `createdAt`/`updatedAt` via JPA auditing on both `Attendance` and `LabAttendance`; no dedicated audit-log trail of who changed a specific attendance mark beyond `markedBy` (faculty FK) and the updated timestamp.

## 6. Known Gaps / Not Yet Implemented

- No standalone "Attendance Report" screen with charts was found under `frontend/src/app/features/attendance/` despite the original milestone (R1-3.3.10) calling for one — reporting appears to be consumed via the `reports`/`alerts` API endpoints rather than a dedicated chart UI in the current codebase. Could not confirm whether charting exists elsewhere (e.g. embedded in a dashboard widget) without a wider search outside this module's folder.
- Attendance-threshold configuration is inline-edited on the Curriculum Map screen's mapping row (per BR-49), not a standalone Attendance Threshold management screen — consistent with BR-49's documented design, noted here so the FRD's screen list isn't read as incomplete.
