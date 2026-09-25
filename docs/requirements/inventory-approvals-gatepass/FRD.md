# Functional Requirements Document — Approvals, Budget & Gate Pass

## 1. Overview
Implements the module's financial-control and physical-movement-control layer: Budget allocation/consumption visibility, a generic sequential/parallel Approval Workflow engine with an exception-bypass path, outward/inward Gate Pass tracking, vendor-owned (Consignment) stock tracking, and internal Service Ticketing.

## 2. Actors & Permissions

| Permission Code | Gates |
|---|---|
| `INVENTORY_BUDGET_VIEW` / `_MANAGE` | Budget CRUD (create/update; no delete endpoint exists) |
| `INVENTORY_APPROVAL_WORKFLOW_VIEW` / `_MANAGE` | Approval Workflow definition CRUD (admin-authoring) |
| `INVENTORY_APPROVAL_VIEW` / `_ACT` | View instances / approve or reject the current stage's action |
| `INVENTORY_APPROVAL_BYPASS` | Bypass (exception override) the current stage regardless of its own required permission |
| `INVENTORY_PURCHASE_REQUISITION_MANAGE` / `INVENTORY_PURCHASE_ORDER_MANAGE` | Also required to start an instance (`/start`) or list eligible workflows (`/eligible`) — the target document's own manage permission |
| `INVENTORY_GATE_PASS_VIEW` / `_MANAGE` / `_APPROVE` / `_VERIFY` / `_RETURN` | View / create / approve-reject / gate-verify / record return (five distinct operations) |
| `INVENTORY_CONSIGNMENT_VIEW` / `_MANAGE` / `_CONVERT` | View / manage agreements + receive stock / record consumption (ownership transfer) |
| `INVENTORY_SERVICE_TICKET_VIEW` / `_MANAGE` / `_ASSIGN` / `_RESOLVE` / `_CLOSE` | View / create+cancel / assign / resolve / close a ticket (four distinct lifecycle operations plus view) |
| `INVENTORY_SERVICE_TICKET_CATEGORY_VIEW` / `_MANAGE` | Ticket Category master CRUD (its own permission pair, not shared with `_TICKET_*`) |

## 3. Screens & UI Behavior
- **Budgets** — list/form; Location + date-range period + allocated amount + notes; list shows live `consumedAmount`/`remainingAmount` and an "Over" flag for `overAllocated` rows. No delete action.
- **Budget vs. Actual** — read-only report (Reporting sub-module), surfaced under the same "Budgets & Approvals" nav group.
- **Approval Workflows** — list/form; document-type selector (`PURCHASE_REQUISITION`/`PURCHASE_ORDER`), optional Location scope, `minAmount` field enabled only for Purchase Order workflows, active flag; embedded ordered step editor (step name, plain text permission-code input validated server-side, `stepOrder`).
- **Approvals** (Approval Instance list/detail) — a "Start Approval" action is available from the target document context (not automatic); detail view lists every stage's `ApprovalAction`, showing not-yet-reached stages as "Pending, not yet at this stage"; Approve/Reject buttons on the current stage only, gated by that step's referenced permission; a separate Bypass action (reason dropdown + notes) gated by `INVENTORY_APPROVAL_BYPASS`.
- **Gate Passes** — list/new/detail; direction (Outward/Inward), returnable toggle, exactly one of Product (via `cms-product-picker`) or Asset (plain `<select>` filtered to `AVAILABLE` assets), quantity, reason, party name/contact, optional linked Purchase Order id (plain numeric field, server-validated) and expected return date; row actions Approve/Reject (reason required, ≤500 chars), Verify at Gate, Return (optional notes); an overdue badge derived at read time for any open, returnable, gate-verified pass past its expected return date.
- **Consignment Agreements** — list/form; Supplier + Location + agreement number + validity window + billing-cycle-days + notes + active flag. No delete action.
- **Consignment Stock** — list of stock lines (agreement × product) showing live `qtyOnHand` (`receivedQty − consumedQty`); Receive action (agreement, product, quantity ≥0.001, consignment price ≥0, notes) posts a real Stock Ledger `RECEIPT`; Consume action (quantity ≥0.001, notes) records ownership transfer without a second stock movement.
- **Service Tickets** — list/new/detail; Location + Category (`cms-product-picker`-style lookup) + requester name + priority (defaults `MEDIUM`) + description (≤1000 chars); row/detail actions Assign, Resolve (notes), Close (optional 1–5 feedback rating), Cancel (reason, only from `OPEN`/`IN_PROGRESS`).
- **Ticket Categories** — simple master, table view only (no card view/sort/query-param persistence, a deliberate simplification versus the fuller `Uom`-style master); real-time name uniqueness via `uniqueFieldValidator` + `/name-exists`; active/inactive status toggle.

