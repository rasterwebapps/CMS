# Software Requirements Specification — Requisition & Issue

**Module:** Inventory Management (OneCMS) — Sub-module 4 of 7
**Corresponds to:** Release 3 Milestone R3-M4 ("Requisition & Issue")
**Status as documented:** Done (100%)

> Standalone, industry-agnostic sub-module — see the parent module note in `inventory-core-data-model/SRS.md` §0. No vertical (hospital/college) naming appears anywhere in this sub-module's schema or UI copy.

## 1. Introduction

### 1.1 Purpose
Defines the requirements for requesting and issuing already-on-hand stock from one Inventory Location to another within the organization (as opposed to buying from a supplier), returning issued stock internally, lending out non-consumed items to an individual borrower, and automatically detecting and raising internal restock requests before a requesting location actually runs out.

### 1.2 Scope
StockIndent/StockIndentItem (renamed 2026-09-21 from StockIssueRequest/StockIssueRequestItem), its embedded Internal Return action, LoanableItemIssue, ProductLocationReorderConfig, and AutoIndentService (the nightly/on-demand reorder-breach detection job). Out of scope for this document: buying from a supplier (Procurement & Vendor Management sub-module — a Stock Indent line that cannot be fulfilled from stock can raise a Purchase Requisition there, but the requisition itself is that sub-module's own document), and returning stock to a supplier (GRN & Stock Control sub-module's Return to Supplier, a distinct concept from this sub-module's Internal Return).

### 1.3 References
- `docs/inventory-management/MILESTONES.md` Phase 4
- `docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §1 (3.3/3.5 source-SRS sections), §2B GAP-26/GAP-28 (loanable item issue), GAP-10 (location model)
- `docs/inventory-management/DECISION_LOG.md`: 2026-09-08 "Stock Issue Request slice", "Auto-restocking (OC-206) skipped...", "Internal Return slice...", "Loanable Item Issue slice..."; 2026-09-11 "Wire ProductVariant into..."; 2026-09-15 "Location-role gate on Stock Issue Request / Stock Transfer, plus a zero-cost Internal Return fix"; 2026-09-21 all four "OC-206 reopened" entries (rename to Stock Indent; per-location reorder config; auto-detection job; two-step approval/fulfillment)
- Source SRS: `docs/inventory-management/source/SRS_v3.2_Updated.pdf` §3.3 (I001–I028, auto-indent portion), §3.5 (N001–N0xx, "ward request"/indent)

**Reconciliation against the source SRS:** the source SRS names this concept "ward request" throughout §3.5, framed as a hospital-only workflow with HOD (Head of Department) approval and, for auto-indent, a Hospital Information System (HIS) integration precondition (`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §2). The as-built system replaces this with a fully generic "requesting location → issuing location" flow with no HIS coupling of any kind — the auto-indent trigger is this sub-module's own per-location reorder configuration, not an external system. The source SRS's auto-indent is a flat min-level trigger with no formal lifecycle beyond issuing; the as-built `StockIndent` gives both manual and auto-generated indents the same real two-decision-point lifecycle (department-head approve/reject, then store fulfill/transfer-in/raise-PO/deny). **Naming note:** the user's own name for this concept is "Indent," not a second, separate document from the originally-built `StockIssueRequest` — the entity, package, permissions, and every reference were renamed in place (2026-09-21), not duplicated.

## 2. Overall Description

### 2.1 Product Perspective
Standalone module component; reuses the Core Data Model sub-module's `Product`/`ProductVariant`, `InventoryLocation` (and its `locationRole` — `STORE`/`REQUESTING_POINT`/`BOTH`), and `StockMovementService`. The auto-indent fulfillment path also reaches into the GRN & Stock Control sub-module's Stock Transfer (transfer-in fulfillment) and the Procurement & Vendor Management sub-module's Purchase Requisition (raise-PO fulfillment). No dependency on any Hospital/Student Information System — a requesting location is a generic `InventoryLocation`.

### 2.2 User Classes
Requesting-location staff (raise/submit indents, borrow loanable items), a department-head-equivalent approver (approve/reject an indent line), store/issuing-location staff (fulfill/transfer-in/raise-PO/deny an approved line, mark a loanable item returned), and a catalog/reorder administrator (per-location Reorder Configuration, on-demand auto-indent run). All role-to-permission mapping is DB-driven, not hard-coded.

