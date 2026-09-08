# Inventory Management — Autonomous Overnight Build Plan

> **Purpose.** On 2026-09-08 the user authorized an unattended, no-confirmation build session
> to use up remaining weekly token budget (weekly budget resets 2026-09-09 08:30 IST — **not**
> a stopping point, see below). This file is the single source of truth for what's left, so
> **any** session — this one continuing, a fresh terminal tab, tomorrow afternoon — can open
> this file and pick up exactly where the last one stopped. Check items off in place as they
> ship; never delete a line, add a note instead (same append-only spirit as `DECISION_LOG.md`).
>
> **Extended horizon (user instruction, 2026-09-08):** keep working autonomously, without
> waiting for confirmation, **until the user returns "tomorrow at 3pm"** — i.e.
> 2026-09-09 ~15:00 IST. The 08:30 weekly-budget reset is a mid-stream quota refill, not a
> deadline — work straight through it. Do not stop early because "overnight" sounds done at
> sunrise; the actual instruction is to run to ~15:00 IST on 2026-09-09.

## Standing rules for every session that works from this file

1. **No specialist-review round, no `AskUserQuestion` stops.** The user explicitly waived
   `CLAUDE.md`'s @Partner confirmation gate for this file's scope only. Make the same class
   of judgment call a specialist round would have settled, using established precedent in
   `DECISION_LOG.md` (mirror IHMS where it fits, ERP-standard patterns where IHMS doesn't,
   this module's own already-shipped shapes as the closest precedent) — then **write the
   decision into `DECISION_LOG.md` exactly as every prior slice did**, tagged
   `**Made autonomously overnight — flag for morning review if this reads wrong.**` This
   keeps the audit trail intact without blocking on anyone being awake.
2. **All other `CLAUDE.md` gates still apply in full** — they are code-quality/safety gates,
   not approval gates: migration column verification, never edit a shipped migration,
   permission-migration DEV_ADMIN/SUPPORT_ADMIN sync block, operation-wise permission
   mapping (own permission per button/action), master-screen uniqueness validator, list-
   screen structural gate, resizable-column cell markup gate, `mlp-*` spacing system,
   badge/status consistency, Component Touch Rule. Do not skip any of these to go faster.
3. **One commit per finished slice**, local only — `git add` the specific touched files
   (never `-A`; the working tree has unrelated in-flight work from other sessions/tabs —
   check `git status` and touch nothing outside this task). Message format matches the
   existing history: `OC-<next>: feat(inventory): Phase <n> <Area> — <slice name>`, body
   summarizing what shipped, same `Co-Authored-By`/`Claude-Session` trailer as recent commits.
   **Do not `git push`** — the inventory commits through OC-201 are also local-only; stay
   consistent and let the user push everything together when they review.
   **Never touch/stage/commit files outside `docs/inventory-management/`, backend inventory
   packages/tests, frontend inventory feature folders, and shared nav/routing entries this
   work itself adds** — the rest of the dirty tree belongs to other concurrent work.
4. **Next OC ticket number: OC-224** (OC-223 was the last used, Asset Depreciation Summary
   Report — OC-206 was skipped, see its checklist item above). Increment per slice.
5. **Next Flyway migration number: V473** (V472 was the last used — none of OC-221/222/223
   needed a new migration, same as Depreciation/OC-211). Increment per file;
   grep the migrations directory yourself before writing a number in case a session already
   claimed the next one after this doc was last saved.
6. After every slice: update this file's checkbox, `MILESTONES.md`'s relevant phase status/
   "doing now" text, `RELEASE_3_MILESTONES.md`'s progress row, add the `DECISION_LOG.md`
   entry, and add/extend a manual-test-case file under `docs/manual-test-cases/` — same
   "in the same change" discipline every prior slice already followed. Run
   `npx tsc -p frontend/tsconfig.app.json --noEmit` and the relevant backend test class
   before committing; don't leave a slice half-compiling for the next session to find.
7. **Stop and leave a clear note in this file's "Session handoff notes" section** (bottom)
   instead of guessing, if a slice turns out to need real product input that no ERP-standard
   default or in-repo precedent can settle (e.g. a genuinely ambiguous money/compliance
   question). Skip to the next independent slice rather than stalling.
8. Work top-to-bottom within a phase, and phases in order (2 → 3 → 4 → ...), since later
   phases assume earlier ones exist — but a slice explicitly marked "independent of phase
   order" below may be taken out of turn if it unblocks nothing else.

---

## Phase 2 — Purchasing & Suppliers (finish the phase)

- [x] **Purchase Order** (OC-201, shipped 2026-09-08). Header + `PurchaseOrderItem` lines, created by picking up
      one or more `APPROVED` Purchase Requisition lines *for the same supplier* (mirrors
      Wanted List's "collective conversion" pattern already shipped). Status lifecycle per
      `DECISION_LOG.md`'s 2026-09-08 "Purchase Requisition slice" entry, decision 5:
      `PENDING → ORDERED → IN_PROGRESS → PARTIALLY_COMPLETED → COMPLETED` + `FORCE_CLOSED`,
      no approval gate. Unit price defaults from `VendorProductMapping`/active
      `RateContractLine` resolution already built; `TaxRule` applied per line. Requisition
      line moves to a new terminal state (e.g. `ORDERED`) once picked into a PO line, so it
      can't be picked twice — same double-booking guard style as Wanted List vs. open
      requisition lines. Plain `currencyCode`/`exchangeRate` fields (flagged, not objected,
      per kickoff decision 5). Permissions: `INVENTORY_PURCHASE_ORDER_VIEW`/`_MANAGE` (+
      `_FORCE_CLOSE` as its own operation-wise permission — force-closing is a distinct,
      audit-worthy action from ordinary status progression). List + form screens under the
      "Purchasing & Suppliers" nav group. **This closes Phase 2** — flip its status to ✅ Done
      in `MILESTONES.md`/`RELEASE_3_MILESTONES.md` once shipped.

## Phase 3 — Receiving & Stock Movement

- [x] **Goods Receipt Note (GRN)** (OC-202, shipped 2026-09-08). `GoodsReceipt` header + `GoodsReceiptLine`,
      raised against one `PurchaseOrder`, two-step save (DRAFT, editable) → confirm
      (CONFIRMED, posts stock) matching IHMS's `Purchase`/`PurchaseItem` draft→confirm shape
      (`DECISION_LOG.md` "Reference architecture pivot" entry). Confirming a line posts a
      `RECEIPT` stock movement through the existing `StockMovementService` at the PO's
      destination `InventoryLocation`, and rolls the parent `PurchaseOrder` into
      `IN_PROGRESS`/`PARTIALLY_COMPLETED`/`COMPLETED` from its lines' cumulative received qty
      vs. ordered qty (the "receipt-progress-driven" lifecycle already decided). Over-receipt
      guard: warn, don't hard-block (real deliveries sometimes run over) — but never allow a
      GRN line to exceed the PO line's still-open qty by more than a configurable tolerance
      of 0%, i.e. block over-receipt for this slice and revisit tolerance later if asked.
      Permissions: `INVENTORY_GRN_VIEW`/`_MANAGE`/`_CONFIRM` (confirm is its own operation —
      it's the action with real stock/financial consequence, mirrors Cycle Count's `_APPROVE`
      split).
- [ ] **Batch/expiry capture on GRN** (fold into the GRN slice above, not separate). GRN line
      reuses the existing "batch/serial number + expiry" fields already on the Record Stock
      Movement form (Phase 1 decision) rather than inventing a second batch-entry UI.
- [x] **Transfers between locations** (OC-203, shipped 2026-09-08). New `StockTransfer` header + lines (source
      `InventoryLocation` → destination `InventoryLocation`), posts a `TRANSFER`-typed
      movement pair (decrease at source, increase at destination) through
      `StockMovementService` — the `TRANSFER` enum value already exists on `StockLedger`
      unused since Phase 1's "narrowed UI surface" decision; this is where it gets a real
      screen. Simple DRAFT → COMPLETED lifecycle (no approval gate this phase, consistent
      with PO). Permission: `INVENTORY_STOCK_TRANSFER_VIEW`/`_MANAGE`.
- [x] **Return-to-supplier** (OC-204, shipped 2026-09-08). Raised against a confirmed GRN line (defective/wrong
      item), posts a decreasing movement at the receiving location using the existing
      `RETURN` enum value, and — if the source PO/GRN pairing supports it — nets against the
      PO's received qty so the PO's progress state reflects the true accepted quantity.
      Permission: `INVENTORY_SUPPLIER_RETURN_VIEW`/`_MANAGE`.

## Phase 4 — Requests, Issues & Returns

> **Naming caution for whoever builds this phase:** "Purchase Requisition" (Phase 2, already
> shipped) requests *buying* from a supplier. This phase's requisition is a *department/
> location requesting stock already on hand* — name the new entity distinctly (e.g.
> `StockIssueRequest`, matching IHMS's `IssueLocationIndent` concept per the reference-
> architecture entry) so the two are never confused in code, nav, or permission names.

- [x] **Stock issue request → issue workflow** (OC-205, shipped 2026-09-08). `StockIssueRequest` header + lines,
      a requesting `InventoryLocation` asks another (usually a central store) for on-hand
      stock; approve/reject per line (mirrors Purchase Requisition's own header/line
      lifecycle almost exactly — closest in-repo precedent); approving posts an `ISSUE`
      movement (enum value already reserved) decreasing the issuing location's balance.
      Permissions: `INVENTORY_ISSUE_REQUEST_VIEW`/`_MANAGE`/`_APPROVE`.
- [ ] **Auto-restocking when items run low** (OC-206) — **SKIPPED, needs real product-policy
      input, do not reopen without asking the user first.** This item's original wording
      (netting Wanted List shortages against open `StockIssueRequest` lines) turned out not to
      make business sense on closer look — see the "Auto-restocking (OC-206) skipped"
      decision-log entry for the full reasoning. A real version of this feature (e.g. preferring
      an internal transfer over a new purchase when another location has surplus) needs actual
      deployment policy no ERP-standard default can safely guess.
- [x] **Internal returns** (OC-207, shipped 2026-09-08). A location returns previously-issued stock back to the
      issuing location; posts the mirrored increase/decrease using the existing `RETURN`
      movement type, linked back to the originating `StockIssueRequest` line for traceability.
- [x] **Generic loan/return tracking for borrowable equipment** (OC-208, shipped 2026-09-08). `LoanableItemIssue`
      per the concept already flagged in `DECISION_LOG.md`'s Phase 2 kickoff entry (`Product
      .isLoanable` reserved flag) — issue a loanable product to a borrower with an expected-
      return date, mark returned (and optionally condition/damage notes) on return, overdue
      surfaced as a simple flag for now (a full alerting/notification pass is Phase 8-
      reporting-adjacent, don't over-build it here). Permissions:
      `INVENTORY_LOAN_ISSUE_VIEW`/`_MANAGE`/`_RETURN`.

## Phase 5 — Equipment & Asset Management

- [x] **Asset register + lifecycle** (OC-209, shipped 2026-09-08). `Asset` entity (links back to the `Product`/
      GRN line it was received against where applicable, plus a standalone "already-owned,
      being onboarded" entry path), status lifecycle (`IN_USE`/`UNDER_MAINTENANCE`/
      `RETIRED`/`DISPOSED` — check IHMS for its own asset-status shape first per the
      reference-architecture pivot rule before inventing one). Permissions:
      `INVENTORY_ASSET_VIEW`/`_MANAGE`.
- [x] **Maintenance scheduling + service contracts** (OC-210, shipped 2026-09-08). `AssetMaintenanceSchedule`
      (recurring or one-off) + `AssetServiceContract` (vendor, coverage window, renewal
      reminder) against an `Asset`. Permission: `INVENTORY_ASSET_MAINTENANCE_VIEW`/`_MANAGE`.
- [x] **Depreciation** (OC-211, shipped 2026-09-08). Standard straight-line depreciation as the default method
      (ERP-standard baseline; do not attempt double-declining/units-of-production unless a
      real need surfaces later), computed from `Asset.purchaseValue`/`purchaseDate`/
      `usefulLifeMonths`/`salvageValue`, exposed as a read-only computed current book value —
      no GL posting/connector work (that's the already-deferred ledger connector from Phase
      1's Backend Architect decision).
- [x] **Disposal/write-off workflow** (OC-212, shipped 2026-09-08). Moves an `Asset` to `DISPOSED` with a reason,
      approver, and disposal value/date; posts a corresponding stock write-off if the asset
      still carries on-hand stock qty. Permission: `INVENTORY_ASSET_DISPOSE`.

## Phase 6 — Budgets & Approvals

- [x] **Budget allocation & tracking** (OC-213, shipped 2026-09-08). `Budget` per `InventoryLocation` (or the
      loose cost-object `Type`+`Id` reference already decided against `Speciality`-coupling
      in the ER-diagram entry) and period, consumed by Purchase Requisition/PO spend;
      surfaced as allocated-vs-consumed, not hard-blocked at this slice (hard budget
      enforcement is a deliberate escalation — flag it as a follow-on, don't silently wire a
      block into an existing screen's submit path without it being its own reviewed slice).
- [x] **Multi-level approval routing** (OC-214, shipped 2026-09-08). Generic approval-chain engine (sequential
      and parallel sign-off), retrofitted as an *optional* gate in front of Purchase
      Order/Purchase Requisition rather than replacing their existing single-permission
      approve/reject — per the original Phase 2 kickoff decision that a real approval engine
      is Phase 6 scope, built once. **Scope actually shipped:** a real, working, standalone
      engine (workflows/steps/instances/actions, sequential+parallel routing, permission-gated
      per step) that a user explicitly starts against an existing PR/PO — it does not rewire
      PurchaseRequisitionService/PurchaseOrderService's own state machines to hard-block on it;
      see the decision-log entry's own "Scope boundary" section for why that's a deliberate
      line, not a shortfall.
- [x] **Exception handling with documented reasons** (OC-215, shipped 2026-09-08). Structured
      reason codes (urgent purchase, single-supplier situation, etc. — mirrors
      `WantedListRejectionReason`'s structured-enum-plus-notes shape already shipped) usable
      when an approval step is bypassed via a new, dedicated `INVENTORY_APPROVAL_BYPASS`
      permission rather than the step's own required permission. **This closes Phase 6.**

## Phase 7 — Gate Pass, Vendor-Owned Stock & Service Requests

- [x] **Outward/inward gate pass** (OC-216, shipped 2026-09-08), including overdue alerts for
      items that should have returned (repair/loan send-outs). Approval and gate/security
      verification are two distinct, separately-permissioned steps.
- [x] **Vendor-owned ("consignment") stock** (OC-217, shipped 2026-09-08) — takes the essence
      of IHMS's `Consignment`/`ConsignmentItem` (not a straight mirror, per the "Reference
      architecture pivot" entry's own later refinement). Receiving posts to the main stock
      ledger immediately (usable from day one); recording consumption is a separate, manual
      ownership-transfer reconciliation that does not move stock a second time and does not
      auto-generate a Purchase/GRN — see the decision log for why. No periodic billing engine
      built (billing-cycle is a captured term only).
- [x] **Internal service ticketing** (OC-218, shipped 2026-09-08) — a configurable
      `ServiceTicketCategory` lookup master (shaped like `Uom` minus code) plus `ServiceTicket`
      itself, entirely independent of Product/Asset, scoped only to a location. Four separate
      permissions for the lifecycle (`Assign`/`Resolve`/`Close`, plus `Manage` for create+
      cancel). **This closes Phase 7 in full, and brings R3-M6 to 100%.**

## Phase 8 — Reporting & Dashboards

- [x] **Concrete breakdown done** (2026-09-08, now that Phases 3–7 have all landed) — recorded
      as `MILESTONES.md`'s Phase 8 Todo list: Inventory Dashboard (shipped, OC-219), Stock
      Valuation report, PO Aging/Cycle-Time report, Price Comparison report, Asset Depreciation
      Summary report, Budget vs. Actual report.
- [x] **Inventory Dashboard** (OC-219, shipped 2026-09-08) — a single at-a-glance overview
      screen, eleven metrics computed live across every phase already shipped (open POs,
      pending requisitions/wanted-list items, active approvals, overdue gate passes/loans, open
      service tickets, over-allocated budgets, outstanding consignment liability, assets under
      maintenance). No new table.
- [x] **Stock Valuation report** (OC-221, shipped 2026-09-09) — total on-hand value grouped by
      Category (optional Location filter), computed live from the already-materialized
      `StockBalance` table. Reconsidered and revised the earlier "needs product input" call in
      this breakdown — a category-grouped total-value rollup is close to zero-ambiguity ERP
      standard and needed no new business logic. Deliberately shows distinct product count, not
      summed quantity (see the decision log for why summing qty across a category is wrong).
- [x] **Purchase Order Aging report** (OC-222, shipped 2026-09-09) — every still-open PO
      bucketed into the standard 0–30/31–60/61–90/90+ day ranges, the same near-universal
      default every major ERP/procurement system ships. Reconsidered and revised the earlier
      "needs product input" call, same as Stock Valuation. Cycle-time (a separate concept —
      how long from PO raised to fully received, only computable for already-completed orders)
      was not attempted here; if wanted later it's a distinct report, not folded into this one.
- [ ] **Price Comparison report** (across suppliers/rate contracts for the same product) —
      needs a real product-input decision on layout/columns — do not build without asking.
- [x] **Asset Depreciation Summary report** (OC-223, shipped 2026-09-09) — purchase value/
      accumulated depreciation/current book value grouped by category, "as of today" (no
      period/monthly time-series — same scope decision as Stock Valuation), disposed assets
      excluded. Reconsidered and revised the earlier "needs product input" call, same as the
      two prior report slices. Reuses the depreciation formula already in `AssetService`,
      extracted into a shared `AssetDepreciationCalculator` rather than duplicated.
- [ ] **Budget vs. Actual Spend report** — needs a real product-input decision on
      period/grouping and whether it differs meaningfully from the Budgets list's own
      allocated-vs-consumed columns — do not build without asking.

---

## Also outstanding (not phase-numbered, pick up opportunistically)

- [ ] `InventoryItem` (legacy lab-consumables) migration — still deferred per standing
      decision. **Re-checked 2026-09-08: still 7/7 labs with `room_id IS NULL`, unchanged from
      2026-09-07.** This is real institutional data (which physical room each lab occupies) no
      ERP-standard default can supply — do not attempt a backfill without asking the user first;
      re-read `DECISION_LOG.md`'s full thread on this before touching it again.
- [x] **Product photo upload** (OC-220, shipped 2026-09-08) — `ProductImage` + MinIO plumbing,
      following the `FloorPlanService`/`MinioStorageService` precedent exactly. First photo
      uploaded auto-becomes primary; deleting the primary auto-promotes the oldest remaining
      one. Dedicated `INVENTORY_PRODUCT_IMAGE_MANAGE` permission (grounded in the
      `FacultyDocumentController` precedent of a dedicated document-manage permission distinct
      from the parent entity's own manage permission). Embedded into the existing
      `ProductFormComponent` as an additive, edit-mode-only side-panel card — Component Touch
      Rule applied (reviewed via code, no existing logic touched, theme tokens only).

---

## Infrastructure notes (checked once, don't re-derive)

- **This repo's `OC-XXX` numbers in this module's commits are a local, sequential
  commit-message convention only — not real Jira tickets.** Verified: `bash scripts/jira.sh
  info OC-200` returns `"Issue Does Not Exist"` against the real Jira instance (connectivity
  itself works — `myself` endpoint returns HTTP 200). Prior sessions building this Inventory
  module never used `scripts/jira.sh` for it, unlike some other modules (see
  `scripts/r2-autonomous-prompt-night.md`, which *does* mandate `jira.sh create`/`start`/
  `review` per slice for Release 2 work). **Stay consistent with this module's own established
  precedent — do not start creating real Jira tickets for Inventory slices**, that would be a
  bigger, unrequested workflow change, not a fix.
- **A stronger cross-session continuation mechanism already exists in this repo and was
  deliberately not used tonight**: `scripts/r2-autonomous-run.sh` + a one-shot system
  `crontab` entry invoking `claude -p "<prompt>" --dangerously-skip-permissions` in a
  brand-new headless process — survives even if the interactive terminal is closed, unlike
  this session's own `CronCreate` nudge (session-scoped, dies with the terminal). Not set up
  for tonight because the user explicitly said they'd keep this terminal open, which already
  covers the failure mode that mechanism exists for — and running two autonomous processes
  against the same plan file at once risks real races (duplicate OC/migration numbers, two
  commits fighting over the same next-number). **If a future session finds this terminal did
  die and no progress happened for a long stretch, this is the documented fallback** — copy
  `scripts/r2-autonomous-prompt-night.md`'s shape into a new `r3-autonomous-prompt.md` that
  points at this plan file, single-instance only (never run alongside a live interactive
  session working from the same file).

## Session handoff notes

*(Append dated entries here — one per session that stops mid-plan — instead of leaving
partial state undocumented. Include: what was in progress, what's verified-working, what's
broken/half-done, and any judgment call made that a future session should sanity-check.)*

- **2026-09-08, session start (this plan's authoring session):** Plan file created fresh;
  no slices attempted yet under it. Starting immediately on Phase 2's Purchase Order slice.
- **2026-09-08, same session, after OC-201:** Purchase Order shipped (backend + frontend +
  migrations V438/V439 + docs), `./gradlew compileJava` and `npx tsc --noEmit` both clean,
  committed locally. **Phase 2 is now fully closed.** User mid-session extended the horizon:
  keep working autonomously (no confirmation stops) until they return "tomorrow at 3pm"
  (~2026-09-09 15:00 IST), not just overnight — see the header note.
- **2026-09-08, same session, after OC-202:** Goods Receipt shipped (new
  `com.cms.inventory.receiving` package: backend + frontend + migrations V440/V441 + docs),
  compile/typecheck both clean, committed locally. User asked mid-session about token/context
  hygiene for this long session — answered, and a memory was saved reinforcing that all real
  progress state lives in this file (not conversation history), so `/clear`/`/compact` are
  always safe.
- **2026-09-08, same session, after OC-203:** Stock Transfer shipped (widened
  `StockMovementService`'s `TRANSFER` support + new `StockTransfer`/`StockTransferLine` in the
  existing `com.cms.inventory.stock` package + frontend + migrations V442/V443 + docs),
  compile/typecheck both clean, committed locally. User confirmed they will not be watching
  the screen overnight and asked to use tokens wisely — no tool exists to self-`/clear`, so
  continuing to work in as few, dense tool calls as reasonable per slice.
- **2026-09-08, same session, after OC-204:** Return to Supplier shipped, **closing Phase 3 in
  full** (Goods Receipt + Stock Transfer + Return to Supplier). Along the way, widened
  `PurchaseOrderService.recalculateReceiptProgress` (shipped earlier tonight in OC-202) so
  `COMPLETED` is no longer treated as terminal — only `FORCE_CLOSED` is — so a return can
  correctly revert an order's status; also gave `StockMovementService` decrease-only handling
  for `RETURN`, flagged with a comment that Phase 4's different "internal returns" concept must
  not assume that same handling still fits. Compile/typecheck both clean, committed locally.
  Next: Phase 4 ("Requests, Issues & Returns") — read its naming-caution note in this file
  before starting, since "Purchase Requisition" (buying) and this phase's new requisition
  concept (requesting on-hand stock) must stay clearly distinct in code/nav/permissions.
- **2026-09-08, same session, after OC-205:** Stock Issue Request shipped (new
  `com.cms.inventory.issue` package + widened `StockMovementService` for `ISSUE` + frontend +
  migrations V446/V447 + docs), compile/typecheck both clean, committed locally. Naming stayed
  cleanly distinct from Purchase Requisition per the standing caution.
- **2026-09-08, same session, after skipping OC-206 and shipping OC-207:** Auto-restocking
  (OC-206) turned out to need real product-policy input on closer look — skipped and flagged
  per the plan's own escape-valve rule, see the decision log. Internal Return (OC-207) shipped
  instead: widened `StockMovementService`'s `RETURN` to be direction-based (was decrease-only),
  added a `returnedQty` running total to `StockIssueRequestItem`, new
  `INVENTORY_ISSUE_REQUEST_RETURN` permission, migrations V448/V449. Compile/typecheck both
  clean, committed locally.
- **2026-09-08, same session, after OC-208:** Loanable Item Issue shipped — standalone
  borrow/return tracking (deliberately not integrated with the stock ledger; see the decision
  log), new nav item, migrations V450/V451, compile/typecheck both clean, committed locally.
  **Phase 4 is now otherwise complete** — only Auto-restocking (OC-206) remains, and it's
  deliberately deferred (needs real product-policy input, not guessed at). Next: Phase 5
  (Equipment & Asset Management) — the largest remaining phase, four slices (Asset register,
  Maintenance/Service Contracts, Depreciation, Disposal).
- **2026-09-08, same session, after OC-209:** Asset register shipped — new
  `com.cms.inventory.asset` package (individual physical-unit tracking, open-ended status
  lifecycle, optional GRN traceability, mandatory asset-tag uniqueness check per CLAUDE.md's
  master-screen pattern), migrations V452/V453, compile/typecheck both clean, committed
  locally. **Could not verify against IHMS's own asset-status shape** (no IHMS repo access from
  this session) — flagged in the decision log for a future session that has it.
- **2026-09-08, same session, after OC-210:** Maintenance Schedules + Service Contracts
  shipped (recurrence advances from performed date, one-off auto-deactivates, contracts reuse
  the existing Supplier master rather than a new vendor entity), migrations V454/V455,
  compile/typecheck both clean, committed locally. Along the way, checked and recorded two
  infrastructure facts in the new "Infrastructure notes" section above (Jira not used for this
  module's OC numbers; a stronger crontab-based continuation mechanism exists but wasn't
  needed).
- **2026-09-08, same session, after OC-211:** Depreciation shipped with **no new migration** —
  `AssetService.toResponse` now computes standard straight-line depreciation live from the
  asset's own already-captured purchase fields (`depreciationApplicable`/
  `accumulatedDepreciation`/`currentBookValue` added to `AssetResponse`); Asset Register list
  gained a "Book Value" column. Compile/typecheck both clean, committed locally.
- **2026-09-08, same session, after OC-212:** Disposal shipped, **closing Phase 5 in full**
  (Asset register + Maintenance/Service Contracts + Depreciation + Disposal). New disposal
  columns on `assets` (V456), new `INVENTORY_ASSET_DISPOSE` permission (V457), disposal writes
  off one unit of on-hand stock for the asset's product/location if any exists (existing
  DISPOSAL movement, no new type), forced through a dedicated dialog rather than the inline
  status select. Compile/typecheck both clean, committed locally. Next: Phase 6 (Budgets &
  Approvals) — the largest remaining phase; note its Multi-level approval routing item is
  explicitly flagged in the plan below as the largest single slice in the whole plan.
- **2026-09-08, same session, after OC-213:** Budget allocation shipped — new
  `com.cms.inventory.budget` package, `consumedAmount` computed live from sent (non-PENDING)
  Purchase Orders via a new `PurchaseOrderItemRepository` aggregate query, purely informational
  (no enforcement, per the plan's own instruction), migrations V458/V459, compile/typecheck
  both clean, committed locally.
- **2026-09-08, same session, after OC-214:** Multi-level approval routing shipped — the
  plan's own flagged largest slice, and it went cleanly: new `com.cms.inventory.approval`
  package (4 entities, 2 services, 2 controllers, full frontend), `./gradlew compileJava` and
  `npx tsc --noEmit` both passed on the first attempt. Built as a real, working, standalone
  engine (sequential+parallel routing genuinely functions, permission-gated per step) that a
  user explicitly starts against an existing PR/PO — deliberately does NOT rewire
  PurchaseRequisitionService/PurchaseOrderService's own state machines to hard-block on it; see
  the decision log's "Scope boundary" section for the full reasoning on why that's a conscious
  line for this slice, not a shortfall, and what a lower-risk follow-on to actually wire it in
  as a submit-time gate would look like. Migrations V460/V461. Next: Exception handling with
  documented reasons (OC-215), the last Phase 6 slice.
- **2026-09-08, same session, after OC-215:** Exception handling shipped — **closing Phase 6
  in full**. Added `ApprovalExceptionReason` enum + `exception_reason` column on
  `approval_actions` (V462), a new dedicated `INVENTORY_APPROVAL_BYPASS` permission (V463,
  seeded to DEV_ADMIN/SUPPORT_ADMIN/ADMIN/COLLEGE_ADMIN with the catch-all sync block) so
  bypassing a step never reuses that step's own required permission, and a new bypass endpoint/
  service method with the same stage/status gating as an ordinary approve/reject. Frontend adds
  a "Bypass (Exception)" control (shown only to `INVENTORY_APPROVAL_BYPASS` holders) and an
  exception-reason chip on the resolved-action row. `./gradlew compileJava` and
  `npx tsc --noEmit` both passed clean. Next: Phase 7 (Gate Pass, Vendor-Owned Stock & Service
  Requests) — Outward/inward gate pass (OC-216) is next up.
- **2026-09-08, same session, after OC-216:** Outward/inward Gate Pass shipped — Phase 7's
  first slice. New `com.cms.inventory.gatepass` package (entity, 2 enums, 3 DTOs, repository,
  service, controller), following the `GatePass` entity already sketched in
  `ER_DIAGRAM_AND_MODULE_BOUNDARIES.md` §6. Exactly one of product/asset enforced by a DB CHECK
  constraint; approval (`INVENTORY_GATE_PASS_APPROVE`) and gate/security verification
  (`INVENTORY_GATE_PASS_VERIFY`) are two distinct, separately-permissioned steps even when held
  by the same person; a non-returnable pass closes on verification, a returnable one goes
  "Gate Verified" and can go overdue (computed live, never stored, same posture as
  `LoanableItemIssue`) until marked returned. Migrations V464/V465. Full frontend (list/new/
  detail, nav entry, status-badge classes for the two new states). `./gradlew compileJava` and
  `npx tsc --noEmit` both passed clean. Next: Vendor-owned ("consignment") stock (OC-217),
  mirroring IHMS's `Consignment`/`ConsignmentItem`.
- **2026-09-08, same session, after OC-217:** Vendor-owned (consignment) stock shipped —
  `ConsignmentAgreement` (simple master, shaped like `Budget`) + `ConsignmentStockLine` (running
  balance, `qtyOnHand` computed live as `receivedQty - consumedQty`, a deliberate divergence from
  the ER doc's flat field, logged in the decision log). Receiving posts a real `RECEIPT` to the
  main `StockLedger` via the existing `StockMovementService` (usable immediately, even though not
  yet owned); recording consumption is a standalone financial reconciliation with no second stock
  movement and no auto-generated Purchase/GRN — both consciously scoped out, reasoning fully in
  the decision log. Migrations V466/V467. Full frontend (agreement list/form, stock-line list
  with Receive/Consume dialogs, nav entries). `./gradlew compileJava` and `npx tsc --noEmit` both
  passed clean. Next: Internal service ticketing (OC-218), the last Phase 7 slice.
- **2026-09-08, same session, after OC-218:** Internal Service Ticketing shipped — **closing
  Phase 7 in full** and bringing R3-M6 ("Approvals & Gate Pass") to 100%. New
  `com.cms.inventory.ticket` package: `ServiceTicketCategory` (a configurable lookup master,
  shaped like `Uom` minus its `code` field, with the mandatory uniqueness validator/`/name-exists`
  endpoint) and `ServiceTicket` itself (no separate stored ticket number/date — `id`/`createdAt`
  serve that role, same posture as every other business document in this app). Entirely
  independent of Product/Asset, scoped only to a location. Four separate permissions
  (`Manage`/`Assign`/`Resolve`/`Close`) drive `OPEN → IN_PROGRESS → RESOLVED → CLOSED`, with
  `CANCELLED` reachable only from `OPEN`/`IN_PROGRESS`. Migrations V468/V469. Full frontend
  (category list/form, ticket list/new/detail with permission-gated stage actions, nav entries,
  `RESOLVED` added to the shared status-badge resolver). `./gradlew compileJava` and
  `npx tsc --noEmit` both passed clean.
  **Phase 7 is now fully closed (Gate Pass, Consignment Stock, Service Ticket — OC-216/217/218).**
  Next per the plan: Phase 8 (Reporting & Dashboards) explicitly requires breaking its checklist
  down into concrete slices first (its own text says "do not start speculatively ahead of
  dependencies") — that breakdown, or picking up one of the "Also outstanding" items
  (`InventoryItem` migration re-check, Product photo upload), is the next unit of work for
  whichever session picks this up.
- **2026-09-08, same session, after OC-219:** Phase 8's checklist item was itself the "break
  this phase down into concrete slices" instruction — did that first (now recorded as
  `MILESTONES.md`'s Phase 8 Todo list, six named report/dashboard slices), then shipped the
  first and only one of them that needed zero product-input judgment call: the **Inventory
  Dashboard**, a single at-a-glance overview screen. New `com.cms.inventory.reporting` package
  (a `dto`+`service`+`controller` only — no entity, no table, every figure computed live via
  `JpaSpecificationExecutor#count` against entities already shipped across every prior phase,
  plus one small new aggregate query added to the existing `ConsignmentStockLineRepository`).
  New `INVENTORY_DASHBOARD_VIEW` permission, no `_MANAGE` counterpart (nothing to manage on a
  pure overview screen). Migration V470 only (no schema change). Frontend: a new
  `stat-grid`/`stat-card` dashboard reusing the app's existing global stat-card styles, added
  as the first item in the "Stock Management" nav group. `./gradlew compileJava` and
  `npx tsc --noEmit` both passed clean. **The remaining five Phase 8 report slices (Stock
  Valuation, PO Aging, Price Comparison, Depreciation Summary, Budget vs. Actual) are all
  explicitly flagged as needing real product input on layout/grouping/filter shape — do not
  build any of them without asking first**, per the plan's own escape-valve rule (Standing Rule
  7). With Phase 8's zero-product-input slice now shipped and every other Phase 8 item requiring
  a real stakeholder decision, and the "Also outstanding" items each needing their own re-
  verification/scoping pass before they're safe to start blind, this is a natural, deliberate
  stopping point for autonomous work under this plan's Standing Rules — not a slice left
  half-finished.
