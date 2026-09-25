# Functional Requirements Document — Asset Management

## 1. Overview
This sub-module implements the individually-tracked-unit side of Inventory Management, sitting alongside (not replacing) the Core Data Model's aggregate `StockBalance` quantity tracking: Asset registration, preventive Maintenance Schedules, supplier Service Contracts, live-computed straight-line Depreciation, and dedicated Disposal.

## 2. Actors & Permissions
All permissions are DB-managed rows (no hard-coded roles); the table below lists the exact codes found in the backend (`com.cms.inventory.asset`).

| Permission Code | Gates |
|---|---|
| `INVENTORY_ASSET_VIEW` / `_MANAGE` | Asset CRUD (create, edit, status change), asset-tag-exists lookup |
| `INVENTORY_ASSET_DISPOSE` | Dedicated Disposal action, separate from `_MANAGE` — a distinct, audit-worthy, terminal action |
| `INVENTORY_ASSET_MAINTENANCE_VIEW` / `_MANAGE` | Asset Maintenance Schedule CRUD + Mark Performed |
| `INVENTORY_ASSET_SERVICE_CONTRACT_VIEW` / `_MANAGE` | Asset Service Contract CRUD — split into its own dedicated pair (V551, 2026-09-22) from a prior, incorrect sharing of the Maintenance Schedule permission pair; the split migration backfilled the new permissions onto every role that held the old maintenance ones, so no role lost Service Contract access as a side effect |

Every permission-seeding/splitting migration ends with the standing DEV_ADMIN/SUPPORT_ADMIN catch-all sync block; actual role assignment beyond that is done entirely in the DB-driven Role Management module.

## 3. Screens & UI Behavior
- **Asset Register list/form** — real-time async asset-tag uniqueness check (`uniqueFieldValidator` + `/asset-tag-exists`), following the mandatory master-screen uniqueness pattern (this is a master-like list+form screen, not a header/line document workflow). List includes a computed "Book Value" column. Status is changed via a plain dropdown for every value except `DISPOSED`, which is never offered there — only reachable through the separate **Dispose** action, its own confirmation dialog requiring a disposal reason (optional sale value/date also captured).
- **Maintenance Schedules list/form** — schedule type select (`ONE_OFF`/`RECURRING`, the latter revealing a plain day-count interval field, not a frequency enum), next due date, active flag. List/detail show computed "Overdue"/"Due Soon" indicators derived from `nextDueDate`. A **Mark Performed** action records `lastPerformedDate` and recomputes `nextDueDate` (recurring) or deactivates (one-off).
- **Service Contracts list/form** — Supplier picker (existing procurement Supplier master, not a new/free-text vendor field), coverage window (start required, end optional), renewal reminder date, coverage details text, active flag. List/detail show a computed "Expired" indicator derived from `endDate`.
- **Depreciation display** — Asset Register's list ("Book Value" column) and detail view surface `accumulatedDepreciation`/`currentBookValue` wherever an asset's master data supports the calculation; an asset missing purchase value/date/useful life shows "—" (not a numeric zero) for both figures.
- **Badges/chips** — this nav group uses no `cms-status-badge` at all; status/overdue/expired all render via locally-defined chip classes in each component's own `.scss`, using only `--cms-*`-prefixed variables (confirmed collision-free in the 2026-09-22 audit).

## 4. Functional Workflows

### 4.1 Asset registration and status lifecycle
`create` — validates asset tag uniqueness, sets initial status `AVAILABLE` unless a different valid status is supplied, captures the optional financial master-data fields (`purchaseValue`/`purchaseDate`/`usefulLifeMonths`/`salvageValue`) even though nothing consumes them until read time. `update` — full-field edit (`PUT`). `updateStatus` — validates only that the target is a real `AssetStatus` enum value; open-ended, no sequence enforcement, except `DISPOSED` is never accepted through this endpoint (only through `dispose`).

### 4.2 Maintenance Schedule: create → mark performed
`create`/`update` — `scheduleType` (`ONE_OFF`/`RECURRING`), `recurrenceIntervalDays` (required only for `RECURRING`), `nextDueDate`, active flag. `markPerformed` — records `lastPerformedDate` as the performed date (defaults to today if not supplied); for `RECURRING`, sets `nextDueDate = performedDate + recurrenceIntervalDays`; for `ONE_OFF`, sets `isActive = false` instead (nothing left to recur to). "Overdue"/"due soon" are read-time derivations from `nextDueDate`, never stored.

### 4.3 Service Contract lifecycle
`create`/`update` — links an existing `Supplier`, `startDate` (required), `endDate`/`renewalReminderDate` (optional), `coverageDetails` free text, active flag. "Expired" is a read-time derivation from `endDate` vs. today, never stored — the same pattern Maintenance Schedule's overdue flag and Loanable Item Issue's overdue flag both already establish elsewhere in this module.

