# Functional Requirements Document — Procurement & Vendor Management

## 1. Overview
Implements the buy-side chain: supplier/rate/tax/currency reference data, demand capture (Purchase Requisition, Wanted List), optional competitive quoting (Quotation Request), and Purchase Order creation through to being sent to a supplier.

## 2. Actors & Permissions

| Permission Code | Gates |
|---|---|
| `INVENTORY_SUPPLIER_VIEW` / `_MANAGE` | Supplier CRUD (view is masked for sensitive fields without `_MANAGE`) |
| `INVENTORY_SUPPLIER_APPROVE` | Supplier approve action (dedicated, not `_MANAGE`) |
| `INVENTORY_RATE_CONTRACT_VIEW` / `_MANAGE` | Rate Contract + nested Rate Contract Line CRUD |
| `INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW` / `_MANAGE` | Vendor Product Mapping CRUD |
| `INVENTORY_TAX_TYPE_VIEW` / `_MANAGE` | Tax Type CRUD |
| `INVENTORY_TAX_RULE_VIEW` / `_MANAGE` | Tax Rule CRUD + nested Tax Sub-Type CRUD (Tax Sub-Type has no permission of its own) |
| `INVENTORY_TAX_JURISDICTION_SETTINGS_VIEW` / `_MANAGE` | Jurisdiction singleton settings |
| `INVENTORY_CURRENCY_SETTINGS_VIEW` / `_MANAGE` | Currency singleton settings |
| `INVENTORY_CURRENCY_EXCHANGE_RATE_VIEW` / `_MANAGE` | Exchange Rate CRUD |
| `INVENTORY_PURCHASE_REQUISITION_VIEW` / `_MANAGE` / `_APPROVE` | Requisition CRUD / per-line approve-reject |
| `INVENTORY_WANTED_LIST_VIEW` / `_MANAGE` / `_RUN` / `_CONVERT` | View / defer-reject-reopen triage / manual "Run Now" trigger / commit to a Requisition |
| `INVENTORY_QUOTATION_VIEW` / `_MANAGE` / `_AWARD` | RFQ CRUD / awarding a line to a winning quote |
| `INVENTORY_QUOTATION_REGENERATE_NUMBERS` | Bulk renumber Quotation Requests (dedicated, not `_MANAGE`) |
| `INVENTORY_PURCHASE_ORDER_VIEW` / `_MANAGE` / `_FORCE_CLOSE` | PO CRUD / early force-close |
| `INVENTORY_PURCHASE_ORDER_REGENERATE_NUMBERS` | Bulk renumber Purchase Orders (dedicated, not `_MANAGE`) |

## 3. Screens & UI Behavior
- **Suppliers** — master list/form; Code uniqueness checked live; bank/tax fields shown masked (last 4 chars) to a view-only user; a distinct "Approve" row action, separate from Edit.
- **Rate Contracts** — form with an embedded Product Rate Lines section (uses the shared `cms-product-picker` component) alongside contract-level value cap/terms/renewal date.
- **Vendor Product Rates** — standalone master (not nested under Supplier or Product); a supplier+product pair uniqueness check (`/pair-exists`) surfaces an inline "already exists" error; a resolved "Effective Price" column shows contract-override pricing where applicable, plus a base-currency conversion hint when the mapping's currency differs from the configured base currency.
- **Tax Types / Tax Rules (+ Tax Sub-Types) / Jurisdiction Settings** — small reference masters; Jurisdiction Settings is a single-record settings form ("not configured" state until an admin sets the home state).
- **Currency Settings / Currency Exchange Rates** — singleton settings form + a dated reference-rate list.
- **Purchase Requisitions** — list/new/detail; DRAFT → SUBMITTED → COMPLETED/CANCELLED header, per-line PENDING → APPROVED/REJECTED with resolution notes.
- **Wanted List** — list with Defer/Reject/Reopen/Convert row actions and a manual "Run Now" button; multi-select convert groups several lines for the same location into one new Requisition.
- **Quotation Requests** — list/new/detail; lines sourced only from `APPROVED` requisition items; a Suppliers tab to invite/remove suppliers; a quote-entry grid (line × supplier) editable while a line is still `PENDING`; Award/Reject per line; Convert groups `AWARDED` lines by winning supplier into one PO each.
- **Purchase Orders** — list/new/detail; "available requisition lines" picker (direct-to-PO path) excludes lines already live on an active Quotation Request; Add Line resolves unit price/tax/jurisdiction defaults which remain editable before confirming the line; a document-number "Regenerate Numbers" admin dialog with a preview step.

## 4. Functional Workflows

### 4.1 Purchase Requisition
Create (DRAFT, against a Location) → add/remove lines → Submit (SUBMITTED) → each line independently Approved or Rejected (with resolution notes) → header auto-completes once every line is resolved. CANCELLED reachable only from DRAFT.

