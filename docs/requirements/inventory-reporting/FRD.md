# Functional Requirements Document — Reporting & Dashboards

## 1. Overview
Implements the module's read-only reporting/analytics layer: one cross-cutting Dashboard and six single-purpose ERP-standard reports, all computed live over data owned by the other six Inventory Management sub-modules. No new domain tables; only one new permission in the entire sub-module.

## 2. Actors & Permissions

| Permission Code | Gates |
|---|---|
| `INVENTORY_DASHBOARD_VIEW` | Inventory Dashboard (new, dedicated — no `_MANAGE` counterpart, nothing to manage) |
| `INVENTORY_STOCK_VIEW` / `_MANAGE` | Stock Valuation Report (reused from the Core Data Model / Stock sub-module) |
| `INVENTORY_PURCHASE_ORDER_VIEW` / `_MANAGE` | PO Aging Report, PO Cycle-Time Report (reused from Procurement & Vendor Management) |
| `INVENTORY_ASSET_VIEW` / `_MANAGE` | Asset Depreciation Summary Report (reused from Asset Management) |
| `INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW` / `_MANAGE` | Price Comparison Report (reused from Procurement & Vendor Management — same permission that already gates the Vendor Product Rates screen its data comes from) |
| `INVENTORY_BUDGET_VIEW` / `_MANAGE` | Budget vs. Actual Report (reused from Approvals, Budget & Gate Pass) |

No actor in this sub-module can write anything — every screen is read-only, so there are no Manage/Approve/Assign-style operation permissions to enumerate here.

