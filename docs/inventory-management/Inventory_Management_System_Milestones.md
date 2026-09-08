# 📦 Core Infrastructure & Inventory Management — Milestones & Feature Showcase

> Built by **Raster**. A **standalone, industry-agnostic** platform capability — the physical-location backbone (**Core Infrastructure**) and the procurement/stock/asset ERP built on top of it (**Inventory Management**). Designed from day one to work for *any* organization that manages buildings, rooms, and physical stock — not built around the assumptions of one kind of institution.

**Snapshot as of 2026-09-08:**

| | |
|---|---|
| 🏗️ **57** database migrations shipped across both modules | 🖥️ **80** application screens/routes live |
| ⚙️ **16** backend service engines | 🗄️ **26** data entities under active management |
| 🧩 **33** frontend modules/components built | 🚀 **1** foundational module complete, **1** major ERP module in active build |

*Every number above is counted directly from the codebase, not estimated.*

---

## 🏆 Why this matters

### 🏢 1. A Real Physical-World Digital Twin, Not Just a Dropdown List
Most systems store "location" as a single free-text field. Here, the entire physical estate — **Organization → Branch → Block → Floor → Zone → Room** — is modeled as a true hierarchy, each level configurable with its own purpose categories and sub-types. Floor plans can even be **imported as diagrams** and rooms mapped visually onto them. This one investment now silently powers Hostel room allocation *and* Inventory stock locations — build once, reuse everywhere, instead of every module inventing its own idea of "where."

### 🏭 2. A Second Product Being Born Inside the First
Inventory Management isn't a bolted-on "asset register." It's being engineered as a genuine, **sellable-on-its-own ERP module** — the same category of system organizations pay dedicated procurement/stock vendors for — that happens to plug into this platform's existing location structure rather than duplicating it. Every design decision is deliberately checked against "would this work for a hospital, a factory, or a college?" before it's built.

### 🔄 3. Smart, Self-Driving Stock — Not Just a Ledger
The system doesn't just record what happened — it watches and acts:
- **Auto-reorder detection** — products that fall below their configured minimum are automatically flagged, nightly or on demand, and already account for anything already requested so the same shortage is never flagged twice
- **One-click bulk requisitioning** — every flagged shortage across the organization can be picked up together into a single purchase request in one step
- **Negotiated-rate intelligence** — a supplier's standard product rate is automatically overridden by a live, time-bound rate contract when one exists, with no manual lookup required
- **Blind physical counts** — stock counters never see the system's expected quantity, so variances reflect reality, not confirmation bias, with every variance reviewed before it touches the books

### 🔐 4. Enterprise-Grade Approval Discipline, Built In From the Start
Every purchase requisition line can be approved or rejected **individually** — not as an all-or-nothing document — and every supplier must be explicitly approved before it can transact, as a distinct, auditable step from simply registering one. This is procurement governance normally reserved for large-enterprise ERP suites, present here from the very first release.

---

## 🏗️ Core Infrastructure — Complete Foundation

The location backbone that Hostel Management, Inventory Management, and every future physical-asset feature builds on.

| Capability | Delivered |
|---|---|
| Full **Organization → Branch → Block → Floor → Zone → Room** hierarchy | ✅ |
| Configurable **Room Purpose Categories** (classroom, lab, storage, ward, office — any category an organization needs) | ✅ |
| Configurable **Room Sub-Types** for finer-grained classification within a purpose | ✅ |
| **Visual floor-plan import** — upload a floor diagram and map real rooms onto it | ✅ |
| Reused as the shared backbone for Hostel room allocation and Inventory stock locations | ✅ |

**Business impact:** this hierarchy is built exactly once and every module that needs "a physical place" — hostel rooms, lab equipment, stock shelves — plugs into it, instead of each module reinventing and duplicating location data that inevitably drifts out of sync.

---

## 📈 Inventory Management — Phase Progress

| Phase | Covers | Status |
|---|---|---|
| **0. Discovery & Design** | Requirements review, general-purpose scope validation, core data-model design | 🟡 In Progress |
| **1. Foundation — Catalog, Stock & Locations** | Item catalog with configurable categories, units of measure, stock-tracking ledger, physical cycle counts, stock locations tied to Core Infrastructure | 🟡 In Progress |
| **2. Purchasing & Suppliers** | Supplier onboarding & approval, rate contracts, vendor product rates, purchase requisitions, auto-reorder ("wanted list") | 🟡 In Progress |
| **3. Receiving & Stock Movement** | Goods-receipt matching against orders, batch/expiry tracking, transfers between locations, supplier returns | ⬜ Not Started |
| **4. Requests, Issues & Returns** | Requisition-to-issue workflow, auto-restocking, internal returns, generic loan/return tracking for borrowed equipment | ⬜ Not Started |
| **5. Equipment & Asset Management** | Asset register and lifecycle, maintenance & service contracts, depreciation, disposal | ⬜ Not Started |
| **6. Budgets & Approvals** | Spending limits per location, multi-level/parallel approval routing, documented exception handling | ⬜ Not Started |
| **7. Gate Pass, Vendor Stock & Service Requests** | Outward/inward gate pass with overdue alerting, vendor-owned ("consignment") stock, internal service ticketing | ⬜ Not Started |
| **8. Reporting & Dashboards** | Stock position, spend, price-comparison and turnaround dashboards | ⬜ Not Started |

