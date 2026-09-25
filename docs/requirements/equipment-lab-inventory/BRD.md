# Business Requirements Document — Equipment & Lab Inventory

**Application:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective

SKSCON's labs (nursing skill labs, computer labs, etc.) contain physical equipment — computers, mannequins, instruments, furniture — that needs to be tracked per lab: what exists, its condition/status, and where it is. The Equipment module gives lab administrators a simple register of this equipment, tied to the `Lab` master, so equipment can be located, its lifecycle status tracked, and (via the separate Maintenance & Repair module) repair requests raised against it.

The original milestone (R1-M4.2) also envisioned a parallel lab-consumables stock-tracking feature. That was attempted as an `InventoryItem` entity but was **never completed or used** (0 production rows) and was formally retired in favor of the general-purpose Release 3 Inventory Management module. This BRD therefore covers **Equipment only**.

## 2. Stakeholders

- **Lab Administrators / College Admin** — register and maintain equipment records, mark status changes (available, in use, under maintenance, out of order, disposed).
- **Faculty using labs** — indirectly benefit from knowing equipment availability (no dedicated faculty-facing screen was found; the equipment list appears to be an admin-facing screen).
- **Maintenance/Repair staff** — consume `Equipment` records as the target of a `MaintenanceRequest` (see `docs/requirements/maintenance-repair/`).
- **Campus Spatial module** — surfaces Equipment status as a colored dot on floor-plan markers (`BR-60`), read-only.

## 3. Business Rules

No BR-N entry in `docs/BUSINESS_REQUIREMENTS.md` covers Equipment tracking specifically — the table of contents was checked and contains no matching heading. The following rules are inferred directly from the shipped code and the milestone tracker; they are numbered `BR-EQIP-*` for this document's internal use only (not an official BR-N).

| ID | Rule | Rationale (inferred) |
|---|---|---|
| BR-EQIP-1 | Every Equipment record must belong to exactly one Lab (`lab_id NOT NULL`). | Equipment is physically housed in a specific lab; tracking without a lab location would be meaningless for lab administration. |
| BR-EQIP-2 | Asset code, if provided, must be unique across all equipment (case-insensitive). | Prevents two physical assets being registered under the same tag/barcode. |
| BR-EQIP-3 | Equipment status is one of `AVAILABLE`, `IN_USE`, `UNDER_MAINTENANCE`, `OUT_OF_ORDER`, `DISPOSED`. | Models the equipment lifecycle from purchase to write-off; `UNDER_MAINTENANCE` is the join point to the Maintenance & Repair workflow. |
| BR-EQIP-4 | Equipment category is one of `COMPUTER`, `PERIPHERAL`, `NETWORKING`, `ELECTRONIC`, `MECHANICAL`, `FURNITURE`, `CONSUMABLE`, `SOFTWARE`. | Classifies asset type for filtering/reporting; note `CONSUMABLE` exists as a category value even though the separate consumables-stock feature (quantity/stock-level tracking) was never built — a `CONSUMABLE`-categorized Equipment row is still tracked as a single countable asset, not a quantity. |
| BR-EQIP-5 | Creating/updating/deleting equipment requires the `EQUIPMENT_MANAGE` permission; exporting requires `EQUIPMENT_EXPORT`. Viewing/listing has no permission gate. | Standard write-protection pattern; read access left open, likely because equipment visibility (e.g. lab availability) is broadly useful across roles. |

## 4. Business Process / Workflow

1. **Registration.** An authorized user (holding `EQUIPMENT_MANAGE`) creates an Equipment record against a Lab, setting name, category, and initial status (default entry point is the create form; status defaults to whatever the form/user selects — no server-side default was found beyond the `@NotNull` validation requiring a value).
2. **Lifecycle status changes.** The same user edits the record over time to move it through `AVAILABLE → IN_USE → UNDER_MAINTENANCE → AVAILABLE` (repair cycle) or eventually to `OUT_OF_ORDER`/`DISPOSED`. There is no workflow engine or status-transition validation in `EquipmentService.update()` — any status can be set to any other status directly; the "workflow" is a free-form field, not an enforced state machine.
3. **Maintenance linkage.** When equipment needs repair, a separate `MaintenanceRequest` record is created against it in the Maintenance & Repair module (out of scope here); Equipment's own `status` field is not automatically synchronized with a `MaintenanceRequest`'s status by any code found in `EquipmentService`.
4. **Retirement/disposal.** Setting status to `DISPOSED` is the modeled end-of-life state; there is no separate disposal approval workflow, cost write-off, or asset-register reconciliation beyond this status flag.
5. **Reporting/export.** Any user holding `EQUIPMENT_EXPORT` can export the current filtered/sorted list to Excel or PDF.

No calculation formulas exist in this module (no depreciation, no stock valuation — those concepts live only in the newer Release 3 Inventory/Asset module).

## 5. Success Criteria

Not formally defined — no KPI or acceptance metric exists in the milestone tracker or BR docs. Success is inferred from feature completeness: CRUD + list/filter/export + lab association shipped and reachable in the app.

## 6. Assumptions & Constraints

- Equipment tracking is scoped to lab equipment only — it is not a general fixed-asset register for the whole institution (that role is filled by the Release 3 Inventory Management module's Asset sub-feature, `frontend/src/app/features/inventory/asset/`).
- No stock-quantity/consumable-level tracking exists for labs; the originally planned `Consumable`/`StockTransaction` entities were never shipped as such, and the closest attempt (`InventoryItem`) was retired unused.
- The module predates the now-mandatory `uniqueFieldValidator` + `/name-exists` async-uniqueness pattern; uniqueness is enforced only on server-side submit.

## 7. Known Gaps / Deferred

- No enforced status state-machine (any-to-any transitions allowed).
- No audit trail of who changed equipment status/details, beyond generic `updated_at`.
- No link from an Equipment record back to its `MaintenanceRequest` history within the Equipment screens themselves (would require the Maintenance module's own screens).
- Frontend field coverage gaps and a status-string mismatch in the create/edit form's preview card — see SRS §6 for details.
