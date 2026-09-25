# Software Requirements Specification — Equipment & Lab Inventory

**Module:** Equipment & Lab Inventory (legacy)
**Application:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.

---

## 1. Introduction

### 1.1 Purpose
This document specifies the requirements for the **Equipment** tracking feature of OneCMS, as actually implemented in the codebase. It was delivered under Release 1, Milestone 4.2 ("Equipment & Inventory Management", `docs/RELEASE_1_MILESTONES.md` lines 509–531).

### 1.2 Scope boundary (important)
Milestone R1-M4.2 originally planned **two** sub-features: Equipment tracking, and a lab-consumables inventory (documented in the milestone tracker as `Consumable` + `StockTransaction` entities). Investigation of the shipped code shows:

- **Equipment tracking is live and in active use.** The `Equipment` entity, `EquipmentController`, `EquipmentService`, and the `features/equipment` Angular module exist essentially as planned and are reachable from the app (routes `/equipment`, `/equipment/new`, `/equipment/:id/edit`).
- **The lab-consumables inventory side was never built as `Consumable`/`StockTransaction`.** It was instead implemented as a single `InventoryItem` entity/`inventory_items` table with `INVENTORY_VIEW/CREATE/EDIT/DELETE/EXPORT/MANAGE` permissions. Per migration `V476__retire_legacy_inventory_item.sql`, this feature **"was never actually completed/used: 0 rows in `inventory_items`, 0 `VirtualLocation` markers... anywhere"**, and was formally retired (table dropped, permissions deleted) once the standalone, general-purpose Inventory Management module (Release 3 — Product/StockBalance system, `frontend/src/app/features/inventory/`, `BR-61`) fully superseded it. That retirement and the new module are **out of scope for this document** — they are covered separately in the Release 3 Inventory Management documentation set.

