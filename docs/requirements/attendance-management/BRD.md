# Business Requirements Document — Attendance Management

**App:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Company:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective

Nursing education is governed by Indian Nursing Council (INC) rules that set minimum attendance thresholds separately per curriculum component (e.g. 80% Theory, 100% Clinical) rather than one blanket percentage. The Attendance Management module gives faculty a fast way to mark attendance per class session (individually or in bulk for a roster) and gives admins/HODs a per-component compliance view so a student's eligibility for examinations can be judged accurately against INC rules rather than a single misleading blended number.

## 2. Stakeholders

- **Faculty / Lab Incharge** — marks attendance for their sessions.
- **HOD / Academic Admin** — monitors low-attendance alerts, configures thresholds.
- **Students** — self-service visibility into their own standing.
- **Parents/Guardians** — self-service visibility into their ward's standing (parent portal).
- **SKSCON management / INC compliance reporting** — consumes attendance data for statutory reporting.

## 3. Business Rules

- **BR-ATTENDANCE-1 (mapped from BR-49).** Attendance thresholds are resolved **per curriculum mapping + component type** (`THEORY`/`LAB`/`CLINICAL`), walking student → course registration → offering → curriculum mapping → an `attendance_thresholds` override row, defaulting to 75% at any missing step. A single subject can therefore carry different minimums for its Theory vs. Clinical components in different terms, matching how INC curricula actually vary hours/category by semester rather than by subject alone.
- **BR-ATTENDANCE-2 (mapped from BR-49).** Attendance reports are always reported **per component**, never as one blended percentage — a student can be compliant on Theory (80%) while failing Clinical (100%) simultaneously, and the report must surface both facts separately.
- **BR-ATTENDANCE-3 (implicit, from `AttendanceStatus`).** A marked attendance record has exactly one status: `PRESENT`, `ABSENT`, `LATE`, or `EXCUSED`. Only `PRESENT` counts toward the attendance percentage numerator; all others (including `LATE`/`EXCUSED`) count as not-present for percentage purposes (confirmed from `AttendanceService.getAttendanceReport`, which counts only `AttendanceStatus.PRESENT`).
- **BR-ATTENDANCE-4 (implicit, from controller).** A guardian may only view attendance for their own verified ward — `studentId` is never trusted directly from the request and is re-validated server-side against the caller's actual guardian-student relationship.
- **BR-ATTENDANCE-5 (implicit, from `LabAttendance`).** Lab/practical attendance additionally records which `Experiment`, `Lab` (room), `labBatch`, and originating `ClassSchedule` the session belongs to — richer context than a plain Theory attendance row, to support lab-specific reporting.

## 4. Business Process / Workflow

1. Faculty opens Attendance Mark for a given date; the system resolves which subjects/sessions they are eligible to mark (`available-subjects`, driven by their timetable for that date) and loads the roster for the chosen subject.
2. Faculty marks each student's status (bulk endpoint for the whole roster, or individual marks for corrections) — `PRESENT`/`ABSENT`/`LATE`/`EXCUSED`.
3. At any time, faculty/admin can pull a per-student, per-component report (`/attendance/reports`) or a subject-wide low-attendance alert list (`/attendance/alerts`), each computed live against the resolved threshold for that component.
4. Admin/HOD configures threshold overrides where a curriculum mapping deviates from the 75% default (inline on the Curriculum Map screen, per BR-49 — not a separate screen in this module).
5. Students and guardians independently check standing via self-service endpoints, without needing faculty involvement.

## 5. Success Criteria

Not formally defined with quantitative KPIs. Inferred from feature completeness: every marked session is queryable per student/subject/date; every component's compliance can be judged independently against its resolved threshold; guardians cannot see any student's data except their own verified ward's.

## 6. Assumptions & Constraints

- Attendance can only be marked against subjects the marking faculty is actually scheduled to teach on that date (resolved via the Timetable/ClassSchedule engine — a dependency of this module, documented separately).
- The 75% default threshold applies whenever no override row exists for a given curriculum mapping + component — this default is hardcoded in `AttendanceThresholdService`, not itself a configurable system-wide setting (per BR-49's description).
- `attendances.type` has no DB-level enum constraint (plain `VARCHAR`), so adding a new `AttendanceType` value (as happened for `CLINICAL`) requires no migration — an intentional flexibility, not an oversight.

## 7. Known Gaps / Deferred

- No dedicated Attendance Report screen with charts was found in the current frontend (see SRS §6) — the original R1 milestone scope called for one; current UI appears to surface reports via API consumption elsewhere rather than a standalone charted screen in this module's own folder.
