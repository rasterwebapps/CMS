# Inventory Management — Progress & Phases

> **Purpose of this document:** a stakeholder-facing view of what the Inventory Management module will cover, grouped into phases, and where things currently stand. **Deliberately no dates or timelines** — phases show what's covered and their sequence, not when. This file is a living reference: as scope changes or work completes, it gets updated in place and the status markers below get moved — it is not a one-time snapshot.
>
> Looking for engineering-level detail (data model, technical decisions, open questions)? See [`README.md`](README.md) for the full documentation index. This file intentionally stays non-technical for sharing outside the engineering team.

**Last updated:** 2026-09-08

---

## What this is

Inventory Management is a new, standalone module being built for the platform — covering purchasing, stock tracking, equipment/asset management, and related approvals. It's designed as a general-purpose module usable by any type of organization managing physical stock and equipment, not built around the assumptions of one particular kind of institution. It's being designed to plug into the platform's existing location/campus structure rather than duplicate it.

---

## Status at a glance

| Phase | Covers | Status |
|---|---|---|
| 0 | Discovery &amp; Design | 🟡 In Progress |
| 1 | Foundation — Catalog, Stock Tracking &amp; Locations | 🟡 In Progress |
| 2 | Purchasing &amp; Suppliers | ✅ Done |
| 3 | Receiving &amp; Stock Movement | ✅ Done |
| 4 | Requests, Issues &amp; Returns | 🟡 In Progress (auto-restocking deliberately deferred — needs real product-policy input) |
| 5 | Equipment &amp; Asset Management | ✅ Done |
| 6 | Budgets &amp; Approvals | ⬜ Not Started |
| 7 | Gate Pass, Vendor-Owned Stock &amp; Service Requests | ⬜ Not Started |
| 8 | Reporting &amp; Dashboards | ⬜ Not Started |

**Status legend:** ⬜ Not Started · 🟡 In Progress · ✅ Done

---

## Phase 0 — Discovery & Design

**Status: 🟡 In Progress**

**Covers:** Reviewing the requirements document supplied for this module, checking it against real needs across different kinds of organizations, deciding what's actually in scope, and producing the first design of how the system will be structured before any building starts.

**Done so far:**
- Reviewed the full requirements document supplied by the business team and identified what it was missing for general-purpose use
- Confirmed the scope: a standalone module that works for different kinds of organizations without being built around any one of them specifically
- Found and reused existing parts of the platform instead of duplicating them (the location/campus structure, and the audit trail that records who changed what)
- Produced a first draft of the complete data design (what information the system tracks and how it connects)

**Still to do:**
- Design draft under review
- One open question on how a future stand-alone version of this module (for a deployment that only needs Inventory and the location structure, nothing else) would be packaged — not yet decided, doesn't block the design review
- A data-quality check needed against the existing lab-inventory feature before it can be migrated (see Phase 1)

---

## Phase 1 — Foundation: Catalog, Stock Tracking & Locations

**Status: 🟡 In Progress**

**Covers:** The basic building blocks everything else depends on — a catalog of items (with categories, images, alternate names), units of measure, a record of what's in stock and where, and connecting stock locations to the platform's existing campus/location structure. Also includes bringing the platform's existing simple lab-inventory feature onto this new, more capable system.

**Doing now:** The item catalog, the core stock-tracking record, and physical stock counts are all in place — items can be received, adjusted, or written off at a named stock location, with a running record of what's on hand and its value, and that record can now be checked against a physical count with variances reviewed before they're posted. Only the lab-inventory migration is left before this phase is fully done.