- **2026-09-08, same session, after OC-219, picking up "Also outstanding" work:** re-read the
  `InventoryItem` migration's full decision-log thread as instructed, re-ran its precondition
  query fresh against the local dev DB (`SELECT count(*) FILTER (WHERE room_id IS NULL) FROM
  labs`) — **still 7 of 7, unchanged from 2026-09-07.** This needs real institutional knowledge
  (which room each lab occupies) no ERP default can supply, and the user's own words on this
  migration make it deliberately their call — left explicitly skipped, not attempted, per
  Standing Rule 7. Picked up the other "Also outstanding" item instead: **Product Image
  (OC-220)**, shipped — new `ProductImage` entity (`storage_key` into MinIO, following
  `FloorPlanService`/`MinioStorageService` exactly), first-upload-becomes-primary /
  delete-promotes-next invariants enforced in the service, a dedicated
  `INVENTORY_PRODUCT_IMAGE_MANAGE` permission (grounded in the `FacultyDocumentController`
  precedent), migrations V471/V472. Frontend: a new self-contained `ProductImagesComponent`
  gallery/upload widget embedded into the existing `ProductFormComponent`'s side panel
  (edit-mode only) — an additive-only change to that existing component (Component Touch Rule
  applied: reviewed for light/dark-safe theme-token usage and permission-gating, no existing
  form logic touched). `./gradlew compileJava` and `npx tsc --noEmit` both passed clean.
  **Both "Also outstanding" items are now resolved** (one shipped, one re-verified-and-still-
  correctly-skipped) and Phase 8's only product-input-free slice is done — remaining work under
  this plan (the five Phase 8 report slices, and eventually the `InventoryItem` migration once
  someone can supply real room assignments) all explicitly need a human decision this session
  cannot make. This is a clean, deliberate stopping point, not a slice left half-finished.