This document therefore covers **Equipment tracking only**, which is the one part of the original R1-M4.2 scope that remains live.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md` lines 509–531 (R1-M4.2)
- `backend/src/main/resources/db/migration/V22__create_equipment_table.sql`
- `backend/src/main/resources/db/migration/V476__retire_legacy_inventory_item.sql`
- No entry in `docs/BUSINESS_REQUIREMENTS.md`'s table of contents is dedicated to Equipment tracking or the old lab inventory — no BR-N document exists for this feature. Business rules in the companion BRD are inferred from code and the milestone tracker, not from a BR document.

## 2. Overall Description

### 2.1 Product Perspective
Equipment tracking is a small standalone master/record-keeping screen inside OneCMS's Academics/Labs area. Each `Equipment` row belongs to exactly one `Lab` (`lab_id` FK, `NOT NULL`). It has no dependency on Fee/Enquiry/Student modules. It is referenced by the Campus Spatial module (`BR-60`) as one of the entity types (`EQUIPMENT`) a floor-plan marker can point at, and conceptually by the Maintenance & Repair module (`MaintenanceRequest.equipment` FK — see `docs/requirements/maintenance-repair/`), though no maintenance workflow logic lives in this module itself.

### 2.2 Actors / User Classes
- **Users holding `EQUIPMENT_MANAGE`** (typically Lab Admin / College Admin roles, assigned via the DB-driven Role Management module) — create, update, delete equipment.
- **Users holding `EQUIPMENT_EXPORT`** — export the equipment list to Excel/PDF.
- **Any authenticated user** — read access (`GET /equipment`, `/equipment/{id}`, `/equipment/asset/{assetCode}`, `/equipment/page`) has no `@PreAuthorize` gate in the controller, i.e. list/view is open to any logged-in user, not permission-gated.

### 2.3 Operating Environment
Angular frontend (`frontend/src/app/features/equipment/`), Spring Boot REST backend (`backend/src/main/java/com/cms/{model,dto,controller,service,repository}` — `Equipment*`), PostgreSQL table `equipment` (Flyway `V22`), Keycloak-authenticated, DB-driven permission checks via `@perm.has(...)`.

### 2.4 Constraints / Assumptions
- `asset_code` is optional but unique when present (case-insensitive uniqueness enforced in `EquipmentService`, not via a dedicated `/name-exists`-style async endpoint — this module predates the `uniqueFieldValidator` pattern now mandatory for new masters).
- `category` and `status` are fixed backend enums (`EquipmentCategory`, `EquipmentStatus`); the frontend renders them as plain `<select>` dropdowns, not master-managed lists.
- Monetary field `purchase_price` (`NUMERIC(10,2)`) — the frontend model/form call it `purchaseCost`, a naming drift from the backend's `purchasePrice` (see §6).

## 3. Functional Requirements

| ID | Requirement | Priority | Dependencies |
|---|---|---|---|
| FR-EQIP-1 | System shall allow creating an Equipment record with name, category, lab, and status mandatory; asset code, serial number, manufacturer, model, purchase date/price, warranty expiry, location, and specifications optional. | Must | Lab master must exist |
| FR-EQIP-2 | System shall reject creating/updating an Equipment record whose (trimmed, case-insensitive) asset code duplicates an existing record's asset code. | Must | FR-EQIP-1 |
| FR-EQIP-3 | System shall allow listing Equipment filtered by `labId`, `status`, or `category` (mutually exclusive query params; only one filter applied per request, `labId` takes precedence). | Must | — |
| FR-EQIP-4 | System shall support a paginated, searchable list (`GET /equipment/page`) matching search text against name, model, or lab name. | Must | — |
| FR-EQIP-5 | System shall allow updating and deleting an existing Equipment record, gated by `EQUIPMENT_MANAGE`. | Must | FR-EQIP-1 |
| FR-EQIP-6 | System shall allow exporting the equipment list (filtered/sorted) to Excel or PDF, gated by `EQUIPMENT_EXPORT`, with sort restricted to an allow-list of columns (name, model, lab name, category, status, purchase date). | Should | FR-EQIP-3 |
| FR-EQIP-7 | System shall allow looking up a single Equipment record by its asset code (`GET /equipment/asset/{assetCode}`). | Could | — |

## 4. External Interface Requirements

### 4.1 Screens
- Equipment List (`/equipment`) — card/table list with status badges, search, export.
- Equipment Form (`/equipment/new`, `/equipment/:id/edit`) — create/edit form with a live preview card.

### 4.2 API Endpoints (high level)
`POST /equipment`, `GET /equipment`, `GET /equipment/{id}`, `GET /equipment/asset/{assetCode}`, `PUT /equipment/{id}`, `DELETE /equipment/{id}`, `GET /equipment/export`, `GET /equipment/page`. See FRD for full contract.

### 4.3 Key DB Entities
`equipment` (Flyway `V22`) — see FRD §6 for columns.

## 5. Non-Functional Requirements

- **Performance:** list/page endpoints use Spring Data `Specification` + `Pageable`; no caching layer; acceptable for expected per-institution equipment volumes.
- **Security/RBAC:** write operations (create/update/delete/export) are permission-gated server-side via `@perm.has(...)`; read operations are **not** permission-gated (any authenticated caller can read). Role-to-permission assignment is DB-only (Role Management module), never hardcoded.
- **Auditability:** `created_at`/`updated_at` timestamps are auto-populated via JPA auditing (`@CreatedDate`/`@LastModifiedDate`); no separate audit-log table records who changed what value.

## 6. Known Gaps / Not Yet Implemented

1. **Frontend/backend field-name and field-set drift.** `frontend/src/app/features/equipment/equipment.model.ts` uses `purchaseCost` where the backend DTO field is `purchasePrice`, and omits `assetCode`, `manufacturer`, `location`, and `specifications` entirely — these four backend fields have no UI to set or view them (confirmed by reading `equipment-form.component.html`, which has no asset-code/manufacturer/location/specifications inputs). This drift is independently flagged in a comment in the Release-3 Spatial module's code (`spatial.service.ts`), which explicitly avoids reusing `features/equipment`'s model for this reason.
2. **Granular permissions seeded but unused.** Migration `V242` seeds `EQUIPMENT_CREATE`, `EQUIPMENT_EDIT`, `EQUIPMENT_DELETE`, `EQUIPMENT_EXPORT` (tier `MASTER`, screen "Equipment") alongside the original `EQUIPMENT_VIEW`/`EQUIPMENT_MANAGE` (V88). `EquipmentController` still gates create/update/delete entirely on the single `EQUIPMENT_MANAGE` permission (only `EXPORT` uses its own granular permission) — the granular CREATE/EDIT/DELETE permissions exist in the DB/Role Management UI but are not individually enforced.
3. **Equipment-form live-preview status-color mismatch.** `equipment-form.component.html`'s preview pill checks `previewStatus() === 'UNDER_REPAIR'` and `'DAMAGED'`, but the real `EquipmentStatus` enum values are `UNDER_MAINTENANCE` and `OUT_OF_ORDER` — the preview pill never turns amber/muted for those statuses (falls through to the default color). This is local to the create/edit form's preview card; the actual list screen uses the shared `<cms-status-badge>` component, whose `resolveClass()` switch correctly includes `UNDER_MAINTENANCE`, `OUT_OF_ORDER`, and `DISPOSED`, so list-view badges render correctly.
4. **No async uniqueness validator on the form.** The asset-code uniqueness check is server-side only (on submit); there is no real-time `/name-exists`-style check while typing, unlike the mandatory pattern for newer masters.
5. **Read endpoints unauthenticated-by-permission.** `GET /equipment`, `/{id}`, `/asset/{assetCode}`, `/page` carry no `@PreAuthorize` — any authenticated user can read equipment data regardless of role.
6. **No stock/consumable tracking exists today** for labs — that capability now lives entirely in the Release 3 Inventory Management module, not here.