## 3. Screens & UI Behavior
- **Dashboard** (`frontend/.../reporting/dashboard/`) — eleven metric tiles (open Purchase Orders, pending Purchase Requisitions, pending Wanted List items, active Approval Instances, overdue Gate Passes, overdue Loanable Item Issues, open Service Tickets, urgent open Service Tickets, over-allocated Budgets, consignment outstanding liability, assets under maintenance) plus a `generatedAt` timestamp; nav-listed under "Stock Management" as the module's landing item.
- **Stock Valuation** (`.../stock-valuation/`) — table of Category rows (product count, total value) with an optional Location dropdown filter, plus a grand-total row; nav-listed under "Stock Management."
- **PO Aging Report** (`.../po-aging/`) — four fixed buckets (0–30/31–60/61–90/90+ days), each showing order count and total value, plus a grand total; nav-listed under "Purchasing & Suppliers."
- **PO Cycle-Time Report** (`.../po-cycle-time/`) — table of Supplier rows (completed order count, average cycle days), plus an overall grand-average row; nav-listed under "Purchasing & Suppliers."
- **Depreciation Summary** (`.../asset-depreciation-summary/`) — table of Category rows (asset count, purchase value, accumulated depreciation, current book value), plus a grand-total row; nav-listed under "Equipment & Asset Management."
- **Price Comparison** (`.../price-comparison/`) — per-product supplier comparison grid (calls the Procurement sub-module's `VendorProductMappingService.getPage({ productId, activeOnly: true })` directly), client-side sorted ascending by `effectivePrice` with the cheapest row visually marked; nav-listed under "Purchasing & Suppliers."
- **Budget vs. Actual** (`.../budget-vs-actual/`) — table of Location rows (budget count, allocated/consumed/remaining amounts, over-allocated flag), plus a grand-total row; nav-listed under "Budgets & Approvals."

No screen in this sub-module has a create/edit form, a uniqueness check, or a status/enum badge system of its own (all are read-only rollup tables); no screen offers export.

## 4. Functional Workflows

### 4.1 Dashboard generation
On screen load, `InventoryDashboardService` issues one lightweight query/count per metric against the owning entity of each of the eleven figures (e.g. `JpaSpecificationExecutor#count` for status-based counts, a small aggregate query for `consignmentOutstandingLiability`) and returns them together with a `generatedAt` timestamp — no caching, recomputed on every request.

### 4.2 Stock Valuation
`StockBalanceRepository.sumValuationByCategory` groups all `StockBalance` rows (optionally filtered to one `locationId`) by `Category`, summing `valueOnHand` and counting distinct products per category; the service assembles category rows plus a grand total. Raw quantity is deliberately never summed across a category (mixed UOMs).

### 4.3 PO Aging
`PurchaseOrderItemRepository.findOpenOrdersForAging` fetches every Purchase Order in the "open" status set (`PENDING`/`ORDERED`/`IN_PROGRESS`/`PARTIALLY_COMPLETED`) grouped with its line totals; the service computes `daysSincePoDate = today − poDate` in Java and assigns each order to its 0–30/31–60/61–90/90+ bucket, summing count and value per bucket plus an overall grand total.

### 4.4 PO Cycle-Time
`GoodsReceiptRepository.findCompletedOrdersForCycleTime` joins `GoodsReceipt` → `PurchaseOrder` → `Supplier` for every `COMPLETED` order, returning each order's `poDate` and its latest confirmed receipt's `confirmedAt`; the service computes `cycleDays = confirmedAt − poDate` per order in Java, then groups and averages by supplier, plus a true grand average computed directly across every order (not an average of the per-supplier averages).

### 4.5 Asset Depreciation Summary
`AssetRepository.findAllWithCategoryExcludingStatus` fetch-joins every non-`DISPOSED` `Asset` with its `Category`; the shared `AssetDepreciationCalculator.compute(...)` (the same calculator the Asset Register itself calls, extracted rather than duplicated) computes each asset's accumulated depreciation and current book value; the service sums per category (asset count, purchase value, accumulated depreciation, book value — the latter two contributing zero, not null, for assets missing full depreciation inputs) plus a grand total.

### 4.6 Price Comparison
Purely a frontend read: the component calls the already-existing `GET /inventory/procurement/vendor-product-mappings/page?productId=<id>&activeOnly=true` endpoint, which already returns each row's `effectivePrice` (contract-override resolved) and `priceSource`; the component sorts the returned rows ascending by `effectivePrice` and visually marks the first (cheapest) row. No backend code in this sub-module is involved.

### 4.7 Budget vs. Actual
The service reuses `BudgetService.findPage`'s already-computed `consumedAmount`/`remainingAmount`/`overAllocated` per active `Budget` row (no re-querying `PurchaseOrderItem` directly), groups by `Location`, sums allocated/consumed/remaining across every currently-active budget for that location regardless of each budget's own period, and flags a location as over-allocated if any of its summed remaining is negative — plus a grand total across all locations.

## 5. API Endpoints
See SRS.md §4.2 for the full list. All are `GET`-only, permission-gated per §2 above. None accepts a body; only Stock Valuation accepts a query parameter (`locationId`, optional).

## 6. Data Model
This sub-module owns no tables. Response shapes (backend DTOs, `com.cms.inventory.reporting.dto`), confirmed from source:
- `InventoryDashboardResponse`: 11 `long`/`BigDecimal` metric fields + `generatedAt` (`Instant`).
- `StockValuationReportResponse`: `List<StockValuationCategoryRow>` (`categoryId`, `categoryName`, `productCount`, `totalValue`) + `grandTotalProductCount`/`grandTotalValue` + `generatedAt`.
- `PurchaseOrderAgingReportResponse`: `List<PurchaseOrderAgingBucketRow>` (`bucketLabel`, `orderCount`, `totalValue`) + `grandTotalOrderCount`/`grandTotalValue` + `generatedAt`.
- `PurchaseOrderCycleTimeReportResponse`: `List<PurchaseOrderCycleTimeSupplierRow>` (`supplierId`, `supplierName`, `completedOrderCount`, `averageCycleDays`) + `grandTotalCompletedOrderCount`/`overallAverageCycleDays` + `generatedAt`.
- `AssetDepreciationSummaryReportResponse`: `List<AssetDepreciationCategoryRow>` (`categoryId`, `categoryName`, `assetCount`, `totalPurchaseValue`, `totalAccumulatedDepreciation`, `totalCurrentBookValue`) + four grand-total fields + `generatedAt`.
- `BudgetVsActualReportResponse`: `List<BudgetVsActualLocationRow>` (`locationId`, `locationName`, `budgetCount`, `allocatedAmount`, `consumedAmount`, `remainingAmount`, `overAllocated`) + four grand-total fields + `generatedAt`.
- Price Comparison has no dedicated response DTO — it consumes the Procurement sub-module's existing `VendorProductMapping` page response as-is.

## 7. Edge Cases & Validation Rules
- Stock Valuation's `locationId` filter is optional; omitting it reports across all locations.
- PO Aging's bucket boundaries are fixed (0–30/31–60/61–90/90+ days) and not configurable per request.
- PO Cycle-Time silently excludes any order that is not `COMPLETED` — an order that is still open, or was `FORCE_CLOSED` before full receipt, contributes no row and no days to any average.
- Depreciation Summary excludes `DISPOSED` assets entirely from the rollup, but assets lacking full depreciation inputs (e.g. no useful life set) still count toward `assetCount`/`totalPurchaseValue` while contributing exactly zero (not null, not omitted) to the two depreciation-derived totals.
- Budget vs. Actual sums every currently-active budget per location regardless of period overlap — two active-but-different-period budgets for the same location are combined into one row, not shown separately (see SRS.md §6 known gap).
- Price Comparison performs no currency normalization — rows from suppliers quoting in different `currencyCode`s are shown and sorted by raw `effectivePrice` as-is, which is only a valid comparison when every row shares the same currency.
- None of the seven screens paginate — each returns its full rollup (bounded by the number of categories/buckets/suppliers/locations, never by transaction volume) in one response.

## 8. Known Gaps / Deferred
See SRS.md §6 — no export on any report, no custom date-range/grouping controls beyond each report's fixed default shape, no scheduling/caching/historical snapshotting, Budget vs. Actual's period-unaware summing across concurrent active budgets, Price Comparison's lack of currency conversion, and PO Cycle-Time's exclusion of `FORCE_CLOSED` orders.
