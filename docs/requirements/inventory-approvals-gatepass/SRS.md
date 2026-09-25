# Software Requirements Specification — Approvals, Budget & Gate Pass

**Module:** Inventory Management (OneCMS) — Sub-module 6 of 7
**Corresponds to:** Release 3 Milestone R3-M6 ("Approvals & Gate Pass")
**Status as documented:** Done (100%)

> Standalone, industry-agnostic sub-module — see the parent module note in `inventory-core-data-model/SRS.md` §0. No vertical (hospital/college) naming appears anywhere in this sub-module's schema or UI copy.

## 1. Introduction

### 1.1 Purpose
Defines the requirements for the module's financial-control and physical-movement-control layer, built across two internal phases (Phase 6 "Budgets & Approvals", Phase 7 "Gate Pass, Vendor-Owned Stock & Service Requests") that together compose R3-M6: budget allocation/consumption tracking, a generic sequential/parallel multi-level approval-routing engine with an exception-bypass path, outward/inward Gate Pass tracking for physical items crossing the premises boundary, vendor-owned (consignment) stock tracking, and an internal Service Ticketing capability.

### 1.2 Scope
`Budget`, `ApprovalWorkflow`/`ApprovalWorkflowStep`, `ApprovalInstance`/`ApprovalAction`, `GatePass`, `ConsignmentAgreement`/`ConsignmentStockLine`, `ServiceTicketCategory`/`ServiceTicket`. Out of scope: the Procurement/GRN documents an approval instance or budget references (Procurement & Vendor Management, GRN & Stock Control sub-modules), the Stock Ledger a gate pass or consignment receipt posts against (Core Data Model sub-module), and the reporting rollups built on top of Budget (`Budget vs. Actual` — Reporting sub-module).