- **2026-09-09, new session, after OC-221:** re-read this file fully and re-checked `git
  status` per the standing instructions before touching anything. Re-examined the five
  remaining Phase 8 report slices that OC-219's breakdown had all flagged as "needs product
  input" — on a second look, **Stock Valuation** specifically didn't actually need any: it's
  the standard ERP "inventory valuation summary" (group by category, sum value), and every
  figure it needs was already fully computed and persisted on `StockBalance`. Shipped it: one
  new repository query (`sumValuationByCategory`, following the existing
  `ReorderShortageProjection` pattern) + its own projection interface, a small
  `reporting.service`/`.dto`/`.controller` addition (reusing `INVENTORY_STOCK_VIEW`/`_MANAGE`
  rather than a new permission — logged why, a deliberate departure from the Dashboard's own
  new-permission call), full frontend (category table + grand total, location filter, new nav
  entry under Stock Management). No new table, no new permission, no new migration.
  `./gradlew compileJava` and `npx tsc --noEmit` both passed clean. The other four Phase 8
  report slices (PO Aging, Price Comparison, Depreciation Summary, Budget vs. Actual) were
  re-examined too and still genuinely need a real layout/grouping/bucket-threshold decision —
  left unbuilt, not just skipped without a fresh look. Next session: re-examine those four with
  the same fresh-eyes scrutiny before accepting the "needs product input" call at face value,
  the same way this session did for Stock Valuation — one or more may turn out to have an
  equally standard, ERP-textbook default shape (e.g. PO Aging's 0-30/31-60/61-90/90+ day
  buckets are about as standard as valuation summaries are) that doesn't actually require asking
  the user first. Do not accept a prior session's "needs product input" note as final without
  that re-check.
