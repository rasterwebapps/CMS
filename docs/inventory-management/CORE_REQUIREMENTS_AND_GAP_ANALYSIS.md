# 📦 Inventory Management System — Core Requirements & Gap Analysis

> See [`README.md`](README.md) for the document index and [`DECISION_LOG.md`](DECISION_LOG.md) for the running record of decisions this document references.

> **Status:** Draft v0.5 — full specialist review round completed, and a first ER diagram & module-boundary draft now exists at [`ER_DIAGRAM_AND_MODULE_BOUNDARIES.md`](ER_DIAGRAM_AND_MODULE_BOUNDARIES.md) (2026-09-07, see §7 and `DECISION_LOG.md`); scope and design not yet approved.
> **Purpose:** Review `SRS_v3.2_Updated.pdf` (the "IHMS" spec supplied by the business team, archived at [`source/SRS_v3.2_Updated.pdf`](source/SRS_v3.2_Updated.pdf)) against the goal of a **single, standalone, industry-agnostic Inventory Management module that functionally covers both a College's and a Hospital's needs without ever branding any part of itself as belonging to either** — identify what's missing or locked to one vertical, and lay down the core skeleton to start work from. College and Hospital are the two concrete verticals its functional coverage is checked against (§2B), but the module itself must read as generic and industry-neutral throughout — no "Healthcare Pack," no "College Pack," no vertical-named anything (see §7 Decision 2, revised).
> **Companion inputs:** the existing SKSCMS `InventoryItem` module (`backend/src/main/java/com/cms/model/InventoryItem.java`) — a minimal lab-consumables tracker (name, itemCode, lab FK, quantity, minimumQuantity, unit, lastRestocked) that this initiative will supersede or absorb.

---

## 1. What SRS v3.2 Actually Covers

The source document (197 pages, "Inventory Management System (IMS)", v1.0, July 2026) is thorough and well-engineered *within its assumed vertical*. Every requirement follows a consistent template (Priority/Status, Description, Preconditions, Inputs, Processing Logic, Outputs, Postconditions, Business Rules, Exception Handling) — good discipline worth keeping.

| # | Module | Pages | Req range | Scope |
|---|--------|-------|-----------|-------|
| 3.1 | Procurement & PO Management | 9–31 | P001–P021 | PR→RFQ→PO, rate contracts, cash PO, spot PO, addendums, landed cost, multi-currency, marketplace import, consolidated/urgent approval emails, price variation |
| 3.2 | Vendor Management & Portal | 32–46 | V001–V013 | Self-registration, Excel catalog/rate-card upload, GST/PAN validation, outstanding & GST (GSTR) reconciliation, quality rating, sample tracking, referral tracking |
| 3.3 | Inventory & Stock Control | 47–74 | I001–I028 | Barcode/QR, vendor-code cross-mapping, warranty/AMC/CMC, preventive maintenance, product lifecycle (Active→Scrap/Sold), FIFO/FEFO issue valuation, depreciation, auto-indent, transfers, short-expiry alerts |
| 3.4 | GRN & Invoice Management | 75–88 | G001–G0xx | 3-way match, partial/short receipt, credit-card reconciliation |
| 3.5 | Indent & Issue Management | 89–99 | N001–N0xx | Ward/dept requests, HOD approval, HIS-driven auto-indent |
| 3.6 | Asset Management | 100–110 | A001–A0xx | Lifecycle, disposal, gate-pass linkage for repair |
| 3.7 | Budget & Finance Integration | 111–119 | F001–F008 | Budget prep, Tally sync, supplier outstanding, monthly MIS |
| 3.8 | CRM & Ticketing | 120–127 | C001–C008 | Service requests, complaint/quality tickets |
| 3.9 | Reports & Analytics | 128–139 | R001–R0xx | Price comparison, cycle-time, dashboards |
| 3.10 | Product Master Management | 140–146 | M001–M0xx | Categorization, aliases, images, dedup |
| 3.11 | Printing Consumables Management | 147–151 | PR001–PR0xx | Forms/price-chart stock |
| 3.12 | Uniforms & Linen Tracking | 152–156 | U001–U0xx | Colour-coded issue/return |
| 3.13 | Workflow & Approvals | 157–166 | W001–W009 | Multi-tier, parallel/quorum routing, delegation, exception reasons |
| 3.14 | Tracking & Monitoring | 167–171 | T001–T004 | SLA timing, urgent-request and returned-item patterns |
| 3.15 | Gate Pass Management | 172–178 | GP001–GP006 | Returnable/non-returnable outward, inward, overdue tracking |
| 3.16 | Consignment Stock Management | 179–185 | CS001–CS006 | Vendor-owned stock, consumption-triggered ownership transfer, periodic billing |
| 4–7 | External Interfaces, NFRs, Data Model, Appendices | 186–197 | REQ-UI/NF | Tally/HIS/SMTP/LDAP integrations, performance/security/DR targets, 21-entity data model, 6-phase rollout plan |