**Todo:**
- Item catalog with configurable categories (so different kinds of items — a lab chemical, a library book, an IT asset — can each carry the details relevant to them) — ✅ done (categories, units of measure, and the product catalog itself, each with its own screen; a photo/image per product is intentionally not included yet — see "Not yet scheduled" below)
- Core stock-tracking record (what's on hand, where, in what batch, at what value) — ✅ done (stock locations tied to existing campus rooms, a Stock Balance report, and a way to record receipts/adjustments/write-offs; issuing stock to a requester and moving stock between locations wait on the workflows those depend on, which come in later phases)
- Physical stock counts / reconciliation — ✅ done (a blind count against a location — the counter doesn't see the system's expected quantity — with any variance reviewed and either approved, which updates stock, or rejected as a counting error)
- Migrate the existing simple lab-inventory feature onto the new system — confirmed deferred until the rest of the Foundation phase is in place; see "Not yet scheduled" note below

---

## Phase 2 — Purchasing & Suppliers

**Status: ✅ Done**

**Covers:** Raising a request for items, getting and comparing quotes, issuing purchase orders, and registering and managing the suppliers those orders go to (including their approved rates and any standing rate agreements).

**Shipped:** Suppliers can be registered, approved, and managed, with a standing rate agreement (Rate Contract) tracked per supplier, a small named-tax-rate list (Tax Rule), a product linked to a supplier with its own rate (Vendor Product Rate) — optionally overridden by a negotiated per-product rate line on that supplier's Rate Contract while it's active — a location can submit a Purchase Requisition (a request to buy specific products), reviewed and approved or rejected per product, a Wanted List auto-flags products that have fallen below their configured reorder level (checked nightly, or on demand), already accounting for anything already requested so the same shortage isn't flagged twice, and a Purchase Order can now be raised against a supplier by picking up one or more approved requisition lines — sent to the supplier, with an optional early Force Close and a running total including tax. Receiving against a Purchase Order (matching quantities, updating stock) is Phase 3's job, not this phase's.

**Todo:**
- Purchase request → purchase order flow — ✅ done (Wanted List, Purchase Requisition, and Purchase Order all shipped)
- Supplier registration, approval, and rate management — ✅ done (Suppliers and Rate Contracts, each with their own screen; approving a supplier is a distinct step from editing one)
- Price comparison and rate-contract support — ✅ done for this phase's scope (a product's rate per supplier is now visible via Vendor Product Rates, with a Rate Contract's negotiated rate overriding it while active; this stays a simple rate lookup rather than a dedicated compare-quotes tool)

---

## Phase 3 — Receiving & Stock Movement

**Status: ✅ Done**

**Covers:** Recording deliveries against purchase orders, checking quantities and prices match what was ordered, tracking batches and expiry where relevant, and moving stock between locations.

**Shipped:** A delivery can be recorded against a sent purchase order — built up as a draft sheet against the order's still-open lines (over-receipt is blocked, not just warned about), then confirmed, which posts the received quantity to stock and rolls the order's own status forward (Ordered → In Progress/Partially Completed → Completed) as its lines get received. Batch/serial number and expiry are captured on the receipt line itself, reusing the same optional fields the stock-movement form already has. Stock can be moved between two locations with a simple draft-then-complete transfer that carries the source's current value across rather than zeroing it. A confirmed receipt's stock can be returned to the supplier it came from, which correctly nets back out of the order's received quantity — including reverting a fully-"Completed" order back to "Partially Completed" if the true accepted quantity drops.

**Todo:**
- Goods-receipt recording and matching against orders — ✅ done
- Batch/expiry tracking — ✅ done (captured on the receipt line, posted the same way Record Stock Movement already does)
- Transfers between locations — ✅ done (a simple draft-then-complete move between two locations, carrying the source's current value across rather than zeroing it)
- Returning defective or incorrect deliveries to the supplier — ✅ done

---

## Phase 4 — Requests, Issues & Returns

**Status: 🟡 In Progress**

**Covers:** A department or location requesting items from stock, issuing them out, and handling returns — plus a general "borrow and return" capability for equipment that gets loaned out and is expected back (sports equipment, hostel items, and similar), rather than consumed.

**Doing now:** A location can request on-hand stock be issued from another location (typically a central store) — reviewed and approved or rejected per product, with approval immediately posting a real stock movement out of the issuing location (distinct from Phase 2's Purchase Requisition, which requests *buying* from a supplier rather than issuing from stock already on hand). Previously-issued stock can be returned back to the issuing location. A loanable product (equipment expected back, not consumed) can be issued to a named borrower with an expected-return date and marked returned with a condition note — overdue is shown live, computed from today's date rather than a background job.

**Todo:**
- Requisition (request-and-issue) workflow — ✅ done
- Auto-restocking when items run low — deliberately deferred; the original plan for this didn't hold up to business scrutiny (see the decision log) and needs real deployment-policy input before it can be designed properly
- Internal returns — ✅ done (return previously-issued stock back to the issuing location from an approved request line)
- Generic loan/return tracking for borrowed equipment — ✅ done

---

## Phase 5 — Equipment & Asset Management

**Status: ✅ Done**

**Covers:** Tracking equipment and other owned assets through their full life — from purchase, through maintenance schedules and service contracts, to eventual retirement or disposal — including the standard depreciation calculations finance needs.

**Shipped:** Individual physical units of a product (a specific laptop, a specific microscope — distinct from the catalog/stock-ledger's aggregate quantity tracking) can be registered with a unique asset tag, optionally traced back to the delivery they were received against, and moved through an open-ended status lifecycle (Available/In Use/Under Maintenance/Retired) as real-world use isn't a strict linear workflow. Recurring or one-off maintenance visits are tracked with live overdue flagging, and vendor service contracts reuse the existing Supplier master. Book value is computed live using standard straight-line depreciation. Disposing an asset requires a documented reason, and writes off any remaining on-hand stock for its product at its location.

**Todo:**
- Asset register and lifecycle tracking — ✅ done
- Maintenance scheduling and service contracts — ✅ done (recurring or one-off maintenance visits with live overdue tracking; vendor service contracts reusing the existing Supplier master)
- Depreciation — ✅ done (standard straight-line, computed live from an asset's own purchase value/date/useful life/salvage value — no new screen, the Asset Register list gained a Book Value column)
- Disposal/write-off workflow — ✅ done

---

## Phase 6 — Budgets & Approvals

**Status: ⬜ Not Started**

**Covers:** Setting spending limits per location or area, and the multi-level sign-off process a purchase or request goes through before it's approved — including handling exceptions (urgent purchases, single-supplier situations) with a documented reason.

**Todo:**
- Budget allocation and tracking
- Multi-level approval routing, including parallel sign-off where more than one person needs to weigh in at once
- Exception handling with documented reasons

---

## Phase 7 — Gate Pass, Vendor-Owned Stock & Service Requests

**Status: ⬜ Not Started**

**Covers:** Tracking items and equipment physically leaving or entering a premises (for repair, loan, or disposal), managing stock that a supplier owns but keeps on-site until it's used, and a general internal service/complaint ticketing capability.

**Todo:**
- Outward/inward gate pass tracking, including overdue alerts for items that should have come back
- Vendor-owned ("consignment") stock tracking and periodic billing
- Internal service ticketing

---

## Phase 8 — Reporting & Dashboards

**Status: ⬜ Not Started**

**Covers:** The dashboards and reports that pull together everything from the phases above — stock positions, spending, price comparisons, and turnaround times — for day-to-day use and management review.

**Todo:**
- Everything in this phase depends on the phases above being in place first

---

## Not yet scheduled into a phase

A couple of items are confirmed as part of the plan but not yet assigned to a specific phase above — called out here so they aren't mistaken for forgotten:

- **Bringing the existing library feature onto this system.** Deliberately deferred — the library feature keeps working as it is today until this is scheduled.
- **Packaging a stand-alone version of this module** (just Inventory plus the location structure, for a deployment that doesn't need the rest of the platform). The design accounts for this being possible later, but exactly how it gets built and packaged is still an open question.
- **Photos on a product's catalog entry.** The product catalog itself (name, code, category, units, alternate names, custom per-category details) is done, but attaching a photo to a product isn't yet — it needs its own upload capability, which is a real piece of work in its own right rather than an extra field.

---

> Updated whenever scope changes or a phase's status moves — check the **Last updated** date above, not just this document's existence, to know how current it is.
