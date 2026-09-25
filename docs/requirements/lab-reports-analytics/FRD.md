# Functional Requirements Document — Lab Reports & Analytics

**Product:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Overview
A single-screen reporting dashboard backed by `ReportController`/`ReportService`. Scope, as actually shipped, is Lab Utilization + Attendance Analytics only. See SRS.md/BRD.md for the gap between this and the milestone tracker's fuller description.

## 2. Actors & Permissions

| Permission | Purpose | Enforced on |
|---|---|---|
| `REPORT_VIEW` | View lab utilization, attendance analytics, and (as of 2026-09-24) student performance reports | `GET /reports/lab-utilization`, `GET /reports/attendance-analytics`, `GET /reports/student-performance/{studentId}` |

`GET /reports/student-performance/{studentId}` had no `@PreAuthorize` until 2026-09-24 — reachable by any authenticated user regardless of role, with no ownership check against the requested `studentId`. It now requires `REPORT_VIEW`, the same staff-facing permission as its two siblings.

`REPORT_VIEW` (and `REPORT_EXPORT`, seeded but unused — see Known Gaps) are seeded in `V88__seed_roles_and_permissions.sql` under category `REPORTS`, screen-labeled "General Reports" by `V247`. Role assignment is DB-only via Role Management; no role names are hardcoded here.

## 3. Screens & UI Behavior

### 3.1 Reports & Analytics Dashboard
- **Loading state:** single spinner while both API calls are in flight; both must resolve (or error) before content renders.
- **Lab Utilization section:**
  - 4 KPI cards: Total Labs, Total Schedules, Avg Schedules / Lab, Total Equipment.
  - 2 breakdown cards: "Equipment by Status", "Labs by Status" — each a simple label/value list built from a `Record<string, number>` map, status text title-cased for display (`formatStatus`: underscores → spaces, each word capitalized).
  - Empty state per breakdown card ("No equipment data" / "No lab data") if the map is empty.
- **Attendance Analytics section:**
  - 2 KPI cards: Total Students, Total Records.
  - 2 breakdown cards: "Attendance by Status", "Attendance by Type", same empty-state pattern.
- **No badges/status-chip components used** — this screen renders plain KPI/breakdown cards, not `cms-status-badge` or `cms-badge` — so the badge/status consistency audit pattern does not apply here.
- **No filters, no date range, no export button, no drill-down navigation** anywhere on the screen.
- Guided tour button (`cms-tour-button`) registers a tour (`REPORTS_DASHBOARD_TOUR`) covering the two section anchors.
- Error handling: each of the two API calls independently shows a toast (`Failed to load lab utilization` / `Failed to load attendance analytics`) on failure but still lets the other section render if it succeeded.

## 4. Functional Workflows

### 4.1 View the dashboard
1. User with `REPORT_VIEW` navigates to the Reports screen.
2. Component fires `getLabUtilization()` and `getAttendanceAnalytics()` in parallel on `ngOnInit`.
3. Spinner shows until both requests settle (success or error tracked independently via a shared `completed` counter).
4. Sections render with whatever data succeeded; failed sections show a toast and remain at their default `null` state (cards show `—` placeholders via `?? '—'`).

There is no second workflow — no report configuration, generation, scheduling, or export step exists.

## 5. API Endpoints

All paths relative to `/api/v1`.

| Method | Path | Response shape | Permission |
|---|---|---|---|
| GET | `/reports/lab-utilization` | `LabUtilizationReportResponse { totalLabs, totalSchedules, averageSchedulesPerLab, totalEquipment, equipmentByStatus: Map<String,Long>, labsByStatus: Map<String,Long> }` | `REPORT_VIEW` |
| GET | `/reports/attendance-analytics` | `AttendanceAnalyticsReportResponse { totalStudents, totalAttendanceRecords, attendanceByStatus: Map, attendanceByType: Map }` | `REPORT_VIEW` |
| GET | `/reports/student-performance/{studentId}` | `StudentPerformanceReportResponse` (student identity + list of that student's `ExamResultResponse`s) | `REPORT_VIEW` (added 2026-09-24) |

## 6. Data Model
No dedicated reporting tables/entities exist — everything is computed on read from:
- `labs` (via `LabRepository`) — for total lab count and labs-by-status.
- `class_schedules` (via `ClassScheduleRepository`, injected as `labScheduleRepository`) — for total schedule count (average = schedules ÷ labs).
- `equipment` (via `EquipmentRepository`) — for total equipment count and equipment-by-status.
- `students`, `attendance` (via `StudentRepository`, `AttendanceRepository`) — for attendance analytics.
- `exam_results` (via `ExamResultRepository`) — for the unused student-performance report.
- `lab_continuous_evaluations` is injected into `ReportService` but not read by any of the three implemented methods reviewed — present for a report that was not found implemented (consistent with the accreditation/CO-PO gap noted in SRS.md).

## 7. Edge Cases & Validation Rules
- `averageSchedulesPerLab` guards divide-by-zero: returns `0.0` if `totalLabs == 0`.
- Status maps are built by iterating `findAll()` and `merge(status.name(), 1L, Long::sum)` — an entity with a `null` status would throw `NullPointerException` on `.name()`; not observed to be reachable given status fields are non-nullable enums in their entities, but no explicit null-guard exists in `ReportService`.
- `student-performance/{studentId}` throws `ResourceNotFoundException` (404) for an unknown id. Until 2026-09-24 it performed no authorization check at all — any authenticated user, including a student account, could query another student's performance report by guessing/incrementing the id. It now requires `REPORT_VIEW`, which is a staff-facing permission; it still has no per-student ownership check, but that's consistent with it being a staff report (not a self-service endpoint) — no student/parent self-service UI calls it.
- No pagination on any of the three endpoints — full-table scans on every dashboard load.

## 8. Known Gaps / Deferred
See SRS.md §6 and BRD.md §7 for the full itemized list of milestone-described-but-not-implemented capabilities (equipment/expense/safety-incident reports, CO/PO attainment & NBA/NAAC accreditation reporting, PDF/Excel/CSV export). Additionally, specific to this FRD:
- `getStudentPerformanceReport` is implemented and routable but has no permission guard and no UI entry point — effectively dead-but-exposed surface area.
- `IncidentReport` (model, request/response DTOs, repository) exists with **no controller and no service** — cannot be exercised at all, dead code.
- `LabContinuousEvaluationRepository` is injected into `ReportService` but never queried by any of its three public methods.