**Genuinely reusable across any vertical (with renaming, see §4):** 3.1, 3.3 (core mechanics), 3.4, 3.13, 3.14, 3.15, most of 3.9. These are legitimately generic procure→receive→stock→issue→approve mechanics.

**Structurally sound but modelled as vendor-owned stock, which generalizes fine once decoupled from pharma framing:** 3.16.

**Named for, and hard-wired to, one vertical (healthcare):** 3.2 (HIS integration baked into indent flow), 3.5, 3.6 (biomedical engineering, AMC/CMC framed for medical equipment), 3.7 (GST/Tally only), 3.8.

**Worded as hospital-only but actually dual-vertical once relabelled** (revised 2026-09-07 once College was confirmed as a co-equal target — see §2B): 3.11 Printing Consumables (exam-paper/certificate/ID-card printing for a college is the same mechanic as prescription-pad printing for a hospital) and 3.12 Uniforms & Linen (student/NCC/sports uniforms and hostel linen map onto the same issue/return/colour-coding mechanic as staff/patient linen).

---

## 2. Critical Finding: This Is a Hospital SRS Wearing a Generic Title

The title page says "Inventory Management System (IMS)" — but the system's own abbreviation, defined in §1.3 Definitions, is **IHMS**, and its full form is never spelled out because it doesn't need to be: every subsequent page assumes a hospital. Evidence, not opinion:

- §1.1 Purpose: *"...for a multi-department **hospital** environment."*
- §2.3 User Classes: Doctors, Ward Staff, Nurses/ward admins are first-class roles; there is no generic "requester"/"consumer" role.
- §2.4/§4.3: **HIS (Hospital Information System)** integration is a *precondition* for auto-indent (REQ-P006 flow, REQ-I009), not an optional connector.
- Indenting is literally called **"ward request"** throughout 3.5, not "department requisition."
- REQ-I007 (Value Capture While Dispensing) links chargeable issue lines to a **Patient ID** and "HIS billing" — inventory consumption is coupled to patient billing, a healthcare-only concept.
- 3.6 Asset Management assumes **Biomedical Engineering** as the maintenance owner and **medical equipment** as the asset class (AMC/CMC terminology, calibration).
- 3.16 Consignment is framed entirely around **pharmaceutical/diagnostic reagent** vendor-managed inventory.
- Appendix B (Glossary) is titled *"Glossary of **Hospital-Specific** Terms."*
- Appendix C (Compliance) lists **Drug & Cosmetics Act, Medical Device Rules (UDI), ISO 13485** — healthcare-only regulations, with zero mention of banking, education, or manufacturing compliance regimes.
- All tax/accounting logic is **India-only and Tally-only**: GST slabs (0/5/12/18/28) are hard-coded into validation (REQ-V011), TDS (194C/194J) is the only withholding-tax model, and Tally ERP is the sole accounting integration named anywhere in the 197 pages.

**Conclusion:** treating this SRS as "the inventory module" as-is would ship a hospital procurement system with a generic-sounding name. It needs a deliberate generalization pass before it can serve a college deployment on equal footing — this is not a translation exercise, it changes the data model and a few core workflows.

---

## 2B. College/Education Coverage Check — Does the Core Actually Serve a College?

College and Hospital are now confirmed as **co-equal**, generically-worded target verticals (clarified 2026-09-07) — not "hospital primary, everything else hypothetical." So the core is checked here against college inventory needs with the same rigor §2 gave hospital, rather than assumed to generalize just because a renaming table exists.

