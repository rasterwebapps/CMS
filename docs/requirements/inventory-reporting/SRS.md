# Software Requirements Specification — Reporting & Dashboards

**Module:** Inventory Management (OneCMS) — Sub-module 7 of 7
**Corresponds to:** Release 3 Milestone R3-M7 ("Reporting")
**Status as documented:** Done (100%)

> Standalone, industry-agnostic sub-module — see the parent module note in `inventory-core-data-model/SRS.md` §0. No vertical (hospital/college) naming appears anywhere in this sub-module's screens or copy.

## 1. Introduction

### 1.1 Purpose
Defines the requirements for the module's read-only reporting/analytics layer: a single at-a-glance Inventory Dashboard, and six standard ERP-style reports (Stock Valuation, Purchase Order Aging, Purchase Order Cycle-Time, Asset Depreciation Summary, Price Comparison, Budget vs. Actual). Every figure is computed live from already-shipped entities across the other six sub-modules — this sub-module introduces **no new tables, no new migrations, and (with one exception) no new permissions.**

### 1.2 Scope
`InventoryDashboardController`/`Service`, `StockValuationReportController`/`Service`, `PurchaseOrderAgingReportController`/`Service`, `PurchaseOrderCycleTimeReportController`/`Service`, `AssetDepreciationSummaryReportController`/`Service`, `BudgetVsActualReportController`/`Service`, plus the frontend-only Price Comparison Report screen. Out of scope: the underlying transactional entities each report reads (owned by their respective sub-modules — Core Data Model, Procurement & Vendor Management, Asset Management, Approvals/Budget) and any future export/scheduling capability.

