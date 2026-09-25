# Business Requirements Document — Goods Receipt & Stock Control

## 1. Executive Summary / Business Objective
`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` identified the "Stock Ledger Updated" step as the terminal node of the source SRS's own process diagram, yet the physical act of turning an ordered Purchase Order into real, on-hand stock (§3.4 GRN & Invoice Management) and moving/returning that stock afterward (part of §3.3) had no dedicated, auditable document trail in the as-built system before this sub-module. The business objective is a reliable receiving discipline — a delivery cannot silently exceed what was ordered, every unit received is traceable back to the order line and, when applicable, a batch/expiry — plus the lateral stock movements (transfer between locations, return to the originating supplier) that a real warehouse operation needs day to day, all posting through the one append-only Stock Ledger the Core Data Model sub-module established.

## 2. Stakeholders
- Receiving/store staff (record deliveries, confirm receipts, action transfers and supplier returns)
- Procurement staff (rely on `PurchaseOrderItem.receivedQty` and the order's own status to know what's still outstanding)
- Finance (relies on the weighted-average cost each receipt/transfer/return carries into `StockBalance.valueOnHand`)
- Platform/DB-driven Role Management administrators (permission assignment)

## 3. Business Rules
| ID | Rule | Rationale |
|---|---|---|
| BR-GSC-1 | A Goods Receipt line always requires an existing Purchase Order line — there is no unplanned, no-PO receipt in this sub-module. | Matches the plan's own scope for this phase; an "unplanned receipt" flow is real, separately-scoped work (Decision Log, 2026-09-08 "Goods Receipt slice"). |
| BR-GSC-2 | Over-receipt against a Purchase Order line is blocked outright, at 0% tolerance, not allowed with a warning. | Without a documented tolerance policy to implement correctly, blocking is the safer default; a configurable tolerance is real, separately-scoped work (Decision Log, 2026-09-08 "Goods Receipt slice"). |
| BR-GSC-3 | A Goods Receipt is a two-step document: `DRAFT` (freely editable, nothing posted) then `CONFIRM` (posts stock, immutable afterward) — never posted immediately on line add. | Mirrors the reference architecture's own draft→confirm shape rather than posting prematurely (Decision Log, 2026-09-08 "Goods Receipt slice"). |
| BR-GSC-4 | A Return to Supplier may only be raised against a `CONFIRMED` Goods Receipt. | A still-`DRAFT` receipt has posted no real stock yet, so there is nothing to return (Decision Log, 2026-09-08 "Return to Supplier slice"). |
| BR-GSC-5 | A Return to Supplier nets its quantity back out of the originating Purchase Order line's `receivedQty`, and the parent order's own status is allowed to revert from `COMPLETED` back to an earlier state as a result. | A return means the true accepted quantity from the supplier is now lower, and the order's status must reflect that truthfully rather than staying stuck at a stale `COMPLETED` (Decision Log, 2026-09-08 "Return to Supplier slice"). |
| BR-GSC-6 | Stock Transfer carries the source location's own current weighted-average unit cost across to both the decrease-at-source and increase-at-destination legs, rather than posting with no cost. | Posting with a null/zero cost would silently zero the destination's value on a brand-new balance row, losing real value on every transfer (Decision Log, 2026-09-08 "Stock Transfer slice"). |
| BR-GSC-7 | A Stock Transfer is rejected only when both the source and destination location are `REQUESTING_POINT` — a sister location returning surplus straight back to a store is legitimate; a store-to-sister or sister-to-store move is always allowed. | Transfer, unlike Stock Indent, has no inherent direction; the only invalid case is bypassing the store entirely between two sister locations (Decision Log, 2026-09-15 "Location-role gate..."). |
| BR-GSC-8 | Stock Transfer scope this pass is unbatched stock only — no batch/serial field on a transfer line. | Same simplification precedent Cycle Count's own posting step already established; real batch-aware transfer is separately-scoped work if a need surfaces (Decision Log, 2026-09-08 "Stock Transfer slice"). |
| BR-GSC-9 | Goods Receipt and Return to Supplier get sequential, human-readable document numbers (`GRN-<YYYYMM>-00001`, `SR-00001`); every other document in this module keeps the plain `#{id}` convention unless a future decision says otherwise. | Explicit user request, reversing a prior "no second document number" precedent for these four specific documents (Goods Receipt, Return to Supplier, Purchase Order, Quotation Request) only (Decision Log, 2026-09-22 "Sequential document numbers..."). |

## 4. Business Process / Workflow Narrative
Once a Purchase Order has been sent to a supplier (Procurement & Vendor Management sub-module), receiving staff create a Goods Receipt against it as deliveries physically arrive. Each delivered line is added against the specific ordered line it fulfils — quantity (optionally in a carton/box-level unit, converted to the base unit), unit cost (defaulting from the order but overridable), and, for a batch- or serial-tracked product, the batch/serial number and expiry date. The receipt can be built up over several sessions while `DRAFT`; confirming it locks the lines, posts a `RECEIPT` stock movement for each one, and rolls the parent order's status forward based on how much of it has now actually arrived.

Separately, store staff move stock laterally between two Inventory Locations with a Stock Transfer — building lines against a source and destination, then completing the transfer to post the movement pair and carry the item's value across intact. If a delivery turns out defective, wrong, damaged, or otherwise unacceptable after it has already been confirmed into stock, receiving staff raise a Return to Supplier against that confirmed receipt, picking specific previously-received lines and quantities (never more than was actually received, net of any prior return), and completing it to post the outbound `RETURN` movement and correct the parent order's received quantity and status.

## 5. Success Criteria
Not formally defined with a numeric KPI. Inferred from feature completeness: `RELEASE_3_MILESTONES.md` marks R3-M3 ("GRN & Stock Control") 100% Done — Goods Receipt, Stock Transfer, and Return to Supplier all shipped, closing Phase 3 in full per the Decision Log.

## 6. Assumptions & Constraints
- Assumes the Procurement & Vendor Management sub-module's Purchase Order already exists and has been sent (`ORDERED` or later) before a Goods Receipt can be raised against it.
- Assumes a single home/base cost basis (weighted-average) is acceptable for valuation on every decrease/transfer leg, not FIFO/FEFO.
- Assumes 0% over-receipt/over-return tolerance is an acceptable default in the absence of a documented tolerance policy.
- Assumes no invoice-matching requirement exists yet — 3-way match (PO/GRN/Invoice) from the source SRS is not built.

## 7. Known Gaps / Deferred
- No true 3-way match — no `Invoice` entity exists anywhere in this module; only PO-quantity-vs-received-quantity (2-way) is enforced.
- No FIFO/FEFO valuation — weighted-average only.
- Batch/serial-tracked products cannot be transferred (Stock Transfer) or returned (Return to Supplier); only received, adjusted, or disposed.
- No configurable over-receipt/over-return tolerance policy.
- See SRS.md §6 for the complete, precise list.