| Module | Fits a college? | Notes |
|---|---|---|
| 3.1 Procurement & PO | Yes, as-is (already generic) | Lab equipment, furniture, stationery, sports goods, library book purchase orders — identical mechanics |
| 3.2 Vendor Management | Yes, once the HIS-coupling is removed | Book publishers, lab-supply vendors, uniform tailors, canteen/mess suppliers register the same way |
| 3.3 Inventory & Stock Control | Yes, core mechanics hold | FIFO/FEFO, barcode, batch/expiry all apply (lab chemicals expire); library books need accession-number tracking instead of FEFO — see GAP-26 |
| 3.4 GRN & Invoice | Yes, as-is | |
| 3.5 Indent & Issue | Yes, once "ward" → "department/lab/hostel" is relabelled | Requesting Location becomes Classroom/Lab/Hostel Block/Department; HOD becomes Dean/Principal/Warden |
| 3.6 Asset Management | Yes, once "biomedical engineering" → "facilities/estate office" is relabelled | Furniture, AV/computer-lab hardware, generators — same depreciation/AMC mechanics as medical equipment |
| 3.7 Budget & Finance | Partially — see GAP-27 | College budgets commonly run on **academic year/semester**, not only fiscal year; the Budget entity is `FinancialYear`-only |
| 3.8 CRM & Ticketing | Yes, as-is | Maintenance requests, IT helpdesk tickets |
| 3.9 Reports & Analytics | Yes, as-is | |
| 3.10 Product Master | Yes, but needs the Category Attribute Schema (GAP-20) | Library books need ISBN/Author/Edition fields a hospital product master has no reason to carry |
| 3.11 Printing Consumables | Yes, once relabelled | Exam papers, certificates, ID cards, admission forms |
| 3.12 Uniforms & Linen | Yes, once relabelled | Student/NCC/sports uniforms, hostel linen |
| 3.13 Workflow & Approvals | Yes, as-is | |
| 3.14 Tracking & Monitoring | Yes, as-is | |
| 3.15 Gate Pass | Yes, as-is | Equipment leaving campus for repair, event/sports equipment loan-out |
| 3.16 Consignment | Rare but plausible | Publisher-supplied books on consignment for a college bookstore |

**College-specific gaps the hospital SRS gives zero coverage for** — equal-weight to the hospital-only gaps in §3, numbered onward from §3's list:

- **GAP-26 — Library circulation isn't the same shape as ward/department stock issue.** A book is issued-and-expected-back to a *person* (student/staff), tracked by accession number, with fines for late return and a hold/reservation queue — none of which resembles the hospital SRS's "issue to a department, consumed" model in 3.5. SKSCMS already has a Library module (`docs/BUSINESS_REQUIREMENTS.md` BR-35/BR-38: rack/shelf master, catalogue, issue/return, fines) — its migration onto this core is explicitly **deferred** per §7 Decision 4 (revised 2026-09-07), but the core's design should not preclude it later, which means the core needs its own **Loan/Return transaction type** (item issued to a Person, due date, fine calculation, reservation queue), distinct from and sitting alongside the department-consumption transaction type, both posting to the same Stock Ledger.
- **GAP-27 — Budget periods are fiscal-year-only.** §6.1's Budget entity is `BudgetID, DepartmentID, FinancialYear, AllocatedAmount, ...` with no period-type flexibility. A college needs to budget/report by academic year/semester/term, which frequently doesn't align with the fiscal year. The Budget entity needs a configurable `PeriodType` (Fiscal Year / Academic Year / Semester / Term), not a hard-coded `FinancialYear` column.
- **GAP-28 — No generic "loan a physical item to an individual" pattern outside Library.** Sports equipment, musical instruments, lab kits, and hostel bedding are commonly loaned to individual students with a due date and sometimes a security deposit/breakage charge — the same *shape* as GAP-26's library loan, just for non-book items. Building this three times (Library, Sports, Hostel) would duplicate logic; the core should have one generic **Loanable Item Issue** transaction type (Person, Item, DueDate, DepositAmount, ConditionOnReturn) that Library, Sports, and Hostel each specialize via the Category Attribute Schema.
- **GAP-29 — Hostel/Mess provisioning has no direct SRS analog.** Bulk groceries/provisions consumption for a mess and bedding/room-inventory for hostels are common college needs; the closest fit is ward-consumables issue (3.5), which works once "ward" is relabelled "hostel block" — flagged here so it's accounted for in the Category Attribute Schema design, not because it needs new transaction mechanics.

---

