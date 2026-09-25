# Functional Requirements Document — Equipment & Lab Inventory

**Application:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Overview

Equipment tracking is a lab-scoped asset register: create/list/edit/delete/export `Equipment` rows, each tied to one `Lab`. It is the surviving half of the original R1-M4.2 milestone; the lab-consumables stock-tracking half (`InventoryItem`) was retired unused via migration `V476` in favor of the separate Release 3 Inventory Management module.

## 2. Actors & Permissions

Permission codes (from `V88__seed_roles_and_permissions.sql` and `V242__granular_screen_permissions.sql`, screen label "Equipment" per `V247`):

| Code | Tier | Enforced where |
|---|---|---|
| `EQUIPMENT_VIEW` | MASTER | Seeded (V88) but **not referenced by any `@PreAuthorize` in `EquipmentController`** — list/get endpoints are open to any authenticated user. |
| `EQUIPMENT_MANAGE` | MASTER | `POST /equipment`, `PUT /equipment/{id}`, `DELETE /equipment/{id}` |
| `EQUIPMENT_CREATE` | MASTER (V242) | Seeded but not independently checked — create is gated by `EQUIPMENT_MANAGE` instead. |
| `EQUIPMENT_EDIT` | MASTER (V242) | Seeded but not independently checked — update is gated by `EQUIPMENT_MANAGE` instead. |
| `EQUIPMENT_DELETE` | MASTER (V242) | Seeded but not independently checked — delete is gated by `EQUIPMENT_MANAGE` instead. |
| `EQUIPMENT_EXPORT` | MASTER (V242) | `GET /equipment/export` |

Role-to-permission assignment is handled entirely via the DB-driven Role Management module (not hardcoded).

## 3. Screens & UI Behavior

### 3.1 Equipment List (`/equipment`)
- Card view and table view (toggle), each item/row shows: name, lab (`cms-badge cms-badge--gray`), category, status (`<cms-status-badge [status]="item.status">`), model/serial.
- Card accent border driven by `statusAccentClass(item.status)`.
- Search box (matches name/model/lab name server-side via `/equipment/page`).
- Export button (Excel/PDF), visible/enabled per `EQUIPMENT_EXPORT`.
- Status badges use the shared `CmsStatusBadgeComponent`; its `resolveClass()` switch correctly covers all 5 `EquipmentStatus` values (`AVAILABLE`→success, `IN_USE`→warning/amber, `UNDER_MAINTENANCE`→warning/amber, `OUT_OF_ORDER`/`DISPOSED`→inactive/red), so list badges render correctly.

### 3.2 Equipment Form (`/equipment/new`, `/equipment/:id/edit`)
Fields: Name* (text), Model, Serial Number, Lab* (select, populated from Lab master), Category* (select, `EquipmentCategory` enum), Status (select, `EquipmentStatus` enum), Purchase Date (date), Warranty Expiry (date), Purchase Cost (number, ₹).//
- `*` = required (matches backend `@NotBlank`/`@NotNull`).
- No fields for `assetCode`, `manufacturer`, `location`, `specifications` — these exist on the backend entity/DTO but have no UI input (see Known Gaps).
- Live preview card shows name, model/serial, category pill, lab pill, status pill (color driven by local string comparison against `'AVAILABLE'`, `'UNDER_REPAIR'`, `'DAMAGED'`, `'DISPOSED'` — the latter two of these four literals do not match the real enum values `UNDER_MAINTENANCE`/`OUT_OF_ORDER`, so the preview pill fails to color for those two statuses; this is local to the preview card only, not the list screen), and cost pill.
- No async uniqueness check while typing asset code (asset code isn't even a form field); uniqueness is enforced only server-side on submit for the `assetCode` value (not exposed in the UI at all, so in practice it is always `null` when created via this form, making the uniqueness constraint effectively dormant from the UI's perspective).

## 4. Functional Workflows

### 4.1 Create Equipment
1. User fills required fields (name, lab, category); status required with no visible default forced by the UI beyond whatever the `<select>` defaults to.
2. On submit, frontend calls `POST /equipment` with `EquipmentRequest`.
3. Backend validates Lab exists (404 `ResourceNotFoundException` if not), checks asset-code uniqueness (`400` `IllegalArgumentException` if duplicate — moot in practice since the form never sends one), persists, returns `201` with `EquipmentResponse`.

### 4.2 Edit Equipment
1. Frontend loads existing record via `GET /equipment/{id}`, pre-fills form (`equipmentCost`→backend `purchasePrice` via a mapping the component performs, per model drift noted in SRS).
2. On submit, `PUT /equipment/{id}` re-validates Lab existence and asset-code uniqueness (excluding self via `existsByAssetCodeIgnoreCaseAndIdNot`).