## 4. Functional Workflows

### 4.1 Budget
Create against a Location with a date range and allocated amount → `consumedAmount` is computed live as `SUM(PurchaseOrderItem.lineTotal)` for that location's Purchase Orders with `status <> PENDING` (a still-`PENDING` order isn't committed spend; `FORCE_CLOSED` still counts) whose `poDate` falls in the budget's period → `remainingAmount = allocatedAmount − consumedAmount`, `overAllocated = remainingAmount < 0`, surfaced as a visual flag only — nothing blocks further Purchase Order creation.

### 4.2 Approval Workflow → Instance
Admin authors an `ApprovalWorkflow` (document type, optional Location/`minAmount` scope, active flag) with ordered `ApprovalWorkflowStep`s, each naming a real `Permission` code and a `stepOrder`. A user with manage rights on an eligible Purchase Requisition/Order explicitly starts an `ApprovalInstance` (blocked if one is already `IN_PROGRESS` for that document) → every stage's `ApprovalAction` row is created up front (`PENDING`) → the user holding a stage's referenced permission Approves or Rejects the action whose `stepOrder == instance.currentStepOrder` while the instance is `IN_PROGRESS` and the action is `PENDING` → Approve advances `currentStepOrder` to the next distinct value (or completes the instance as `APPROVED` if none remain, once every action sharing the final `stepOrder` has approved); Reject immediately sets the instance to `REJECTED`. A `INVENTORY_APPROVAL_BYPASS` holder may Bypass the current stage instead, supplying a required `ApprovalExceptionReason` (`URGENT_PURCHASE`/`SINGLE_SUPPLIER_SITUATION`/`EMERGENCY`/`APPROVER_UNAVAILABLE`/`OTHER`) + notes, which advances the instance exactly as an approve would (via the shared `markApprovedAndAdvance` path) but stamps the exception reason on the action.

### 4.3 Gate Pass
Create (`PENDING_APPROVAL`) against exactly one of Product/Asset, a Location, direction, returnable flag, party details → Approve (`APPROVED`) or Reject (`REJECTED`, reason required) → Gate Verify (`GATE_VERIFIED` if returnable; `CLOSED` directly if non-returnable) by a security/gate role, distinct from the approver role → for a returnable, gate-verified pass: overdue is derived at read time whenever `expectedReturnDate` has passed and no `actualReturnDate` is recorded yet → Return (`RETURNED`, optional notes) records `actualReturnDate`.

### 4.4 Consignment
Create a `ConsignmentAgreement` (Supplier + Location + validity + billing-cycle term) → Receive posts stock against a `ConsignmentStockLine` for (agreement, product): increments `receivedQty` and immediately posts a real `RECEIPT` movement to the main Stock Ledger via `StockMovementService.recordMovement` (the stock is usable right away even though unpaid) → whenever the business determines vendor-owned stock has actually been consumed (through independent, normal issue/consumption activity elsewhere in the module), a separate `_CONVERT`-permissioned Consume action increments `consumedQty` (validated ≤ `receivedQty`) purely as a financial/audit reconciliation — no second Stock Ledger entry, no auto-generated Purchase Order/GRN.

### 4.5 Service Ticket
Create (`OPEN`, Location + Category + requester + priority + description) → Assign (`IN_PROGRESS`, assignee) → Resolve (`RESOLVED`, resolution notes) → Close (`CLOSED`, optional 1–5 feedback rating). Cancel (`CANCELLED`, reason) is reachable only from `OPEN`/`IN_PROGRESS` — a `RESOLVED` or `CLOSED` ticket cannot be cancelled.

## 5. API Endpoints
See SRS.md §4.2 for the full list. All permission-gated per §2 above.

