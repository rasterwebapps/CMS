# Functional Requirements Document — Maintenance & Repair

**Module:** Maintenance & Repair (Module 7.7) · **App:** OneCMS / College Management System

## 1. Overview
CRUD screen + status workflow for logging and tracking repair/maintenance work on lab `Equipment`. Backed by `MaintenanceRequest` (`backend/src/main/java/com/cms/model/MaintenanceRequest.java`), `MaintenanceRequestController`/`Service`/`Repository`, and Angular `features/maintenance/`.

## 2. Actors & Permissions
Exact permission codes found seeded (`V88__seed_roles_and_permissions.sql`, category `INFRASTRUCTURE`) and later split (`V242__granular_screen_permissions.sql`):

| Code | Purpose | Actually enforced where |
|---|---|---|
| `MAINTENANCE_VIEW` | View the list screen | Frontend route guard only (`GET` endpoints carry no `@PreAuthorize`) |
| `MAINTENANCE_MANAGE` | Create/edit/delete | Frontend route guards + backend `@PreAuthorize` on `POST`/`PUT`/`DELETE /maintenance` |
| `MAINTENANCE_CREATE` | Seeded (V242), display name "Raise Maintenance Request" | Not referenced anywhere in controller or frontend — dormant |
| `MAINTENANCE_EDIT` | Seeded (V242) | Not referenced anywhere — dormant |
| `MAINTENANCE_DELETE` | Seeded (V242) | Not referenced anywhere — dormant |
| `MAINTENANCE_EXPORT` | Seeded (V242), "Export Maintenance Log" | No export feature exists on this screen — dormant |

Roles are assigned to permissions via the DB-driven Role Management module (not hardcoded in code). `V89__fix_role_permission_gaps.sql` backfills `MAINTENANCE_MANAGE` onto certain roles; `V242` backfills `MAINTENANCE_CREATE/EDIT/DELETE` onto any role that already held `MAINTENANCE_MANAGE`.

## 3. Screens & UI Behavior

### 3.1 Maintenance List (`/maintenance`)
- Route guard: `withPermission('MAINTENANCE_VIEW', 'MAINTENANCE_MANAGE')`.
- Loads the full unfiltered list via `GET /maintenance` on init; client-side search/sort/paginate via `MatTableDataSource` (page sizes 5/10/25/50, default 25).
- Columns (user-toggleable via column picker, `equipmentName` and `actions` are mandatory/non-hideable): Equipment, Requested By, Priority (`cms-type-badge` with `palette="urgency"`), Status (`cms-status-badge`), Assigned Technician, Created At, row actions (Edit, Delete icon buttons — always visible, no permission-based hiding).
- Delete opens a `ConfirmDialogComponent`; on confirm calls `DELETE /maintenance/{id}` and reloads.
- No create/edit validation happens on this screen; it only links to `/maintenance/new` and `/maintenance/:id/edit`.
- Empty state: "No maintenance requests yet" (no data) vs "No maintenance requests found" (search yields none).

### 3.2 Maintenance Form (`/maintenance/new`, `/maintenance/:id/edit`)
- Route guard: `withPermission('MAINTENANCE_MANAGE')` on both create and edit routes.
- Fields and validators (reactive form, `frontend/src/app/features/maintenance/maintenance-form/maintenance-form.component.ts`):
  - `equipmentId` — required; populated from `GET /equipment` (id+name only).
  - `requestedBy` — required, free text, max length 255.
  - `description` — required, max length 1000.
  - `priority` — required, one of `LOW`/`MEDIUM`/`HIGH`/`CRITICAL`.
  - `status` — optional select, options `REQUESTED`/`ASSIGNED`/`IN_PROGRESS`/`COMPLETED`/`CANCELLED`, defaults to `REQUESTED`.
  - `assignedTechnician` — optional free text, max length 255.
  - `repairCost` — optional number, `min(0)`.
  - `completedDate` — optional date.
  - No `title` or `maintenanceType` field exists on the form at all.
- Live preview side panel mirrors form values (equipment name, requester, priority pill, status pill, formatted cost via `InrPipe`, technician, description).
- On submit, builds a `MaintenanceRequestDto` (the **frontend's own** interface, not identical to the backend's) and calls `create()`/`update()` on `MaintenanceService`.
- Edit mode pre-populates from `GET /maintenance/{id}`, mapping `item.requestedBy`/`item.assignedTechnician`/`item.repairCost`/`item.completedDate` — none of which exist on the backend's actual response payload (see §7).

## 4. Functional Workflows

### 4.1 Create request
1. User opens `/maintenance/new`, fills form, submits.
2. Frontend sends `POST /maintenance` with `{equipmentId, requestedBy, description, priority, status?, assignedTechnician?, repairCost?, completedDate?}`.
3. Backend `MaintenanceRequestController.create` requires `@PreAuthorize('MAINTENANCE_MANAGE')`, validates the **backend's** `MaintenanceRequestDto` (`equipmentId`, `title` NotBlank, `maintenanceType` NotNull, `priority` NotNull, `status` NotNull, `requestDate` NotNull are all required).
4. Service loads `Equipment` by id (404 if missing), optionally loads `Faculty` for `requestedById`/`assignedToId` (404 if given but missing), persists, and — if `status == IN_PROGRESS` — flips the equipment to `UNDER_MAINTENANCE`.

### 4.2 Update / status transition
1. `PUT /maintenance/{id}` with the same DTO shape as create (full replace, not partial patch).
2. Service re-resolves `Equipment`/`Faculty` FKs, overwrites all fields, and applies the same equipment-status side effect: `COMPLETED` → `Equipment.status = AVAILABLE`; `IN_PROGRESS` → `Equipment.status = UNDER_MAINTENANCE`. Any other status value (`PENDING`, `REQUESTED`, `SCHEDULED`, `CANCELLED`) leaves equipment status untouched.

