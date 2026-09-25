# Software Requirements Specification — Inventory Core Data Model

**Module:** Inventory Management (OneCMS) — Sub-module 1 of 7
**Corresponds to:** Release 3 Milestone R3-M1 ("Core data model")
**Status as documented:** Partially built (~60%) — see §6 Known Gaps

> Inventory Management is a standalone, industry-agnostic stock/procurement/asset module of **OneCMS (College Management System)**, built by **Raster / Raster Images Pvt. Ltd.**, initially deployed for **SKSCON (SKS College Of Nursing)**. This sub-module and its contents carry **no** hospital- or college-specific naming, fields, or workflow — it is designed to be reused by any organization that tracks physical stock. Any OneCMS/SKSCON references in this document describe only the parent product/client, never the module's own data model.

## 1. Introduction

### 1.1 Purpose
Defines the requirements for the foundational data layer that every other Inventory Management sub-module builds on: the item catalog (Category, UOM, Product and its extensions), the physical stock-location model, and the append-only stock ledger with its derived on-hand balance.

### 1.2 Scope
In scope: Category/CategoryAttribute, UOM + UOM Conversion Templates, Product (with Brand, ProductAlias, ProductAttributeValue, ProductImage, ProductVariant, barcode, tracking mode, pricing, dimensions), Inventory Location (Room-based) with Rack/Bin sub-locations, Stock Batch, Stock Ledger, Stock Balance (+ Stock Bin Allocation), Cycle Count, and per-(product, location) Reorder Configuration data. Out of scope for this document: procurement documents, GRN, requisition/issue documents, assets, approvals, and reporting — each has its own sub-module SRS.

### 1.3 References
- `docs/RELEASE_3_MILESTONES.md`, `docs/inventory-management/MILESTONES.md` (Phase 1)
- `docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` (GAP-07 stock ledger, GAP-09 cycle count, GAP-10 location model, GAP-12 UOM, GAP-20 category attributes)
- `docs/inventory-management/ER_DIAGRAM_AND_MODULE_BOUNDARIES.md` §2, §4
- `docs/inventory-management/DECISION_LOG.md` (2026-09-07 Phase 1 entries; 2026-09-11 "Product/Inventory extension program"; 2026-09-15/16 variant, rack/bin work)
- Source SRS: `docs/inventory-management/source/SRS_v3.2_Updated.pdf` ("IHMS", v1.0, July 2026)

**Reconciliation against the source SRS (SRS_v3.2_Updated.pdf):** the source document's §6.1 data model has **no Stock Ledger/Stock Batch/Inventory Balance entity at all**, despite "Stock Ledger Updated" being the terminal step of its own §3.3 process diagram (gap analysis GAP-07). The as-built system closes this gap by making an append-only `StockLedger` the single source of truth, with `StockBalance` a materialized, kept-in-sync derived view — this is a genuine extension beyond the source SRS, not a reconciliation of something it specified. The source SRS's flat Location Master (`LocationID, LocationName, LocationType(Store/Ward/Facility), ParentLocationID`, GAP-10) is not built at all; the as-built system instead wraps OneCMS's own pre-existing `Organization → Branch → Block → Floor → Zone → Room` Campus Infrastructure hierarchy via a thin `InventoryLocation` entity — a deliberate divergence, not an oversight. The source SRS's Product Master (§3.10) has one fixed field set; the as-built `Category`/`CategoryAttribute` EAV schema (GAP-20) replaces it with a configurable-per-category model. The source SRS's cost/valuation logic (FIFO/FEFO, §3.3) is **not** implemented in this sub-module — decrease-side valuation uses weighted-average cost as a deliberate simplification (see §6). The source SRS has no physical stock-take workflow beyond an unelaborated flow-diagram box (GAP-09); Cycle Count here is a fully modeled blind-count workflow. Barcode/GTIN capture (source §3.3 I001-series) is implemented; the source's India-only GST/HSN concepts are represented as a plain optional `hsnSacCode` string and a nullable `defaultTaxRuleId` reference into the (pluggable) Tax Rule master documented under the Procurement sub-module, not hard-coded into this module.

## 2. Overall Description

### 2.1 Product Perspective
Standalone, industry-agnostic module. No vertical branding anywhere in code, schema, or UI copy (enforced project-wide rule). Reuses two pieces of the parent OneCMS platform rather than duplicating them: the Campus Infrastructure physical hierarchy (`Room`/`Zone`) for stock locations, and the platform's generic `audit_log` table for audit trail. Every other entity described here is new, standalone Inventory schema under its own backend package namespace (`com.cms.inventory.catalog`, `com.cms.inventory.stock`).