### 1.3 References
- `docs/RELEASE_3_MILESTONES.md` — R3-M7 row
- `docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §3 (3.9 Reports & Analytics genuinely reusable), §6 (no dedicated reporting entities proposed — reports are rollups of existing entities)
- `docs/inventory-management/DECISION_LOG.md`: 2026-09-08 "Inventory Dashboard slice (OC-219)"; 2026-09-09 "Stock Valuation Report slice (OC-221)", "Purchase Order Aging Report slice (OC-222)", "Asset Depreciation Summary Report slice (OC-223)", "Price Comparison Report slice (OC-224)", "Budget vs. Actual Report slice (OC-225)", "Purchase Order Cycle-Time Report slice (OC-226)"
- Source SRS: `docs/inventory-management/source/SRS_v3.2_Updated.pdf` §3.9 (Reports & Analytics, R001–R0xx)

**Reconciliation against the source SRS:** §3.9 was assessed as "genuinely reusable across any vertical" by the gap analysis, and the as-built reports follow standard ERP report shapes (category-grouped valuation, 0–30/31–60/61–90/90+ day PO aging buckets, per-supplier cycle-time averages) rather than the source SRS's own specific layout, which this session had no access to reconcile line-by-line beyond the section's stated scope (price comparison, cycle-time, dashboards). Two deliberate simplifications recur across every report in this sub-module: (1) every report is a rollup/summary screen with **no per-row drill-down or listing** — the module's existing list screens (Stock Balance, Purchase Orders, Asset Register, Budgets) already serve that detail, so no report duplicates them; (2) every report is **computed live on each request**, never stored as a snapshot or scheduled/cached, matching this module's "computed live, never stored" discipline used throughout (overdue flags, depreciation, budget consumption).

## 2. Overall Description

### 2.1 Product Perspective
Standalone module component; reuses entities from every other Inventory Management sub-module (`StockBalance`/`Category` — Core Data Model; `PurchaseOrder`/`PurchaseOrderItem`/`Supplier`/`VendorProductMapping` — Procurement & Vendor Management; `GoodsReceipt` — GRN & Stock Control; `Asset` — Asset Management; `Budget` — Approvals, Budget & Gate Pass; `ApprovalInstance`, `GatePass`, `LoanableItemIssue`, `ServiceTicket`, `ConsignmentStockLine` — also Approvals & Gate Pass). Introduces no new domain data of its own — it is purely a read layer.

### 2.2 User Classes
Any user holding the relevant underlying bounded context's existing view/manage permission (e.g. a Stock viewer sees Stock Valuation; a Purchase Order viewer sees PO Aging/Cycle-Time; a Budget viewer sees Budget vs. Actual). A separate `INVENTORY_DASHBOARD_VIEW` permission exists only for the Dashboard, since it aggregates across many independently-permissioned contexts that no single existing permission could correctly gate. All role-to-permission mapping is DB-driven, not hard-coded.

### 2.3 Operating Environment
Same stack as the rest of the module (Angular + Spring Boot + PostgreSQL/Flyway + Keycloak). Backend package `com.cms.inventory.reporting` (dashboard/report-specific code only — each report's actual data still comes from repository queries added to, or reused from, the owning sub-module's existing repositories); frontend `frontend/src/app/features/inventory/reporting/`. Unlike every other sub-module, Reporting has **no dedicated nav group** — its screens are surfaced contextually inside the nav group of the data they report on (Dashboard and Stock Valuation under "Stock Management"; Price Comparison, PO Aging, and PO Cycle-Time under "Purchasing & Suppliers"; Depreciation Summary under "Equipment & Asset Management"; Budget vs. Actual under "Budgets & Approvals").

### 2.4 Constraints & Assumptions
- No report supports export (CSV/PDF/Excel) — every report is an in-app, read-only screen only.
- No report supports a custom date-range/grouping control beyond what's documented per report in §3 — each ships its one ERP-standard default shape, not a general-purpose report builder.
- No scheduling, caching, or snapshotting exists anywhere in this sub-module — every figure is recomputed from live data on every page load.
- Price Comparison required **zero new backend code** — it is a pure frontend presentation of the Procurement sub-module's already-existing `VendorProductMapping` `/page?productId=` endpoint output (`effectivePrice`/`priceSource`), sorted client-side.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-RPT-1 | Provide an **Inventory Dashboard** showing eleven live-computed metrics spanning every other sub-module: open Purchase Orders, pending Purchase Requisitions, pending Wanted List items, active Approval Instances, overdue Gate Passes, overdue Loanable Item Issues, open Service Tickets, urgent open Service Tickets, over-allocated Budgets, consignment outstanding liability value, and assets under maintenance — each an obvious "what needs attention right now" figure, no report/filter/export decisions involved. | Must | Every other sub-module's core entities |
| FR-RPT-2 | Provide a **Stock Valuation Report**: total stock value and distinct-product count grouped by Category, with an optional Location filter, plus a grand total — deliberately never sums raw quantity across a category (mixed UOMs make that meaningless). | Must | Core Data Model `StockBalance`/`Category` |
| FR-RPT-3 | Provide a **Purchase Order Aging Report**: open Purchase Orders (statuses `PENDING`/`ORDERED`/`IN_PROGRESS`/`PARTIALLY_COMPLETED`) bucketed by days elapsed since `poDate` into 0–30/31–60/61–90/90+ ranges, with order count and total value per bucket plus a grand total. | Must | Procurement `PurchaseOrder`/`PurchaseOrderItem` |
| FR-RPT-4 | Provide a **Purchase Order Cycle-Time Report**: average days from `poDate` to the latest confirmed Goods Receipt among a `COMPLETED` order's own receipts, grouped and averaged by Supplier, plus an overall grand average computed across every order directly (not an average of per-supplier averages). | Should | Procurement `PurchaseOrder`, GRN & Stock Control `GoodsReceipt` |
| FR-RPT-5 | Provide an **Asset Depreciation Summary Report**: purchase value, accumulated depreciation, and current book value totals grouped by Category (excluding `DISPOSED` assets), plus asset count and a grand total — reusing the exact same depreciation formula the Asset Register itself uses (`AssetDepreciationCalculator`, extracted for this purpose, not duplicated). | Should | Asset Management `Asset` |
| FR-RPT-6 | Provide a **Price Comparison Report**: per-product, side-by-side supplier `effectivePrice`/`priceSource` rows (reusing the Vendor Product Mapping list's own effective-price resolution), sorted ascending with the cheapest row visually marked; no currency conversion across differing `currencyCode`s. | Should | Procurement `VendorProductMapping` |
| FR-RPT-7 | Provide a **Budget vs. Actual Report**: allocated/consumed/remaining totals and an over-allocated flag, grouped by Location, summed across every currently-active Budget for that location regardless of each budget's own period, plus a grand total. | Should | Approvals, Budget & Gate Pass `Budget` |

## 4. External Interface Requirements

### 4.1 Key Screens
Inventory Dashboard, Stock Valuation, PO Aging Report, PO Cycle-Time Report, Depreciation Summary, Price Comparison, Budget vs. Actual — each embedded in its data-owning nav group per §2.3, not a standalone "Reporting" nav section.

### 4.2 Key API Endpoints
- `GET /inventory/reporting/dashboard` — `INVENTORY_DASHBOARD_VIEW`
- `GET /inventory/reporting/stock-valuation` (optional `locationId`) — `INVENTORY_STOCK_VIEW`/`_MANAGE`
- `GET /inventory/reporting/purchase-order-aging` — `INVENTORY_PURCHASE_ORDER_VIEW`/`_MANAGE`
- `GET /inventory/reporting/purchase-order-cycle-time` — `INVENTORY_PURCHASE_ORDER_VIEW`/`_MANAGE`
- `GET /inventory/reporting/asset-depreciation-summary` — `INVENTORY_ASSET_VIEW`/`_MANAGE`
- `GET /inventory/reporting/budget-vs-actual` — `INVENTORY_BUDGET_VIEW`/`_MANAGE`
- Price Comparison has no dedicated endpoint — the frontend calls the Procurement sub-module's existing `GET /inventory/procurement/vendor-product-mappings/page?productId=&activeOnly=true` directly.

### 4.3 Key DB Entities
None owned by this sub-module — every report reads existing tables from Core Data Model (`stock_balances`, `categories`), Procurement (`purchase_orders`, `purchase_order_items`, `vendor_product_mappings`), GRN (`goods_receipts`), Asset Management (`assets`), and Approvals/Budget (`budgets`).

## 5. Non-Functional Requirements
- **Security/RBAC:** six of the seven screens deliberately reuse their underlying bounded context's existing view/manage permission rather than minting a new one — only the Dashboard (which aggregates across many separately-permissioned contexts) has its own dedicated `INVENTORY_DASHBOARD_VIEW`, with no `_MANAGE` counterpart since there is nothing to manage.
- **Auditability:** reports are read-only; no writes occur in this sub-module, so nothing is posted to `audit_log` from here.
- **Performance:** every report uses a dedicated aggregate repository query (`GROUP BY`/`SUM`/`COUNT` at the database) rather than loading full entity graphs into memory, except where per-entity domain logic (aging-bucket math, depreciation calculation) genuinely requires row-level computation in Java after a lean fetch.
- **Consistency:** the PO Aging Report's "open" status set is deliberately identical to the Dashboard's "Open Purchase Orders" tile definition, so the two screens never disagree on what counts as open.

## 6. Known Gaps / Not Yet Implemented
- No export (CSV/PDF/Excel) on any report.
- No custom date-range/grouping controls beyond each report's single documented default shape — none of these are configurable report builders.
- No scheduling, caching, or historical snapshotting — every figure is a live, as-of-now computation; there is no month-over-month or trend view anywhere in this sub-module.
- Budget vs. Actual sums every active Budget per Location regardless of period — a location running concurrent non-overlapping budgets (e.g. Q1 and Q2 both still marked active) has its figures combined into one number, which could misrepresent "this period's" spend for a deployment that runs period-aware budgeting. Logged as a known, accepted limitation extending the Dashboard's own pre-existing simplification, not solved here — a genuinely period-aware version needs a real answer to "what period does this deployment's budgeting calendar use," out of this sub-module's scope.
- Price Comparison does no currency conversion — rows in different currencies are shown as-is, not normalized for a true side-by-side comparison.
- Purchase Order Cycle-Time only counts `COMPLETED` orders — `FORCE_CLOSED` orders (closed before full receipt) contribute no cycle-time figure, since they were never actually fully received.