### 1.3 References
- `docs/RELEASE_3_MILESTONES.md` — R3-M6 row
- `docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §3 (GAP-27 budget period types), §6 (`Budget`, `ApprovalStep`/`ApproverMatrix`/`ExceptionReason`, `GatePass`, `ConsignmentStockLedger` proposed entities)
- `docs/inventory-management/ER_DIAGRAM_AND_MODULE_BOUNDARIES.md` §5 (Asset, Budget & Approvals), §6 (Gate Pass, Consignment & Service Ticket)
- `docs/inventory-management/DECISION_LOG.md`: 2026-09-08 "Budget allocation slice", "Multi-level approval routing slice", "Exception handling slice (OC-215)", "Gate Pass slice (OC-216)", "Consignment stock slice (OC-217)", "Service Ticket slice (OC-218)"
- Source SRS: `docs/inventory-management/source/SRS_v3.2_Updated.pdf` §3.7 (Budget & Finance, F001–F008), §3.13 (Workflow & Approvals, W001–W009), §3.15 (Gate Pass Management, GP001–GP006), §3.16 (Consignment Stock Management, CS001–CS006), §3.8 (CRM & Ticketing, C001–C008)

**Reconciliation against the source SRS:** the source SRS's Budget section (§3.7) is `BudgetID, DepartmentID, FinancialYear, AllocatedAmount, ...` — fiscal-year-only, and named the gap GAP-27 in the core gap analysis. The as-built `Budget` deliberately does **not** close GAP-27's `PeriodType` (Fiscal Year/Academic Year/Semester/Term) proposal — it ships a plain `periodStartDate`/`periodEndDate` date range instead, a simpler and equally industry-agnostic alternative (a hospital deployment has no academic year at all) that the Backend Architect chose over the ER doc's own richer `PeriodType` enum proposal; this is a deliberate divergence, not an unmet gap. Budget enforcement is informational only (`overAllocated` is a flag, never a hard submit-time block), unlike the source SRS's F-series implying real budget-gated spend controls. The source SRS's Workflow & Approvals section (§3.13, "genuinely reusable across any vertical" per the gap analysis) is built close to as designed: sequential/parallel/quorum routing, delegation, and exception reasons all landed, though "quorum" specifically is represented as same-`stepOrder` parallel steps requiring all to approve, not a partial-N-of-M quorum threshold. Gate Pass (§3.15) and Consignment (§3.16) both landed close to the source SRS's own shape (returnable/non-returnable outward+inward; vendor-owned stock with a separate side-ledger), generalized away from §3.16's pharmaceutical/reagent framing per the core gap analysis's finding that this section "generalizes fine once decoupled from pharma framing." CRM & Ticketing (§3.8) landed as a single generic `ServiceTicket`, not the source SRS's two-track "service request / complaint-quality ticket" split.

## 2. Overall Description

### 2.1 Product Perspective
Standalone module component; reuses the Core Data Model sub-module's `InventoryLocation` and `Product`/`Asset` entities, and the Procurement sub-module's `Supplier`/`PurchaseRequisition`/`PurchaseOrder` entities as approval/budget/gate-pass targets. Every write posts to the platform's shared `audit_log` (no parallel audit table). No dependency on any Hospital Information System or Student Information System.

### 2.2 User Classes
Location/finance staff who allocate and monitor Budgets; a workflow author who designs Approval Workflows (admin-tier); document owners/approvers who act on an in-progress Approval Instance at their step; a bypass-authority holder for Exception handling; a Gate Pass requester, an approver, and a security/gate-verifier (three distinct actors, though one person may hold more than one permission); consignment-receiving store staff and a finance/procurement role authorized to record consumption (ownership transfer); a Service Ticket requester, a coordinator who assigns, a technician who resolves, and a closer (often the original requester or a supervisor). All role-to-permission mapping is DB-driven, not hard-coded.

### 2.3 Operating Environment
Same stack as the rest of the module (Angular + Spring Boot + PostgreSQL/Flyway + Keycloak). Backend packages `com.cms.inventory.budget`, `com.cms.inventory.approval`, `com.cms.inventory.gatepass`, `com.cms.inventory.consignment`, `com.cms.inventory.ticket`; frontend `frontend/src/app/features/inventory/{budget,approval,gate-pass,consignment,service-ticket}/`.

### 2.4 Constraints & Assumptions
- Budget's `overAllocated` flag and every consumption figure are computed live at read time from `PurchaseOrderItem.lineTotal` (orders with status other than `PENDING`, including `FORCE_CLOSED`) — never stored on the `Budget` row, and never enforced as a submit-time block on Purchase Requisition/Order.
- Purchase Requisition spend is never counted toward a Budget or an `ApprovalWorkflow`'s `minAmount` threshold — a requisition carries quantity/product only, no monetary value; only `PurchaseOrder` line totals represent real committed spend.
- Starting an Approval Instance is a deliberate, separate user action ("Start Approval") — submitting/sending the underlying Purchase Requisition or Purchase Order does **not** automatically start or block on a workflow. The engine is fully functional but not wired as a hard submit-time gate on those two document services.
- A document that already completed one Approval Instance (`APPROVED`/`REJECTED`) can have a second, different instance started against it; only one `IN_PROGRESS` instance per document is blocked.
- Consignment `qtyOnHand` is computed live (`receivedQty − consumedQty`), never stored; recording consumption is a manual, periodic financial reconciliation action that does **not** post a second Stock Ledger movement — the physical decrease already happened independently through a normal issue/consumption flow.
- No periodic consignment billing engine exists — `billingCycleDays` is a plain informational term on the agreement, not a scheduler or invoice generator.
- No outbound "send Gate Pass to security" or "notify approver" integration exists — every action (approve, verify, return, assign, resolve, close) is a manual in-app step.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-AG-1 | Maintain a **Budget** per Inventory Location (date-range period, allocated amount, active flag, notes), with `consumedAmount`/`remainingAmount`/`overAllocated` computed live from sent (non-`PENDING`) Purchase Order line totals against that location within the period. | Must | Core Data Model FR-CDM-9 (or equivalent Location entity), Procurement PurchaseOrder |
| FR-AG-2 | Maintain an **Approval Workflow** definition (name, target document type [`PURCHASE_REQUISITION`/`PURCHASE_ORDER`], optional Location scope, optional `minAmount` threshold applying only to Purchase Order workflows, active flag) with ordered **Approval Workflow Step**s, each naming a real existing platform `Permission` code as its "who may act" gate and a shared/distinct `stepOrder` (same `stepOrder` = parallel, all must approve; distinct values = sequential ascending). | Must | — |
| FR-AG-3 | Support explicitly starting an **Approval Instance** against an eligible Purchase Requisition or Purchase Order (blocked while one is already `IN_PROGRESS` for that same document), pre-creating every stage's `ApprovalAction` row up front so a not-yet-reached stage is visibly "Pending, not yet at this stage." | Must | FR-AG-2 |
| FR-AG-4 | Support **Approve**/**Reject** on the current stage's action for a user holding that step's own referenced permission — a single rejection at any stage immediately fails the whole instance (`REJECTED`); the instance completes (`APPROVED`) once every stage clears. | Must | FR-AG-3 |
| FR-AG-5 | Support **Bypass** (exception override) of the current stage by a user holding the dedicated `INVENTORY_APPROVAL_BYPASS` permission regardless of the step's own required permission, with a required structured reason (fixed enum + free-text notes) stamped on the resulting action row. | Must | FR-AG-3 |
| FR-AG-6 | Support creating an **outward or inward Gate Pass** against exactly one of a Product or an Asset (DB `CHECK`-enforced), returnable or non-returnable, with party (carrier) name/contact, quantity, reason, and an optional link to a Purchase Order (service/repair linkage), through a distinct **Approve/Reject** step and a distinct **Gate Verify** step (always two separate permissioned actions even if held by the same person). | Must | Core Data Model, Asset FR (Asset sub-module) |
| FR-AG-7 | Support recording a **Return** against a returnable, gate-verified Gate Pass, and derive an "overdue" state at read time from `expectedReturnDate` for any pass currently in the open, returnable, gate-verified window (never stored). | Must | FR-AG-6 |
| FR-AG-8 | Maintain a **Consignment Agreement** per Supplier + Location (agreement number, validity window, billing-cycle-days term, active flag) and per-product **Consignment Stock Line**s tracking `receivedQty`/`consumedQty` (`qtyOnHand` derived live), with **Receive** posting a real `RECEIPT` movement to the main Stock Ledger immediately and **Consume** (record ownership transfer) as a separate, non-stock-moving financial reconciliation action. | Must | Procurement Supplier, Core Data Model Stock Ledger |
| FR-AG-9 | Maintain a **Service Ticket Category** master (name/description/active flag, real-time name uniqueness) and a **Service Ticket** (location, category, priority, description, requester) lifecycle: create → **Assign** → **Resolve** (notes) → **Close** (optional feedback rating), with **Cancel** reachable only from `OPEN`/`IN_PROGRESS`. | Must | Core Data Model FR-CDM-9 (or equivalent Location entity) |
| FR-AG-10 | Every write across this sub-module (Budget, Approval Workflow/Instance/Action, Gate Pass, Consignment, Service Ticket) posts a row to the platform's existing, shared `audit_log` under its own `entityType`, rather than a new parallel audit table. | Must | — |

## 4. External Interface Requirements

### 4.1 Key Screens
Budgets (list/form), Budget vs. Actual (Reporting sub-module, surfaced under the "Budgets & Approvals" nav group), Approval Workflows (list/form with an embedded step editor), Approvals (Approval Instance list/detail — approve/reject/bypass per stage), Gate Passes (list/new/detail — approve/reject/verify-gate/return), Consignment Agreements (list/form), Consignment Stock (list — receive/consume), Service Tickets (list/new/detail — assign/resolve/close/cancel), Ticket Categories (list/form) — under the "Budgets & Approvals" and "Gate Pass & Service Requests" nav groups.

### 4.2 Key API Endpoints
- `/inventory/budget/budgets` (POST, `/page`, GET{id}, PUT{id}) — `INVENTORY_BUDGET_VIEW`/`_MANAGE`
- `/inventory/approval/workflows` (POST, `/page`, GET{id}, PUT{id}, `/eligible`) — `INVENTORY_APPROVAL_WORKFLOW_VIEW`/`_MANAGE` (`/eligible` gated by `INVENTORY_PURCHASE_REQUISITION_MANAGE`/`INVENTORY_PURCHASE_ORDER_MANAGE`)
- `/inventory/approval/instances` (`/start`, `/page`, GET{id}, `/{id}/actions/{actionId}/approve`, `/{id}/actions/{actionId}/reject`, `/{id}/actions/{actionId}/bypass`) — `INVENTORY_APPROVAL_VIEW`/`_ACT`/`_BYPASS` (plus `_MANAGE` on the target document for `/start`)
- `/inventory/gate-passes` (POST, `/page`, GET{id}, `/{id}/approve`, `/{id}/reject`, `/{id}/verify-gate`, `/{id}/return`) — `INVENTORY_GATE_PASS_VIEW`/`_MANAGE`/`_APPROVE`/`_VERIFY`/`_RETURN`
- `/inventory/consignment/agreements` (POST, `/page`, GET{id}, PUT{id}) — `INVENTORY_CONSIGNMENT_VIEW`/`_MANAGE`
- `/inventory/consignment/stock-lines` (`/receive`, `/page`, GET{id}, `/{id}/consume`) — `INVENTORY_CONSIGNMENT_VIEW`/`_MANAGE`/`_CONVERT`
- `/inventory/ticket/tickets` (POST, `/page`, GET{id}, `/{id}/assign`, `/{id}/resolve`, `/{id}/close`, `/{id}/cancel`) — `INVENTORY_SERVICE_TICKET_VIEW`/`_MANAGE`/`_ASSIGN`/`_RESOLVE`/`_CLOSE`
- `/inventory/ticket/categories` (POST, GET, `/page`, GET{id}, PUT{id}, `/{id}/status`, `/name-exists`) — `INVENTORY_SERVICE_TICKET_CATEGORY_VIEW`/`_MANAGE`

### 4.3 Key DB Entities
`budgets`, `approval_workflows`, `approval_workflow_steps`, `approval_instances`, `approval_actions`, `gate_passes`, `consignment_agreements`, `consignment_stock_lines`, `service_ticket_categories`, `service_tickets`.

## 5. Non-Functional Requirements
- **Security/RBAC:** every distinct operation on every entity in this sub-module has its own dedicated permission (operation-wise mapping rule) — e.g. `INVENTORY_GATE_PASS_APPROVE` is never conflated with `INVENTORY_GATE_PASS_VERIFY`, and `INVENTORY_CONSIGNMENT_MANAGE` (receive) is never conflated with `INVENTORY_CONSIGNMENT_CONVERT` (consume). All new permissions seeded to DEV_ADMIN/SUPPORT_ADMIN/ADMIN/COLLEGE_ADMIN via the standard catch-all sync block.
- **Auditability:** every create/approve/reject/bypass/verify/return/assign/resolve/close/cancel action records actor and timestamp on the row itself (`initiatedBy`/`actedBy`/`approvedBy`/`rejectedBy`/`gateVerifiedBy`/`returnedBy`/`assignedTo`/`resolvedBy`/`closedBy`/`cancelledBy` + matching `*At` columns) in addition to the shared `audit_log`.
- **Data integrity:** a Gate Pass's exactly-one-of-Product-or-Asset is enforced by both a DB `CHECK` constraint and a service-layer check; `ConsignmentStockLine.consumedQty ≤ receivedQty` is a DB `CHECK` constraint.
- **Performance:** the Approval Workflow step editor validates a permission code exists server-side (no client-side picker, to avoid coupling this module to core Role Management's own `ROLE_VIEW`-gated `/permissions/all` endpoint).

## 6. Known Gaps / Not Yet Implemented
- No real submit-time approval gate on `PurchaseRequisitionService`/`PurchaseOrderService` — the engine is fully functional but only reachable via an explicit, separate "Start Approval" action; a document can still be submitted/sent without ever being routed through an approval workflow. Flagged in the decision log as a deliberate, lower-risk follow-on, not a shortfall.
- Budget allocation is purely informational — `overAllocated` is a visual flag only, never a hard block on Purchase Requisition/Order submission.
- No `PeriodType` (Fiscal Year/Academic Year/Semester/Term) on Budget — a deliberate divergence from the core gap analysis's GAP-27 proposal in favor of a plain date range; a multi-budget-per-location-per-period scenario (e.g. concurrent Q1/Q2 budgets) is summed together without period-awareness anywhere this data is aggregated (see also Reporting sub-module's Budget vs. Actual known gap).
- No approval "quorum" (partial N-of-M) threshold — only "all must approve" same-`stepOrder` parallel steps and single-rejection-fails-everything semantics; no partial-rejection/re-route/resubmit recovery flow.
- No frontend permission-code picker on the Approval Workflow step editor — a plain text input, validated server-side, to avoid coupling to core Role Management's `ROLE_VIEW`-gated endpoint.
- No dedicated Asset picker component on Gate Pass's asset-target path — a plain `<select>` populated from available assets; no dedicated Purchase Order picker for the optional service-PO link either (plain numeric field, server-validated).
- No automatic document generation from Consignment consumption (no auto-created Purchase Order/GRN on ownership transfer) and no periodic consignment billing/invoice-generation engine.
- No outbound notification (email/SMS) to an approver, gate security, or ticket assignee at any stage — every transition is a manual, in-app action a user must discover and click.
