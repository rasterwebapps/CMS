# Software Requirements Specification — Faculty Management

**Module:** Faculty Management (OneCMS / College Management System)
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Milestone:** R1-M2.5 (Release 1, Milestone 2 — Core Academic & Lab Mapping; wider README Module 3), substantially extended by later milestones (BR-26, BR-42)

## 1. Introduction

### 1.1 Purpose
Specifies requirements for the Faculty master — the record of a teaching/non-teaching staff member — as actually shipped, reverse-engineered from `FacultyController`/`FacultyService`, the `Faculty` entity, its DTOs, and migration history. The milestone's original scope (`R1-2.5.1`–`R1-2.5.7`) covered a minimal core-fields CRUD; the shipped entity has grown substantially since (BR-42's extended HR-adjacent profile, BR-26's document review) and this SRS documents the module as it exists today.

### 1.2 Scope
Covers the `Faculty` entity (core identity, employment, and its extended profile: personal/identity, bank details, address, experience breakdown, workload-capacity overrides) and its colocated document-management sub-features (`faculty_documents`, `faculty_document_type_requirements` — the "Faculty Doc Config" screen), all living under `features/faculty` and the `/faculty*` API family.

**Explicitly out of scope** (documented elsewhere, despite sharing the "Faculty" name and being consumed from the Faculty Detail screen):
- **Faculty Availability** (`features/faculty-availability`, entity `FacultyAvailability`) — recurring weekly unavailability blocks, a Timetable constraint (BR-56/57).
- **Faculty Absence & Substitute** (`features/timetable/faculty-absence`, entity `FacultyAbsence`) — one-off date-specific absence + substitute workflow (BR-57).
- **Faculty Session Swap** (`FacultySessionSwapController`) — Timetable swap mechanism (BR-57).
- **Faculty Workload Rules** (`features/timetable/faculty-workload-rules`) — the scoped editor over Timetable's weekly/daily/continuous hour-cap `system_configurations` rows (BR-56, OC-122).
- **Faculty Workload (advisory report) / Schedule Workload** — `FacultyController`'s `/{id}/workload`, `/workload-summary`, `/{id}/schedule-workload` endpoints live in `FacultyController` but delegate entirely to Timetable services (`TimetableGlobalAutoScheduleService`, `ClassScheduleService`) and back the Faculty Detail "Courses" tab; documented under the Timetable BRs (BR-56), referenced here only as an endpoint that happens to be on this controller.
- **Designation Master** (`DesignationMaster`, `DESIGNATION_VIEW`/`_MANAGE`) — a separate DB-driven master Faculty has a mandatory FK to; not itself documented here.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md`, R1-M2.5 (lines 225-239)
- `docs/BUSINESS_REQUIREMENTS.md`, BR-26 (Faculty Document Review Summary and Verification Locks), BR-42 (Faculty Extended Profile & Document-Type Requirements Engine)
- `backend/src/main/java/com/cms/controller/FacultyController.java`, `FacultyDocumentController.java`, `FacultyDocumentTypeRequirementController.java`
- `backend/src/main/java/com/cms/model/Faculty.java`, `FacultyDocument.java`, `FacultyDocumentTypeRequirement.java`, `FacultyDocumentHistory.java`
- `backend/src/main/java/com/cms/model/enums/FacultyStatus.java`, `FacultyType.java`, `FacultyQualification.java`
- Migrations: `V6__create_faculty_table.sql`, `V93__faculty_extended_profile_and_documents.sql`, `V126__create_faculty_document_type_requirements.sql`, `V127__create_document_history_tables.sql`, `V137`, `V144`, `V159`, `V199`, `V219`, `V249`, `V373`, `V387`, `V418`, `V523`
- `docs/manual-test-cases/faculty-management.md`

## 2. Overall Description

### 2.1 Product Perspective
A `Faculty` record is scoped to exactly one `Speciality` and one `DesignationMaster` (both mandatory FKs). Beyond the core identity fields (employee code, name, email, phone, joining date, status), the record carries an extended HR-adjacent profile added by BR-42: identity/demographics (PAN, Aadhaar, DOB, gender, marital status, nationality, religion, blood group), bank details, a postal address (mirroring Student's `Address` embeddable), a six-way teaching/clinical × UG/PG/PhD experience breakdown, highest qualification, and an NRTS (nursing-council registration) number unique when present. It also carries three advisory/hard-cap workload-override fields (`plannedWeeklyHoursOverride`, `plannedDailyHoursOverride`, `plannedContinuousHoursOverride`) consumed by the Timetable module's capacity checks. Supporting documents (ID proofs, certificates, etc.) are tracked per-faculty in `faculty_documents`, with which document types are mandatory driven by a rules table (`faculty_document_type_requirements`, editable via the "Faculty Doc Config" screen) rather than hardcoded.

### 2.2 User Classes
- **Admin / College Admin** — full Faculty CRUD, document upload/review, Doc Config rule management (`FACULTY_MANAGE`, `FACULTY_DOC_CONFIG_MANAGE`).
- **Any authenticated user holding `FACULTY_WORKLOAD_VIEW`** — can view a faculty member's term workload/schedule workload (Faculty Detail "Courses" tab).
- **Any authenticated user** — read Faculty list/detail (used wherever a Faculty dropdown is needed — Lab in-charge assignment, Subject eligible-faculty, Course Offering instructor pickers, etc.).

### 2.3 Operating Environment
Angular (`features/faculty`), Spring Boot REST (`/faculty`, `/faculty/{facultyId}/documents`, `/faculty-document-type-requirements`), PostgreSQL (`faculty`, `faculty_documents`, `faculty_document_type_requirements`, `faculty_document_history`), MinIO object storage for uploaded document files (V249's `storage_key`, matching the pattern also used for Floor Plans), Keycloak JWT auth, DB-driven RBAC.

### 2.4 Constraints / Assumptions
- `Faculty.speciality` and `Faculty.designation` are mandatory FKs; a Faculty cannot exist without both.
- `Faculty.employeeCode`, `email` are DB-unique (`UNIQUE` constraint), checked at the service layer on create/update, and — as of 2026-09-24 — each has a dedicated `/employee-code-exists`/`/email-exists` endpoint (gated `FACULTY_MANAGE`, matching the route permission needed to reach the form) so the Faculty Form has a real-time async uniqueness check on them, same as `nrtsNumber`.
- `nrtsNumber` is unique only when present (nullable unique column) and has a `/faculty/nrts-exists` real-time check endpoint (unauthenticated-beyond-login, unlike the other two — a pre-existing inconsistency, not changed by this fix).
- Which document types are "required" for a given Faculty is computed by OR-matching any rule in `faculty_document_type_requirements` against that Faculty's designation, speciality, or highest qualification (BR-42) — not hardcoded per Faculty.
- Document review status is a derived summary (not a stored field) computed from the Faculty's document set; it must never overload or replace `FacultyStatus` (BR-26 explicit rule).

## 3. Functional Requirements

| ID | Requirement | Priority | Dependencies |
|---|---|---|---|
| FR-FAC-1 | System shall allow creation of a Faculty member with employee code, name, email, phone, speciality, designation, specialization, lab expertise, joining date, and status. | Must | Speciality Management, Designation Master |
| FR-FAC-2 | System shall allow listing all Faculty, paginated/searchable/filterable by speciality, status, and document-review state. | Must | FR-1 |
| FR-FAC-3 | System shall allow fetching a Faculty by ID. | Must | FR-1 |
| FR-FAC-4 | System shall allow updating a Faculty's core and extended-profile details. | Must | FR-1 |
| FR-FAC-5 | System shall allow deleting a Faculty. | Should | FR-1 |
| FR-FAC-6 | System shall reject creating/updating a Faculty with a duplicate employee code, email, or (when present) NRTS number. | Must | FR-1 |
| FR-FAC-7 | System shall capture an extended profile (identity/demographics, bank details, address, teaching/clinical experience breakdown, highest qualification) per Faculty, all fields optional. | Should | FR-1 |
| FR-FAC-8 | System shall allow uploading, reviewing (verify/reject), and re-uploading Faculty documents, with per-document-type requirement rules configurable by an Admin. | Must | FR-1 |
| FR-FAC-9 | System shall show a derived document-review summary badge on the Faculty list (both card and table view) without altering `FacultyStatus`. | Should | FR-8 |
| FR-FAC-10 | System shall allow exporting the Faculty list (filtered/sorted) to Excel/PDF. | Should | FR-2 |
| FR-FAC-11 | System shall allow a per-faculty override of the advisory weekly/daily/continuous teaching-hour caps used by Timetable capacity checks, via a minimal "Raise Cap" action distinct from a full profile edit. | Could | FR-1, Timetable module |

## 4. External Interface Requirements

### 4.1 Screens
- **Faculty List** (`faculty-list.component`) — card + Material-table dual view, filters by speciality/status/document-review; document-review and term-workload badges.
- **Faculty Form** (`faculty-form.component`) — create/edit, covering core + extended profile fields.
- **Faculty Detail** (`faculty-detail.component`) — full profile, documents, and a "Courses" tab showing term/schedule workload; includes the "Raise Cap" flyout (`raise-cap-flyout.component`).
- **Faculty Doc Config** (`faculty-doc-config.component`) — Admin screen to define which document types are required, scoped by designation/speciality/qualification.

### 4.2 API Endpoints (high level)
`POST /faculty`, `GET /faculty`, `GET /faculty/{id}`, `PUT /faculty/{id}`, `PATCH /faculty/{id}/daily-cap`, `DELETE /faculty/{id}`, `GET /faculty/page`, `GET /faculty/export`, `GET /faculty/nrts-exists`, `GET /faculty/employee-code-exists`, `GET /faculty/email-exists`, `GET /faculty/{id}/workload`, `GET /faculty/workload-summary`, `GET /faculty/{id}/schedule-workload`; `GET/PUT/DELETE /faculty/{facultyId}/documents/*`, `POST /faculty/{facultyId}/documents/upload`, `GET /faculty/{facultyId}/documents/{id}/download`; `GET/POST/DELETE /faculty-document-type-requirements`.

### 4.3 Key DB Entities
`faculty` (FK `speciality_id`, `designation_id`; embedded `Address`), `faculty_documents` (FK `faculty_id`), `faculty_document_type_requirements`, `faculty_document_history`.

## 5. Non-Functional Requirements
- **Performance:** Server-side pagination/search/sort on the Faculty list; sort-forwarding wired into the export endpoint.
- **Security/RBAC:** Mutations gated by `hasAny(FACULTY_CREATE|EDIT|DELETE, FACULTY_MANAGE)` (fixed 2026-09-24 — the granular V242 permissions were seeded and nav-configured but never checked by the controller; now live, purely additive alongside the existing `FACULTY_MANAGE` fallback so nobody's access changed); document workflow by `FACULTY_DOC_CONFIG_MANAGE`; workload views by `FACULTY_WORKLOAD_VIEW`; reads otherwise authentication-only.
- **Auditability:** JPA-audited timestamps on Faculty and documents; document review actions logged to `faculty_document_history`.
- **File storage:** Faculty documents stored in MinIO (`storage_key`), not as DB `bytea` (legacy `bytea` columns were dropped, V523).

## 6. Known Gaps / Not Yet Implemented
- ~~`FACULTY_CREATE`, `FACULTY_EDIT`, `FACULTY_DELETE` permissions exist... but are not checked~~ **Fixed 2026-09-24:** `FacultyController`'s create/update/delete now check `hasAny(FACULTY_CREATE|EDIT|DELETE respectively, FACULTY_MANAGE)`. This was safe to do without any DB backfill because migration V242 itself already granted the granular codes to every role that held `FACULTY_MANAGE` at the time it ran; the `FACULTY_MANAGE` fallback additionally covers any role configured since then. See `docs/requirements/FINDINGS.md` for the same fix applied to Examination/Exam Result/Curriculum Version, and a note that this same orphaned-permission pattern likely exists in other V242-touched modules not yet audited.
- No pre-delete dependency guard was found in `FacultyService.delete()` beyond an existence check — deleting a Faculty referenced by active Course Offerings, Lab in-charge assignments, or Class Schedules is not explicitly blocked at the service layer.
- `LabInChargeAssignment.assigneeId` (Lab Setup module) has no FK to `Faculty`, so a Lab Incharge assignment can silently reference a Faculty that no longer exists.