## 3. Gap List — What's Missing for a True Global/Any-Industry Core

Numbered `GAP-##` for traceability when the business team responds. Ordered roughly by how foundational each gap is.

### A. Tenancy, Localization, Tax & Accounting
- **GAP-01 — No multi-tenant / multi-legal-entity model.** Nothing in the data model or NFRs addresses running one deployment for multiple independent organizations (or multiple legal entities/branches within one org) with data isolation, per-tenant configuration, and per-tenant branding. §5.4 Scalability only mentions "multi-location deployment capability," which is one org with many sites, not multi-tenant.
- **GAP-02 — Tax engine is India-GST-only and hard-coded.** REQ-V011's validation literally enumerates `{0,5,12,18,28}`. A global product needs a configurable tax-rule engine (rate, type — VAT/GST/sales-tax/none —, inclusive/exclusive, reverse-charge, exemptions) per country/region, not a fixed slab list.
- **GAP-03 — Single, named accounting integration (Tally).** No abstraction for QuickBooks, Xero, SAP, Oracle, NetSuite, Zoho Books, Odoo, or a generic GL-posting/webhook interface. Every posting requirement ("posts journal entry to Tally") should target a generic ledger-posting contract with Tally as one adapter.
- **GAP-04 — No base-currency / multi-book accounting.** REQ-P010 handles *foreign-vendor* PO currency, but there's no concept of the *organization itself* operating multi-currency books (e.g., a group with INR, USD, and AED entities) or currency revaluation at period close.
- **GAP-05 — i18n is an afterthought.** Only "vendor portal supports English, Hindi, Tamil, Telugu" (REQ-V001) is mentioned — no locale framework for arbitrary languages, date/number/address formats, or RTL.
- **GAP-06 — No data-residency / regional compliance model.** Appendix C compliance list is India-only (GST/TDS/Companies Act/Drug & Cosmetics Act). A global product needs a pluggable compliance/regulatory pack per deployment region (GDPR/CCPA-style data handling, e-invoicing mandates, etc.), not one fixed appendix.

### B. Core Inventory Mechanics
- **GAP-07 — The central "stock on hand" entity is never modelled.** Section 6.1's Data Model lists 21 entities (Product Master, PO, GRN, Indent, Asset Register, etc.) but **there is no Stock Ledger / Inventory Balance / Stock Batch entity** anywhere in it — even though "Stock Ledger Updated" is the terminal node of the entire 3.3 process-flow diagram and every issue/receipt/transfer requirement mutates it. This is the single most load-bearing table in an inventory system and it's missing from the spec's own data model.
- **GAP-08 — Only FIFO/FEFO valuation.** No Weighted-Average or Standard Costing, both standard in manufacturing/retail and required for many statutory regimes outside India.
- **GAP-09 — No formal physical stock-take / cycle-count workflow.** "Physical Verification / Stock Reconciliation" appears only as an unelaborated diamond in the 3.3 flow diagram — there's no REQ for blind counts, cycle-count scheduling (ABC-driven), count-sheet generation, or variance approval, despite this being one of the most universal inventory features across every vertical.
- **GAP-10 — Location model is too flat for real warehousing — *largely resolved, revised 2026-09-07*.** The SRS's own Location Master (`LocationID, LocationName, LocationType(Store/Ward/Facility), ParentLocationID`) is indeed too flat. But SKSCMS already has a real, deep physical hierarchy for this: `Organization → Branch → Block → Floor → Zone → Room` (`backend/src/main/java/com/cms/model/{Organization,Branch,Block,Floor,Zone,Room}.java`), explicitly documented as "generic physical room, shared across future consumers." **Inventory should not build a new Location Master — it should reference this existing hierarchy directly.** What's still genuinely missing (per the user, 2026-09-07): a thin **Inventory Location** layer that (a) marks which Infra node(s) — typically a Room, occasionally a Zone — act as a stock/requesting location, and (b) gives that node a **virtual/display name for inventory purposes** (e.g., Room "204" displayed as "Pharmacy Store" or "Chemistry Lab Store" in Inventory screens) without renaming or duplicating the underlying Infra record. See the revised `Inventory Location` entity in §6 (replaces the withdrawn `Organization Unit` entity, which duplicated the already-existing `Organization`/`Branch` tables). Aisle/rack/bin-level slotting and pick-path/wave concepts below Room level are still genuinely absent and remain a real gap for a manufacturing-DC/3PL-scale deployment, but that's out of v1 scope per Decision 3 (no plant-floor/DC operations).
- **GAP-11 — No vendor RMA / return-to-supplier workflow distinct from internal returns.** REQ-I012's "Stock Return" is framed as internal (ward → stores, "Not of Use"). There's no debit-note-driven, quality-rejection-driven return-to-vendor flow with replacement tracking.
- **GAP-12 — Multi-UOM / UOM conversion isn't first-class master data.** A "conversion factor" is mentioned once, inline, in REQ-I002 (vendor code mapping) rather than as a reusable UOM/conversion table (e.g., Box of 10 → Each, Kg → Litre for density-based items).

