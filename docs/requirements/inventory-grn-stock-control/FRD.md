# Functional Requirements Document — Goods Receipt & Stock Control

## 1. Overview
This sub-module implements the three documents that turn an ordered Purchase Order into physical, valued stock and keep it moving correctly afterward: Goods Receipt (receive against a PO), Stock Transfer (move between two locations), and Return to Supplier (send previously-received stock back). Every stock-affecting action in this sub-module posts through the Core Data Model sub-module's single `StockMovementService.recordMovement` — none of these three services writes `StockLedger`/`StockBalance` directly.

## 2. Actors & Permissions
All permissions are DB-managed rows (no hard-coded roles); the table below lists the exact codes found in the backend (`com.cms.inventory.receiving`, `com.cms.inventory.stock`).

| Permission Code | Gates |
|---|---|
| `INVENTORY_GRN_VIEW` / `_MANAGE` / `_CONFIRM` | Goods Receipt CRUD (view/manage) and the stock-posting Confirm action (its own permission) |
| `INVENTORY_GRN_REGENERATE_NUMBERS` | Bulk regenerate-all-receipt-numbers admin action (never bundled into `_MANAGE`) |
| `INVENTORY_STOCK_TRANSFER_VIEW` / `_MANAGE` | Stock Transfer CRUD, complete, cancel — no separate "complete" permission (judged not to warrant a narrower split, see BRD/decision log) |
| `INVENTORY_SUPPLIER_RETURN_VIEW` / `_MANAGE` | Return to Supplier CRUD, complete, cancel — same reasoning as Stock Transfer |
| `INVENTORY_SUPPLIER_RETURN_REGENERATE_NUMBERS` | Bulk regenerate-all-return-numbers admin action |

Every permission-seeding migration ends with the standing DEV_ADMIN/SUPPORT_ADMIN catch-all sync block; actual role assignment beyond that is done entirely in the DB-driven Role Management module.

## 3. Screens & UI Behavior
- **Goods Receipts list/new/detail** — new/detail screens pick a Purchase Order, then add lines from that order's `receivable-lines` (only lines with open quantity remaining); each line captures quantity (with an optional UOM-level entry that converts to base), unit cost (pre-filled from the PO line), optional batch/serial + expiry, optional bin. `DRAFT` receipts support add/remove-line and Confirm; `CONFIRMED` receipts are read-only. List/detail show the generated `receiptNumber` (`GRN-<YYYYMM>-00001`) alongside `#{id}`.
- **Stock Transfers list/new/detail** — new/detail pick a source and destination `InventoryLocation`, then add lines (product, variant if applicable, quantity); the New form does not filter either location dropdown by role, relying on the backend's symmetric location-pair gate and the generic error-toast wiring to surface a rejection. `DRAFT` transfers support add/remove-line, Complete, Cancel.
- **Supplier Returns list/new/detail** — new/detail pick a `CONFIRMED` Goods Receipt, then add lines from that receipt's `returnable-lines` (only lines with returnable quantity remaining), an optional structured `reason` picklist at header level. `DRAFT` returns support add/remove-line, Complete, Cancel. List/detail show the generated `returnNumber` (`SR-00001`) alongside `#{id}`.
- **Status badges** — `GoodsReceiptStatus.CONFIRMED` renders via the shared `cms-status-badge` terminal-success-state bucket (fixed 2026-09-16, was previously falling to the unstyled default case). `SupplierReturnStatus`/`StockTransferStatus` (`DRAFT`/`COMPLETED`/`CANCELLED`) render via the same shared component's standard draft/success/cancelled buckets.

## 4. Functional Workflows