### 4.4 Depreciation (computed, never stored)
`AssetDepreciationCalculator.compute(purchaseValue, purchaseDate, usefulLifeMonths, salvageValue)` — a pure, static function, shared verbatim with the Reporting sub-module's Asset Depreciation Summary report so the two never drift apart:
1. `applicable = purchaseValue != null && purchaseDate != null && usefulLifeMonths != null && usefulLifeMonths > 0`; if not applicable, return nulls for both figures.
2. `salvage = salvageValue ?? 0`; `depreciableBase = purchaseValue − salvage`.
3. `monthsElapsed = clamp(monthsBetween(purchaseDate, today), 0, usefulLifeMonths)` (whole calendar months via `java.time.Period`, never a naive day-count divide).
4. `monthlyDepreciation = depreciableBase / usefulLifeMonths` (4-decimal precision, `HALF_UP`).
5. `accumulatedDepreciation = min(monthlyDepreciation × monthsElapsed, depreciableBase)`, 2-decimal `HALF_UP`.
6. `currentBookValue = max(purchaseValue − accumulatedDepreciation, salvage)`, 2-decimal `HALF_UP`.
Computed inline in `AssetService.toResponse` on every read — no scheduled job, no stored column, no GL posting.

### 4.5 Disposal
`dispose(id, request)` on a non-`DISPOSED` asset — sets `status = DISPOSED`, records `disposalReason` (required), optional `disposalValue`/`disposalDate`, `disposedBy`/`disposedAt`; then looks up the asset's product's unbatched `StockBalance` at the asset's own location and, if a positive balance exists, posts a `DISPOSAL` movement (quantity 1) through `StockMovementService.recordMovement` — silently a no-op (not an error) if there is nothing on hand for that product/location. Reachable only via the dedicated `/dispose` endpoint and its own confirmation dialog, never the generic status-update endpoint/dropdown.

## 5. API Endpoints
See SRS.md §4.2 for the full list of paths. All are permission-gated per §2 above; list endpoints (`/page`) accept standard search/sort/pagination query parameters consistent with the rest of the app's Material-table screens.

## 6. Data Model
See SRS.md §4.3 for the table list. Notable real-world column shapes confirmed directly from entity source:
- `Asset`: `product`/`location` FKs (both non-nullable), `assetTag` (unique, required), `serialNumber` (nullable), `status` enum (`AVAILABLE`/`IN_USE`/`UNDER_MAINTENANCE`/`RETIRED`/`DISPOSED`, default `AVAILABLE`), `goodsReceiptLine` FK (nullable), `purchaseValue`/`salvageValue` (`BigDecimal`, nullable), `purchaseDate` (nullable), `usefulLifeMonths` (`Integer`, nullable), `disposalReason`/`disposalValue`/`disposalDate`/`disposedBy`/`disposedAt` (all nullable, populated only by `dispose`), `notes`, `createdAt`/`updatedAt` (JPA-audited).
- `AssetMaintenanceSchedule`: `asset` FK, `scheduleType` enum (`ONE_OFF`/`RECURRING`), `recurrenceIntervalDays` (`Integer`, nullable — meaningful only for `RECURRING`), `nextDueDate` (required), `lastPerformedDate` (nullable), `isActive` (default true), `notes`, `createdAt`/`updatedAt`.
- `AssetServiceContract`: `asset`/`supplier` FKs (both non-nullable), `contractNumber` (nullable), `startDate` (required), `endDate`/`renewalReminderDate` (nullable), `coverageDetails` (nullable, up to 1000 chars), `isActive` (default true), `createdAt`/`updatedAt`.

## 7. Edge Cases & Validation Rules
- An asset tag must be unique; the async check runs on every keystroke against `/asset-tag-exists` per the mandatory uniqueness pattern.
- `updateStatus` accepts any real `AssetStatus` value except `DISPOSED`, which the endpoint itself never sets — only `dispose` can.
- Depreciation shows as not-applicable (nulls, not zero) whenever purchase value, purchase date, or useful life is missing; a missing salvage value silently defaults to zero without blocking the calculation.
- `monthsElapsed` is clamped to `[0, usefulLifeMonths]` — an asset purchased in the future never shows negative depreciation, and one past its useful life never shows more than 100% depreciated.
- `currentBookValue` is floored at `salvageValue` (or zero if unset) and never allowed to go negative.
- Disposal's stock write-off looks only at the unbatched `StockBalance` for the asset's exact product/location pair; it is silently skipped (no error) when none exists, and only ever writes off exactly one unit, never the asset's full "value."
- A `RECURRING` maintenance schedule with no `recurrenceIntervalDays` set has nothing to advance by — the schedule's own save validation requires the interval whenever `scheduleType = RECURRING`.

## 8. Known Gaps / Deferred
See SRS.md §6 — no GL/accounting posting for depreciation, `AssetStatus`'s shape unverified against the external IHMS reference system, no depreciation method beyond straight-line, and no enforced status-transition state machine beyond the `DISPOSED`-only-via-dedicated-action rule.