### C. Manufacturing, Fulfillment & Demand (explicitly relevant to "industry" and retail-style verticals the user named)
- **GAP-13 — No BOM / Work Order / kitting-assembly support.** Zero mention of Bill of Materials, production orders, or raw-material-to-finished-good consumption. "Stock Conversion (REQ-I028)" in the flow diagram appears to be a UOM/pack conversion, not manufacturing — needs confirmation, but as written there is no manufacturing-inventory path at all.
- **GAP-14 — No outbound/sales side.** The entire SRS is procure-in → consume-internally. There is no sales order, customer/external-order fulfillment, pick-pack-ship, or sales-return workflow. Section 3.1's marketplace integration is *buy-side only* (importing purchase orders from Amazon Business/Flipkart/IndiaMART) — there's no sell-side channel integration. Any manufacturing, distribution, or retail-flavoured deployment needs this.
- **GAP-15 — Demand planning is reorder-level only.** REQ-I009 (Auto-Indent) is a static min-level trigger; AI-based forecasting is explicitly pushed to Appendix E "Future Enhancement Roadmap." Acceptable to defer, but flag that seasonal/industrial-scale demand planning is not in the current core at all, not even a basic moving-average safety-stock model.

### D. Vertical Coverage — Original Wide List (retained for architectural awareness only, NOT v1 scope)

The original ask named "hospital / college / bank / industry / school / laboratory / any industry." Scope was narrowed on 2026-09-07 (see §2B, §7 Decision 2) to **College + Hospital only** as concrete v1 targets. College's gaps were real ones and are now tracked at full weight as GAP-26 through GAP-29 in §2B, not here. The remaining items below (bank, laboratory, general industry/manufacturing) are **kept only as a reference check** — so the generic core's naming and data model (§4, §6) don't accidentally re-lock to a second vertical the same way the original SRS locked to hospital — they are not being designed for or built in this phase:
- **GAP-16 — Banking (out of v1 scope):** no serialized-instrument inventory (chequebooks, security forms, cards), no vault/currency-chest reconciliation, no dual-custody/maker-checker pattern specific to cash-equivalents (the existing approval-matrix concept is close but not framed for this).
- **GAP-18 — Laboratory as a standalone vertical, beyond a college's teaching labs (out of v1 scope):** no chain-of-custody, hazardous-material/SDS tracking, controlled-substance/narcotic register, or calibration-traceability-to-reference-standard (distinct from the generic AMC/CMC reminder in 3.6).
- **GAP-19 — General industry/manufacturing (out of v1 scope, and separately deferred per §7 Decision 3):** covered by GAP-13/14 above (BOM, outbound) plus no shop-floor/production-line consumption point distinct from a "department."

### E. Master Data & Extensibility
- **GAP-20 — Fixed schema per product, no configurable attributes.** Product Master has one fixed field set (+ IsAsset/IsConsumable/IsService flags). Different verticals need different attribute sets per category (e.g., "shelf life" for pharma, "denomination" for banking stationery, "calibration due" for lab instruments) — this needs a category-driven custom-attribute/EAV or JSON-schema layer, not more hard-coded columns.
- **GAP-21 — Roles are hospital-shaped.** The 8 predefined roles (Store Manager, Department HOD, Ward Staff, Doctors...) need to become a configurable role/permission model per SKSCMS's own **DB-only Role Management** convention (see CLAUDE.md) rather than hard-coded hospital titles.
- **GAP-22 — No public API/webhook catalog for extensibility.** All integrations named (§4.3) are specific inbound connectors (Tally, HIS, LDAP, SMS gateway). A horizontal product needs a documented REST/webhook surface so a vertical-specific add-on (library, bank teller till, lab LIMS) can be built against it without re-opening the core.

