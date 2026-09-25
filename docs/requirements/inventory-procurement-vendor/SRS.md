# Software Requirements Specification — Procurement & Vendor Management

**Module:** Inventory Management (OneCMS) — Sub-module 2 of 7
**Corresponds to:** Release 3 Milestone R3-M2 ("Procurement & Vendor Management")
**Status as documented:** Done (100%)

> Standalone, industry-agnostic sub-module — see the parent module note in `inventory-core-data-model/SRS.md` §0. No vertical (hospital/college) naming appears anywhere in this sub-module's schema or UI copy.

## 1. Introduction

### 1.1 Purpose
Defines the requirements for sourcing and buying: supplier registration/approval, standing rate agreements, per-product vendor pricing, a demand-request chain (Purchase Requisition → optional Quotation Request/RFQ → Purchase Order), automatic reorder detection (Wanted List), and the tax/currency reference data procurement pricing depends on.

### 1.2 Scope
Supplier, RateContract/RateContractLine, VendorProductMapping, TaxType/TaxRule/TaxSubType, Tax Jurisdiction Settings, Currency Settings/Currency Exchange Rate, Purchase Requisition, Wanted List, Quotation Request (RFQ), and Purchase Order. Out of scope: receiving against a PO (GRN sub-module), the demand source being on-hand-stock requisition rather than a supplier purchase (Requisition & Issue sub-module), and Budget/Approval routing that can optionally gate these documents (Approvals & Gate Pass sub-module).

