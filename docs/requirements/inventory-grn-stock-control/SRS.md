# Software Requirements Specification — Goods Receipt & Stock Control

**Module:** Inventory Management (OneCMS) — Sub-module 3 of 7
**Corresponds to:** Release 3 Milestone R3-M3 ("GRN & Stock Control")
**Status as documented:** Done (100%)

> Standalone, industry-agnostic sub-module — see the parent module note in `inventory-core-data-model/SRS.md` §0. No vertical (hospital/college) naming appears anywhere in this sub-module's schema or UI copy.

## 1. Introduction

### 1.1 Purpose
Defines the requirements for turning a Purchase Order into physical stock on hand, moving stock laterally between locations, and returning previously-received stock to the supplier it came from: Goods Receipt (two-step draft→confirm posting), Stock Transfer, and Return to Supplier.

### 1.2 Scope
GoodsReceipt/GoodsReceiptLine, StockTransfer/StockTransferLine, SupplierReturn/SupplierReturnLine, and the sequential document-numbering feature that later gave Goods Receipt and Return to Supplier a human-readable number. Out of scope for this document: the Purchase Order these receipts are raised against (Procurement & Vendor Management sub-module), the core append-only Stock Ledger/Stock Balance write path itself (Core Data Model sub-module — every posting in this sub-module goes through it, none write ledger/balance rows directly), and internal (non-supplier) returns of previously-issued stock (Requisition & Issue sub-module).