### 4.3 Delete
`DELETE /maintenance/{id}` — hard delete, `@PreAuthorize('MAINTENANCE_MANAGE')`, 404 if not found, 204 on success.

### 4.4 Filtered listing
`GET /maintenance` supports (mutually exclusive, checked in this order by the controller): `pendingOnly=true` → `findPendingRequests()` (status not in `COMPLETED`/`CANCELLED`); else `equipmentId` → `findByEquipmentId`; else `status` → `findByStatus`; else `assignedToId` → `findByAssignedToId`; else `findAll()`.

No calculation formulas exist in this module beyond storing `estimatedCost`/`actualCost` as independent values (no variance % or auto-total computed).

## 5. API Endpoints

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/maintenance` | `MaintenanceRequestDto` (backend shape, see §3.2) | `MaintenanceRequestResponse`, 201 | `MAINTENANCE_MANAGE` |
| GET | `/maintenance` | Query params: `equipmentId?`, `status?`, `assignedToId?`, `pendingOnly?` | `List<MaintenanceRequestResponse>`, 200 | none enforced |
| GET | `/maintenance/{id}` | — | `MaintenanceRequestResponse`, 200 (404 if missing) | none enforced |
| PUT | `/maintenance/{id}` | `MaintenanceRequestDto` | `MaintenanceRequestResponse`, 200 | `MAINTENANCE_MANAGE` |
| DELETE | `/maintenance/{id}` | — | 204 (404 if missing) | `MAINTENANCE_MANAGE` |

**`MaintenanceRequestResponse` shape:** `id, equipmentId, equipmentName, equipmentAssetCode, labId, labName, title, description, maintenanceType, priority, status, requestedById, requestedByName, requestDate, scheduledDate, completionDate, assignedToId, assignedToName, estimatedCost, actualCost, resolutionNotes, createdAt, updatedAt`.

## 6. Data Model

**Table `maintenance_requests`** (`V24__create_maintenance_requests_table.sql`):

| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| equipment_id | BIGINT NOT NULL | FK → `equipment(id)` |
| title | VARCHAR(255) NOT NULL | |
| description | TEXT | |
| maintenance_type | VARCHAR(255) NOT NULL | enum: `PREVENTIVE, CORRECTIVE, EMERGENCY, ROUTINE` |
| priority | VARCHAR(255) NOT NULL | enum: `LOW, MEDIUM, HIGH, CRITICAL` |
| status | VARCHAR(255) NOT NULL | enum: `PENDING, REQUESTED, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED` |
| requested_by | BIGINT | FK → `faculty(id)`, nullable |
| request_date | DATE NOT NULL | |
| scheduled_date | DATE | nullable |
| completion_date | DATE | nullable |
| assigned_to | BIGINT | FK → `faculty(id)`, nullable |
| estimated_cost | NUMERIC(10,2) | nullable |
| actual_cost | NUMERIC(10,2) | nullable |
| resolution_notes | TEXT | nullable |
| created_at / updated_at | TIMESTAMPTZ NOT NULL | JPA-audited |

**Relationships:** `MaintenanceRequest N:1 Equipment` (mandatory); `MaintenanceRequest N:1 Faculty` (requester, optional); `MaintenanceRequest N:1 Faculty` (assignee, optional). `Equipment` itself belongs to a `Lab` (surfaced in the response as `labId`/`labName`).

## 7. Edge Cases & Validation Rules
- `equipmentId` on create/update must reference an existing `Equipment` row, else `404 ResourceNotFoundException`.
- `requestedById`/`assignedToId`, if supplied, must reference existing `Faculty`, else 404.
- Backend rejects create/update lacking `title`, `maintenanceType`, `priority`, `status`, or `requestDate` (Bean Validation `@NotNull`/`@NotBlank`) with a 400.
- **Observed contract mismatch (static code finding, not runtime-verified):**
  - Frontend never populates/sends `title`, `maintenanceType`, or `requestDate` → given the above validation, `POST`/`PUT` calls from the shipped UI are very likely to 400.
  - Frontend sends `requestedBy` (string) and `assignedTechnician` (string); backend DTO has no such fields (only `requestedById`/`assignedToId`, both `Long`) — these are silently dropped by Jackson rather than erroring, but the intended data (who requested/who's assigned) is never actually persisted via the current form.
  - Frontend's `repairCost`/`completedDate` have no corresponding backend fields (backend has `estimatedCost`/`actualCost`/`completionDate`) — also silently dropped.
  - Backend response returns `requestedByName`/`assignedToName`; the frontend list/table and form's edit-prefill both read `requestedBy`/`assignedTechnician`, which are absent from the response — these fields render as `undefined`/blank in the UI.
  - Frontend's status dropdown includes `ASSIGNED`, which is not a valid `MaintenanceStatus` value; submitting it would fail backend enum deserialization.
- `PUT` is a full overwrite (no partial-update/PATCH semantics) — every field must be resent or it's cleared/re-required per the DTO's validation.
- Equipment-status side effects only trigger for `IN_PROGRESS`→`UNDER_MAINTENANCE` and `COMPLETED`→`AVAILABLE`; no side effect for `CANCELLED`, meaning a cancelled-while-in-progress request leaves the equipment stuck as `UNDER_MAINTENANCE` unless manually corrected.

## 8. Known Gaps / Deferred
See SRS §6 and BRD §7 for the consolidated list (contract mismatch, dormant granular permissions, no export, no permission-based UI hiding, unpaginated list API).
