# Business Requirements Document — Procurement & Vendor Management

## 1. Executive Summary / Business Objective
`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` assessed the source SRS's Procurement (§3.1) and Vendor Management (§3.2) sections as the most directly reusable parts of the original hospital-shaped spec, once decoupled from Hospital Information System integration. The business objective of this sub-module is a complete, industry-agnostic source-to-order chain — supplier onboarding, negotiated pricing, demand capture, optional competitive quoting, and purchase ordering — that any deployment can run without a hospital's demand-source assumptions, while leaving real accounting/tax-engine integration (GAP-02/03/04) explicitly deferred rather than half-built.

## 2. Stakeholders
- Requesting-location staff (raise Purchase Requisitions)
- Procurement/purchasing staff (Suppliers, Rate Contracts, Quotation Requests, Purchase Orders)
- Finance/admin (Tax, Currency, Jurisdiction settings)
- Suppliers (referenced as master data; no self-service portal exists)
- Receiving staff (downstream consumer of a sent Purchase Order — GRN sub-module)

## 3. Business Rules
| ID | Rule | Rationale |
|---|---|---|
| BR-PV-1 | A Purchase Order carries no approval gate of its own — it ships with a plain status field; real multi-level/parallel approval routing is built once, generically, in the Approvals sub-module rather than twice. | Decision Log, 2026-09-08 "Phase 2 kickoff". |
| BR-PV-2 | Supplier bank account number, tax registration ID, and legal registration number are masked to their last 4 characters for any user holding only view (not manage) access. | Security Lead round decision — same masking principle the source SRS applies to bank/PAN data extended to this module (Decision Log, 2026-09-07 Security round; implemented 2026-09-08). |
| BR-PV-3 | A Rate Contract Line's negotiated rate overrides a Vendor Product Mapping's price only while the parent contract is active and today falls within its date window — resolved fresh at read time, never stored. | A lapsing contract or an edited line must be reflected immediately (Decision Log, 2026-09-08 "VendorProductMapping slice"). |
| BR-PV-4 | The Wanted List's shortage calculation is real MRP netting (`ReorderLevel − (QtyOnHand + QtyOnOrder)`), not a naive on-hand-vs-reorder-level check, and nets against open Purchase Requisition lines so the same shortage is never re-flagged after it's already been requested. | Explicit user direction to "think globally like an ERP application," not replicate a narrower reference pattern (Decision Log, 2026-09-08 "Wanted List slice"). |
| BR-PV-5 | A Quotation Request is entirely optional — direct-to-PO conversion from an approved requisition line remains available at all times; awarding is per-line (a request's lines may land on different winning suppliers, producing more than one resulting PO); no minimum quote count is required before awarding. | Confirmed in a full specialist round before building the RFQ feature, since it reverses an earlier explicit deferral (Decision Log, 2026-09-16 "Quotation Request (RFQ) slice"). |
| BR-PV-6 | A requisition line already live on a non-rejected Quotation Request line cannot also be picked up by a direct Purchase Order, and vice versa. | Prevents the same demand being double-booked into two different Purchase Orders while keeping the optional path genuinely optional (Decision Log, 2026-09-16). |
| BR-PV-7 | Purchase Order line tax/jurisdiction (interstate vs. intrastate) is resolved and its component amounts snapshotted once, at line-creation time, never recomputed live. | Matches the module's "amounts computed and stored once" precedent, so GST filing figures stay explainable even after a later TaxRule edit (migration V477 design comment). |
| BR-PV-8 | Product codes and, separately, Quotation Request/Purchase Order/Goods Receipt/Return-to-Supplier document numbers are both auto-generated and never user-typed; existing rows may be retroactively renumbered only via an explicit, permission-gated admin action, never silently. | User-requested reversal of the earlier "no second document number" convention, deliberately scoped to these four documents only (Decision Log, 2026-09-22 "Sequential document numbers"). |

## 4. Business Process / Workflow Narrative
A Supplier is registered and separately approved (approval is not implied by save). Suppliers can be linked to negotiated Rate Contracts and per-product Vendor Product Mappings that establish default pricing. Demand originates as a Purchase Requisition raised against a Location (either by a person, or auto-flagged by the nightly/on-demand Wanted List job netting current stock against reorder levels and already-open demand); each requisition line is approved or rejected individually. An approved line can go one of two ways: picked up directly into a Purchase Order against a chosen Supplier (unit price defaulting from the effective Vendor Product Mapping/Rate Contract rate, tax resolved by jurisdiction), or — optionally — first routed through a Quotation Request that invites multiple suppliers, records their quotes per line, and lets each line be awarded independently before converting all awarded lines (grouped by winning supplier) into one Purchase Order per supplier. Once a Purchase Order is sent ("ordered"), its status advances automatically as receipts land against it in the GRN sub-module, or it can be force-closed early with a reason at any point before full receipt.

## 5. Success Criteria
Not formally defined with a numeric KPI. Inferred from feature completeness: `MILESTONES.md` Phase 2 and `RELEASE_3_MILESTONES.md` R3-M2 both record 100% completion — "Purchase request → purchase order flow," "Supplier registration, approval, and rate management," and "Price comparison and rate-contract support" are each marked done for this phase's defined scope.

## 6. Assumptions & Constraints
- Assumes a single domestic tax jurisdiction (one home state compared against each supplier's own state) rather than a multi-country tax regime.
- Assumes currency conversion is informational only — no deployment currently needs a document's stored total to be recomputed across currencies.
- Assumes suppliers are contacted and quoted outside the system (phone/email); no outbound integration exists to actually transmit a Quotation Request.

## 7. Known Gaps / Deferred
See SRS.md §6: no generalized multi-region tax engine, no accounting/ERP posting connector, no true multi-currency computation, no outbound supplier communication, no variant picker on Quotation Request conversion. Additionally, the tax-engine migration (V477) cites a "2026-09-09 GAP-02 pickup" decision-log entry that does not actually exist in `DECISION_LOG.md` — a documentation-process gap, not a functional one, flagged here for correction.