## 6. Data Model
See SRS.md §4.3 for the table list. Notable shapes confirmed from entity/migration source:
- `Budget`: `location` FK, `periodStartDate`/`periodEndDate`, `allocatedAmount` (precision 14,2), `notes`, `isActive` — no `consumedAmount` column (computed live).
- `ApprovalWorkflow` → `ApprovalWorkflowStep` (`stepOrder`, `stepName`, `permission` FK to the platform's real `Permission` entity) → `ApprovalInstance` (nullable FKs to exactly one of `PurchaseRequisition`/`PurchaseOrder`, `status` [`IN_PROGRESS`/`APPROVED`/`REJECTED`], `currentStepOrder`, `initiatedBy`/`initiatedAt`, `completedAt`) → `ApprovalAction` (`workflowStep` FK, `status` [`PENDING`/`APPROVED`/`REJECTED`], `actedBy`/`actedAt`, `notes`, `exceptionReason` [nullable enum]).
- `GatePass`: `direction` [`OUTWARD`/`INWARD`], `returnable` (boolean), `product`/`asset` (exactly one non-null, DB `CHECK`), `location`, `quantity` (`BigDecimal`, consistent with the rest of the module even for a serialized asset), `reason`, `partyName`/`partyContact`, `linkedPurchaseOrder` (nullable, plain FK, no picker), `passDate`, `expectedReturnDate`/`actualReturnDate`, `status` [`PENDING_APPROVAL`/`APPROVED`/`REJECTED`/`GATE_VERIFIED`/`RETURNED`/`CLOSED`], separate `approvedBy`/`rejectedBy`/`gateVerifiedBy`/`returnedBy` actor+timestamp columns.
- `ConsignmentAgreement`: `supplier`/`location` FKs, `agreementNumber`, `startDate`/`endDate`, `billingCycleDays` (informational only), `notes`, `isActive`.
- `ConsignmentStockLine`: `agreement`/`product` FKs, `consignmentPrice`, `receivedQty`/`consumedQty` (both default `BigDecimal.ZERO`, `qtyOnHand` computed as their difference — not a column), `lastReceivedBy`/`lastReceivedAt`, `lastConsumedBy`/`lastConsumedAt`.
- `ServiceTicketCategory`: `name` (unique), `description`, `isActive` — no `code` field (a deliberate simplification vs. the module's `Uom` master).
- `ServiceTicket`: `location`/`category` FKs, `requestedBy`, `priority` [`LOW`/`MEDIUM`/`HIGH`/`URGENT`, default `MEDIUM`], `status` [`OPEN`/`IN_PROGRESS`/`RESOLVED`/`CLOSED`/`CANCELLED`], `description`, `assignedTo`/`assignedAt`, `resolutionNotes`/`resolutionDate`/`resolvedBy`, `feedbackRating` (1–5, nullable), `closedBy`/`closedAt`, `cancelledBy`/`cancelledAt`/`cancellationReason` — no separate stored ticket number/date (`id`/`createdAt` serve that role, matching Purchase Order/Gate Pass/Consignment Agreement's own "#{id}" convention).

## 7. Edge Cases & Validation Rules
- Starting an Approval Instance is blocked while one is already `IN_PROGRESS` against the same document, but a document that already completed one (`APPROVED`/`REJECTED`) can have a new instance started.
- Approve/Reject/Bypass are only valid when the instance is `IN_PROGRESS`, the target action is `PENDING`, and the action's `stepOrder` equals the instance's `currentStepOrder` — acting on a future or already-resolved stage is rejected.
- A `minAmount` threshold on a workflow only ever gates `PURCHASE_ORDER`-typed workflows; a `PURCHASE_REQUISITION` workflow applies unconditionally.
- Gate Pass requires exactly one of `productId`/`assetId` — both set or both null is rejected by both a DB `CHECK` constraint and the service layer.
- A non-returnable Gate Pass transitions straight to `CLOSED` on gate verification; only a `returnable` pass can reach `GATE_VERIFIED`→overdue→`RETURNED`.
- `ConsignmentStockLine.consumedQty` cannot exceed `receivedQty` — enforced by a DB `CHECK` constraint; the Consume request additionally validates quantity ≥ 0.001.
- Service Ticket Cancel is rejected once the ticket is `RESOLVED` or `CLOSED`.
- Ticket Category names are validated live for uniqueness via `/name-exists` before save (mandatory master-screen uniqueness pattern).
- Every action on every entity in this sub-module stamps its own actor/timestamp columns in addition to writing to the shared `audit_log`.

## 8. Known Gaps / Deferred
See SRS.md §6 — no submit-time approval gate wired into Purchase Requisition/Order services (Approval Instances are opt-in, not automatic or blocking), Budget over-allocation is advisory only, no `PeriodType` flexibility on Budget, no quorum (partial N-of-M) approval threshold, no frontend permission-code picker on the workflow step editor, no dedicated Asset/Purchase Order pickers on Gate Pass, no automatic document generation or billing/invoicing engine triggered by Consignment consumption, and no outbound notification (email/SMS) at any transition in this sub-module.
