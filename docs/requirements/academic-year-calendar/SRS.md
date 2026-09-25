# Software Requirements Specification — Academic Year & Calendar

**Module:** Academic Year & Calendar (OneCMS / College Management System)
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Milestone:** R1-M2.3 (Release 1, Milestone 2 — Core Academic & Lab Mapping)

## 1. Introduction

### 1.1 Purpose
Specifies requirements for the Academic Year master, the Term (formerly "Semester") lifecycle it auto-generates, and the base Academic Calendar (events) screen, as actually shipped.

### 1.2 Scope
Covers `AcademicYear`, `TermInstance` (the entity that superseded the originally-milestoned `Semester` entity), and `CalendarEvent`. Does not cover the Holiday Template auto-block system, Blocked Periods, or the timetable-engine's day-detail flyout (BR-56/57/58 — separate, later modules), nor Term Billing Schedules / Student Term Enrollment / Term Lifecycle confirmation dialogs (BR-53, operational logistics, documented separately).

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md`, R1-M2.3 (lines 187-200)
- `backend/src/main/java/com/cms/controller/AcademicYearController.java`, `CalendarEventController.java`, `TermInstanceController.java`
- `backend/src/main/java/com/cms/model/AcademicYear.java`, `TermInstance.java`, `CalendarEvent.java`
- Migrations: `V3__create_academic_years_table.sql`, `V4__create_semesters_table.sql`, `V60__create_calendar_events_table.sql`, `V61__add_status_to_semesters.sql`, `V62__create_term_instances_table.sql`, `V115__migrate_lab_schedules_to_term_instance.sql`, `V116__drop_semester_fk_from_calendar_and_examinations.sql`, `V117__drop_semesters_table.sql`, `V118__rename_semester_to_term.sql`, `V295__index_calendar_events_date_range.sql`
- `docs/BUSINESS_REQUIREMENTS.md` — BR-59 (Academic Calendar Base Module), BR-53 (Term Lifecycle Confirmation, referenced but not duplicated here)
- `docs/manual-test-cases/academic-year-management.md`, `academic-calendar.md`

### 1.4 Important Terminology Note
The original milestone (R1-2.3.2) specified a `Semester` entity (`id`, `name`, `academicYear`, `startDate`, `endDate`, `semesterNumber`). That entity was fully retired (`V115`–`V117`, 2026-05-07) in favor of `TermInstance`, which is auto-generated (not manually CRUD'd) — exactly two `TermInstance` rows (`ODD`, `EVEN`) are created per `AcademicYear` at creation time. This SRS documents the shipped `TermInstance` model as the functional successor of the milestone's "Semester entity."

## 2. Overall Description

### 2.1 Product Perspective
`AcademicYear` is the top-level academic-calendar container (e.g. "2026-2027"). Creating one automatically provisions its two `TermInstance` rows (ODD/EVEN) via `TermInstanceService.createTermInstancesForAcademicYear`. `CalendarEvent` records (holidays, exams, cultural events, etc.) are scoped to an `AcademicYear` only — they carry no term-level FK (the original `semester_id` FK was dropped in the same V115-117 retirement).

### 2.2 User Classes
- **Admin / College Admin** — creates Academic Years, advances Term status (`PLANNED → OPEN → LOCKED`), manages Calendar Events (`ACADEMIC_YEAR_MANAGE`, `ACADEMIC_CALENDAR_MANAGE`, and the legacy-named `SEMESTER_MANAGE` for Term updates).
- **Faculty / Student / Parent** — read-only view of the Academic Calendar (Timeline/Month-Grid views), Print/Export always visible regardless of role.

### 2.3 Operating Environment
Angular (`features/academic-year`, including `academic-calendar/` subfolder), Spring Boot REST (`/academic-years`, `/term-instances`, `/calendar-events`), PostgreSQL, Keycloak JWT auth, DB-driven RBAC.

### 2.4 Constraints / Assumptions
- `AcademicYear.name` is globally unique.
- Exactly one `AcademicYear` should be marked `isCurrent=true` at a time (enforced by service logic, not a DB partial-unique-index in the reviewed code).
- `TermInstance` uniqueness is `(academic_year_id, term_type)` — at most one ODD and one EVEN term per academic year.
- `TermInstance` status transitions are one-directional (`PLANNED → OPEN → LOCKED`), enforced in `AcademicYearService`/`TermInstanceService`.
- The permission gating `TermInstanceController`'s write endpoints is `SEMESTER_MANAGE` — a legacy name retained from before the Semester→Term rename.

## 3. Functional Requirements

| ID | Requirement | Priority | Dependencies |
|---|---|---|---|
| FR-ACADCAL-1 | System shall allow creation of an Academic Year with name, start date, end date, and current-year flag, auto-creating its ODD/EVEN Term Instances. | Must | — |
| FR-ACADCAL-2 | System shall allow listing, paginated search, get-by-ID, and get-current-year lookup of Academic Years. | Must | FR-1 |
| FR-ACADCAL-3 | System shall allow updating an Academic Year's dates/name (with a "full update" variant), validating that existing Term Instances still fit within the new bounds. | Must | FR-1 |
| FR-ACADCAL-4 | System shall allow deleting an Academic Year and provide real-time name-uniqueness validation. | Should | FR-1 |
| FR-ACADCAL-5 | System shall allow viewing and advancing a Term Instance's status, gated by a pre-transition checklist (out of scope here, see BR-53), and configuring which nth-Saturday-of-month occurrences count as working days for that term. | Must | FR-1 |
| FR-ACADCAL-6 | System shall allow creating, listing, updating, and deleting Calendar Events scoped to an Academic Year, each typed as one of HOLIDAY/EXAM/CULTURAL/SPORTS/WORKSHOP/OTHER. | Must | FR-1 |
| FR-ACADCAL-7 | System shall detect overlapping Calendar Events within an Academic Year for a proposed date range before create/update. | Should | FR-6 |
| FR-ACADCAL-8 | System shall render an Academic Calendar screen with Timeline and Month-Grid views, computing all summary statistics (weeks, term count, days remaining, event counts, progress bars) client-side. | Must | FR-6 |
| FR-ACADCAL-9 | System shall support browser Print and client-side CSV Export of calendar events from the Academic Calendar screen, available to all roles regardless of edit permission. | Should | FR-8 |

## 4. External Interface Requirements

### 4.1 Screens
- **Academic Year List / Form** (`features/academic-year`).
- **Academic Calendar** (`features/academic-year/academic-calendar/`) — Timeline/Month-Grid toggle, Add/Edit Event, Print, CSV Export.
- **Term Advance Checklist Dialog** (`term-advance-checklist-dialog/`) — pre-transition confirmation (full detail under BR-53, not duplicated here).

### 4.2 API Endpoints (high level)
`/academic-years` (CRUD + `/page`, `/current`, `/{id}/full`, `/name-exists`); `/term-instances` (`GET`, `PUT /{id}`, `/{id}/advance-checklist`, `/{id}/working-saturdays`); `/calendar-events` (CRUD + `/academic-year/{id}`, `/academic-year/{id}/overlapping`, `/{id}/series`).

### 4.3 Key DB Entities
`academic_years`, `term_instances` (+ `term_working_saturdays` element collection), `calendar_events`.

## 5. Non-Functional Requirements
- **Performance:** `calendar_events` has an index on `(academic_year_id, event_type, start_date, end_date)` (V295) supporting date-range lookups used by both this module and the downstream timetable engine.
- **Security/RBAC:** Academic Year writes require `ACADEMIC_YEAR_MANAGE`; Term Instance writes require the legacy-named `SEMESTER_MANAGE`; Calendar Event writes require `ACADEMIC_CALENDAR_MANAGE`; all reads require only authentication.
- **Auditability:** JPA-audited timestamps on all three entities; Term status transitions are guarded by validation but not separately logged to an audit table within this module's own scope (BR-53 layers a notification/alert system on top, documented separately).

## 6. Known Gaps / Not Yet Implemented
- The milestone's originally-planned `Semester` entity does not exist under that name — succeeded by `TermInstance`, which is system-generated (2 per Academic Year) rather than freely created/named by an admin as the milestone envisioned.
- `TermInstanceController` write endpoints use the legacy permission code `SEMESTER_MANAGE`, not a `TERM_*`-named code, despite the entity itself being fully renamed.
- No dedicated backend "calendar stats" endpoint exists — all Academic Calendar summary figures are computed client-side (confirmed by BR-59), so any future non-Angular consumer (e.g. a mobile app or reporting export) would need to reimplement that computation.
- Full Term Lifecycle confirmation/alerting (checklist details, overdue notifications) is documented separately under BR-53 and intentionally not duplicated in this SRS.