### F. Documentation-Consistency Gaps (evidence the spec itself needs a pass, independent of vertical scope)
- **GAP-23** — Consignment Agreement (REQ-CS001) has its own field set described in prose but is **not represented in §6.1's Data Model** (only "Consignment Stock" is listed there).
- **GAP-24** — Printing Consumables (3.11) and Uniforms & Linen (3.12) are full functional sections with no corresponding entities anywhere in §6.1.
- **GAP-25** — Cash PO/Cash GRN (REQ-P008) — a distinct numbering series and workflow — isn't reflected as a distinguishing field/entity in the data model's PO/GRN rows.

---

## 4. Renaming Pass — Hospital Term → Generic Core Term

Needed before (or as part of) modelling the core entities, so the schema and UI copy don't ship hospital language into a bank/school/factory deployment. Vertical "skins" can re-introduce the familiar word via label overrides.

College and Hospital are both v1 build targets (§7 Decision 2), so both get an explicit column. Other verticals are kept only as reference examples showing the core term doesn't accidentally re-lock to a second vertical — they are not v1 scope (§3, Section D).

| Hospital term (SRS) | Generic core term | College equivalent | Other vertical examples (reference only, not v1 targets) |
|---|---|---|---|
| Ward | Inventory Location (a virtual-named Room/Zone from the existing `Organization → Branch → Block → Floor → Zone → Room` hierarchy — see GAP-10) | A Classroom, Lab, or Hostel Block *is* a Room/Zone in the same hierarchy, virtual-named accordingly | Any Room/Zone works identically for a bank branch counter, an industrial shop floor, or a lab bench |
| Indent | Requisition | Requisition (same word works as-is) | — |
| HOD (Head of Department) | Approving Authority / Location Owner | HOD / Dean / Principal / Warden | Branch Manager (bank), Lab In-Charge |
| HIS (Hospital Information System) | Source System (generic upstream-demand connector) | SIS/ERP (Student Information System) | Core Banking System, MES (manufacturing), LIMS (lab) |
| Patient ID / chargeable billing | Cost Object (Job/Project/Patient/Account) | Student / Admission ID | Account (bank), Work Order (industry), Sample/Case (lab) |
| Biomedical Engineering | Asset Maintenance Owner | Facilities / Estate Office | Plant Maintenance (industry), Instrumentation (lab) |
| AMC/CMC | Maintenance Contract | Maintenance Contract (same word works as-is) | — |
| Doctors / Ward Staff / Nurses | Requester / Approver (role-configurable) | Faculty / Lab Assistant / Warden | mapped per vertical via DB-driven Role Management |

---

## 5. Recommended Core Architecture Principles

1. **One standalone, industry-agnostic module — no named vertical packages.** *(Revised 2026-09-07 — see `DECISION_LOG.md`: the module must never brand any part of itself toward hospital or college.)* Every module (Procurement, Vendor, Stock Ledger, GRN, Requisition & Issue, Asset, Approvals, Gate Pass, Reporting) ships generic, full stop — there is no separate "Healthcare Pack" or "College Pack" layered on top, and none should ever be introduced. Vertical-specific needs (patient billing vs. student billing, ward vs. classroom, AMC/CMC medical framing vs. general maintenance) are satisfied entirely through the module's own generic, configurable constructs: the Cost Object concept (§4), the Category Attribute Schema (GAP-20), and DB-driven role/label configuration — never through a named extension a deployment has to opt into.
2. **Stock Ledger is the one source of truth.** Every module (GRN, Issue, Transfer, Adjustment, Consignment consumption, Asset disposal) posts to a single append-only ledger; on-hand balances are a derived/materialized view. This closes GAP-07 and makes costing-method changes (GAP-08) and multi-warehouse (GAP-10) tractable later.
3. **Tax and accounting are pluggable, not hard-coded.** A `TaxRule` and `LedgerConnector` abstraction from day one avoids an India/Tally rewrite later (GAP-02/03/04).
4. **Everything that's currently a fixed enum/role list becomes DB-driven config**, consistent with this repo's existing **Role management is DB-only** and **Operation-wise permission mapping** conventions in `CLAUDE.md` — do not hard-code hospital roles or a fixed tax-slab list into code.
5. **Category-driven attribute schema** for Product/Asset master data (GAP-20), so a "lab reagent" and a "bank cheque book" and a "classroom projector" can each carry their own fields without schema churn per vertical.
6. **Multi-tenant from the schema up**, even if the first customer is single-tenant — retrofitting tenant isolation later is materially more expensive than designing for it now (GAP-01).
7. **Design Infra + Inventory as independently buildable/deployable from day one** *(added 2026-09-07 — see `DECISION_LOG.md`)*, since the confirmed plan is to eventually ship a standalone frontend+backend build containing only these two modules to a customer (a hospital, per the stated plan). Two open technical questions this raises — **not resolved yet, must be addressed in the module-boundary design (§8)**, not discovered during the actual extraction:
   - **Code boundary:** the codebase is currently one flat, monolithic package structure (`com.cms.model`/`controller`/`service`/`repository`, no per-module packages except `com.cms.spatial`) with no precedent for a module-scoped build. Decide whether Infra+Inventory get genuine package/build-module separation (e.g., Gradle multi-module) or a single-codebase runtime toggle that hides non-Infra/Inventory surface area.
   - **Schema boundary:** there are 421 sequential Flyway migrations covering the whole CMS in one linear history — not separable by module. A hospital-only deployment needs its own database; decide whether that's a curated from-scratch migration baseline for just Infra+Inventory tables, or the full migration history run against a DB where unrelated tables simply stay unused.