### 1.3 References
- `docs/inventory-management/MILESTONES.md` Phase 2
- `docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §1 (3.1/3.2 source-SRS sections), GAP-02/03/04 (tax/accounting/currency)
- `docs/inventory-management/DECISION_LOG.md`: 2026-09-08 "Phase 2 kickoff", "VendorProductMapping slice", "Reference architecture pivot" (IHMS), "Purchase Requisition slice", "Wanted List slice", "Purchase Order slice"; 2026-09-11 multi-currency FX; 2026-09-16 "Quotation Request (RFQ) slice"; 2026-09-22 auto-generated codes and sequential document numbers
- Source SRS: `docs/inventory-management/source/SRS_v3.2_Updated.pdf` §3.1 (P001–P021), §3.2 (V001–V013)

**Reconciliation against the source SRS:** the source SRS's Procurement section (§3.1) was assessed as "genuinely reusable across any vertical" by the gap analysis — its core PR→RFQ→PO mechanics carried over largely as-is. Divergences, all deliberate: (1) the source SRS ties Vendor Management to HIS (Hospital Information System) integration for auto-indent; the as-built Wanted List instead runs a standard MRP netting formula against this module's own Purchase Requisition pipeline, with no hospital-system coupling. (2) The source SRS's tax model is India-GST-only with hard-coded slabs (GAP-02); the as-built `TaxType → TaxRule → TaxSubType` hierarchy plus a home-state Jurisdiction Setting is a real, if still India-GST-shaped, configurable engine — closer to closing GAP-02 than the source SRS's own hard-coded validation, though still not a fully generic multi-region tax engine. (3) The source SRS names Tally as the sole accounting integration (GAP-03); no ledger-posting connector of any kind is built in this sub-module. (4) The source SRS's RFQ flow (embedded in §3.1) was deliberately deferred for most of this module's build and only picked up as a dedicated Quotation Request feature late in the timeline (2026-09-16), once Requisition/PO existed to make comparison meaningful — the as-built RFQ is optional and per-line-awardable, a materially more granular design than a single-supplier-per-request RFQ. (5) Cash/Spot PO variants and multi-currency-book accounting (GAP-04) named in the source SRS are not built; `PurchaseOrder`/`VendorProductMapping` instead carry plain, unconverted `currencyCode`/`exchangeRate` fields with a separate, simple base-currency FX resolution utility for display purposes only.

## 2. Overall Description

### 2.1 Product Perspective
Standalone module component; reuses the Core Data Model sub-module's `Product`, `InventoryLocation`, and `Uom` entities. No dependency on any Hospital Information System or Student Information System — a Purchase Requisition's `location` is a generic `InventoryLocation`.

### 2.2 User Classes
Requesting-location staff (raise Purchase Requisitions), procurement/purchasing staff (Suppliers, Rate Contracts, Vendor Product Rates, Purchase Orders, Quotation Requests), an approver role for requisition-line approve/reject and supplier approval, and a finance/admin role for Tax/Currency/Jurisdiction settings. All role-to-permission mapping is DB-driven, not hard-coded.

### 2.3 Operating Environment
Same stack as the rest of the module (Angular + Spring Boot + PostgreSQL/Flyway + Keycloak). Backend package `com.cms.inventory.procurement`; frontend `frontend/src/app/features/inventory/procurement/`.

### 2.4 Constraints & Assumptions
- No PO approval gate is native to this sub-module — a PO ships with a plain receipt-progress-driven status lifecycle; real multi-level approval is an optional, separately-started routing from the Approvals sub-module.
- Purchase Requisition carries no monetary value (quantity/product only) — only Purchase Order line totals represent real committed spend, which the Budget sub-module relies on.
- Multi-currency support is display/reference only: `currencyCode`/`exchangeRate` on `PurchaseOrder` and `VendorProductMapping` are plain, uncoverted fields; `CurrencyExchangeRate`/`InventoryCurrencySetting` provide a manual, admin-maintained base-currency conversion shown as a hint, never used to recompute a document's own totals.
- The tax jurisdiction engine assumes a single home-state institution comparing against a supplier's own `state` field to resolve INTERSTATE vs. INTRASTATE — a domestic-single-country tax model, not a generalized multi-country VAT/sales-tax engine.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-PV-1 | Maintain a **Supplier** master (code, name, state, tax/legal registration, masked bank details, contact info, active flag) with a distinct **Approve** action (`isApproved`/`approvalDate` settable only via that action) and real-time code/name uniqueness. | Must | — |
| FR-PV-2 | Maintain a **Rate Contract** per Supplier (validity window, value cap, terms, renewal reminder) with child **Rate Contract Line**s (per-product negotiated rate) that override a Vendor Product Mapping's price only while the parent contract is active and within its date window, resolved at read time. | Must | FR-PV-1 |
| FR-PV-3 | Maintain **Vendor Product Mapping** (Supplier × Product, unit price, currency, UOM override, min order qty, lead time, preferred flag, vendor's own part number/name) as a standalone master, at most one active mapping per (supplier, product) pair. | Must | FR-PV-1, Core Data Model FR-CDM-4 |
| FR-PV-4 | Maintain a configurable tax engine: **TaxType** (regime, e.g. GST), **TaxRule** ("Tax", rate % under a TaxType), **TaxSubType** (interstate/intrastate components of a TaxRule, split percentages summing to 100 per jurisdiction mode), and a singleton **Tax Jurisdiction Setting** (home state) used to auto-resolve INTERSTATE vs. INTRASTATE per PO line by comparing against the supplier's state. | Must | FR-PV-1 |
| FR-PV-5 | Maintain a singleton **Currency Setting** (base currency) and a dated **Currency Exchange Rate** reference table (one row per currency+effective-date), used only to resolve/display a mapping's price in the base currency — never to recompute stored totals. | Should | — |
| FR-PV-6 | Support raising, adding/removing lines on, submitting, and cancelling a **Purchase Requisition** against an Inventory Location, with per-line Approve/Reject (structured reason + notes) distinct from header submit/cancel. | Must | Core Data Model FR-CDM-9 |
| FR-PV-7 | Run a **Wanted List** shortage-detection job (nightly and on-demand "Run Now") that nets `ReorderLevel − (QtyOnHand + QtyOnOrder)` per (product, location) against already-open Purchase Requisition lines, suggests a hybrid-lot-sized quantity, and lets several lines for the same location be collectively converted into one new Purchase Requisition; a line may instead be Deferred (reopenable) or Rejected (structured reason). | Must | FR-PV-6 |
| FR-PV-8 | Support an optional **Quotation Request (RFQ)**: header against a Location, lines sourced one-for-one from `APPROVED` Purchase Requisition items, invite multiple Suppliers, record each supplier's per-line quote, award a line to a specific supplier's quote (no minimum quote count required), and convert all `AWARDED` lines grouped by winning supplier into one Purchase Order per supplier. | Should | FR-PV-6, FR-PV-1 |
| FR-PV-9 | Support creating a **Purchase Order** against a Supplier + Location either directly (picking up `APPROVED` requisition lines) or via Quotation Request conversion, with a receipt-progress-driven status lifecycle (`PENDING → ORDERED → IN_PROGRESS → PARTIALLY_COMPLETED → COMPLETED`, plus `FORCE_CLOSED` reachable from any non-terminal state), line-level tax computation (jurisdiction-resolved TaxSubType components, snapshotted at line-creation time), and unit-price defaulting from the effective Vendor Product Mapping/Rate Contract rate. | Must | FR-PV-1, FR-PV-2, FR-PV-3, FR-PV-4, FR-PV-6 |
| FR-PV-10 | Generate a sequential, human-readable document number for Quotation Request, Purchase Order, Goods Receipt, and Return to Supplier (configurable reset cadence per series), with an on-demand, permission-gated Regenerate Numbers admin action per document type; every other document in this module keeps the plain `#{id}` convention. | Should | — |

## 4. External Interface Requirements

### 4.1 Key Screens
Suppliers, Rate Contracts, Tax Types (implicit via Tax Rule form), Tax Rules (+ nested Tax Sub-Types), Tax Jurisdiction Settings, Currency Settings, Currency Exchange Rates, Vendor Product Rates, Purchase Requisitions (list/new/detail), Wanted List, Quotation Requests (list/new/detail), Purchase Orders (list/new/detail) — all under the "Purchasing & Suppliers" nav group.

