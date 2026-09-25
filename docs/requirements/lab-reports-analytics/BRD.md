# Business Requirements Document — Lab Reports & Analytics

**Product:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective
Give SKSCON administration a quick, at-a-glance view of how lab infrastructure (rooms, equipment) is being used and what overall attendance activity looks like, without needing to open the underlying Lab, Equipment, or Attendance screens individually. What was actually delivered is a single read-only dashboard covering exactly that — lab/equipment utilization counts and attendance-record counts. It does **not** deliver the broader accreditation/compliance reporting (CO/PO attainment, NBA/NAAC) or operational reports (equipment utilization detail, lab expense, safety incidents) that the milestone tracker's module description names, nor any data export.

## 2. Stakeholders
- College administration / Principal's office (consumers of the dashboard)
- Lab/Equipment management staff (indirect — their data populates the dashboard, but they have no dedicated report screen of their own)
- Students (indirectly, via the unused `student-performance` endpoint — no student-facing report UI exists)

## 3. Business Rules
No `BR-N` entry in `docs/BUSINESS_REQUIREMENTS.md` covers this module (only BR-40, a general dashboard-widget framework, is report-adjacent and out of scope here). The following are derived from the shipped code, stated as implicit rules:

- **BR-LRA-1 (derived):** "Average schedules per lab" is computed as `totalSchedules / totalLabs` across **all** class schedules regardless of type or date range — it is not restricted to lab-room schedules specifically in a way this review could confirm beyond the repository name (`ClassScheduleRepository`, aliased as `labScheduleRepository`), and it is not scoped to a term or academic year; it is a running total since the first record.
- **BR-LRA-2 (derived):** Equipment/labs "by status" breakdowns use each entity's own current `status` enum value verbatim (e.g. AVAILABLE/IN_USE/UNDER_MAINTENANCE for equipment, ACTIVE/UNDER_MAINTENANCE for labs) with no re-bucketing or business-friendly relabeling beyond title-casing the raw enum name for display.
- **BR-LRA-3 (derived):** The dashboard has no time dimension — every figure is "as of now," not "for this term" or "this month." There is no historical/trend view.

## 4. Business Process / Workflow Narrative
1. An admin with `REPORT_VIEW` opens the Reports & Analytics screen.
2. The screen fires two parallel API calls (lab utilization, attendance analytics) and shows a spinner until both resolve.
3. Results render as KPI cards plus status-breakdown cards; there is no further interaction — no drill-down, no filter, no refresh button, no export.
4. That is the entire process. There is no report-request/report-generation workflow (e.g., "generate report for date range X, download as PDF") anywhere in this module.

## 5. Success Criteria
Not formally defined — inferred from feature completeness against the shipped screen only (not the fuller milestone-tracker description, which was not delivered).

## 6. Assumptions & Constraints
- Assumes "reports" here means live read-only aggregate counts, not generated/downloadable documents.
- Assumes institution-wide scope is sufficient — no cohort/department/program filtering exists.
- Constrained by in-memory aggregation (`findAll()` + Java-side grouping) rather than database aggregate queries, which will not scale gracefully to large equipment/attendance volumes; this is a technical constraint carried into the business process as "the dashboard may become slow to load as data grows," not yet observed as a defect.

## 7. Known Gaps / Deferred
See SRS.md §6 for the itemized list (equipment utilization detail, lab expense, safety incident, CO/PO attainment/NBA/NAAC accreditation reporting, PDF/Excel/CSV export, student-performance widget). None of these business capabilities exist today despite being named in the R1-M5.2 milestone description; if SKSCON needs them, they represent new scope, not a bug fix to existing behavior.
