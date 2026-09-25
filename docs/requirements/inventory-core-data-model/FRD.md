# Functional Requirements Document — Inventory Core Data Model

## 1. Overview
This sub-module implements the catalog, location, and stock-ledger foundation used by every other Inventory Management sub-module. It has no external-facing "document" of its own (no PO, no requisition) — its outputs are master data (Category/UOM/Product/Location) and the Stock Ledger/Balance records that downstream transactional sub-modules post into.

## 2. Actors & Permissions
All permissions are DB-managed rows (no hard-coded roles); the table below lists the exact codes found in the backend (`com.cms.inventory.catalog`, `com.cms.inventory.stock`).

| Permission Code | Gates |
|---|---|
| `INVENTORY_CATEGORY_VIEW` / `_MANAGE` | Category master + nested CategoryAttribute CRUD |
| `INVENTORY_UOM_VIEW` / `_MANAGE` | UOM master |
| `INVENTORY_UOM_TEMPLATE_VIEW` / `_MANAGE` | UOM Conversion Template master |
| `INVENTORY_BRAND_VIEW` / `_MANAGE` | Brand master |
| `INVENTORY_PRODUCT_VIEW` / `_MANAGE` | Product CRUD, aliases, attribute values, UOM chain versions, barcode |
| `INVENTORY_PRODUCT_IMAGE_MANAGE` | Product image upload/delete/set-primary (view piggybacks on Product view/manage) |
| `INVENTORY_PRODUCT_VARIANT_VIEW` / `_MANAGE` | Product Variant CRUD |
| `INVENTORY_PRODUCT_REGENERATE_CODES` | Bulk regenerate-all-product-codes admin action (never bundled into `_MANAGE`) |
| `INVENTORY_LOCATION_VIEW` / `_MANAGE` | Inventory Location CRUD |
| `INVENTORY_RACK_VIEW` / `_MANAGE` | Rack CRUD |
| `INVENTORY_BIN_VIEW` / `_MANAGE` | Bin CRUD |
| `INVENTORY_STOCK_VIEW` / `_MANAGE` | Record Stock Movement, Stock Balance list/bin-allocations |
| `INVENTORY_STOCK_CONVERT_VARIANT` | Convert-to-variant action on a stranded null-variant balance (dedicated, not `_MANAGE`) |
| `INVENTORY_CYCLE_COUNT_VIEW` / `_MANAGE` / `_APPROVE` | Cycle Count CRUD / variance approve-reject |
| `INVENTORY_REORDER_CONFIG_VIEW` / `_MANAGE` | Per-(Product, Location) Reorder Configuration CRUD |

Every permission-seeding migration ends with the standing DEV_ADMIN/SUPPORT_ADMIN catch-all sync block; actual role assignment beyond that is done entirely in the DB-driven Role Management module.