### 4.2 Wanted List → Purchase Requisition
Nightly job (or manual Run Now) computes, per (product, location) pair already holding a StockBalance row: `NetRequirement = ReorderLevel − (QtyOnHand + QtyOnOrder)`, where QtyOnOrder sums quantity in open (non-DRAFT, non-REJECTED) Purchase Requisition lines. A shortfall creates a PENDING line with `SuggestedQty = max(Product.reorderQty, NetRequirement)`, snapshotting the inputs used. A (product, location) pair with an already-unresolved line is skipped on the next run. Selected PENDING lines for the same location convert together into one new Purchase Requisition (one line per product); a line may instead be Deferred (reopenable back to PENDING) or Rejected (structured reason + notes).

### 4.3 Quotation Request (RFQ) — optional
Create (DRAFT, against a Location) → add lines (one per APPROVED requisition item — required, not optional) → invite suppliers → Submit (SUBMITTED) → record each supplier's quote per line (editable while PENDING) → Award a line to one supplier's quote (AWARDED) or Reject it → Convert groups every AWARDED-not-yet-ORDERED line by winning supplier and creates one Purchase Order per supplier, reusing `PurchaseOrderService.create`/`addLine` (tax/UOM/variant resolution is not re-derived). CANCELLED reachable only from DRAFT.

### 4.4 Purchase Order
Create against a Supplier + Location → add lines either from available APPROVED requisition lines directly, or via Quotation Request conversion → unit price defaults from the effective Vendor Product Mapping/Rate Contract rate (editable) → tax resolved by comparing the order's Location's institution home state against the Supplier's state (INTERSTATE/INTRASTATE), applying the matching TaxSubType components, snapshotted on the line → Order (send to supplier, `PENDING → ORDERED`) → status advances automatically (`IN_PROGRESS`/`PARTIALLY_COMPLETED`/`COMPLETED`) as Goods Receipts confirm against its lines (GRN sub-module) → Force Close reachable from any non-terminal state with a required reason.

## 5. API Endpoints
See SRS.md §4.2 for the full list. All permission-gated per §2.

## 6. Data Model
See SRS.md §4.3 for the table list. Notable shapes confirmed from entity source:
- `Supplier`: code/name unique, `state` (mandatory, drives jurisdiction resolution), masked bank fields (`bankAccountNumber`/`bankIfscCode`/`bankName`/`bankAccountHolder`), `taxRegistrationId`/`legalRegistrationNo`, `isApproved`/`approvalDate`, `portalAccessEnabled` (reserved, unused).
- `TaxType → TaxRule (tax_rules table) → TaxSubType`: `TaxSubType.jurisdictionMode` (INTERSTATE/INTRASTATE) + `splitPercent`, unique per (taxRule, jurisdictionMode, componentName); split percents must sum to 100 per jurisdiction mode (service-enforced).
- `PurchaseOrderItem`: links both `purchaseRequisitionItem` and `quotationRequestLine` (both nullable — a line traces its origin either way or neither), `variant`/`uomLevel` FKs, `unitPrice`/`taxAmount`/`jurisdictionMode`/`lineTotal`/`receivedQty`; `purchase_order_item_tax_components` child rows snapshot each applied TaxSubType component.
- `WantedListItem`: `qtyOnHandSnapshot`/`qtyOnOrderSnapshot`/`reorderLevelSnapshot`/`suggestedQty` (all snapshotted at generation time), `rejectionReason` enum, `convertedPurchaseRequisition(Item)` trace FKs.
- `QuotationRequest` → `QuotationRequestLine` (→ `purchaseRequisitionItem`, `awardedResponseLine`) → `QuotationRequestSupplier` (bridge) / `QuotationResponseLine` (one per line×supplier, at most one per pair).

## 7. Edge Cases & Validation Rules
- Supplier approval fields are settable only through the dedicated approve endpoint, never the regular create/update path.
- At most one active Vendor Product Mapping per (supplier, product) pair (partial unique index + service check).
- A Rate Contract Line override applies only when `isActive` and today is within `[startDate, endDate]`; otherwise the mapping's own `unitPrice` applies.
- A Wanted List line is skipped from regeneration while an unresolved (PENDING/DEFERRED) line already exists for the same (product, location).
- A requisition item already `ORDERED` (picked into a PO) or live on a non-REJECTED Quotation Request line cannot be picked up a second time by either path.
- Force Close is reachable from `PENDING` (never sent) as well as any in-progress state — there is no separate "cancel a not-yet-sent order" state.
- TaxSubType components are only applied when the Jurisdiction Setting is configured; an unconfigured jurisdiction blocks tax computation on a PO line rather than silently defaulting to a guess.

## 8. Known Gaps / Deferred
See SRS.md §6 — no generalized multi-region tax engine, no accounting connector, no true multi-currency computation, no outbound RFQ transmission, no variant picker on Quotation Request conversion, and the "GAP-02 pickup" decision-log citation in `V477` that does not correspond to an actual log entry.