### 1.3 References
- `docs/inventory-management/MILESTONES.md` Phase 3
- `docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §1 (3.3/3.4 source-SRS sections), GAP-07 (stock ledger), GAP-08 (valuation), GAP-11 (vendor RMA)
- `docs/inventory-management/DECISION_LOG.md`: 2026-09-08 "Goods Receipt slice", "Stock Transfer slice", "Return to Supplier slice"; 2026-09-11 "Wire ProductVariant into..."; 2026-09-15 "Location-role gate on Stock Issue Request / Stock Transfer, plus a zero-cost Internal Return fix", "Bulk demo data for Stock Management..."; 2026-09-16 "Overnight cron never fired..." (GoodsReceiptStatus badge fix); 2026-09-22 "Sequential document numbers for Quotation Request, Purchase Order, Goods Receipt, Return to Supplier"
- Source SRS: `docs/inventory-management/source/SRS_v3.2_Updated.pdf` §3.3 (I001–I028), §3.4 (G001–G0xx)

**Reconciliation against the source SRS:** the source SRS's §3.4 GRN & Invoice Management names 3-way match (PO ↔ GRN ↔ Invoice) as a requirement, and §3.3 names FIFO/FEFO issue valuation. Neither is built as specified. **No `Invoice` entity or invoice-matching step exists anywhere in this sub-module or the wider Inventory backend** (confirmed by a full-codebase search) — what is built is a real, enforced **2-way match**: a `GoodsReceiptLine` always requires an existing `PurchaseOrderItem`, and over-receipt against that PO line is blocked outright at 0% tolerance, both while the receipt is still `DRAFT` and again, authoritatively, at `confirm` time. A true 3-way match against a supplier invoice is not implemented and is not merely deferred paperwork — there is no invoice concept in the schema to match against. Valuation on the decrease side of every movement in this sub-module (Stock Transfer's source leg, Supplier Return) uses **weighted-average cost**, the same deliberate simplification the Core Data Model sub-module documents, not FIFO/FEFO. Batch/expiry capture (source SRS I001-series) is implemented on the Goods Receipt line itself, reusing the Core Data Model's optional batch/serial fields rather than a dedicated batch-entry screen.

## 2. Overall Description

### 2.1 Product Perspective
Standalone module component; reuses the Core Data Model sub-module's `Product`/`ProductVariant`, `InventoryLocation`, `StockBatch`, and `StockMovementService` (the sole writer of every `StockLedger`/`StockBalance` row — this sub-module never writes those tables directly), and the Procurement & Vendor Management sub-module's `PurchaseOrder`/`PurchaseOrderItem`. Goods Receipt and Return to Supplier live in their own bounded-context package (`com.cms.inventory.receiving`), distinct from Procurement even though they read its entities directly; Stock Transfer lives alongside the Core Data Model's own stock entities in `com.cms.inventory.stock`, since it reads and writes nothing outside that package's own `InventoryLocation`/`StockBalance`.

### 2.2 User Classes
Receiving/store staff (confirm Goods Receipts, action Stock Transfers, raise Returns to Supplier). All role-to-permission mapping is DB-driven, not hard-coded.

### 2.3 Operating Environment
Same stack as the rest of the module (Angular + Spring Boot + PostgreSQL/Flyway + Keycloak). Backend packages `com.cms.inventory.receiving` (Goods Receipt, Return to Supplier) and `com.cms.inventory.stock` (Stock Transfer); frontend `frontend/src/app/features/inventory/receiving/` and `frontend/src/app/features/inventory/stock/stock-transfer/`.

### 2.4 Constraints & Assumptions
- **A Goods Receipt line always requires an existing `PurchaseOrderItem`** — there is no direct, unplanned receipt with no PO behind it in this sub-module.
- **Unbatched stock only for Stock Transfer** — no `batchOrSerialNo` field on a transfer line; a batch/serial-tracked product cannot be transferred through this screen (see §6).
- **0% over-receipt / over-return tolerance** — a real delivery running over what was ordered, or a return exceeding what was actually received net of prior returns, is blocked outright rather than allowed with a warning, in the absence of a documented tolerance policy.
- **No approval gate is native to any of the three documents** — each has a plain status lifecycle; real multi-level approval, if ever applied to these documents, is a separately-started routing from the Approvals sub-module.
- **Return to Supplier can only be raised against a `CONFIRMED` Goods Receipt** — a still-`DRAFT` receipt has posted no stock yet, so there is nothing real to return.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-GSC-1 | Create a **Goods Receipt** (`DRAFT`) against one Purchase Order, add/remove lines each against a specific receivable `PurchaseOrderItem` with quantity (optionally entered in a non-base UOM level, converted to base), unit cost (defaulting from the PO line's own price), optional batch/serial number + expiry date, and optional bin. | Must | Procurement FR-PV-9, Core Data Model FR-CDM-11/FR-CDM-13 |
| FR-GSC-2 | Block adding/confirming a line whose quantity, combined with the PO line's already-`receivedQty`, would exceed the ordered quantity (0% over-receipt tolerance), re-checked authoritatively at confirm time against the PO line's live `receivedQty`. | Must | FR-GSC-1 |
| FR-GSC-3 | **Confirm** a `DRAFT` Goods Receipt: post a `RECEIPT` stock movement per line through `StockMovementService`, update each line's `PurchaseOrderItem.receivedQty`, and recompute the parent Purchase Order's status (`PENDING → ORDERED → IN_PROGRESS → PARTIALLY_COMPLETED → COMPLETED`) via `PurchaseOrderService.recalculateReceiptProgress`. A confirmed receipt's lines are immutable. | Must | FR-GSC-1, FR-GSC-2 |
| FR-GSC-4 | Move stock between two Inventory Locations via a **Stock Transfer**: `DRAFT` (build lines, product+optional variant+quantity per line) → **Complete** (posts a decrease-at-source / increase-at-destination movement pair per line, carrying the source's own current weighted-average unit cost across so value is never silently zeroed) → no further transition; `CANCELLED` reachable only from `DRAFT`. | Must | Core Data Model FR-CDM-9, FR-CDM-11 |
| FR-GSC-5 | Gate a Stock Transfer's source/destination location pair: reject only when **both** locations are `REQUESTING_POINT` (sister-to-sister, bypassing the store entirely) — either side being `STORE`/`BOTH` is valid in either direction. | Must | FR-GSC-4 |
| FR-GSC-6 | Raise a **Return to Supplier** against one `CONFIRMED` Goods Receipt: `DRAFT` (build lines, each against a specific returnable receipt line, up to its received quantity net of prior returns) → **Complete** (posts a decreasing `RETURN` stock movement per line, and nets the returned quantity back out of the source `PurchaseOrderItem.receivedQty`, correctly reverting the parent PO's status from `COMPLETED` back to `PARTIALLY_COMPLETED`/`IN_PROGRESS`/`ORDERED` as appropriate); `CANCELLED` reachable only from `DRAFT`. | Must | FR-GSC-3 |
| FR-GSC-7 | Block any Return to Supplier line quantity that would exceed the originating receipt line's received quantity net of quantity already returned (0% over-return tolerance), with the same own-draft-only guard plus a fresh re-check at complete time. | Must | FR-GSC-6 |
| FR-GSC-8 | Capture an optional structured **reason** (`DEFECTIVE`/`WRONG_ITEM`/`DAMAGED_IN_TRANSIT`/`QUALITY_ISSUE`/`OTHER`) on a Return to Supplier header; not mandatory. | Should | FR-GSC-6 |
| FR-GSC-9 | Require a variant selection on any Goods Receipt/Stock Transfer/Return to Supplier line whose product has active variants; a Goods Receipt line has no variant field of its own and always inherits the variant from the `PurchaseOrderItem` it receives against, and a Return to Supplier line inherits the variant from the original receipt line it returns. | Must | Core Data Model FR-CDM-7 |
| FR-GSC-10 | Generate a sequential, human-readable document number for Goods Receipt (`GRN-<YYYYMM>-00001`, monthly reset) and Return to Supplier (`SR-00001`, never resets), with an on-demand, permission-gated Regenerate Numbers admin action per document type (preview + apply, gap-free chronological reflow). | Should | — |

## 4. External Interface Requirements

### 4.1 Key Screens (frontend/src/app/features/inventory/...)
Goods Receipts (list/new/detail with confirm action), Stock Transfers (list/new/detail with complete/cancel actions), Supplier Returns (list/new/detail with complete/cancel actions) — all under the "Stock Management" nav group's "Inbound / lateral movement" section.

### 4.2 Key API Endpoints
- `/inventory/receiving/goods-receipts` (POST, `/page`, GET`{id}`, `/receivable-lines`, `/{id}/lines` POST/DELETE`{lineId}`, `/{id}/confirm`, `/regenerate-numbers/preview`, `/regenerate-numbers`)
- `/inventory/stock/transfers` (POST, `/page`, GET`{id}`, `/{id}/lines` POST/DELETE`{lineId}`, `/{id}/complete`, `/{id}/cancel`)
- `/inventory/receiving/supplier-returns` (POST, `/page`, GET`{id}`, `/returnable-lines`, `/{id}/lines` POST/DELETE`{lineId}`, `/{id}/complete`, `/{id}/cancel`, `/regenerate-numbers/preview`, `/regenerate-numbers`)

### 4.3 Key DB Entities
`goods_receipts`, `goods_receipt_lines`, `stock_transfers`, `stock_transfer_lines`, `supplier_returns`, `supplier_return_lines`.

## 5. Non-Functional Requirements
- **Security/RBAC:** `INVENTORY_GRN_VIEW`/`_MANAGE`/`_CONFIRM`/`_REGENERATE_NUMBERS`; `INVENTORY_STOCK_TRANSFER_VIEW`/`_MANAGE`; `INVENTORY_SUPPLIER_RETURN_VIEW`/`_MANAGE`/`_REGENERATE_NUMBERS` — confirm/regenerate-numbers each their own dedicated permission per the operation-wise permission mapping rule (see FRD §2). Stock Transfer and Supplier Return deliberately have no separate "complete" permission beyond `MANAGE` — completing either only moves/returns stock already accounted for, judged not to warrant a narrower permission the way Goods Receipt's confirm (brings new stock into the system) does.
- **Auditability:** every write reuses the shared `audit_log` table; every stock-affecting action posts through `StockMovementService` onto the permanent, append-only Stock Ledger.
- **Data integrity:** over-receipt/over-return are rejected at the service layer, re-checked fresh against the live PO/receipt line at confirm/complete time (not just against the current document's own draft lines) so no race between two open drafts can ever actually over-post.
- **Performance:** `PurchaseOrderService.recalculateReceiptProgress` and the returnable/receivable-lines lookups use dedicated repository queries rather than in-memory computation over full entity graphs.

## 6. Known Gaps / Not Yet Implemented
- **No true 3-way match (PO ↔ GRN ↔ Invoice)** — there is no `Invoice` entity anywhere in the Inventory backend; only a 2-way PO-quantity-vs-received-quantity match is enforced. GAP-11's vendor RMA workflow is otherwise closed by Return to Supplier, but invoice-driven matching is not built at all, not merely deferred.
- **No FIFO/FEFO issue valuation** — weighted-average cost is used on every decrease/transfer leg, consistent with the Core Data Model sub-module's own documented simplification.
- **Batch/serial-tracked products cannot be transferred or returned through Stock Transfer/Return to Supplier** — neither `StockTransferLine` nor the goods-receipt-line inheritance path carries a batch/serial field on the transfer side; a `BATCH`/`SERIAL`-tracked product can be received, adjusted, or disposed, but not transferred (confirmed as a real, unfixed cross-cutting gap first hit while seeding demo data, 2026-09-15).
- **A partial `DRAFT` receipt racing another open draft against the same PO line** shows a quantity that can turn invalid if the other draft confirms first — the fresh re-check at `confirm` time always prevents any actual over-post, but the draft's own displayed "still open" quantity is not live-updated against sibling drafts.
- **No configurable over-receipt/over-return tolerance** — 0% is hard-coded; a tolerance policy was explicitly deferred pending a real business requirement rather than guessed at.