### 4.1 Goods Receipt: create → confirm
Create `DRAFT` Goods Receipt against a Purchase Order → add lines, each against a specific receivable `PurchaseOrderItem` (quantity blocked from exceeding that line's still-open ordered quantity, checked against the PO line's live `receivedQty` plus this draft's own already-added lines) → **Confirm**: for each line, resolve/auto-create a `StockBatch` if a batch/serial number was given, post a `RECEIPT` movement through `StockMovementService.recordMovement` at the order's own `InventoryLocation` (inheriting the line's variant from `PurchaseOrderItem.getVariant()`), update `PurchaseOrderItem.receivedQty`, then call `PurchaseOrderService.recalculateReceiptProgress` to roll the order's status forward. A fresh re-check against the PO line's live `receivedQty` runs again at confirm time regardless of how many concurrent drafts existed, so no stock can ever actually over-post even though a draft's own displayed "still open" quantity can go stale if a sibling draft confirms first.

### 4.2 Stock Transfer: create → complete
Create `DRAFT` transfer with a source and destination location → add lines (product + variant if applicable + quantity) → **Complete**: for each line, resolve the source location's current unbatched weighted-average unit cost (`valueOnHand / qtyOnHand` on its `StockBalance` row) and pass that same cost to two `recordMovement` calls in the same transaction — a `DECREASE` at the source, an `INCREASE` at the destination — so the item's value is preserved end to end; the existing negative-stock guard inside `recordMovement` is the only check for "not enough stock to transfer" (no separate pre-check). Location-pair validation (`requireAtLeastOneStore`) rejects only a transfer where both source and destination are `REQUESTING_POINT`.

### 4.3 Return to Supplier: create → complete
Create `DRAFT` return against a `CONFIRMED` Goods Receipt → add lines, each against a specific returnable `GoodsReceiptLine` (quantity blocked from exceeding that line's received quantity net of quantity already returned across all returns) → **Complete**: for each line, post a `RETURN` movement (`DECREASE` direction, inheriting the variant from `receiptLine.getPurchaseOrderItem().getVariant()`) through `StockMovementService.recordMovement`, then net the quantity back out of the source `PurchaseOrderItem.receivedQty` and call `PurchaseOrderService.recalculateReceiptProgress` again — which, unlike its original Phase 3 behavior, now correctly treats only `FORCE_CLOSED` as truly terminal, so a previously `COMPLETED` order can revert to `PARTIALLY_COMPLETED`/`IN_PROGRESS`/`ORDERED` as a return lowers its true received quantity.

### 4.4 Sequential document numbering
`ApplicationNumberSequenceService.nextNumberForDate(seriesCode, date)` assigns `receiptNumber`/`returnNumber` in the same transaction as the document's own initial save (`create()`), the same "exactly one save, no after-the-fact PATCH" pattern the Product Code feature established the same day. Goods Receipt's series resets `FINANCIAL_MONTH` (highest volume of the four document types this feature covers); Return to Supplier's series never resets (`NONE`, lowest volume). Each exposes its own permission-gated `/regenerate-numbers/preview` (dry run) and `/regenerate-numbers` (apply) admin action, which resets the touched scope period's counter and reassigns gap-free numbers in chronological order — safely reflowing already-numbered rows, not just filling blanks.

## 5. API Endpoints
See SRS.md §4.2 for the full list of paths. All are permission-gated per §2 above; list endpoints (`/page`) accept standard search/sort/pagination query parameters consistent with the rest of the app's Material-table screens.

## 6. Data Model
See SRS.md §4.3 for the table list. Notable real-world column shapes confirmed directly from entity source:
- `GoodsReceipt`: `receiptNumber` (nullable, sequential display field), `purchaseOrder` FK, `status` enum (`DRAFT`/`CONFIRMED`), `receiptDate`, `notes`, `createdBy`/`createdAt`, `confirmedBy`/`confirmedAt`, `updatedAt`.
- `GoodsReceiptLine`: `goodsReceipt`/`purchaseOrderItem` FKs, `receivedQty` (always base-unit), `uomLevel` FK (nullable, display/audit only) + `enteredQty` (the raw as-typed quantity in that level), `unitCost`, `batchOrSerialNo`, `expiryDate`, `bin` FK (nullable — stock stays unbinned if null), `notes`.
- `StockTransfer`: `sourceLocation`/`destinationLocation` FKs, `status` enum (`DRAFT`/`COMPLETED`/`CANCELLED`), `transferDate`, `notes`, `createdBy`/`createdAt`, `completedBy`/`completedAt`, `updatedAt`.
- `SupplierReturn`: `returnNumber` (nullable, sequential display field), `goodsReceipt` FK, `status` enum (`DRAFT`/`COMPLETED`/`CANCELLED`), `reason` enum (`DEFECTIVE`/`WRONG_ITEM`/`DAMAGED_IN_TRANSIT`/`QUALITY_ISSUE`/`OTHER`, nullable), `returnDate`, `notes`, `createdBy`/`createdAt`, `completedBy`/`completedAt`, `updatedAt`.
- `StockTransferLine`/`SupplierReturnLine`: line-level product/variant/quantity/notes shape, each scoped uniquely per header (`UNIQUE NULLS NOT DISTINCT (header_id, product_id, variant_id)` on the transfer line).

## 7. Edge Cases & Validation Rules
- A Goods Receipt line cannot be added or confirmed if it would push the PO line's total received quantity past what was ordered (0% tolerance); the authoritative check re-runs at confirm time against the PO line's then-current `receivedQty`.
- A Return to Supplier line cannot exceed its originating receipt line's received quantity net of quantity already returned, with the same own-draft-plus-fresh-recheck pattern as over-receipt.
- A Stock Transfer where both source and destination are `REQUESTING_POINT` locations is rejected; every other combination is allowed.
- A Stock Transfer or Return to Supplier line for a product with any active variant and no variant resolvable (Return to Supplier always inherits it from the original receipt line; Stock Transfer requires it explicitly) is rejected.
- A `BATCH`/`SERIAL`-tracked product cannot be added to a Stock Transfer line — `StockMovementService.requireTrackingModeCompliance` fails since neither `StockTransferAddLineRequest` nor the transfer's `recordMovement` calls carry a batch/serial field (known gap, see §8).
- A Return to Supplier can only be raised against a Goods Receipt whose status is `CONFIRMED`.
- Completing a Stock Transfer relies solely on `recordMovement`'s existing negative-stock guard for "not enough stock at source" — no separate pre-check is duplicated in `StockTransferService`.

## 8. Known Gaps / Deferred
See SRS.md §6 — no true 3-way match (no `Invoice` entity at all), weighted-average-only valuation, batch/serial-tracked products cannot be transferred or returned (only received, adjusted, or disposed), a draft's displayed "still open" PO/receipt-line quantity can go stale against a sibling draft (though the fresh re-check at confirm/complete time always prevents an actual over-post), and no configurable over-receipt/over-return tolerance.
