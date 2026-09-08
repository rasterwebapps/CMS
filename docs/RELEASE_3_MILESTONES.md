# 🚀 Release 3 — Milestone Tracker

> **Inventory Management** — a new, standalone, industry-agnostic module (procurement, vendor management, stock ledger, GRN, requisition & issue, asset lifecycle, approvals, gate pass, reporting) reusing the existing Campus Infrastructure hierarchy (BR-60) for locations. Full requirements, gap analysis, and decision history live in [`docs/inventory-management/`](inventory-management/README.md) — read that before touching this tracker or any implementation.
>
> **Status as of 2026-09-07:** Core ER diagram and module boundaries drafted ([`ER_DIAGRAM_AND_MODULE_BOUNDARIES.md`](inventory-management/ER_DIAGRAM_AND_MODULE_BOUNDARIES.md)); implementation started against R3-M1 (Category &amp; Uom masters first) per user direction — see `docs/inventory-management/DECISION_LOG.md`'s 2026-09-07 "Phase 1 kickoff" entry for delivery order and scope decisions. `InventoryItem` migration (R3-M8) explicitly deferred until the rest of the core is in place.
>
> **Prerequisite:** none formally, but this module is designed to reuse [Release 1](RELEASE_1_MILESTONES.md)'s Campus Infrastructure (Infra) module and its DB-driven Role Management — both should already be in place.
>
> **This is the technical/engineering tracker.** For the plain-language, stakeholder-facing version (phases and what's covered, no timelines) shared outside the engineering team, see [`inventory-management/MILESTONES.md`](inventory-management/MILESTONES.md) instead — keep both updated together when a phase's status changes, they describe the same work at two altitudes and must not drift apart.

---

## 📋 Table of Contents

- [Release 3 Scope](#-release-3-scope)
- [Release 3 Definition of Done](#-release-3-definition-of-done)
- [Release 3 Progress Tracking](#-release-3-progress-tracking)

---

## 🎯 Release 3 Scope

Derived from `docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §5's core module list. These will be broken into concrete milestones (R3-M1.1, R3-M1.2, ...) once the ER diagram/module-boundary design is agreed — listed here as placeholders so the scope is visible before that happens.

| Milestone | Module | Key Outcome |
|-----------|--------|-------------|
| **R3-M1** | Core data model | Stock Ledger, Stock Batch, UOM, Category Attribute Schema, Inventory Location (referencing existing Infra `Room`/`Zone`), Tax Rule — the foundational entities from §6 |
| **R3-M2** | Procurement & Vendor Management | Purchase requisition → PO lifecycle, vendor registration/portal, rate contracts |
| **R3-M3** | GRN & Stock Control | Goods receipt, 3-way match, batch/expiry tracking, FIFO/FEFO issue valuation |
| **R3-M4** | Requisition & Issue | Generic requisition workflow against an Inventory Location, auto-reorder |
| **R3-M5** | Asset Management | Lifecycle, depreciation, maintenance contracts, disposal |
| **R3-M6** | Approvals & Gate Pass | Multi-tier/parallel approval routing, outward/inward gate pass |
| **R3-M7** | Reporting | Price comparison, cycle-time, stock/asset dashboards |
| **R3-M8** | `InventoryItem` migration | Migrate the existing lab-consumables module onto the new core (Library migration explicitly deferred — see `DECISION_LOG.md`) |
| **R3-M9** | Standalone deployability | Package Infra + Inventory as an independently buildable/deployable frontend+backend unit (open technical questions on code/schema boundary — see `DECISION_LOG.md`) |

---

## ✅ Release 3 Definition of Done

| Criterion | Requirement |
|-----------|-------------|
| **Specialist review** | @Partner round complete for the milestone's scope before implementation starts |
| **Migration safety** | Any migration touching `InventoryItem` tested on staging with a rollback path before production cutover (per `CLAUDE.md`) |
| **DB-driven roles/permissions** | No hard-coded role enum; every new action gets its own dedicated permission (per `CLAUDE.md`) |
| **Manual test cases** | Authored per completed feature in `docs/manual-test-cases/`, incrementally |
| **Documentation** | `docs/inventory-management/` updated in the same change as any scope/architecture decision; this tracker's status row updated |
| **Component Touch Rule** | Light/dark mode, all roles, all existing features re-verified for any touched screen (per `CLAUDE.md`) |

---

## 📊 Release 3 Progress Tracking

| Milestone | Status | Progress |
|-----------|--------|----------|
| R3-M1: Core data model | 🟡 In Progress — Category/Uom/Product/CategoryAttribute masters + InventoryLocation/StockBatch/StockLedger/StockBalance + CycleCount (physical stock counts) shipped; only the deferred `InventoryItem` migration (R3-M8) is left in Phase 1 | 55% |
| R3-M2: Procurement & Vendor Management | ✅ Done — TaxRule/Supplier/RateContract/VendorProductMapping/Purchase Requisition/Wanted List/Purchase Order all shipped (see `docs/inventory-management/DECISION_LOG.md`'s "Purchase Order slice" entry) | 100% |
| R3-M3: GRN & Stock Control | 🟡 In Progress — Goods Receipt and Stock Transfer shipped; Supplier Returns still to come | 50% |
| R3-M4: Requisition & Issue | ⬜ Not Started | 0% |
| R3-M5: Asset Management | ⬜ Not Started | 0% |
| R3-M6: Approvals & Gate Pass | ⬜ Not Started | 0% |
| R3-M7: Reporting | ⬜ Not Started | 0% |
| R3-M8: InventoryItem migration | ⬜ Not Started | 0% |
| R3-M9: Standalone deployability | ⬜ Not Started — open technical questions unresolved | 0% |

---

> **Note:** This tracker will be restructured once the core ER diagram and module boundaries (`docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §8) are drafted and agreed — treat the milestones above as scope markers, not a committed plan.