---

## 6. Proposed Additions to the Data Model (on top of SRS §6.1's 21 entities)

| New Entity | Why | Closes |
|---|---|---|
| **Stock Ledger** (LedgerID, ProductID, LocationID, BatchID, TxnType, Qty, UnitCost, RunningBalance, RefType, RefID, TxnDate) | Single append-only source of truth for on-hand quantity/value | GAP-07 |
| **Stock Batch** (BatchID, ProductID, BatchNo/SerialNo, ExpiryDate, ReceiptCost, GRNLineID) | First-class batch/serial entity instead of fields scattered across GRN/Indent lines | GAP-07, GAP-08 |
| **UOM & UOM Conversion** (UOMCode, BaseUOM, ConversionFactor) | Reusable pack/unit conversion | GAP-12 |
| **Tax Rule** (RuleID, Region, RateType, Rate, InclusiveFlag, ExemptionRef) | Configurable tax engine | GAP-02 |
| **Inventory Location** (LocationID, InfraNodeType [Room/Zone], InfraNodeID, VirtualName, LocationRole [Store/RequestingPoint/…], IsActive) — *withdrawn 2026-09-07: an `Organization Unit` entity was proposed here, but `Organization` and `Branch` already exist in the codebase's Campus Infrastructure hierarchy (`Organization → Branch → Block → Floor → Zone → Room`) and fully cover multi-branch config within a deployment; building a parallel one would duplicate it* | References an existing Infra Room/Zone as a stock location and gives it an inventory-specific virtual/display name, instead of a new flat Location Master | GAP-10 (revised) |
| **Category Attribute Schema** (CategoryID, AttributeName, DataType, Required) + **Product Attribute Value** | Configurable per-category fields | GAP-20 |
| **Cycle Count / Physical Verification** (CountID, LocationID, ScheduleType, CountedQty, SystemQty, VarianceApprovedBy) | Formal stock-take workflow | GAP-09 |
| **Vendor Return / RMA** (RMAID, GRNLineID, Reason, DebitNoteRef, ReplacementPOID) | Return-to-supplier distinct from internal returns | GAP-11 |
| **Ledger Connector Config** (ConnectorType [Tally/QuickBooks/SAP/…], TenantID, MappingConfig) | Pluggable accounting integration | GAP-03 |
| **Consignment Agreement** (promoted to its own table, per REQ-CS001 fields) | Fixes the SRS's own documentation gap | GAP-23 |
| **Loanable Item Issue** (LoanID, ItemID, PersonID, IssueDate, DueDate, DepositAmount, ReturnDate, ConditionOnReturn, FineAmount) | One generic loan/return transaction type shared by Library, Sports, and Hostel — instead of building the same pattern three times | GAP-26, GAP-28 |
| **Budget** — add `PeriodType` (Fiscal Year / Academic Year / Semester / Term) alongside `FinancialYear` | Lets a college budget by academic year/semester without forcing fiscal-year alignment | GAP-27 |

