# Software Requirements Specification — Speciality Management

**Module:** Speciality Management (OneCMS / College Management System)
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Milestone:** R1-M2.1 (Release 1, Milestone 2 — Core Academic & Lab Mapping)

## 1. Introduction

### 1.1 Purpose
This document specifies the requirements for the Speciality Management module, reverse-engineered from the shipped backend (`SpecialityController`/`SpecialityService`/`SpecialityRepository`), the frontend (`frontend/src/app/features/speciality/`), and the Flyway migration history. Speciality is the top-level academic-organization master (equivalent to a "Department") that Programs, Labs, Faculty and Subjects are all scoped under.

### 1.2 Scope
Covers CRUD, activation/deactivation, uniqueness validation, and paginated listing of the `Speciality` master. Does not cover Programs, Courses, Labs, or Faculty themselves (documented separately), only the Speciality entity they reference.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md`, R1-M2.1 (lines 147-170)
- `backend/src/main/java/com/cms/controller/SpecialityController.java`
- `backend/src/main/java/com/cms/model/Speciality.java`
- `backend/src/main/resources/db/migration/V1__create_departments_table.sql`, `V200__rename_departments_to_specialities.sql`, `V229__add_is_active_to_designations_and_specialities.sql`, `V153__add_hod_faculty_id_to_departments.sql`
- `docs/manual-test-cases/speciality-list-status-toggle.md`
- `docs/BUSINESS_REQUIREMENTS.md` — BR-32 (Master Lifecycle Status Management, related pattern)

## 2. Overall Description

### 2.1 Product Perspective
Speciality (originally named "Department") is one of the earliest masters in the system (`V1__create_departments_table.sql`) and was renamed to "Speciality" in `V200` to better match nursing-college terminology (e.g. "General Nursing", "Cardiology"). It anchors the academic hierarchy: `Speciality` → `Lab` (via `speciality_id`), `Speciality` → `Faculty` (via `speciality_id`), and `Speciality` → `Subject` (optional `speciality_id`).

### 2.2 User Classes
- **Admin / College Admin / DEV_ADMIN / SUPPORT_ADMIN** — full create/update/delete/status-toggle access (permission `DEPT_MANAGE`, the legacy code retained from the pre-rename "Department" naming).
- **Any authenticated user** — read access (list, get-by-id, paginated search); required so Speciality can populate dropdowns across Lab, Faculty, Subject and reporting screens.

### 2.3 Operating Environment
Angular frontend (lazy-loaded `features/speciality` route), Spring Boot REST backend under `/specialities`, PostgreSQL (`specialities` table), Keycloak-issued JWT bearer auth, DB-driven permission checks via `@perm.has(...)`.

### 2.4 Constraints / Assumptions
- `code` is globally unique (DB `UNIQUE` constraint); `name` uniqueness is enforced at the service layer via the `/name-exists` check consumed by the frontend's `uniqueFieldValidator`.
- The permission code is `DEPT_MANAGE`/`DEPT_VIEW` (not `SPECIALITY_*`) — a naming artifact surviving the V200 rename; not corrected because permission-code renames are a separate concern from the entity rename.
- `hodFacultyId` is a soft reference (plain `Long` column, not a JPA `@ManyToOne`/FK) to `Faculty.id` — no DB-level referential integrity is enforced between Speciality and Faculty for the HOD assignment.

## 3. Functional Requirements

| ID | Requirement | Priority | Dependencies |
|---|---|---|---|
| FR-SPECIALITY-1 | System shall allow an authorized user to create a Speciality with name, code, description, HOD faculty reference, and HOD name. | Must | — |
| FR-SPECIALITY-2 | System shall allow any authenticated user to list all Specialities, optionally filtered to active-only (`activeOnly=true`). | Must | FR-1 |
| FR-SPECIALITY-3 | System shall allow any authenticated user to fetch a single Speciality by ID. | Must | FR-1 |
| FR-SPECIALITY-4 | System shall allow an authorized user to update an existing Speciality's fields. | Must | FR-1 |
| FR-SPECIALITY-5 | System shall allow an authorized user to hard-delete a Speciality. | Should | FR-1 |
| FR-SPECIALITY-6 | System shall allow an authorized user to toggle a Speciality's active/inactive status via a dedicated status-update endpoint, capturing an optional reason. | Must | FR-1 |
| FR-SPECIALITY-7 | System shall provide a server-backed paginated, searchable list (`GET /specialities/page`) sorted by name by default. | Must | FR-1 |
| FR-SPECIALITY-8 | System shall provide real-time name-uniqueness and code-uniqueness checks (`/name-exists`, `/code-exists`) for use by the frontend's async uniqueness validator, excluding the record's own ID when editing. | Must | FR-1 |

## 4. External Interface Requirements

### 4.1 Screens
- **Speciality List** (`speciality-list.component`) — Material table, search, pagination, status badge/toggle, edit/delete row actions.
- **Speciality Form** (`speciality-form.component`) — create/edit dialog/flyout with name, code, description, HOD name fields and live uniqueness validation.

### 4.2 API Endpoints (high level)
`POST /specialities`, `GET /specialities`, `GET /specialities/{id}`, `PUT /specialities/{id}`, `DELETE /specialities/{id}`, `PATCH /specialities/{id}/status`, `GET /specialities/page`, `GET /specialities/name-exists`, `GET /specialities/code-exists`.

### 4.3 Key DB Entities
`specialities` (`id`, `name`, `code` UNIQUE, `description`, `hod_faculty_id`, `hod_name`, `is_active`, `created_at`, `updated_at`).

## 5. Non-Functional Requirements
- **Performance:** List/page endpoints must support server-side pagination for large speciality counts; default page size 25.
- **Security/RBAC:** Write operations gated by `DEPT_MANAGE`; read operations require only authentication (no fine-grained view permission is enforced at the controller level beyond being logged in).
- **Auditability:** `created_at`/`updated_at` are tracked via Spring Data JPA auditing (`@CreatedDate`/`@LastModifiedDate`); no dedicated audit-log table exists for Speciality changes.

## 6. Known Gaps / Not Yet Implemented
- No dedicated `SPECIALITY_VIEW` permission distinct from `DEPT_MANAGE` — read access is authentication-only, and write access uses the legacy `DEPT_*` naming rather than the `SPECIALITY_*` convention used elsewhere in the app.
- `hodFacultyId` has no FK constraint or cascade behavior if the referenced Faculty is deleted/deactivated.
- Speciality is **not** in BR-32's formally documented master-lifecycle scope table even though it follows the identical `PATCH /{id}/status` contract — same situation as Designation Master (BR-46), which explicitly calls this out as a documentation gap for itself; Speciality was not separately called out but exhibits the same pattern.