### 4.2 Key API Endpoints (all under `/inventory/procurement/...`)
- `suppliers` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/{id}/approve`, `/page`, `/code-exists`)
- `rate-contracts` (POST/GET{id}/PUT{id}/DELETE{id}, `/page`)
- `vendor-product-mappings` (POST/GET{id}/PUT{id}/DELETE{id}, `/page`, `/pair-exists`)
- `tax-types`, `tax-rules`, `tax-sub-types` (standard CRUD + `/page`/`/name-exists` where applicable)
- `tax-jurisdiction-settings` (GET/PUT)
- `currency-settings` (GET/PUT), `currency-exchange-rates` (POST/GET/GET{id}/PUT{id}/DELETE{id}, `/page`, `/pair-exists`)
- `purchase-requisitions` (POST, `/page`, GET{id}, `/{id}/lines` POST/DELETE{lineId}, `/{id}/submit`, `/{id}/lines/{lineId}/approve`, `/{id}/lines/{lineId}/reject`, `/{id}/cancel`)
- `wanted-list` (`/page`, `/run`, `/{id}/defer`, `/{id}/reopen`, `/{id}/reject`, `/convert`)
- `quotation-requests` (POST, `/page`, `/regenerate-numbers/preview`, `/regenerate-numbers`, GET{id}, `/available-requisition-lines`, `/{id}/lines` POST/DELETE{lineId}, `/{id}/suppliers` POST/DELETE{supplierId}, `/{id}/submit`, `/{id}/lines/{lineId}/responses/{supplierId}`, `/{id}/lines/{lineId}/award`, `/{id}/lines/{lineId}/reject`, `/{id}/convert`, `/{id}/cancel`)
- `purchase-orders` (POST, `/page`, `/regenerate-numbers/preview`, `/regenerate-numbers`, GET{id}, `/available-requisition-lines`, `/{id}/lines` POST/DELETE{lineId}, `/{id}/order`, `/{id}/force-close`)

### 4.3 Key DB Entities
`suppliers`, `rate_contracts`, `rate_contract_lines`, `vendor_product_mappings`, `tax_types`, `tax_rules`, `tax_sub_types`, `inventory_tax_jurisdiction_settings`, `inventory_currency_settings`, `currency_exchange_rates`, `purchase_requisitions`, `purchase_requisition_items`, `wanted_list_items`, `quotation_requests`, `quotation_request_lines`, `quotation_request_suppliers`, `quotation_response_lines`, `purchase_orders`, `purchase_order_items`, `purchase_order_item_tax_components`.

## 5. Non-Functional Requirements
- **Security/RBAC:** every action its own dedicated permission per the operation-wise mapping rule — see FRD §2; bank account number/tax registration ID/legal registration number are masked to their last 4 characters for a viewer holding only `INVENTORY_SUPPLIER_VIEW`.
- **Auditability:** every write posts to the shared `audit_log`; PO line tax components and totals are snapshotted at line-creation time (never recomputed live) so a later TaxRule change never silently alters an already-created line.
- **Data integrity:** `TaxSubType` split-percent-sums-to-100-per-jurisdiction-mode is enforced at the service layer (not expressible as a single-row DB check); requisition-line "already picked into a PO/Quotation Request" double-booking is prevented by excluding lines already live on a non-`REJECTED` Quotation Request line from the direct-to-PO picker, and vice versa.
- **Performance:** Wanted List netting and PO/GRN aging-style reads use dedicated aggregate repository queries rather than in-memory computation over full entity graphs.

## 6. Known Gaps / Not Yet Implemented
- No real pluggable multi-region tax engine (GAP-02) — the tax model is a real jurisdiction-aware hierarchy but still shaped around a single domestic (India-style interstate/intrastate GST) regime.
- No accounting/ERP posting connector (GAP-03) — no Tally, QuickBooks, SAP, or generic ledger-posting adapter exists anywhere in this sub-module.
- No true multi-currency computation (GAP-04) — currency/exchange-rate fields are informational only; no document total is ever recomputed in a different currency.
- No outbound "send RFQ to supplier" mechanism — Quotation Request is an internal record only; there is no email service in this backend, and `Supplier.portalAccessEnabled` remains an unused reserved flag with no vendor portal login built against it.
- No variant-selection UI on Quotation Request → Purchase Order conversion — a product with active variants surfaces the same "select one for this line" error a manual PO add-line hits, rather than a dedicated picker.
- `V477__create_inventory_tax_engine_and_jurisdiction.sql`'s own migration header references "the 2026-09-09 'GAP-02 pickup' decision-log entry" — **no such entry exists in `DECISION_LOG.md`** (confirmed by full-file search); this SRS's tax-engine description is reconstructed directly from the migration and entity source, not from a decision-log narrative, which is itself a documentation-consistency gap worth flagging.
