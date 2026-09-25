# Business Requirements Document — Asset Management

## 1. Executive Summary / Business Objective
`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` found the source SRS's Asset Management section (§3.6) framed entirely around Biomedical Engineering and medical equipment — a hospital-only lens that would have locked any general-purpose deployment out of tracking its own individually-valuable items (computers, furniture, generators, lab instruments) the same way. The business objective of this sub-module is a genuinely generic asset register: one physical unit tracked from registration through preventive maintenance, supplier service coverage, computed depreciation, and eventual disposal, usable by any organization for any class of individually-tracked item without any clinical or academic framing baked into the schema.

## 2. Stakeholders
- Asset register administrators (registration, edits, status changes)
- Maintenance coordinators (preventive maintenance schedules, service contracts)
- Finance (relies on the computed book value/accumulated depreciation for reporting)
- Disposal authority (a distinct permission from ordinary asset management)
- Platform/DB-driven Role Management administrators (permission assignment)

## 3. Business Rules
| ID | Rule | Rationale |
|---|---|---|
| BR-AM-1 | An Asset tracks one physical, individually-identified unit (its asset tag is its unique identity), distinct from and coexisting with a Product's own aggregate `StockBalance` quantity tracking. | A Product isn't required to be "asset-only" or "stock-only" — the same catalog item can be both bulk stock-tracked and separately have individually-tracked units registered (Decision Log, 2026-09-08 "Asset register slice"). |
| BR-AM-2 | An Asset's link back to the Goods Receipt it arrived through is optional. | Supports both a "received through the normal procure→receive flow" onboarding path and a standalone "already-owned, being onboarded" entry with no receipt behind it (Decision Log, 2026-09-08 "Asset register slice"). |
| BR-AM-3 | Asset status changes are open-ended, validated only as "a real enum value," with `DISPOSED` reachable exclusively through the dedicated Disposal action. | Real asset status legitimately cycles (`IN_USE` ↔ `UNDER_MAINTENANCE` repeatedly) before an eventual terminal state, unlike every DRAFT/SUBMITTED workflow elsewhere in this module — but disposal has a real, non-reversible side effect a bare status flip shouldn't trigger silently (Decision Log, 2026-09-08 "Asset register slice", "Disposal slice"). |
| BR-AM-4 | A recurring maintenance schedule's next due date advances from the date the visit was actually performed, not from the old scheduled due date. | Standard preventive-maintenance practice — a visit that happens late doesn't compress the next interval (Decision Log, 2026-09-08 "Maintenance & Service Contracts slice"). |
| BR-AM-5 | An Asset Service Contract links the module's own existing Supplier master, never a free-text vendor name or a new vendor entity. | A maintenance vendor is the same kind of thing a purchasing vendor is — the same reuse-over-duplicate discipline already applied to the Campus Infrastructure hierarchy and `audit_log` (Decision Log, 2026-09-08 "Maintenance & Service Contracts slice"). |
| BR-AM-6 | Depreciation is standard straight-line only, computed live on every read and never stored or posted to any ledger. | Explicit scope instruction not to attempt double-declining-balance/units-of-production without a real need, and the module-wide ledger-connector work is separately deferred (Decision Log, 2026-09-08 "Depreciation slice"). |
| BR-AM-7 | An Asset missing any of purchase value, purchase date, or useful life shows depreciation as not applicable (nulls), never a misleading zero; a missing salvage value defaults to zero without blocking. | A `0` would read as "worthless" for an asset that simply has incomplete master data, which is materially different from a genuinely fully-depreciated asset (Decision Log, 2026-09-08 "Depreciation slice"). |
| BR-AM-8 | Disposing an asset writes off exactly one unit of its product's bulk on-hand stock at its location, if any exists — silently a no-op if there is nothing to write off. | A product can be simultaneously asset-tracked (this register) and separately bulk stock-tracked if it was ever received through a GRN; disposing one physical unit should decrement that bulk count by exactly one where applicable, but a purely asset-tracked product with no bulk balance is a completely normal case, not an error (Decision Log, 2026-09-08 "Disposal slice"). |
| BR-AM-9 | Service Contracts get their own dedicated permission pair, separate from Maintenance Schedules', even though both were originally bundled under one shared pair. | The original bundling directly violated the operation-wise permission mapping rule; the rule's own stated answer to "should this share a permission?" is always no (Decision Log, 2026-09-22 "Overnight Phase 3..."). |

## 4. Business Process / Workflow Narrative
An asset administrator registers a physical unit against its Product and Location, giving it a unique asset tag and, where known, its serial number, purchase value, purchase date, useful life, and salvage value — either linked back to the specific Goods Receipt it arrived through, or entered standalone for an already-owned item being onboarded into the register for the first time. From there its status is updated freely as it moves through real-world states (deployed into use, pulled for maintenance, returned to use, eventually retired) with no fixed sequence enforced.

Separately, a maintenance coordinator can attach one or more preventive-maintenance schedules to an asset — either a single planned visit or a recurring cadence — and mark each visit performed as it happens, which pushes the next due date forward from that real performed date. The same coordinator can record a supplier service contract covering the asset, drawing the vendor from the module's existing Supplier master, with a coverage window and an optional renewal reminder.

At any point, anyone viewing the asset sees its current book value and accumulated depreciation, computed live from its purchase master data using standard straight-line depreciation — no separate calculation step or posting action required. When an asset reaches end of life, a disposal authority (a distinct permission from ordinary asset management) disposes it through a dedicated action requiring a reason, which locks in its final `DISPOSED` status and, where the asset's product also carries bulk on-hand stock at that location, quietly writes off one unit of it.

## 5. Success Criteria
Not formally defined with a numeric KPI. Inferred from feature completeness: `RELEASE_3_MILESTONES.md` marks R3-M5 ("Asset Management") 100% Done — Asset register, Maintenance & Service Contracts, Depreciation, and Disposal all shipped, closing Phase 5 in full per the Decision Log; a 2026-09-22 audit re-verified all four screens structurally clean and found/fixed one real permission-conflation defect (Service Contracts sharing Maintenance's permission pair).

## 6. Assumptions & Constraints
- Assumes standard straight-line depreciation is an acceptable method for all deployments; no alternative method is offered.
- Assumes the shipped `AssetStatus` 5-state shape is an acceptable ERP-standard default; it was not verified against the source "IHMS" reference system due to lack of access in the session that built it.
- Assumes no GL/accounting posting requirement exists yet for depreciation figures.
- Assumes a purely asset-tracked product (no corresponding bulk `StockBalance`) is a normal, expected configuration, not a data-quality problem.

## 7. Known Gaps / Deferred
- No GL/accounting posting for depreciation.
- `AssetStatus`'s shape unverified against the external IHMS reference system.
- No double-declining-balance or units-of-production depreciation methods.
- No enforced status-transition state machine beyond "must be a real enum value" and "`DISPOSED` only via the dedicated action."
- See SRS.md §6 for the complete, precise list.