### 2.3 Operating Environment
Same stack as the rest of the module (Angular + Spring Boot + PostgreSQL/Flyway + Keycloak). Backend packages `com.cms.inventory.indent` (Stock Indent, Auto-Indent), `com.cms.inventory.issue` (Loanable Item Issue), `com.cms.inventory.stock` (Reorder Configuration); frontend `frontend/src/app/features/inventory/indent/`, `.../issue/loanable-item-issue/`, `.../reorder-config/`.

### 2.4 Constraints & Assumptions
- **Two-tier auto-indent only** (Requesting Point → Store) — a `STORE` location itself running low stays the separate Wanted List's job (Procurement sub-module); multi-tier (Store → a bigger Main Store) is explicitly deferred, not designed.
- **Per-location reorder policy is separate from `Product`'s own global `reorderLevel`/`reorderQty`**, which continue to feed the Wanted List's supplier-side auto-reorder unchanged — the two signals are deliberately not merged.
- **Store-side fulfillment is always manual** — an auto-generated, department-head-approved indent line lands on a decision screen; nothing auto-posts a fulfillment outcome, since no ERP-standard sourcing-preference default could be safely assumed.
- **A product with active variants is skipped by auto-indent detection** — reorder policy stays product-level only; there is no way to guess which variant needs restocking from an aggregate shortage signal.
- **Loanable Item Issue is deliberately not integrated with the Stock Ledger/Stock Balance** — a loaned item isn't consumed, it's expected back; modeling it as an `ISSUE`/`RETURN` stock movement pair would either wrongly remove it from on-hand stock or require a whole new "on-loan quantity" concept, judged real, separately-scoped work.
- **No borrower entity** — a borrower is captured as plain text (name + optional contact), per the standing no-vertical-branding rule; a college's students and a hospital's staff are both just "a borrower" to this generic core.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-RI-1 | Raise a **Stock Indent**: `DRAFT` (add/remove lines — product, optional variant, requested quantity — against a requesting and an issuing location) → **Submit** (`SUBMITTED`) → header auto-completes (`COMPLETED`) once every line reaches a terminal state; `CANCELLED` reachable only from `DRAFT`. | Must | Core Data Model FR-CDM-9 |
| FR-RI-2 | Gate a Stock Indent's location pair: `requestingLocation` must be `REQUESTING_POINT`/`BOTH`; `issuingLocation` must be `STORE`/`BOTH` — a `STORE` never requests, a `REQUESTING_POINT` never issues (an asymmetric gate, unlike Stock Transfer's symmetric one). | Must | FR-RI-1 |
| FR-RI-3 | **Department-head decision point:** approve or reject a `PENDING` line (structured resolution notes) — approval no longer posts any stock movement itself (reversed 2026-09-21; see §4.2), it only advances the line to `APPROVED`, awaiting the store's own decision. | Must | FR-RI-1 |
| FR-RI-4 | **Store fulfillment decision point:** on an `APPROVED` line, the store chooses exactly one of **Fulfill** (direct issue from the issuing location's own stock, posts an `ISSUE` movement), **Fulfill via Transfer** (pulls surplus from a third location into the issuing location first via a real Stock Transfer, then issues — two movements, never a direct hop), **Raise PO** (creates/links a Purchase Requisition line when no stock exists anywhere — terminal for this line; the eventual restock is a separate, disconnected event), or **Deny**. | Must | FR-RI-3, GRN & Stock Control FR-GSC-4, Procurement FR-PV-6 |
| FR-RI-5 | Support an **Internal Return**: on an `APPROVED`-then-`FULFILLED` line, accumulate a `returnedQty` running total (up to `requestedQty`) via a direction-based `RETURN` (`INCREASE`) movement back at the issuing location. | Must | FR-RI-4 |
| FR-RI-6 | Maintain per-(Product, Location) **Reorder Configuration** (reorder level, reorder qty, optional max stock qty, auto-indent enabled flag) scoped to `REQUESTING_POINT`/`BOTH` locations, plus an explicit **default supplying location** field per requesting location; saving with auto-indent enabled requires that supplying location already be set. | Must | Core Data Model FR-CDM-9, FR-CDM-16 |
| FR-RI-7 | Run an **Auto-Indent** detection job (nightly 05:30, offset from the Wanted List's own 05:00 run, plus an on-demand "Run Now"): net `reorderLevel − (qtyOnHand + open indent quantity)` per (product, location) against every active, auto-indent-enabled Reorder Configuration, and bundle every product shortfall for the same (requesting location, supplying store) pair into one new `StockIndent` with multiple lines. | Must | FR-RI-6 |
| FR-RI-8 | Support **Loanable Item Issue**: a single direct **Issue** action (product flagged `isLoanable`, borrower name + optional contact, issue date, expected return date, deposit amount, condition-on-issue) and a single direct **Return** action (actual return date, condition-on-return) — no draft/submit workflow. "Overdue" is computed at read time from `expectedReturnDate`, never stored. | Should | Core Data Model FR-CDM-4 |

## 4. External Interface Requirements

### 4.1 Key Screens (frontend/src/app/features/inventory/...)
Reorder Configuration (list/form), Stock Indents (list/new/detail with submit, approve/reject, fulfill/fulfill-via-transfer/raise-PO/deny, and internal-return actions), Loanable Item Issues (list/new with issue action, detail with return action) — all under the "Stock Management" nav group's "Outbound movement" section.

### 4.2 Key API Endpoints
- `/inventory/stock/reorder-configs` (POST, `/page`, GET`{id}`, PUT`{id}`, DELETE`{id}`, `PATCH {id}/status`, `/pair-exists`)
- `/inventory/indent/stock-indents` (POST, `/page`, GET`{id}`, `/{id}/lines` POST/DELETE`{lineId}`, `/{id}/submit`, `/{id}/lines/{lineId}/approve`, `/{id}/lines/{lineId}/reject`, `/{id}/lines/{lineId}/fulfillment-context`, `/{id}/lines/{lineId}/fulfill`, `/{id}/lines/{lineId}/fulfill-via-transfer`, `/{id}/lines/{lineId}/raise-po`, `/{id}/lines/{lineId}/deny`, `/{id}/lines/{lineId}/return`, `/{id}/cancel`, `/auto-run`)
- `/inventory/issue/loanable-item-issues` (POST, `/page`, GET`{id}`, `/{id}/return`)

### 4.3 Key DB Entities
`product_location_reorder_configs`, `stock_indents`, `stock_indent_items`, `loanable_item_issues`.

## 5. Non-Functional Requirements
- **Security/RBAC:** `INVENTORY_REORDER_CONFIG_VIEW`/`_MANAGE`; `INVENTORY_STOCK_INDENT_VIEW`/`_MANAGE`/`_APPROVE`/`_FULFILL`/`_RETURN`/`_AUTO_RUN`; `INVENTORY_LOAN_ISSUE_VIEW`/`_MANAGE`/`_RETURN` — one permission per decision point (approve covers both approve/reject outcomes; fulfill covers all four store outcomes), not per possible outcome, per the operation-wise permission mapping rule (see FRD §2).
- **Auditability:** every write reuses the shared `audit_log`; every stock-affecting action posts through `StockMovementService` onto the permanent, append-only Stock Ledger; the store's own fulfillment decision audit trail (`storeDecidedBy`/`storeDecidedAt`/`storeDecisionNotes`) is deliberately kept separate from the department head's own (`resolvedBy`/`resolvedAt`/`resolutionNotes`).
- **Data integrity:** an Internal Return can never exceed `requestedQty − returnedQty` (enforced both in the service and a DB check constraint); Stock Indent's location-pair gate and Reorder Configuration's auto-indent-requires-a-supplying-store gate are both enforced at save time.
- **Performance:** `AutoIndentService`'s shortage-candidate and open-quantity-netting queries are native repository queries run directly against real schema/data, not in-memory computation over full entity graphs.

## 6. Known Gaps / Not Yet Implemented
- **A product with active variants is skipped by Reorder Configuration/Auto-Indent** — reorder policy stays product-level only, same simplification the Wanted List already established.
- **A Reorder Configuration whose location later loses its default supplying store is not blocked from re-saving** (only blocked at initial auto-indent-enable time) — it is instead surfaced as a "drifted" warning tag on the list screen ("On, no store set") and skipped for that night's detection run, rather than being auto-corrected or hard-blocked.
- **Batch/serial-tracked products cannot be indented (issued) or internally returned** — neither `StockIndentAddLineRequest` nor the internal-return path carries a batch/serial field, the same cross-cutting gap documented in the GRN & Stock Control and Core Data Model sub-modules.
- **No real "auto-restocking" sourcing-preference automation** — the store's fulfillment decision (own stock / transfer-in / raise PO / deny) is always manual; a version that would automatically prefer transferring from a surplus location over raising a purchase requires real deployment policy input no ERP-standard default could safely assume, and was deliberately left unbuilt (original OC-206 finding, 2026-09-08; resolved into the current manual-decision design on 2026-09-21).
- **Multi-tier auto-indent (Store → a bigger Main Store) is not designed** — only the two-tier Requesting Point → Store trigger exists; a `STORE` location running low is the separate Wanted List's concern.
