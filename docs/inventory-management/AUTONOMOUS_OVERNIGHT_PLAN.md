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
4. **Next OC ticket number: OC-205** (OC-204 was the last used, Return to Supplier). Increment per slice.
5. **Next Flyway migration number: V446** (V445 was the last used). Increment per file;
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

- [ ] **Stock issue request → issue workflow** (OC-205). `StockIssueRequest` header + lines,
      a requesting `InventoryLocation` asks another (usually a central store) for on-hand
      stock; approve/reject per line (mirrors Purchase Requisition's own header/line
      lifecycle almost exactly — closest in-repo precedent); approving posts an `ISSUE`
      movement (enum value already reserved) decreasing the issuing location's balance.
      Permissions: `INVENTORY_ISSUE_REQUEST_VIEW`/`_MANAGE`/`_APPROVE`.
- [ ] **Auto-restocking when items run low** (OC-206, independent of phase order — can be
      done any time after this phase's issue workflow exists). Extends the existing Wanted
      List shortage job so a location marked for auto-restock also nets against open
      `StockIssueRequest` lines the same way it already nets against open Purchase
      Requisition lines, so the two request types don't double-count the same shortage.
- [ ] **Internal returns** (OC-207). A location returns previously-issued stock back to the
      issuing location; posts the mirrored increase/decrease using the existing `RETURN`
      movement type, linked back to the originating `StockIssueRequest` line for traceability.
- [ ] **Generic loan/return tracking for borrowable equipment** (OC-208). `LoanableItemIssue`
      per the concept already flagged in `DECISION_LOG.md`'s Phase 2 kickoff entry (`Product
      .isLoanable` reserved flag) — issue a loanable product to a borrower with an expected-
      return date, mark returned (and optionally condition/damage notes) on return, overdue
      surfaced as a simple flag for now (a full alerting/notification pass is Phase 8-
      reporting-adjacent, don't over-build it here). Permissions:
      `INVENTORY_LOAN_ISSUE_VIEW`/`_MANAGE`/`_RETURN`.

## Phase 5 — Equipment & Asset Management

- [ ] **Asset register + lifecycle** (OC-209). `Asset` entity (links back to the `Product`/
      GRN line it was received against where applicable, plus a standalone "already-owned,
      being onboarded" entry path), status lifecycle (`IN_USE`/`UNDER_MAINTENANCE`/
      `RETIRED`/`DISPOSED` — check IHMS for its own asset-status shape first per the
      reference-architecture pivot rule before inventing one). Permissions:
      `INVENTORY_ASSET_VIEW`/`_MANAGE`.
- [ ] **Maintenance scheduling + service contracts** (OC-210). `AssetMaintenanceSchedule`
      (recurring or one-off) + `AssetServiceContract` (vendor, coverage window, renewal
      reminder) against an `Asset`. Permission: `INVENTORY_ASSET_MAINTENANCE_VIEW`/`_MANAGE`.
- [ ] **Depreciation** (OC-211). Standard straight-line depreciation as the default method
      (ERP-standard baseline; do not attempt double-declining/units-of-production unless a
      real need surfaces later), computed from `Asset.purchaseValue`/`purchaseDate`/
      `usefulLifeMonths`/`salvageValue`, exposed as a read-only computed current book value —
      no GL posting/connector work (that's the already-deferred ledger connector from Phase
      1's Backend Architect decision).
- [ ] **Disposal/write-off workflow** (OC-212). Moves an `Asset` to `DISPOSED` with a reason,
      approver, and disposal value/date; posts a corresponding stock write-off if the asset
      still carries on-hand stock qty. Permission: `INVENTORY_ASSET_DISPOSE`.

## Phase 6 — Budgets & Approvals

- [ ] **Budget allocation & tracking** (OC-213). `Budget` per `InventoryLocation` (or the
      loose cost-object `Type`+`Id` reference already decided against `Speciality`-coupling
      in the ER-diagram entry) and period, consumed by Purchase Requisition/PO spend;
      surfaced as allocated-vs-consumed, not hard-blocked at this slice (hard budget
      enforcement is a deliberate escalation — flag it as a follow-on, don't silently wire a
      block into an existing screen's submit path without it being its own reviewed slice).
- [ ] **Multi-level approval routing** (OC-214). Generic approval-chain engine (sequential
      and parallel sign-off), retrofitted as an *optional* gate in front of Purchase
      Order/Purchase Requisition rather than replacing their existing single-permission
      approve/reject — per the original Phase 2 kickoff decision that a real approval engine
      is Phase 6 scope, built once. This is the largest single slice in the whole plan;
      consider breaking it into its own sub-checklist in `DECISION_LOG.md` when started
      rather than one commit.
- [ ] **Exception handling with documented reasons** (OC-215). Structured reason codes
      (urgent purchase, single-supplier situation, etc. — mirrors `WantedListRejectionReason`
      's structured-enum-plus-notes shape already shipped) usable when an approval step is
      bypassed.

## Phase 7 — Gate Pass, Vendor-Owned Stock & Service Requests

- [ ] **Outward/inward gate pass** (OC-216), including overdue alerts for items that should
      have returned (repair/loan send-outs).
- [ ] **Vendor-owned ("consignment") stock** (OC-217) — mirrors IHMS's `Consignment`/
      `ConsignmentItem` converting into an owned `Purchase`/GRN as it's consumed, per the
      reference-architecture entry.
- [ ] **Internal service ticketing** (OC-218) — general complaint/service-request tracking,
      independent of phase order, can be built any time.

## Phase 8 — Reporting & Dashboards

- [ ] Depends on Phases above being in place; break down into concrete report/dashboard
      slices once Phase 3–7 land enough real data shapes to report on. Do not start this
      phase's checklist items speculatively ahead of its dependencies.

---

## Also outstanding (not phase-numbered, pick up opportunistically)

- [ ] `InventoryItem` (legacy lab-consumables) migration — still deferred per standing
      decision; the `labs WHERE room_id IS NULL` precondition (7/7 on local data as of
      2026-09-07) must be re-checked and backfilled before this is attempted. Do not start
      without re-reading `DECISION_LOG.md`'s full thread on this first.
- [ ] Product photo upload (`ProductImage` + MinIO plumbing) — deferred, real scoped work,
      follow the `FloorPlanService`/`MinioStorageService` precedent when picked up.

---

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