### 2.2 User Classes
No hard-coded roles. Every screen/action is gated by a DB-managed permission (`INVENTORY_*` codes, §5). Typical functional user classes seen in a deployment: catalog administrator (masters), store/warehouse staff (stock movements, cycle counts), and a read-only viewer (reports/lookups) — actual role-to-permission mapping is done entirely through OneCMS's Role Management module, never in this module's code.

### 2.3 Operating Environment
Angular frontend (`frontend/src/app/features/inventory/...`), Spring Boot backend (`com.cms.inventory.catalog`, `com.cms.inventory.stock`), PostgreSQL via Flyway migrations (V422 onward), Keycloak-authenticated, image assets stored via the existing generic `StorageService`/MinIO integration (same pattern as floor-plan uploads).

### 2.4 Constraints & Assumptions
- **Room-only locations in this pass.** `InventoryLocation` takes a real, non-nullable FK to `Room`; `Zone`-level locations are explicitly not built yet (a real FK was chosen over a polymorphic reference once Zone support was dropped for now).
- **Weighted-average, not FIFO/FEFO.** Decrease-side valuation (disposal, decreasing adjustment) uses the existing `StockBalance`'s `valueOnHand / qtyOnHand` when no unit cost is supplied — a deliberate simplification, not the source SRS's FIFO/FEFO.
- **No live inheritance.** A `ProductVariant`'s attribute values, tracking mode, pricing, and barcode are copied from the parent `Product` at creation time and become independent afterward; a later edit to the parent never propagates.
- **Cross-module boundary discipline.** `Product.defaultTaxRuleId` is a bare `Long`, not a JPA relationship, to avoid the `catalog` package importing from the `procurement` package (see the Product/Tax decision, 2026-09-11).
- **Standalone-deployability constraint (out of functional scope, noted per task instruction):** R3-M9 ("package Infra + Inventory as an independently buildable/deployable unit") is an open, unresolved packaging/deployment question — not a functional requirement of this data model, and not designed against here.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-CDM-1 | Maintain a hierarchical **Category** master (self-referencing parent, unique `shortCode`, `isActive`) with real-time name/short-code uniqueness validation. | Must | — |
| FR-CDM-2 | Maintain per-Category **CategoryAttribute** definitions (Text/Number/Date/Boolean/Enum, required flag, display order, comma-separated enum options) nested under the owning Category, with no attribute inheritance from ancestor categories. | Must | FR-CDM-1 |
| FR-CDM-3 | Maintain a flat **UOM** master (unique code + name) and reusable **UOM Conversion Templates** (named, ordered unit chains with one base level) that a Product's own Unit Hierarchy can copy from at creation time (no live link). | Must | — |
| FR-CDM-4 | Maintain the **Product** catalog: code (auto-generated `<Category.shortCode>-<sequence>`, read-only), name (unique within Category), Category, base UOM, Brand (optional), reorder level/qty, standard cost/list price, HSN/SAC code, default Tax Rule reference, asset/consumable/service/loanable flags, tracking mode (NONE/BATCH/SERIAL), depreciation rate, warranty period, dimensions/weight, barcode (optional, unique), description, active flag, plus child **ProductAlias** and typed **ProductAttributeValue** collections. | Must | FR-CDM-1, FR-CDM-3 |
| FR-CDM-5 | Support uploading, listing, deleting, and marking-primary one or more **ProductImage**s per product (MinIO-backed). | Should | FR-CDM-4 |
| FR-CDM-6 | Maintain a flat **Brand** master (name, description, active flag) optionally linked from a Product. | Should | — |
| FR-CDM-7 | Support **ProductVariant** child SKUs per Product (own tracking mode, pricing, barcode, attribute-value overrides; shares the parent's UOM chain, category, brand, tax/dimensions) with its own uniqueness-checked variant code and barcode. | Should | FR-CDM-4 |
| FR-CDM-8 | Provide barcode capture, exact-match lookup, and printable PNG label generation for both Product and ProductVariant. | Should | FR-CDM-4, FR-CDM-7 |
| FR-CDM-9 | Maintain **Inventory Location** records, each wrapping exactly one existing Campus Infrastructure `Room`, with a virtual/display name (globally unique), a `locationRole` (`STORE`/`REQUESTING_POINT`/`BOTH`), and an optional default supplying location. | Must | Campus Infrastructure (`Room`) |
| FR-CDM-10 | Maintain optional **InventoryRack** and **InventoryBin** sub-locations (strict 3-level `Location → Rack → Bin` hierarchy, uniqueness scoped per parent) as locators; the Stock Ledger/Balance system of record stays keyed at the Location level, with `StockBinAllocation` as an organizational breakdown only. | Could | FR-CDM-9 |
| FR-CDM-11 | Record every stock movement (`RECEIPT`, `ADJUSTMENT`, `DISPOSAL` exposed in this sub-module's own UI surface; the underlying `StockTxnType` vocabulary also carries `ISSUE`/`TRANSFER`/`RETURN`/`CONSIGNMENT_CONSUMPTION` consumed by other sub-modules) as an **append-only Stock Ledger** entry (product, variant, location, batch, qty delta, unit cost, reference type/id, timestamp, performer), through a single service that is the sole writer of ledger/balance data. | Must | FR-CDM-4, FR-CDM-9 |
| FR-CDM-12 | Maintain a derived, always-in-sync **Stock Balance** per (product, variant, location, batch) — quantity and value on hand — updated transactionally on every Stock Ledger write; reject any movement that would drive a balance negative. | Must | FR-CDM-11 |
| FR-CDM-13 | Capture an optional batch/serial number and expiry date on any stock movement, auto-creating a **StockBatch** row the first time a given batch/serial number is used for a product+variant; enforce `Product.trackingMode` (`BATCH` requires a batch number, `SERIAL` requires a serial number and restricts the movement to quantity 1). | Must | FR-CDM-11 |
| FR-CDM-14 | Provide a **Cycle Count** (physical stock-take) workflow: blind count against a Location (system quantity hidden until submission), full-location default scope with ad-hoc add/remove of lines, per-line variance approval that auto-posts an `ADJUSTMENT` (zero-variance lines auto-close with no approval), against the unbatched balance only. | Must | FR-CDM-9, FR-CDM-11, FR-CDM-12 |
| FR-CDM-15 | Maintain a **convert-to-variant** action that migrates a stranded null-variant Stock Balance (created before a Product gained variants) onto a chosen active variant via a real, audit-logged pair of `ADJUSTMENT` postings — never an in-place row edit, never deleting the original "Unassigned" bucket. | Should | FR-CDM-7, FR-CDM-11 |
| FR-CDM-16 | Maintain per-(Product, Location) **Reorder Configuration** (reorder level, reorder qty, max stock qty, auto-indent enabled flag) scoped to `REQUESTING_POINT`/`BOTH` locations, feeding the Auto-Indent job documented under the Requisition & Issue sub-module. Saving with auto-indent enabled requires the location to already have a default supplying location set. | Should | FR-CDM-9 |

## 4. External Interface Requirements

### 4.1 Key Screens (frontend/src/app/features/inventory/...)
Category, CategoryAttribute (nested), UOM, UOM Conversion Templates, Brand, Product (list/form incl. embedded Images, Variants, UOM Hierarchy sections), Product Variant form, Barcode preview/print dialog, Inventory Location, Rack, Bin, Stock Movement (record), Stock Balance list (incl. Convert-to-Variant dialog and bin-allocation drill-down), Cycle Count list/new/detail, Reorder Configuration list/form. All under the merged "Stock Management" top-level nav group.

### 4.2 Key API Endpoints (all under `/inventory/...`, method + path)
- `categories` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/page`, `/name-exists`, `/short-code-exists`)
- `categories/{categoryId}/attributes` (GET/POST/PUT{attributeId}/DELETE{attributeId})
- `uoms` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/page`, `/code-exists`, `/name-exists`)
- `uom-conversion-templates` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/page`, `/name-exists`)
- `brands` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/page`, `/name-exists`)
- `products` (POST/GET{id}/PUT{id}/DELETE{id}, `/page`, `/code-exists`, `/next-code`, `/name-exists`, `/barcode-exists`, `/by-barcode`, `/{id}/barcode.png`, `/regenerate-codes/preview`, `/regenerate-codes`)
- `products/{productId}/images` (GET/POST multipart/`/{id}/set-primary`/DELETE{id}/`/{id}/download`)
- `products/{productId}/uom-chain` (`/active`, `/versions` GET/POST)
- `products/{productId}/variants` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/code-exists`, `/barcode-exists`)
- `product-variants` (`/by-barcode`, `/{id}/barcode.png`)
- `locations` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/page`, `/name-exists`)
- `racks` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/page`, `/name-exists`, `/code-exists`)
- `bins` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/page`, `/name-exists`, `/code-exists`)
- `stock/movements` (POST), `stock/balances/page` (GET), `stock/balances/{id}/bin-allocations` (GET), `stock/balances/{id}/convert-to-variant` (POST)
- `stock/cycle-counts` (POST, `/page`, `/{id}`, `/{id}/lines` POST/DELETE{lineId}, `/{id}/lines/{lineId}/count` PUT, `/{id}/submit`, `/{id}/lines/{lineId}/approve`, `/{id}/lines/{lineId}/reject`, `/{id}/cancel`)
- `stock/reorder-configs` (POST/GET`/page`/GET{id}/PUT{id}/DELETE{id}, `/pair-exists`)

