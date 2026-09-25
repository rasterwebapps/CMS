# Software Requirements Specification — Asset Management

**Module:** Inventory Management (OneCMS) — Sub-module 5 of 7
**Corresponds to:** Release 3 Milestone R3-M5 ("Asset Management")
**Status as documented:** Done (100%)

> Standalone, industry-agnostic sub-module — see the parent module note in `inventory-core-data-model/SRS.md` §0. No vertical (hospital/college) naming appears anywhere in this sub-module's schema or UI copy. This is the new, general-purpose Inventory asset register — distinct from the pre-existing, lab-specific `equipment-lab-inventory` and `maintenance-repair` modules documented elsewhere in this repo, which this sub-module neither replaces nor depends on.

## 1. Introduction

### 1.1 Purpose
Defines the requirements for registering and tracking one physical, individually-identified unit of a Product (an asset, as distinct from the Core Data Model's aggregate stock-quantity tracking), its preventive-maintenance schedule and supplier service contracts, its computed straight-line depreciation, and its eventual disposal.

### 1.2 Scope
Asset, AssetMaintenanceSchedule, AssetServiceContract, the read-only straight-line depreciation calculation, and Asset Disposal. Out of scope for this document: the Asset Depreciation Summary Report (a cross-Asset aggregate view, part of the Reporting sub-module, though it reuses this sub-module's own depreciation formula), and the older, lab-specific equipment/maintenance modules noted above, which are separate systems.

### 1.3 References
- `docs/inventory-management/MILESTONES.md` Phase 5
- `docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §1 (3.6 source-SRS section)
- `docs/inventory-management/DECISION_LOG.md`: 2026-09-08 "Asset register slice", "Maintenance & Service Contracts slice", "Depreciation slice", "Disposal slice"; 2026-09-09 "Asset Depreciation Summary Report slice"; 2026-09-22 "Overnight Phase 3: Equipment & Asset Management permission split + bulk demo data"
- Source SRS: `docs/inventory-management/source/SRS_v3.2_Updated.pdf` §3.6 (A001–A0xx)

**Reconciliation against the source SRS:** the source SRS's §3.6 Asset Management assumes Biomedical Engineering as the maintenance owner and medical equipment as the asset class, with AMC/CMC terminology framed specifically around clinical instrumentation (`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §2). The as-built `Asset`/`AssetMaintenanceSchedule`/`AssetServiceContract` model keeps the AMC/CMC contract-type labels — judged industry-standard maintenance-contract terminology, not hospital-specific — but attaches no biomedical/clinical framing anywhere else; an asset is any individually-tracked unit of any Product, and its maintenance owner/vendor is the module's own generic `Supplier` master, reused rather than duplicated. The source SRS's asset-status shape could not be directly verified against the actual "IHMS" reference codebase in the autonomous session that built this slice (no repository access from that session) — the shipped `AssetStatus` enum (`AVAILABLE`/`IN_USE`/`UNDER_MAINTENANCE`/`RETIRED`/`DISPOSED`) is the plan's own 4-state sketch plus one small, defensible addition (`AVAILABLE`, a "registered but not yet deployed" starting state), not a verified match to any external system. Depreciation is standard straight-line only, per explicit scope instruction — no double-declining-balance or units-of-production method exists.

## 2. Overall Description

### 2.1 Product Perspective
Standalone module component; reuses the Core Data Model sub-module's `Product` and `InventoryLocation`, the GRN & Stock Control sub-module's `GoodsReceiptLine` (an optional traceability link for an asset onboarded through the normal procure→receive flow), the Procurement & Vendor Management sub-module's `Supplier` (for service contracts), and the Core Data Model's `StockMovementService` (disposal's optional stock write-off). A `Product` is not required to be "asset-only" or "stock-only" — the same product can be both bulk stock-tracked (`StockBalance`) and separately have individual `Asset` rows registered for units that need per-unit tracking.

### 2.2 User Classes
Asset register administrators (register/edit assets, change status), maintenance coordinators (schedule/mark-performed preventive maintenance, manage service contracts), and an authority empowered to dispose an asset (its own dedicated permission, distinct from ordinary edit access). All role-to-permission mapping is DB-driven, not hard-coded.

### 2.3 Operating Environment
Same stack as the rest of the module (Angular + Spring Boot + PostgreSQL/Flyway + Keycloak). Backend package `com.cms.inventory.asset`; frontend `frontend/src/app/features/inventory/asset/`.

### 2.4 Constraints & Assumptions
- **No GL/accounting posting.** Depreciation is computed live on every read and never posted anywhere — consistent with the module-wide deferred ledger-connector decision (Core Data Model / Procurement sub-modules).
- **Status changes are open-ended, no state-machine validation.** Unlike every DRAFT/SUBMITTED-style workflow elsewhere in this module, real asset status cycles (`IN_USE` ↔ `UNDER_MAINTENANCE` many times before an eventual terminal state) — `updateStatus` only validates the target is a real enum value, with one exception: `DISPOSED` is reachable only through the dedicated Disposal action, never the plain status dropdown.
- **Standard straight-line depreciation only** — no double-declining-balance, no units-of-production, per explicit scope instruction not to build either without a real need.
- **Disposal's stock write-off is unbatched-only and silently a no-op when there is nothing on hand** — a purely asset-tracked product with no separate bulk `StockBalance` is a completely normal case, not an error.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-AM-1 | Register an **Asset**: unique `assetTag` (real-time async uniqueness check), Product, Location, optional serial number, optional link to the `GoodsReceiptLine` it was received through, optional `purchaseValue`/`purchaseDate`/`usefulLifeMonths`/`salvageValue` (captured as master data even before depreciation consumes them), status (defaults `AVAILABLE`), notes. | Must | Core Data Model FR-CDM-4, FR-CDM-9 |
| FR-AM-2 | Edit an Asset's full field set (`PUT`), and separately change only its status (`PATCH .../status`) to any other real `AssetStatus` value except `DISPOSED`, which is reachable only via the dedicated Dispose action. | Must | FR-AM-1 |
| FR-AM-3 | Maintain an **Asset Maintenance Schedule** per Asset: `ONE_OFF` or `RECURRING` (plain day-count interval, no frequency enum), `nextDueDate`, active flag. **Mark Performed** records `lastPerformedDate` and, for a `RECURRING` schedule, advances `nextDueDate` forward from the performed date (never the old due date — a late visit doesn't compress the next interval); a `ONE_OFF` schedule deactivates instead. "Overdue" is computed at read time from `nextDueDate`, never stored. | Must | FR-AM-1 |
| FR-AM-4 | Maintain an **Asset Service Contract** per Asset: linked Supplier, optional contract number, coverage window (`startDate` required, `endDate` optional), optional renewal reminder date, coverage details text, active flag. "Expired" is computed at read time from `endDate`, never stored. | Should | FR-AM-1, Procurement FR-PV-1 |
| FR-AM-5 | Compute **straight-line depreciation** live on every Asset read: `monthlyDepreciation = (purchaseValue − salvageValue) / usefulLifeMonths`; `accumulatedDepreciation = monthlyDepreciation × wholeCalendarMonthsElapsed`, capped at the depreciable base; `currentBookValue = purchaseValue − accumulatedDepreciation`, floored at salvage value. Not applicable (nulls, not zeros) whenever purchase value, purchase date, or useful life is missing; a missing salvage value defaults to zero without blocking. | Must | FR-AM-1 |
| FR-AM-6 | Support **Disposal** of a non-disposed Asset via a dedicated action (reason required, optional sale/disposal value, disposal date, disposer identity/timestamp recorded): sets status `DISPOSED` and, if a corresponding bulk `StockBalance` exists for the asset's product at its location, writes off exactly one unit via a `DISPOSAL` stock movement through `StockMovementService` (unbatched only; silently a no-op if nothing is on hand). Reachable only through this dedicated dialog/endpoint, never the generic status dropdown. | Must | FR-AM-1, FR-AM-5, Core Data Model FR-CDM-11 |

## 4. External Interface Requirements

### 4.1 Key Screens (frontend/src/app/features/inventory/asset/...)
Asset Register (list/form with a "Book Value" column and a dedicated Dispose dialog), Maintenance Schedules (list/form with a Mark Performed action), Service Contracts (list/form) — all under the "Equipment & Asset Management" nav group. (Depreciation Summary is a Reporting sub-module screen that reuses this sub-module's own formula — see §1.2.)

### 4.2 Key API Endpoints (all under `/inventory/asset/...`)
- `assets` (POST, `/page`, GET`{id}`, PUT`{id}`, `PATCH {id}/status`, `/asset-tag-exists`, `/{id}/dispose`)
- `maintenance-schedules` (POST, `/page`, GET`{id}`, PUT`{id}`, `/{id}/mark-performed`)
- `service-contracts` (POST, `/page`, GET`{id}`, PUT`{id}`)

### 4.3 Key DB Entities
`assets`, `asset_maintenance_schedules`, `asset_service_contracts`.

## 5. Non-Functional Requirements
- **Security/RBAC:** `INVENTORY_ASSET_VIEW`/`_MANAGE`/`_DISPOSE`; `INVENTORY_ASSET_MAINTENANCE_VIEW`/`_MANAGE`; `INVENTORY_ASSET_SERVICE_CONTRACT_VIEW`/`_MANAGE` — Dispose is its own dedicated permission per the operation-wise permission mapping rule (a terminal, real-consequence action, same reasoning as Purchase Order's force-close and Goods Receipt's confirm); Service Contracts were split into their own dedicated permission pair (V551, 2026-09-22) after an audit found them incorrectly sharing Maintenance Schedules' permission pair, a direct violation of that same rule (see FRD §2).
- **Auditability:** every write reuses the shared `audit_log`; a Disposal's stock write-off (when applicable) posts through `StockMovementService` onto the permanent, append-only Stock Ledger, never bypassing it.
- **Data integrity:** asset tag uniqueness is enforced at both the async check and (implicitly) the service layer; the Internal Return-style "never negative" guard on `StockMovementService` protects Disposal's write-off the same way it protects every other movement.
- **Performance:** depreciation is computed on read via a pure, allocation-light static calculator (`AssetDepreciationCalculator`), never a stored/materialized column, and shared verbatim with the Reporting sub-module's aggregate summary to avoid a second, potentially-drifted copy of the same financial formula.

## 6. Known Gaps / Not Yet Implemented
- **No GL/accounting posting for depreciation** — computed and displayed only, consistent with the module-wide deferred ledger-connector decision.
- **`AssetStatus`'s shape was never verified against the source "IHMS" reference system** — built from the autonomous session's own plan sketch plus one defensible addition (`AVAILABLE`), since that session had no access to the external reference codebase; flagged for a future review with real access to confirm or revise.
- **Standard straight-line depreciation only** — no double-declining-balance or units-of-production method, by explicit scope decision, not an oversight.
- **Asset status transitions are unvalidated as a sequence** (any status → any other status via the plain PATCH, except into `DISPOSED`) — there is no state-machine enforcement of, for example, `RETIRED` being a dead end in practice, only a convention.