### 4.3 Delete Equipment
`DELETE /equipment/{id}` — hard delete (no soft-delete/`isActive` flag on this entity), 404 if not found, `204` on success. No confirmation of downstream references (e.g. existing `MaintenanceRequest` rows pointing at the equipment) is performed in `EquipmentService.delete()` — a delete could orphan/break a FK depending on the `MaintenanceRequest.equipment` constraint (see Maintenance & Repair docs for that FK's `ON DELETE` behavior).

### 4.4 List / Filter / Search / Export
- `GET /equipment?labId=&status=&category=` — mutually exclusive filters; if none supplied, returns all.
- `GET /equipment/page?search=&sort=&page=&size=` — search matches name/model/lab name (case-insensitive `LIKE`).
- `GET /equipment/export?format=excel|pdf&search=&sort=&direction=` — sort restricted to an allow-list (`name`, `model`, `lab.name`, `category`, `status`, `purchaseDate`); unrecognized sort falls back to `name ASC` via `ExportSortUtils.resolve`.

## 5. API Endpoints

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/equipment` | `EquipmentRequest` | `201 EquipmentResponse` | `EQUIPMENT_MANAGE` |
| GET | `/equipment` | query: `labId?`, `status?`, `category?` | `200 List<EquipmentResponse>` | none |
| GET | `/equipment/{id}` | — | `200 EquipmentResponse` / `404` | none |
| GET | `/equipment/asset/{assetCode}` | — | `200 EquipmentResponse` / `404` | none |
| PUT | `/equipment/{id}` | `EquipmentRequest` | `200 EquipmentResponse` | `EQUIPMENT_MANAGE` |
| DELETE | `/equipment/{id}` | — | `204` / `404` | `EQUIPMENT_MANAGE` |
| GET | `/equipment/export` | query: `format`, `search?`, `sort?`, `direction?` | file (`byte[]`) | `EQUIPMENT_EXPORT` |
| GET | `/equipment/page` | query: `search?`, page/size/sort (Pageable) | `200 Page<EquipmentResponse>` | none |

`EquipmentRequest`: `name*`, `assetCode`, `serialNumber`, `category*`, `labId*`, `manufacturer`, `model`, `status*`, `purchaseDate`, `purchasePrice`, `warrantyExpiry`, `location`, `specifications`.
`EquipmentResponse`: adds `id`, `labName`, `createdAt`, `updatedAt` to the above.

## 6. Data Model

**Table `equipment`** (Flyway `V22__create_equipment_table.sql`):

| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| name | VARCHAR(255) NOT NULL | |
| asset_code | VARCHAR(255) UNIQUE | nullable |
| serial_number | VARCHAR(255) | |
| category | VARCHAR(255) NOT NULL | enum string |
| lab_id | BIGINT NOT NULL | FK → `labs(id)` |
| manufacturer | VARCHAR(255) | |
| model | VARCHAR(255) | |
| status | VARCHAR(255) NOT NULL | enum string |
| purchase_date | DATE | |
| purchase_price | NUMERIC(10,2) | |
| warranty_expiry | DATE | |
| location | VARCHAR(255) | free text, independent of the `Lab`/Room hierarchy |
| specifications | VARCHAR(255) | free text |
| created_at / updated_at | TIMESTAMPTZ NOT NULL | JPA-audited |

Relationships: `Equipment.lab` → `Lab` (many-to-one, mandatory). Referenced by `MaintenanceRequest.equipment` (Maintenance & Repair module) and by `VirtualLocation` (`entityType='EQUIPMENT'`, Campus Spatial module, BR-60) — both external to this module.

`EquipmentCategory`: `COMPUTER, PERIPHERAL, NETWORKING, ELECTRONIC, MECHANICAL, FURNITURE, CONSUMABLE, SOFTWARE`.
`EquipmentStatus`: `AVAILABLE, IN_USE, UNDER_MAINTENANCE, OUT_OF_ORDER, DISPOSED`.

## 7. Edge Cases & Validation Rules

- Blank `name` → `400` (`@NotBlank`).
- Missing `category`/`labId`/`status` → `400` (`@NotNull`).
- Non-existent `labId` → `404 ResourceNotFoundException`.
- Duplicate `assetCode` (case-insensitive, trimmed) on create/update → `400 IllegalArgumentException`.
- Empty-string optional fields are normalized to `null` server-side (`trim()` helper in `EquipmentService`).
- Deleting equipment referenced by a `MaintenanceRequest` — behavior depends on that FK's constraint, not enforced/checked in `EquipmentService` itself (potential gap — see BRD/SRS).
- All read endpoints are unauthenticated-by-permission (open to any logged-in user) — a caller without `EQUIPMENT_VIEW` can still list/read equipment.

## 8. Known Gaps / Deferred

1. Frontend model (`equipment.model.ts`) omits `assetCode`, `manufacturer`, `location`, `specifications`, and renames `purchasePrice`→`purchaseCost` — these fields are write-only from the backend's perspective when created via this UI.
2. `EQUIPMENT_CREATE`/`EQUIPMENT_EDIT`/`EQUIPMENT_DELETE` permissions (V242) exist in the DB but are not individually checked by the controller — all three actions collapse onto `EQUIPMENT_MANAGE`.
3. Equipment-form preview card's status-color mapping uses stale literal strings (`UNDER_REPAIR`, `DAMAGED`) that don't match the real enum (`UNDER_MAINTENANCE`, `OUT_OF_ORDER`) — cosmetic, local to the create/edit form only.
4. No state-machine validation on `status` transitions.
5. No real-time async uniqueness validator (`/name-exists` pattern) — this module predates that now-mandatory convention.
6. No stock-quantity/consumable tracking in this module (see SRS §1.2 scope boundary — that capability lives in the Release 3 Inventory Management module).