### 4.3 Key DB Entities
`categories`, `category_attributes`, `uoms`, `uom_conversion_templates(_levels)`, `brands`, `products`, `product_aliases`, `product_attribute_values`, `product_images`, `product_variants`, `product_variant_attribute_values`, `product_uom_chain_versions/levels`, `category_product_sequences`, `inventory_locations`, `inventory_racks`, `inventory_bins`, `stock_batches`, `stock_ledger`, `stock_balances`, `stock_bin_allocations`, `cycle_counts`, `cycle_count_lines`, `product_location_reorder_configs`.

## 5. Non-Functional Requirements
- **Security/RBAC:** every endpoint gated by a dedicated `INVENTORY_*` permission (view/manage split per master; approve/convert/regenerate-codes each their own dedicated permission per the operation-wise permission mapping rule — see FRD §2). No hard-coded roles anywhere.
- **Auditability:** every stock-affecting write reuses the platform's shared `audit_log` table (`entityType`/`entityId`/`actor`/`detail`); every Stock Ledger row itself is permanent and append-only (never updated or deleted).
- **Data integrity:** DB-level uniqueness/check constraints back every uniqueness rule and enum (e.g. `chk_products_tracking_mode`, partial unique index on active barcode, `UNIQUE NULLS NOT DISTINCT` balance keys); negative stock is rejected at the service layer before it can be committed.
- **Performance:** Stock Balance is a materialized/derived table kept in sync on write specifically so on-hand lookups never require summing the full ledger at read time.

