# Software Requirements Specification — Lab Reports & Analytics

**Module:** Lab Reports & Analytics (R1-M5.2, referenced in milestone tracker as Modules 7.10 & 13)
**Product:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Introduction

### 1.1 Purpose
Documents what was actually built for lab/attendance reporting and analytics, reverse-engineered from `ReportController`/`ReportService` (backend) and `frontend/src/app/features/reports/` (frontend). This is scoped narrowly to what shipped — it is materially smaller than the ambition described in the milestone tracker (see §6).

### 1.2 Scope
- A single **Reports & Analytics dashboard** screen with two sections: Lab Utilization and Attendance Analytics.
- A backend `ReportService` exposing exactly 3 endpoints: lab utilization, student performance (by student id), attendance analytics.
- Nothing else described under "Lab Reports & Analytics" in the milestone tracker (equipment utilization reports, lab expense reports, safety incident reports, CO/PO attainment, NBA/NAAC compliance data, PDF/Excel/CSV export) was found implemented anywhere in the codebase.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md` lines 599–627 (R1-M5.2)
- `docs/manual-test-cases/reports-analytics.md` — its expected-value test cases match the shipped screen exactly (2 sections, no export, no CO/PO), corroborating that this was the actual delivered scope, not the fuller milestone-tracker description.
- `docs/BUSINESS_REQUIREMENTS.md` — no dedicated BR-N section; the only report-adjacent entry found is BR-40 (Role/User-Customizable Dashboard & Analytics Widget System), which is a general-purpose home-dashboard widget framework, not this module.

## 2. Overall Description

### 2.1 Product Perspective
Reads aggregate counts from existing modules (Lab/Equipment masters, Class Schedules, Attendance, Exam Results, Lab Continuous Evaluation) via their repositories — it does not own any data itself; it is a pure read/aggregation layer. `getStudentPerformanceReport` is wired in the service and controller but is not called from the dashboard screen's UI (no widget/link renders it).

### 2.2 Actors / User Classes
- Any authenticated user with `REPORT_VIEW` — the dashboard has no role-conditional rendering; the same two sections render for every viewer with access.
- `getStudentPerformanceReport` (student-scoped) previously had no `@PreAuthorize` on its controller method; **fixed 2026-09-24** — it now requires `REPORT_VIEW`, matching its two siblings.

### 2.3 Operating Environment
Same stack as the rest of the app: Angular standalone component, Spring Boot REST controller under `/api/v1/reports`, PostgreSQL aggregation via repository `count()`/`findAll()` calls (in-memory grouping, not SQL `GROUP BY`).

### 2.4 Constraints / Assumptions
- Aggregation is computed by pulling full result sets into memory and grouping in Java (`equipmentRepository.findAll().forEach(...)`), not via database-side aggregate queries — a performance constraint at scale that isn't yet a problem at current data volumes.
- No date-range or cohort filtering on any of the three reports — each returns a whole-institution snapshot as of "now."

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|----|-------------|----------|---------------|
| FR-LRA-1 | Display lab utilization KPIs: total labs, total schedules, average schedules per lab, total equipment | Must | Lab, ClassSchedule (lab-type), Equipment masters |
| FR-LRA-2 | Break down equipment count by status and labs count by status | Must | FR-LRA-1 |
| FR-LRA-3 | Display attendance analytics KPIs: total students, total attendance records | Must | Attendance module |
| FR-LRA-4 | Break down attendance records by status and by type | Must | FR-LRA-3 |
| FR-LRA-5 | Provide a per-student performance report (exam results list) via API | Implemented, not surfaced in UI | Exam Result subsystem |
| FR-LRA-6 | *(Described in milestone tracker, not found implemented)* Equipment utilization report, lab expense report, safety incident report | Not implemented | — |
| FR-LRA-7 | *(Described in milestone tracker, not found implemented)* CO/PO attainment calculation, experiment completion rates, NBA/NAAC compliance data (accreditation reporting) | Not implemented | — |
| FR-LRA-8 | *(Described in milestone tracker, not found implemented)* Report data export to PDF/Excel/CSV | Not implemented | — |

## 4. External Interface Requirements

### 4.1 Screens
- **Reports & Analytics dashboard** (route not confirmed beyond component path `features/reports/reports-dashboard`) — loading spinner, then two KPI-card sections each followed by two side-by-side "breakdown" cards (status-keyed lists), a guided-tour button, no filters, no export controls.

### 4.2 API Endpoints
- `GET /api/v1/reports/lab-utilization` — `LabUtilizationReportResponse`, permission `REPORT_VIEW`.
- `GET /api/v1/reports/attendance-analytics` — `AttendanceAnalyticsReportResponse`, permission `REPORT_VIEW`.
- `GET /api/v1/reports/student-performance/{studentId}` — `StudentPerformanceReportResponse`, permission `REPORT_VIEW` (added 2026-09-24; previously had no permission annotation).

### 4.3 Key DB Entities Queried
No dedicated report tables exist. Aggregates read from: `labs`, `class_schedules` (lab-typed), `equipment`, `attendance`, `exam_results`, `lab_continuous_evaluations`.

## 5. Non-Functional Requirements
- **Performance:** in-memory aggregation over `findAll()` — not indexed/paginated; a real scale concern flagged for future work, not yet a defect at current volumes.
- **Security/RBAC:** all three `/reports/*` GET endpoints require `REPORT_VIEW` (fixed 2026-09-24 for `student-performance`, which previously required no permission). `student-performance/{studentId}` still takes an arbitrary `studentId` path variable with no ownership check — any caller who holds `REPORT_VIEW` (an intentionally staff-facing report permission) can pull any student's exam-result-based performance report by id; this is now gated to staff rather than open to any authenticated user, but still has no per-student ownership check, which is fine for a staff report screen and not applicable here since no student/parent self-service UI calls this endpoint.
- **Auditability:** none — these are read-only reporting endpoints with no audit trail requirement.

## 6. Known Gaps / Not Yet Implemented
The milestone tracker (R1-M5.2) describes a materially larger module than what shipped. Confirmed **not implemented** anywhere in the backend or frontend:
- Equipment utilization reports (distinct from the simple equipment-by-status breakdown that does exist)
- Lab expense reports
- Safety incident reports (`IncidentReport` model/DTO/repository exist in the codebase but have **no controller or service** — dead code, not wired to any endpoint)
- Accreditation reporting: CO/PO attainment calculation, experiment completion rates, NBA/NAAC compliance data (the `co-po-mapping` frontend screen under `features/curriculum/` is a curriculum-design tool for defining CO→PO mappings, not an attainment/compliance report, and is out of scope for this module)
- Data export (PDF/Excel/CSV) — no export code found in `ReportsService`/`ReportController` or the dashboard component
- A student-performance widget on the dashboard itself — the endpoint exists but nothing in `reports-dashboard.component.ts/html` calls `getStudentPerformance`
