# Business Requirements Document — Requisition & Issue

## 1. Executive Summary / Business Objective
`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §2B (GAP-26/GAP-28) found the source SRS's internal-request model ("ward request," hospital-only, HIS-coupled) needed a genuinely generic replacement before it could serve any deployment on equal footing, and separately flagged that no ERP-standard default could safely automate *how* a shortage gets resolved (transfer surplus vs. buy new) without real deployment policy input. This sub-module's business objective is twofold: give any requesting location a reliable way to draw already-on-hand stock from a store (with real department-head and store-side sign-off, not a rubber stamp), and — once the user supplied that missing policy input — automatically detect a location running low and raise the request before someone notices stock is out, without ever silently auto-committing a sourcing decision on the business's behalf.

## 2. Stakeholders
- Requesting-location staff (raise indents, borrow loanable items)
- Department-head-equivalent approvers (sign off on a requested line before the store acts on it)
- Store/issuing-location staff (decide how each approved line gets fulfilled)
- Reorder/catalog administrators (configure per-location auto-indent policy)
- Platform/DB-driven Role Management administrators (permission assignment)

## 3. Business Rules
| ID | Rule | Rationale |
|---|---|---|
| BR-RI-1 | "Indent" is the deployment's own name for this concept — it is the same entity as the originally-built Stock Issue Request, renamed throughout, never a second parallel document. | Explicit user direction, rejecting a second entity in favor of a rename (Decision Log, 2026-09-21 "OC-206 reopened... Stock Issue Request renamed to Stock Indent"). |
| BR-RI-2 | A Stock Indent's location pair is asymmetric: the requesting location must be able to request (`REQUESTING_POINT`/`BOTH`) and the issuing location must be able to issue (`STORE`/`BOTH`) — a request always flows sister → store, never the reverse. | An indent has an inherent direction, unlike a Stock Transfer's symmetric push (Decision Log, 2026-09-15 "Location-role gate..."). |
| BR-RI-3 | Manual and auto-generated indents share exactly one lifecycle — the two-step (department-head approval, then store fulfillment decision) split applies to both, not just auto-generated ones. | Explicit, confirmed user decision reversing the original single-step "approve = issue" shape for manual indents too (Decision Log, 2026-09-21 "OC-206 reopened..."). |
| BR-RI-4 | Reorder policy for the internal auto-indent trigger is scoped per (product, location), separate from `Product`'s own global reorder level/quantity, which keep feeding the Wanted List's supplier-side job unchanged. | The two are genuinely different signals — one an internal transfer request, one a purchase trigger — and merging them was explicitly rejected as incorrect (Decision Log, 2026-09-08 "Auto-restocking (OC-206) skipped..."). |
| BR-RI-5 | Saving a Reorder Configuration with auto-indent enabled requires the location to already have a default supplying store set; the store can still be unset afterward without retroactively invalidating the config, surfaced instead as a drift warning. | A config can never be created already pointing nowhere, but there is no DB-level dependency forcing the two to stay in sync afterward — treated as a warning, not a hard failure, once already configured (Decision Log, 2026-09-21 "OC-206 reopened, Phase B..."). |
| BR-RI-6 | Store-side fulfillment is always a manual decision among four outcomes (fulfill / fulfill via transfer-in / raise a purchase requisition / deny) — nothing auto-posts. | No ERP-standard sourcing-preference default could safely be assumed without real deployment policy input; starting manual and tunable later, once real usage is observed, was the explicit resolution (Decision Log, 2026-09-08 "Auto-restocking (OC-206) skipped...", resolved 2026-09-21). |
| BR-RI-7 | Transfer-in fulfillment always routes via the issuing store (surplus location → store → requester, two movements), never a direct hop from the surplus location to the requester. | So the store's own stock ledger always reflects everything it actually dispatched (Decision Log, 2026-09-21 "OC-206 reopened, Phase D..."). |
| BR-RI-8 | Raising a Purchase Requisition from an indent line is terminal for that line — the eventual restock is a separate, disconnected event via that requisition's own PO/GRN cycle, not tracked back to the original indent line. | Keeps the indent's own lifecycle simple and avoids inventing a cross-document tracking link that wasn't asked for (Decision Log, 2026-09-21 "OC-206 reopened, Phase D..."). |
| BR-RI-9 | A Loanable Item Issue is a standalone tracking record, deliberately not posted to the Stock Ledger/Stock Balance. | The item isn't consumed — it's expected back; treating it as a stock movement would either wrongly remove it from on-hand stock or require a new "on-loan quantity" concept, judged out of scope for this slice (Decision Log, 2026-09-08 "Loanable Item Issue slice..."). |
| BR-RI-10 | A borrower on a Loanable Item Issue is captured as plain free text, with no dedicated borrower entity. | Per the standing no-vertical-branding rule — a college's students and a hospital's staff are both just "a borrower" to this generic core (Decision Log, 2026-09-08 "Loanable Item Issue slice..."). |

## 4. Business Process / Workflow Narrative
A requesting location (a department, lab, ward, or any `REQUESTING_POINT`/`BOTH` location) raises a Stock Indent against a specific issuing store, adding one line per product it needs with a requested quantity. Once submitted, each line first goes to a department-head-equivalent approver, who either approves or rejects it — approval alone posts nothing to stock, it only hands the line to the store. The store then makes its own fulfillment call per line: issue directly from its own stock, pull in surplus from a third location first (via a real Stock Transfer that always routes through the store itself) and then issue, raise a Purchase Requisition if nothing is available anywhere, or deny the line outright. Once stock has actually been issued, the requesting location can return some or all of it back to the store as an Internal Return, up to what was originally issued.

Separately, a reorder/catalog administrator configures a per-(product, location) Reorder Configuration for any requesting location that should be auto-monitored — a reorder level, a reorder quantity, an optional stock ceiling, and a default supplying store. Every night (and on demand), the Auto-Indent job nets each configured product's on-hand quantity against its reorder level, bundles every shortfall for the same requesting-location/store pair into one new Stock Indent, and lets that indent flow through the exact same two-step approval/fulfillment lifecycle a manually raised one would.

Independently of all the above, any product flagged as loanable (sports equipment, tools, and similar non-consumed items) can be issued out to a named borrower with an expected return date and optional deposit, and later marked returned with a condition assessment — a lightweight, single-action workflow with no approval step, since it never touches the Stock Ledger.

## 5. Success Criteria
Not formally defined with a numeric KPI. Inferred from feature completeness: `RELEASE_3_MILESTONES.md` marks R3-M4 ("Requisition & Issue") 100% Done — Stock Indent (with its rename), Internal Return, Loanable Item Issue, and Auto-Indent (all four phases: rename, per-location config, auto-detection, two-step lifecycle) all shipped per the Decision Log's 2026-09-21 entries.

## 6. Assumptions & Constraints
- Assumes the Core Data Model sub-module's `InventoryLocation.locationRole` topology (`STORE`/`REQUESTING_POINT`/`BOTH`) is already correctly set up for every location this sub-module's screens will reference.
- Assumes a two-tier (Requesting Point → Store) auto-indent topology is sufficient; multi-tier chaining (Store → a bigger Main Store) is not assumed or designed for.
- Assumes store-side fulfillment should remain a human decision indefinitely absent a specific future ask to automate a sourcing preference.
- Assumes a loanable item's "on loan" state never needs to be reflected in on-hand stock quantity for this phase.

## 7. Known Gaps / Deferred
- No real sourcing-preference automation for store fulfillment (deliberate, policy-gated deferral, not an oversight).
- Reorder Configuration/Auto-Indent skip any product with active variants.
- Batch/serial-tracked products cannot be indented or internally returned.
- No multi-tier auto-indent.
- See SRS.md §6 for the complete, precise list.