## 6. Known Gaps / Not Yet Implemented
- **R3-M8 — `InventoryItem` (legacy lab-consumables) migration: NOT built, and now effectively moot.** The original plan was to migrate the old `InventoryItem` table's data onto this new core. On investigation the table held **zero real rows in any environment** (confirmed by direct query), so there was nothing to migrate — the legacy `InventoryItem` entity/controller/service/repository, its screens, and its 6 permissions were **retired outright** (migration `V476__retire_legacy_inventory_item.sql`), not migrated. `RELEASE_3_MILESTONES.md`'s tracker still lists R3-M8 as "Not Started," reflecting that no formal migration ceremony ever ran — but there is no longer any legacy data or screen left to migrate.
- **No Zone-level Inventory Locations** — only Room-level locations are supported today; Zone support was explicitly deferred.
- **No FIFO/FEFO issue valuation** — weighted-average is used as a stand-in on the decrease side; this is called out in the ER diagram as real, deferred work (originally scoped for GRN/batch work), not fully closed by this sub-module or the GRN sub-module.
- **Stock Ledger has no browsing/history screen** — only the write path (Record Movement) and the derived balance read exist; a ledger-history read endpoint was deliberately not added ahead of a real consumer.
- **Stranded null-variant balances**: converting a legacy (pre-variant) balance to a variant is a manual, ad-hoc action (FR-CDM-15); there is no bulk/automatic conversion, and no blocking prompt at the moment a variant is first activated.
- **Batch/serial-tracked products cannot be issued or transferred through the Requisition & Issue sub-module's screens today** — those add-line requests carry no batch/serial field; a batch/serial-tracked product can only be received, adjusted, or disposed. Logged as a known, unfixed cross-module gap (found 2026-09-15), not part of this data model's own scope to close.
- **`InventoryRack`/`InventoryBin`/`StockBinAllocation` (migrations V507–V511) have no corresponding `DECISION_LOG.md` entry** — the schema, service, and screens exist and are wired (own permissions `INVENTORY_RACK_*`/`INVENTORY_BIN_*`), but the design rationale was not recorded in the module's own decision log at the time it was built, an internal documentation-process gap noted here for transparency rather than inferred.
- **R3-M9 (standalone Infra+Inventory deployability)** — not a functional gap of this data model, but an explicitly unresolved packaging/deployment question (Gradle multi-module vs. runtime toggle; shared vs. curated migration baseline) that constrains how this module could eventually be deployed on its own. Not designed against in this sub-module.
