# Software Requirements Specification — Maintenance & Repair

**Module:** Maintenance & Repair (Module 7.7)
**Application:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Introduction

### 1.1 Purpose
Specifies requirements for the Maintenance & Repair feature, delivered under Release 1, Milestone 4.3 (`docs/RELEASE_1_MILESTONES.md` lines 532–545). It tracks repair/maintenance requests raised against `Equipment` records (see `docs/requirements/equipment-lab-inventory/`).

### 1.2 Scope
Covers `MaintenanceRequest` — a single work-order-style record per equipment repair/maintenance event, with a workflow field (`status`) and an assignment field (`assignedTo`, a `Faculty`). This is distinct from the newer `AssetMaintenanceSchedule` feature (`backend/src/main/java/com/cms/inventory/asset/`, Release 3 Inventory Management module) which schedules *recurring preventive maintenance* for Inventory Assets — a separate entity/screen (`/inventory/asset/maintenance-schedules`) not covered here.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md` lines 532–545 (R1-M4.3)
- `backend/src/main/resources/db/migration/V24__create_maintenance_requests_table.sql`
- No BR-N entry exists in `docs/BUSINESS_REQUIREMENTS.md`'s table of contents for Maintenance/Repair — none was found; business rules are inferred from code alone.

## 2. Overall Description

### 2.1 Product Perspective
A `MaintenanceRequest` references one `Equipment` (mandatory FK) and optionally a `Faculty` who requested it and a `Faculty` assigned to perform it. Creating/updating a request with certain statuses side-effects the linked `Equipment.status` (see FR-MNT-5).

### 2.2 Actors / User Classes
- Users holding `MAINTENANCE_MANAGE` — create/update/delete requests.
- Any authenticated user — list/view requests (no `@PreAuthorize` on GET endpoints, same open-read pattern as Equipment).

### 2.3 Operating Environment
Angular (`frontend/src/app/features/maintenance/`), Spring Boot (`MaintenanceRequest*` classes under `com.cms`), PostgreSQL `maintenance_requests` table (Flyway V24), Keycloak-authenticated, DB-driven permissions.

### 2.4 Constraints / Assumptions
No export capability exists for this screen (unlike Equipment) despite `MAINTENANCE_EXPORT` being seeded as a permission (see §6).

## 3. Functional Requirements

| ID | Requirement | Priority | Dependencies |
|---|---|---|---|
| FR-MNT-1 | System shall allow creating a maintenance request against an existing Equipment record with title, maintenance type, priority, status, and request date mandatory. | Must | Equipment must exist |
| FR-MNT-2 | System shall allow optionally linking a requesting Faculty and an assigned Faculty (technician) by ID. | Should | Faculty master |
| FR-MNT-3 | System shall allow listing/filtering requests by equipment, status, assigned-to, or a "pending only" flag. | Must | — |
| FR-MNT-4 | System shall allow updating and deleting a request, gated by `MAINTENANCE_MANAGE`. | Must | FR-MNT-1 |
| FR-MNT-5 | System shall automatically set the linked Equipment's status to `UNDER_MAINTENANCE` when a request is created or updated with status `IN_PROGRESS`, and to `AVAILABLE` when updated to `COMPLETED`. | Must | Equipment module |
| FR-MNT-6 | System shall record estimated cost, actual cost, and resolution notes for a request. | Should | — |

## 4. External Interface Requirements

### 4.1 Screens
- Maintenance List (`/maintenance`) — table with priority badge (`cms-type-badge` urgency palette) and status badge (`cms-status-badge`).
- Maintenance Form (`/maintenance/new`, `/maintenance/:id/edit`).

### 4.2 API Endpoints (high level)
`POST /maintenance`, `GET /maintenance`, `GET /maintenance/{id}`, `PUT /maintenance/{id}`, `DELETE /maintenance/{id}`. No `/export` endpoint exists despite the permission being seeded (see FRD §8).

### 4.3 Key DB Entities
`maintenance_requests` (Flyway V24) — see FRD §6.

## 5. Non-Functional Requirements

- **Security/RBAC:** write operations gated by `MAINTENANCE_MANAGE`; reads open to any authenticated user, same pattern as Equipment.
- **Auditability:** `created_at`/`updated_at` via JPA auditing only; no dedicated change-history log.
- **Performance:** simple `findAll`/`findBy*` queries, no pagination endpoint (unlike Equipment's `/page`) — the list screen loads the full unpaginated result set.

## 6. Known Gaps / Not Yet Implemented

1. **Frontend/backend contract mismatch — the create/edit form appears non-functional as shipped.** Static comparison of the frontend and backend contracts found:
   - Backend `MaintenanceRequestDto` requires `title` (`@NotBlank`), `maintenanceType` (`@NotNull`, enum `PREVENTIVE/CORRECTIVE/EMERGENCY/ROUTINE`), and `requestDate` (`@NotNull`) — **none of these three mandatory fields exist anywhere in the Angular form** (`maintenance-form.component.ts`'s `FormGroup` has no `title`, `maintenanceType`, or `requestDate` controls).
   - The frontend sends `requestedBy`/`assignedTechnician` as free-text strings; the backend DTO instead expects `requestedById`/`assignedToId` as numeric Faculty IDs — the field names don't even match, so these values would be silently dropped by Jackson's default unknown-property handling rather than populate `requestedBy`/`assignedTo`.
   - The frontend's status dropdown offers `REQUESTED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED` — `ASSIGNED` is not a valid `MaintenanceStatus` enum value (`PENDING, REQUESTED, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED`), and `PENDING`/`SCHEDULED` are missing from the dropdown.
   - Net effect (based on static code inspection only — not confirmed by running the app): submitting the current form would very likely be rejected by the backend's `@Valid` validation (`400`) due to the missing mandatory `title`/`maintenanceType`/`requestDate`, and selecting `ASSIGNED` status would fail enum deserialization. This reads as a frontend that was never updated after the backend's DTO was extended beyond the original milestone-planned shape (`id, equipment, requestedBy, description, priority, status, assignedTechnician, completedDate, repairCost`), which is exactly the shape the frontend model still reflects.
2. **`MAINTENANCE_EXPORT` permission seeded (V242) but no export endpoint or button exists** in `MaintenanceRequestController`/the list screen — dead permission.
3. **`MAINTENANCE_CREATE`/`EDIT`/`DELETE` granular permissions (V242) seeded but not individually enforced** — controller still gates all three on `MAINTENANCE_MANAGE` only (same pattern as Equipment).
4. **No pagination/search/export on the list endpoint** — `GET /maintenance` returns the unfiltered/unpaginated full list unless a specific filter query param is supplied.
5. Read endpoints have no permission gate (open to any authenticated user).
