# Business Requirements Document — Inventory Core Data Model

## 1. Executive Summary / Business Objective
`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` found that the business-supplied source SRS ("IHMS") was a hospital-specific procurement spec wearing a generic title, and — independent of that vertical lock-in — was missing the single most load-bearing entity an inventory system needs: a Stock Ledger (GAP-07). The business objective of this sub-module is to lay a genuinely industry-agnostic foundation — a configurable item catalog, a location model that reuses the parent platform's existing physical-space hierarchy instead of duplicating it, and an append-only stock ledger with a always-correct derived balance — that every other Inventory sub-module (procurement, receiving, requisition, assets, approvals, reporting) builds on without re-solving these same problems.

## 2. Stakeholders
- Store/warehouse staff (day-to-day catalog maintenance, stock movement recording, cycle counts)
- Procurement/catalog administrators (category/attribute/brand/UOM setup)
- Finance (relies on Stock Balance's valuation figures downstream, e.g. Stock Valuation Report)
- Platform/DB-driven Role Management administrators (permission assignment)
- End customer deployments outside the initial SKSCON client, since the module must remain reusable

## 3. Business Rules
| ID | Rule | Rationale |
|---|---|---|
| BR-CDM-1 | A Product's catalog code is system-generated as `<Category.shortCode>-<sequence>` and never editable by a user. | Prevents code collisions/typos and keeps codes predictable per category (Decision Log, 2026-09-22 "Auto-generated Product codes"). |
| BR-CDM-2 | Product name uniqueness is scoped to its Category, not global; a Product may additionally carry any number of Aliases. | `ProductAlias` already exists to let the same item be called different things across locations — forcing global name uniqueness would fight that (Decision Log, 2026-09-07 "Product slice"). |
| BR-CDM-3 | Inventory Locations must wrap a real, existing Campus Infrastructure `Room` — there is no separate, freestanding Location Master. | The parent platform already has a documented, reusable physical hierarchy; building a parallel one would duplicate it (Decision Log, 2026-09-07 "Reuse the existing Campus Infrastructure hierarchy"). |
| BR-CDM-4 | Every location that holds physical stock must have a real, non-nullable physical Room behind it — this constraint is never loosened, even to unblock a data-migration edge case. | Re-derived and confirmed the same day a blocked migration tempted a shortcut; a stock location without a real physical footprint is a modeling error, not a convenience (Decision Log, 2026-09-09 "Lab room-assignment migration"). |
| BR-CDM-5 | The Stock Ledger is append-only; nothing is ever updated or deleted from it. Stock Balance is always a derived, kept-in-sync view, never the primary record. | Closes GAP-07 and makes every other module's postings auditable and reconcilable to one source of truth (`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §5 principle 2). |
| BR-CDM-6 | A stock movement that would drive on-hand quantity negative is rejected outright. | Defensive validation adopted specifically because no reconciliation/cycle-count tooling existed yet to catch bad data after the fact (Decision Log, 2026-09-07 "Stock Tracking slice"). |
| BR-CDM-7 | Physical stock counts (Cycle Count) are always blind — the counter never sees the system's expected quantity before submitting a count. | Standard stock-take practice; explicitly the one thing GAP-09 called out as missing from the source SRS. |
| BR-CDM-8 | A `BATCH`-tracked product requires a batch number on every movement; a `SERIAL`-tracked product requires a serial number and is restricted to quantity 1 per movement; a `NONE`-tracked product (the default) enforces nothing. | `NONE` was deliberately made a true no-op so every pre-existing product's fully free-form behavior stayed byte-for-byte unchanged when the flag was introduced (Decision Log, 2026-09-11 "Serial/batch tracking-mode flag"). |
| BR-CDM-9 | A Product Variant's attribute values, tracking mode, pricing, and barcode are copied from the parent Product only once, at creation — never live-linked. | A genuine live-inheritance-with-override system would be a substantially bigger feature than this slice justified (Decision Log, 2026-09-11 "ProductVariant"). |
| BR-CDM-10 | A stranded (pre-variant) null-variant stock balance is never deleted or silently relabeled when converted to a variant — it is always left as an addressable "Unassigned" bucket, and conversion is a real, audit-logged pair of ledger postings. | Preserves a correct credit target for any future return of stock that was recorded before the conversion happened (Decision Log, 2026-09-15 "Convert-to-variant fix"). |
| BR-CDM-11 | The legacy `InventoryItem` (lab consumables) feature was retired outright, not migrated, once confirmed to hold zero real rows in any environment. | Migrating empty data would have been ceremony with no value; removing dead code/permissions instead (Decision Log, 2026-09-09 "Legacy InventoryItem retirement"). |

## 4. Business Process / Workflow Narrative
A catalog administrator first defines Categories (optionally nested) and, per category, the custom attributes items of that category should carry (e.g. shelf life, ISBN, calibration due) — this is what lets one generic Product Master serve wildly different item types without schema changes. UOMs and reusable UOM Conversion Templates are set up once and reused across products. A Product is then created against a Category and base UOM, picks up an auto-generated code, and can be enriched with a Brand, images, pricing, tax defaults, physical dimensions, a barcode, and — for products sold/issued in sub-variants (e.g. size/color) — one or more Product Variants.

Separately, a facilities/physical-space administrator (via Campus Infrastructure, outside this module) has already set up the Organization → Branch → Block → Floor → Zone → Room hierarchy. An Inventory administrator then marks specific Rooms as Inventory Locations, giving each a virtual display name (e.g. "Main Store") and a role (Store / Requesting Point / Both), optionally breaking a location down further into Racks and Bins for physical locator purposes.

Day to day, store staff record stock movements (receipts, adjustments, disposals — issues/transfers/returns are recorded by the sub-modules that own those workflows) against a Product/Variant/Location/Batch, which post to the Stock Ledger and immediately update the derived Stock Balance. Periodically, a Cycle Count is run against a Location: a blind count sheet is built (defaulting to every product currently holding balance there), counted, submitted, and any nonzero variance is reviewed and either approved (posting a real adjustment) or rejected as a counting error.

## 5. Success Criteria
Not formally defined with a numeric KPI. Inferred from feature completeness against the Phase 1 breakdown in `MILESTONES.md`: the catalog, stock-tracking core, and physical stock-count workflow are all shipped and considered "done" for their own scope; the one explicitly incomplete item is the (now largely moot) legacy migration ceremony (see §7).

## 6. Assumptions & Constraints
- Assumes Campus Infrastructure (Organization/Branch/Block/Floor/Zone/Room) and DB-driven Role Management are already in place in any deployment (stated prerequisite in `RELEASE_3_MILESTONES.md`).
- Assumes Room-level granularity is sufficient for stock locations in the current phase (no Zone-level locations yet).
- Assumes weighted-average costing is an acceptable stand-in for FIFO/FEFO until real costing-method requirements are gathered.

## 7. Known Gaps / Deferred
- The legacy `InventoryItem` → new-core migration (R3-M8) never happened in the originally-planned sense — the old table had no real data, so the feature was retired rather than migrated. No stock data-migration risk remains from this decision, but the milestone tracker still technically shows R3-M8 as "Not Started."
- FIFO/FEFO costing, Zone-level locations, a Stock Ledger browsing screen, bulk/automatic stranded-balance conversion, and batch/serial support on the Requisition & Issue sub-module's add-line forms are all real, acknowledged gaps — see SRS.md §6 for the complete, precise list.