- **2026-09-09, same session, after OC-222:** followed through on the previous entry's own
  suggestion immediately — **Purchase Order Aging** turned out to be exactly as
  product-input-free as Stock Valuation was: the standard 0–30/31–60/61–90/90+ day bucket
  scheme is a near-universal ERP default, and every figure needed (`poDate`, `lineTotal`) was
  already captured. Shipped: one new repository query
  (`PurchaseOrderItemRepository.findOpenOrdersForAging`, grouping `lineTotal` by PO) + its own
  projection interface, bucketing done in Java in a new `PurchaseOrderAgingReportService`
  (reusing `INVENTORY_PURCHASE_ORDER_VIEW`/`_MANAGE`, same reasoning as Stock Valuation reusing
  the Stock permission), full frontend (bucket table + grand total, warning styling on the
  61–90/90+ rows, new nav entry under Purchasing & Suppliers). No new table/permission/
  migration. `./gradlew compileJava` and `npx tsc --noEmit` both passed clean. **Note:** the
  original checklist item was named "PO Aging / Cycle-Time report" — only the Aging half was
  built. Cycle-Time (average/median days from a PO being raised to being fully received — only
  meaningful for already-`COMPLETED` orders, a genuinely different metric from "how long is a
  still-open order outstanding") was deliberately NOT folded into this slice; it's real,
  separately-scoped work for a future slice, not silently dropped. The remaining Phase 8 items
  — Price Comparison, Asset Depreciation Summary, Budget vs. Actual, and now PO Cycle-Time —
  should each get the same fresh-eyes "is this actually product-input-free?" check before being
  accepted as blocked; two of five originally-flagged items turned out not to need it at all.
- **2026-09-09, same session, after OC-223:** **Asset Depreciation Summary** turned out to be
  the same shape as Stock Valuation Report (a category rollup, "as of today," no time-series),
  and its own hardest-looking part — the depreciation formula itself — was already fully solved
  in `AssetService`. Rather than write a second, potentially-drifting copy of that formula for
  the report, extracted it into a new shared `AssetDepreciationCalculator.compute(...)` and
  pointed `AssetService.toResponse` at the extracted version too (a pure, behavior-preserving
  refactor — verified by re-reading the moved code and a clean `./gradlew compileJava`, no
  dedicated `AssetServiceTest` exists to run). New fetch-join query
  (`AssetRepository.findAllWithCategoryExcludingStatus`), reuses `INVENTORY_ASSET_VIEW`/
  `_MANAGE`. No new table/permission/migration. Full frontend (category table + grand total,
  new nav entry under Equipment & Asset Management). `./gradlew compileJava` and
  `npx tsc --noEmit` both passed clean, committed locally.
- **2026-09-09, same session, after OC-223, before committing OC-224:** while building **Price
  Comparison** next, discovered it needs **zero new backend code at all** — the existing
  `VendorProductMappingController`'s `/page?productId=` endpoint already returns every active
  supplier's rate for a product with `effectivePrice`/`priceSource` (the rate-contract-override
  resolution) already computed, exactly what a price-comparison screen needs. Built as a
  frontend-only addition reusing the existing `VendorProductMappingService`/model as-is (client-
  side sorted by `effectivePrice`, cheapest row highlighted), gated by the same
  `INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW`/`_MANAGE` that already gates that endpoint. Because
  its route/nav-entry edits landed in the same working tree as OC-223's before either was
  committed, the two slices' `app.routes.ts`/`nav-config.ts` additions were temporarily
  interleaved — OC-224's lines were removed again before the OC-223 commit and re-added for
  OC-224's own commit, so each commit's diff stays scoped to exactly one slice (flagging this
  here in case a future multi-slice turn hits the same interleaving and needs the same care).