### What's live today

- **Full item catalog** — categories, units of measure, and products, each independently configurable, each with its own management screen
- **Live stock-tracking ledger** — receipts, adjustments, and write-offs recorded per stock location, with a running on-hand quantity and value at all times
- **Physical cycle counts** — blind counting against a location, with variances reviewed and either approved (updating stock) or rejected as a counting error
- **Supplier lifecycle** — registration, a distinct approval step, and standing rate agreements (Rate Contracts) tracked per supplier
- **Vendor product rates** — a per-supplier price for any product, automatically overridden by a negotiated Rate Contract line while it's active
- **Purchase Requisitions** — a location can request specific products; each line is reviewed and approved or rejected independently
- **Auto-reorder ("Wanted List")** — products below their configured reorder level are automatically flagged and can be bundled into a requisition in one step
- **Named tax rules** ready for the upcoming Purchase Order stage

---

## 🗺️ What's Next — Full Roadmap Ahead

The immediate next step, followed by every remaining phase already scoped for this module, in build order:

**Right now:** Ship the **Purchase Order** screen — the last piece of Phase 2, the direct next step after an approved Purchase Requisition.

| Phase | Business capability being added | Why it matters |
|---|---|---|
| **3. Receiving & Stock Movement** | Match incoming deliveries against what was ordered, track batches and expiry dates, move stock between locations, and return defective or incorrect deliveries to the supplier | Closes the loop from "ordered" to "actually on the shelf, verified correct" — the single biggest source of stock discrepancies in any organization |
| **4. Requests, Issues & Returns** | Any department or location can request items from stock and have them issued, with automatic restocking when levels run low; a generic borrow-and-return capability for equipment that's loaned out and expected back (rather than consumed) | Turns stock from "something warehoused" into something actively used day-to-day, without manual paper requisitions |
| **5. Equipment & Asset Management** | Full asset register and lifecycle tracking from purchase through maintenance schedules and service contracts to eventual retirement, including standard depreciation calculations | Gives finance and operations one place to see everything the organization owns, its condition, and its book value — instead of a spreadsheet that goes stale |
| **6. Budgets & Approvals** | Spending limits per location or department, multi-level sign-off (including parallel approval where more than one person must weigh in), and documented exception handling for urgent or single-supplier purchases | Enterprise-grade financial control and audit trail on every rupee spent through the module |
| **Financial integration via OneBook** | Supplier payments, purchase-order settlement, and budget reconciliation routed through **OneBook** — the same platform already live in this system for fee collection, commission payouts, and refunds, and being built out into a full financial/accounting backbone that goes **beyond what a tool like Tally offers today** | Sets up a single, purpose-built financial system for the entire organization — not a bolted-on payment gateway bridging to third-party accounting software, but the accounting system itself, built for exactly this business |
| **7. Gate Pass, Vendor-Owned Stock & Service Requests** | Track items physically leaving or entering the premises (repair, loan, disposal) with overdue alerts; manage stock a supplier owns but keeps on-site until used, billed periodically; general internal service/complaint ticketing | Closes the remaining real-world gaps — nothing leaves or enters the building untracked, and supplier-owned stock stops being an off-books blind spot |
| **8. Reporting & Dashboards** | Stock position, spend analysis, price comparison across suppliers, and turnaround-time dashboards, pulling together everything from every phase above | The payoff phase — leadership gets a live, single-screen view of the entire supply chain instead of assembling reports by hand |

**Beyond the phase list:** carry the design forward toward a **fully standalone deployment** of Core Infrastructure + Inventory Management — packaged so an organization that needs only this capability can run it with nothing else from the wider platform, opening the door to offering it as its own product.

---

*This is a plain-language summary intended for business sharing — no internal engineering task IDs or timelines included. For the full technical tracker, see `docs/RELEASE_3_MILESTONES.md`, `docs/inventory-management/MILESTONES.md`, and `docs/inventory-management/README.md`.*