## 3. Screens & UI Behavior
- **Category / UOM / Brand / UOM Conversion Template masters** — simple list + form screens; each has real-time async uniqueness validation (`uniqueFieldValidator` + a matching `-exists` backend endpoint) on its natural key (Category name+short code scoped to sibling, UOM code+name, Brand name, Template name).
- **Category Attribute** — nested CRUD under its owning Category's detail screen (no standalone list); data type select (Text/Number/Date/Boolean/Enum) with a comma-separated options field shown only for Enum.
- **Product form** — Code field read-only (live preview while creating via `/products/next-code`); Name uniqueness checked scoped to the selected Category; embedded sections revealed once the product is saved: Aliases, typed Attribute Values (rendered per the Category's attribute schema), Images (upload/delete/set-primary gallery), Unit Hierarchy (apply a UOM Conversion Template or build one from scratch), Variants (table + routed add/edit form), Barcode field with a "Print barcode label" action.
- **Inventory Location form** — Room picker (via the existing Campus Infrastructure service, not the venue-scoped `cms-room-picker`), Virtual Name (globally unique), Location Role select, optional Default Supplying Location.
- **Rack / Bin** — nested masters scoped to their parent Location/Rack respectively; uniqueness checked per parent, not globally.
- **Record Stock Movement** — reactive form: Product (with Variant select appearing only once the product has active variants, required in that case), Location, Txn Type (Receipt/Adjustment/Disposal), quantity, optional unit cost, optional batch/serial number + expiry date.
- **Stock Balance list** — Material table with paginator; shows an "Unassigned" badge (`cms-badge--soft-amber`) and a "Convert" row action only on a genuinely stranded null-variant row for a product that has active variants (`productHasActiveVariants` flag distinguishes this from an ordinary non-variant product's balance).
- **Cycle Count list/new/detail** — DRAFT (blind counting) → SUBMITTED (variance computed, approve/reject per line) → COMPLETED; CANCELLED only reachable from DRAFT.
- **Reorder Configuration list/form** — filtered to `REQUESTING_POINT`/`BOTH` locations; a "drifted" warning tag ("On, no store set") shown when auto-indent is enabled but the location's default supplying location has since been unset.

## 4. Functional Workflows

### 4.1 Catalog setup
Category (optionally nested) → CategoryAttribute definitions → UOM/UOM Conversion Template → Brand → Product (auto-coded, Category+UOM required) → optional Images/Variants/Barcode.

### 4.2 Stock movement posting
Every write to Stock Ledger/Stock Balance goes exclusively through one service method (`StockMovementService.recordMovement`), regardless of which sub-module's screen triggered it. Steps: resolve Product (+ Variant if the product has any active variant — otherwise rejected), resolve/auto-create Stock Batch if a batch/serial number is given, enforce `trackingMode` compliance, compute the qty delta by transaction type (direction-based for Adjustment/Transfer/Return; decrease-only for Disposal/Issue/Return-to-supplier), reject if the resulting balance would go negative, append the Stock Ledger row, upsert the Stock Balance row.

### 4.3 Cycle Count
Create (DRAFT, full-location snapshot or ad-hoc lines, `systemQtySnapshot` fixed at line-creation time) → count each line blind → Submit (variance computed: zero-variance lines auto-close as MATCHED) → Approve (posts an ADJUSTMENT via the same `recordMovement` path) or Reject (dismissed as a counting error, no stock change) each nonzero-variance line → header auto-completes once every line is terminal. Only the unbatched balance is ever auto-posted; a genuinely batch-spread variance surfaces a clear error pointing the user at Record Stock Movement instead.

### 4.4 Convert-to-Variant
On a stranded null-variant Stock Balance row (nonzero quantity, no variant, product now has active variants): pick a target active variant belonging to the same product → service posts two ordinary ADJUSTMENT ledger rows in one transaction (decrease on the null-variant bucket, increase on the variant bucket, carrying a system-authored note) → both balance rows upserted → the null-variant row is left at zero, never deleted, as a permanent "Unassigned" bucket.

## 5. API Endpoints
See SRS.md §4.2 for the full list of paths. All are permission-gated per §2 above; list endpoints (`/page`) accept standard search/sort/pagination query parameters consistent with the rest of the app's Material-table screens.

## 6. Data Model
See SRS.md §4.3 for the table list and the ER diagram (`ER_DIAGRAM_AND_MODULE_BOUNDARIES.md` §2, §4) for relationships. Notable real-world column shapes confirmed directly from the entity source:
- `Product`: `productCode` (unique), `productName` (unique per category), `barcode` (unique when present, partial index), `category`/`baseUom`/`brand` FKs, `reorderLevel`/`reorderQty`/`standardCost`/`listPrice` (`BigDecimal`), `hsnSacCode`, `defaultTaxRuleId` (bare `Long`, no JPA relation — deliberate cross-package-boundary decision), `isAsset`/`isConsumable`/`isService`/`isLoanable` flags, `trackingMode` enum (`NONE`/`BATCH`/`SERIAL`), `depreciationRate`, `warrantyPeriodMonths`, `lengthCm`/`widthCm`/`heightCm`/`weightKg`, `isActive`.
- `InventoryLocation`: `room` FK (non-nullable), `virtualName` (globally unique), `locationRole` enum, `defaultSupplyingLocation` (self-FK), `isActive`.
- `StockLedger`: `product`/`location`/`batch`/`variant` FKs, `txnType` enum, `qtyDelta`, `unitCost`, `refType`/`refId`, `notes`, `performedBy`, `txnDate` — append-only, no update columns.
- `StockBalance`: same key shape as `StockLedger` minus `txnType`, plus `qtyOnHand`/`valueOnHand`/`lastUpdated` — unique on `(product, variant, location, batch)` with `NULLS NOT DISTINCT`.
- `CycleCount`/`CycleCountLine`: header status enum (DRAFT/SUBMITTED/COMPLETED/CANCELLED), line status enum (PENDING_COUNT/MATCHED/PENDING_REVIEW/APPROVED/REJECTED), `systemQtySnapshot`/`countedQty`/`varianceQty`, resolution actor/timestamp/notes, optional `bin` FK.
- `InventoryRack`/`InventoryBin`: simple parent-scoped masters (`location_id`/`rack_id` FK, name+code unique per parent).
- `ProductLocationReorderConfig`: `product`/`location` FKs, `reorderLevel`/`reorderQty`/`maxStockQty`, `autoIndentEnabled` flag.

## 7. Edge Cases & Validation Rules
- A movement for a product with any active variant and no variant specified is rejected.
- A `SERIAL`-tracked movement for quantity other than 1 is rejected.
- A movement that would drive `qtyOnHand` negative is rejected before any write.
- Cycle Count approval on a variance that spans multiple batches (cannot be attributed to a single unbatched balance) surfaces a clear error rather than silently misattributing the adjustment.
- Saving a Reorder Configuration with `autoIndentEnabled = true` on a location with no default supplying location set is rejected at save time; the reverse (unsetting the store afterward) is allowed and surfaced as a drift warning, not blocked.
- Regenerating all product codes hard-stops, listing every under-configured category, if any category referenced by an existing product has no `shortCode` yet.
- Product barcode uniqueness is enforced only when a barcode is actually set (partial unique index) — blank/absent barcodes never collide.

## 8. Known Gaps / Deferred
See SRS.md §6 — the legacy `InventoryItem` migration (retired instead, not migrated), no Zone-level locations, weighted-average-only costing, no Stock Ledger browsing screen, no automatic/bulk stranded-balance conversion, and the cross-module batch/serial gap on Requisition & Issue add-line forms. The `InventoryRack`/`InventoryBin`/`StockBinAllocation` schema also has no corresponding `DECISION_LOG.md` design-rationale entry, unlike every other slice in this module.