---

## 7. Foundational Decisions — Recorded 2026-09-07

| # | Decision | Answer | Scope impact |
|---|---|---|---|
| 1 | Product shape | **Per-deployment module**, not a shared multi-tenant SaaS product. | GAP-01 (multi-tenancy) is **not** built for v1. The Tenant entity in §6 is downgraded: keep an **Organization Unit** concept for multi-branch config within one deployment, but drop cross-customer data isolation, per-tenant billing, and tenant-scoped feature flags from scope. Do not add tenant-partitioning complexity to the schema. |
| 2 | Sequencing & branding | **One standalone, industry-agnostic module — no vertical packs at all.** *(Revised 2026-09-07: supersedes the original "Healthcare Pack + College/Academic Pack" framing — see `DECISION_LOG.md`. The module must never explicitly brand any part of itself as belonging to hospital or college; it is a single generic module supporting all industries.)* | There is **no** pack layering and **no** vertical-named module, table, folder, or doc section, ever. College- and hospital-shaped needs (Library loan/return per GAP-26, academic-period budgeting per GAP-27, generic Loanable Item Issue per GAP-28, hostel/mess provisioning per GAP-29, patient billing, HIS connector, AMC/CMC framing) are all satisfied as generic, configurable capabilities of the one core — none of them ship as a named extension. Printing Consumables (3.11) and Uniforms & Linen (3.12) are core-adjacent generic modules, not tied to any pack. |
| 3 | Manufacturing / outbound scope | **Out of scope for v1.** | GAP-13 (BOM/kitting/work orders) and GAP-14 (outbound/sales fulfillment) are **deferred, not designed for**. The core is confirmed as procure → receive → stock → consume-internally only. "Industry/manufacturing" support means an industrial site's internal MRO/stores, not plant-floor production inventory or sell-side order fulfillment. Do not add BOM/sales-order tables in the first core schema. |
| 4 | Migration of existing modules | **Migrate `InventoryItem` (lab consumables) now; defer Library migration to a later phase.** *(Revised 2026-09-07 — see `DECISION_LOG.md`: Library migration explicitly deferred, not part of this build.)* | The Category Attribute Schema (GAP-20) must represent today's `InventoryItem` fields (name, itemCode, lab, quantity, minimumQuantity, unit, lastRestocked) as a day-one category under the new Product Master + Stock Ledger. Library's item/rack model is **not** migrated in this phase — GAP-26's Loanable Item Issue design should still leave room for Library as a future consumer so it isn't precluded later, but no Library migration work, field-mapping, or cutover happens now. A data-migration plan (old `InventoryItem` table → new schema, with a cutover and rollback path per this repo's **Production Data Safety** rules in `CLAUDE.md`) is required before `InventoryItem` is retired. |

*New decisions must be appended to [`DECISION_LOG.md`](DECISION_LOG.md) as they're made — this table only mirrors the four foundational ones so this document reads standalone.*

---

## 8. Suggested Next Step

Do **not** start entity/API design against the SRS's data model as-is (§6.1) — it's missing the load-bearing Stock Ledger entity (GAP-07) and is India/hospital-locked at the schema level. With §7's decisions now recorded, the sequence is:
1. ✅ **@Partner specialist round** (Product Owner, Frontend/Backend/DBA/QA/Security/Docs) — complete, see `DECISION_LOG.md`.
2. ✅ **Core ER diagram and module boundaries** — first draft complete: [`ER_DIAGRAM_AND_MODULE_BOUNDARIES.md`](ER_DIAGRAM_AND_MODULE_BOUNDARIES.md), including the concrete `InventoryItem` field-mapping (Library's is deferred with it) and two more reuse-vs-duplicate findings (existing `audit_log`; `Speciality` explicitly flagged as unsafe to reuse).
3. **Not yet done:** review/approval of that ER draft, resolution of its carried-forward open items (Infra+Inventory extraction boundary; whether `Lab` has a `Room` FK), and only then does implementation (migrations, entities, APIs) begin.

---

*Derived from `SRS_v3.2_Updated (1).pdf` (v1.0, July 2026, Raster) on 2026-09-07. All `GAP-##` items are traceable to specific REQ-IDs/section numbers cited inline for the business team to verify against the source document.*
