# Inventory Management — Decision Log

This is the running, chronological record of every scope/architecture decision made on the Inventory Management initiative. It exists so a decision's rationale and date survive independently of whichever spec document is being edited that week.

**Rules for this file:**
1. Append only — never edit or delete a past entry, even if a later decision reverses it. If a decision changes, add a new dated entry that says so and references the entry it supersedes.
2. Every entry needs: date, the decision, who/what prompted it, and the scope impact.
3. When a decision changes what [`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md`](CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md) or the future ER diagram/module-boundary doc says, update those documents in the same change and note it here.

---

## 2026-09-07 — Scope narrowed from "any industry, global" to College + Hospital

**Prompted by:** user clarification during the initial gap-analysis review.
**Decision:** The target is a vertical-agnostic core that fully serves **both a College and a Hospital deployment**, in generic terms (not worded toward one specific college or hospital) — not the broader "any industry: bank/school/lab/manufacturing" framing used in the original ask.
**Impact:** `CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §2B was added to check college coverage with the same rigor previously given only to hospital; Section D's bank/lab/manufacturing gaps were downgraded to "reference only, not v1 scope"; the renaming table (§4) got an explicit College column.

## 2026-09-07 — Foundational decisions (product shape, sequencing, manufacturing scope, migration)

**Prompted by:** four scoping questions put to the user after the initial gap analysis.
**Decisions (see `CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §7 for full rationale/scope-impact text):**
1. **Product shape:** Per-deployment module, not a shared multi-tenant SaaS product. Full multi-tenancy (GAP-01) is out of scope for v1.
2. **Sequencing:** Generic core first; Healthcare Pack and College/Academic Pack are built as co-equal v1 vertical packs on top of it, not two forks.
3. **Manufacturing/outbound:** Out of scope for v1. The system is procure → receive → stock → consume-internally only; no BOM/kitting, no sales-order/outbound fulfillment.
4. **Migration:** The existing SKSCMS `InventoryItem` (lab consumables) module and the Library module are migrated onto the new core, not left standalone.

## 2026-09-07 — Documentation moved into a dedicated tracked folder

**Prompted by:** explicit user request that all documentation for this module be tracked and maintained properly, not left as a single loose file.
**Decision:** All Inventory Management documentation now lives under `docs/inventory-management/`, indexed by [`README.md`](README.md), with this decision log as the single place standing decisions get recorded going forward. The original SRS PDF was archived to `docs/inventory-management/source/SRS_v3.2_Updated.pdf` for traceability (the business team's copy in `Downloads/` is not under version control and could change or disappear).
**Impact:** The former top-level `docs/INVENTORY_MANAGEMENT_CORE_REQUIREMENTS.md` was moved to `docs/inventory-management/CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md`; `docs/README.md` was updated to point here instead.

## 2026-09-07 — No vertical-named packs; Library migration deferred (supersedes parts of the two entries above)

**Prompted by:** direct user correction during the specialist-review round.
**Decisions:**
1. **No vertical branding, anywhere.** User's own words: *"I dont want to explicitly mention any part of inventory management belonging to hospital or college, it should be a standalone module supporting all industries, understand!"* This **supersedes** the "Healthcare Pack and College/Academic Pack as co-equal v1 vertical packs" framing from the first 2026-09-07 entry above. There is no pack architecture at all — one generic, standalone module. College- and hospital-shaped needs (Library loan/return, academic-period budgeting, patient billing, HIS connector, etc.) are all satisfied as generic, configurable capabilities of the single core (Cost Object concept, Category Attribute Schema, DB-driven role/label config) — never as a named extension, module, table, folder, or doc section branded toward one vertical.
2. **Library migration deferred.** Only `InventoryItem` (lab consumables) migrates onto the new core now. Library's migration is explicit future work — the second 2026-09-07 entry's "Migrate `InventoryItem` and Library onto the new core" is **superseded**: Library is not migrated in this phase, though the Loanable Item Issue design (GAP-26/28) should not preclude it later.
3. **No first-pack decision needed** — since there are no packs, the "which vertical ships first" question from the specialist round is moot. The single module ships with whatever functional coverage is built, usable by any deployment.

**Impact:** `CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §5 principle 1, §7 Decisions 2 and 4, GAP-26's note, and §8's next steps were all revised in place (their content changes; per this file's append-only rule it is *this log entry*, not those edits, that is the permanent record of why). A corresponding lesson was also saved to this session's persistent memory so future sessions don't reintroduce vertical-named packages when working on this module.

## 2026-09-07 — Reuse the existing Campus Infrastructure hierarchy for locations instead of a new Location Master

**Prompted by:** user input during the Frontend Architect specialist round: *"We have infra module sitting on the top, that will be used to geographically locate the organization > branch > blocks > floors > zones > rooms, these will be given a virtual name in Inventory Management System and managed accordingly."*
**Finding confirmed in code:** `backend/src/main/java/com/cms/model/{Organization,Branch,Block,Floor,Zone,Room}.java` — a real, already-built `Organization → Branch → Block → Floor → Zone → Room` hierarchy, explicitly documented in `Room.java`'s own Javadoc as "generic physical room, shared across future consumers."
**Decision:** Inventory Management does **not** build its own Location Master or Organization/Branch entity. It references the existing Infra hierarchy (typically at Room level, occasionally Zone) through a new, thin **Inventory Location** entity that adds only what Inventory needs on top: a virtual/display name for that node, and a location role (e.g., Store vs. requesting point). This closes GAP-10 largely for free and means the `Organization Unit` entity proposed earlier the same day (in the "foundational decisions" entry above) is **withdrawn as redundant** — never build it.
**Impact:** `CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` GAP-10, §6's entity table, and §4's renaming table were all updated to reflect this. Also confirmed: new Inventory nav/menus (e.g., "Stock Management") get their own top-level menu entries rather than folding under an existing section.

## 2026-09-07 — Backend Architect round: balance strategy, ledger connector, InventoryItem retirement

**Decisions:**
1. **Stock Ledger balance strategy:** append-only ledger for history, with a separate materialized/derived balance table kept in sync on write — not computed by summing the ledger on every read.
2. **Ledger/accounting connector:** build the generic posting-connector interface now (so nothing hard-codes Tally into the core), but defer writing the actual Tally adapter itself to a later phase.
3. **`InventoryItem` backend retirement:** checked for existing callers — `InventoryItemService`/`Controller`/`Repository` (`backend/src/main/java/com/cms/...`) have **no callers outside themselves** except seed-data loaders (`LocalDataSeeder`, `DataLoader`); the frontend's only consumers are its own `inventory-list`/`inventory-form` components and the `app.routes.ts` entry. No other module depends on it. **Decision: deprecate and remove the old `InventoryItem` API entirely once migrated — no facade needed**, since nothing else in the app calls it.

## 2026-09-07 — DBA round: field-mapping ownership and migration safety

**Decisions:**
1. **Field-mapping ownership:** Backend Architect drafts the concrete mapping from `InventoryItem`'s existing columns (`lab_id`, `quantity`, `minimum_quantity`, `unit`, `last_restocked`) into the new Category Attribute Schema; DBA reviews it for migration-safety and column-verification before it's finalized (per this repo's migration hard gates in `CLAUDE.md`).
2. **Migration safety confirmed:** the `InventoryItem` migration will be tested against a staging copy of real data, with a documented rollback path, before any production cutover — no exception, per `CLAUDE.md`'s Production Data Safety rules.

## 2026-09-07 — QA Lead round: regression scope and test-case timing

**Decisions:**
1. **Library regression testing:** not needed in this phase — Library migration is deferred entirely (per the earlier 2026-09-07 entry), so there's nothing to regression-test against it yet. Revisit when Library migration is actually scheduled.
2. **Manual test cases:** authored per completed feature, incrementally, matching this repo's existing `docs/manual-test-cases/README.md` convention — tracked from `docs/inventory-management/README.md`'s index, not batched at the end.

## 2026-09-07 — Security Lead round: roles/permissions and data masking

**Decisions:**
1. **Roles/permissions:** confirmed — this module's roles and every new button/action's permission go entirely through the existing DB-only Role Management module and the Operation-wise permission mapping rule (`CLAUDE.md`). No hard-coded role enum anywhere in this module's code, no exceptions.
2. **Data masking:** confirmed — the same masking principle the SRS applies to bank accounts/PAN for non-finance users extends to whatever sensitive reference a deployment's Cost Object carries (e.g., a patient/student/account reference on a requisition), masked for users without the relevant permission.

## 2026-09-07 — Documentation Engineer round: BR entries and milestone tracking

**Decisions:**
1. **Business Requirements entries:** `docs/inventory-management/` is the authoritative source for this module's detail; `docs/BUSINESS_REQUIREMENTS.md` gets a short pointer entry rather than duplicating full BR-## write-ups for every shipped feature.
2. **Milestone tracking:** a new `docs/RELEASE_3_MILESTONES.md` is created alongside the existing `RELEASE_1_MILESTONES.md`/`RELEASE_2_MILESTONES.md`, following the repo-wide release-tracker pattern — **not** a module-local tracker inside `docs/inventory-management/` as originally placeholder-listed in that folder's README (corrected there in the same change).

**This closes the specialist review round** started earlier today (Product Owner, Frontend Architect, Backend Architect, DBA, QA Lead, Security Lead, Documentation Engineer all answered). Per `CLAUDE.md`'s @Partner protocol, alignment is now confirmed for this scope; next step is drafting the core ER diagram and module boundaries (`CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §8).

## 2026-09-07 — Correction: InventoryItem does have a real dependent (Spatial module virtual locations)

**Prompted by:** re-reading `docs/BUSINESS_REQUIREMENTS.md` BR-60 while adding an unrelated pointer entry, which mentioned Equipment/InventoryItem status shown on Spatial floor-plan markers — contradicting the "Backend Architect round" entry above, which claimed `InventoryItem` has no callers outside itself.
**Correction:** `backend/src/main/java/com/cms/spatial/service/VirtualLocationService.java` lets a floor-plan marker (`VirtualLocation`) polymorphically link to an `InventoryItem` row via an `entityType="INVENTORY_ITEM"` / `entityId` pair (alongside `EQUIPMENT`, `BLOCK`, `ZONE`, `ROOM`), gated by the `INVENTORY_MANAGE` permission. It's a soft/polymorphic reference, not a DB foreign key, but it is a real dependent the earlier "no callers outside itself" claim missed.
**Impact:** the `InventoryItem` retirement decision (deprecate and remove once migrated) still stands, but the migration plan must additionally: (a) either migrate existing `virtual_locations` rows with `entityType='INVENTORY_ITEM'` to reference the new core's replacement entity (with a new `entityType` value), or explicitly decide those markers are dropped/relinked manually; (b) update `VirtualLocationService`'s `requireLinkPermission` switch and the Spatial frontend's link-kind picker (`spatial.model.ts`, `virtual-location-form-flyout.component.ts`) to point at the new core instead of `InventoryItem`. This is now a required checklist item for whoever executes the `InventoryItem` migration — add it to the eventual migration plan/ER design doc, don't rediscover it then.

## 2026-09-07 — Deployment model: Infra + Inventory must be independently buildable/deployable

**Prompted by:** user's own words: *"At the end of the development, I will build the frontend and backend of the application by choosing infra and inventory modules alone, that should be deployed to hospital!"*
**Decision:** the Infra module (`Organization → Branch → Block → Floor → Zone → Room`) and the new Inventory module must be designed so a **standalone frontend+backend build containing only those two modules** can be produced and deployed independently (to a hospital, per the stated plan, or any other customer) — separate from the full SKSCMS application. This is a forward-looking design constraint on module boundaries now, not immediate extraction work ("at the end of development" per the user).
**Open technical risk flagged, not yet resolved — checked in-session:** the codebase today is a flat, monolithic package structure (`com.cms.model`, `com.cms.controller`, `com.cms.service`, `com.cms.repository` — no per-module packages except `com.cms.spatial`), and carries **421 sequential Flyway migrations** spanning the entire CMS (Academics, Fees, Admissions, Library, Timetable, etc.) in one linear history. Two unresolved questions this raises for whoever does the module-boundary design (§8's next step):
1. **Code boundary:** does "Infra + Inventory alone" mean a genuine package/build-module split (e.g., Gradle multi-module: `:infra`, `:inventory`, `:core`) so a hospital build only compiles what it needs, or a single codebase with a runtime/config toggle that just doesn't expose non-Infra/Inventory endpoints and nav?
2. **Schema boundary:** a hospital deployment needs its own database with only Infra+Inventory tables — but Flyway's 421-migration history is one linear sequence for the whole CMS, not separable by module. This needs either a curated, from-scratch migration set for a fresh Infra+Inventory-only database (a genuinely new migration baseline, not a subset of the existing V1–V421 history), or running the full history against a DB where the non-Infra/Inventory tables simply stay empty/unused.
**Impact:** flagged as an open item for the module-boundary design phase — not blocking current documentation work, but the ER diagram/module-boundary doc (§8) must explicitly address both questions before implementation begins, since retrofitting a clean boundary after the fact is expensive.

## 2026-09-07 — Infra+Inventory extraction approach: explicitly deferred, not decided

**Prompted by:** user asked to settle the two open technical questions from the previous entry (code boundary, schema boundary), then answered "not sure / decide later" to both when presented with concrete options.
**Outcome:** **Neither question is resolved.** This is a genuine deferral, not a silent default — do not treat either of the "Recommended" options presented as chosen. Both remain open:
1. Code boundary (package restructuring vs. full Gradle multi-module split vs. runtime-toggle-only) — undecided.
2. Schema boundary (shared migration history vs. a separate curated hospital-only migration baseline) — undecided.
**Guidance until revisited:** default day-to-day engineering hygiene that doesn't foreclose either path — e.g., writing new Inventory code under its own clean package namespace rather than scattering it through the existing flat `com.cms.*` structure — is reasonable to do regardless, since it's low-cost and reversible either way. But this is ordinary good practice, not a resolution of the open question, and no migration-strategy decision should be assumed. **Revisit both before the actual standalone hospital build is planned** — do not let this stay silently unresolved indefinitely.

## 2026-09-07 — First ER diagram & module boundaries draft

**Produced:** `ER_DIAGRAM_AND_MODULE_BOUNDARIES.md` — ~46 entities across 12 bounded contexts (Catalog, Vendor, Procurement, Receiving, Stock, Requisition & Issue, Asset, Budget & Finance, Approvals, Gate Pass, Consignment, Service Ticket), built from `CORE_REQUIREMENTS_AND_GAP_ANALYSIS.md` §6 and every decision above.
**Two more reuse-vs-duplicate findings caught before they became mistakes** (same discipline as the Infra-hierarchy discovery):
1. **`audit_log` already exists** (`V90__create_audit_log.sql`, generic `actor/action/entityType/entityId/detail/occurredAt` shape, currently used for role/permission/user changes). Inventory reuses it directly — no new `AuditLog` entity was added to the model.
2. **`Department` is a stub redirecting to `Speciality`** (`name, code, description, hodFacultyId, hodName, isActive`) — an academic-program concept tied to a Faculty HOD. **This is explicitly not a safe generic-reuse target**, unlike Infra/`audit_log` — it would smuggle college-specific naming back into the core. `Budget` and `RequisitionLineItem`'s cost-attribution fields use a loose `Type`+`Id` reference instead of any FK to `Speciality` or an equivalent hospital table.
**One open item flagged, not resolved:** whether `Lab` (the FK target of `InventoryItem.lab`) already has its own path into the Infra `Room` hierarchy needs checking before the `InventoryItem` migration is actually drafted — noted in the ER doc §7/§9 rather than assumed.

## 2026-09-07 — Resolved: Lab→Room FK question

**Checked:** `backend/src/main/java/com/cms/model/Lab.java` has a `room` field (`@ManyToOne`, `room_id` FK to `Room`), documented as "Nullable/non-unique."
**Finding:** the FK path exists, confirming the `InventoryItem.lab` → `InventoryLocation` mapping in `ER_DIAGRAM_AND_MODULE_BOUNDARIES.md` §7 is sound. Non-uniqueness (multiple Labs sharing one Room) needs no design change — `InventoryLocation` already allows multiple rows against the same Infra node. Nullability is a genuine precondition, not a design flaw: any `Lab` with `room = null` (legacy free-text-only labs) has nothing for its `InventoryItem` stock to attach to. **Action required before the migration is written, not before this design:** run `SELECT count(*) FROM labs WHERE room_id IS NULL` against real data (not determinable from source code alone) and backfill any hits via the existing Campus Infrastructure module.
**Impact:** ER doc §7 and §9 updated; this was the last open item from the initial ER draft.

## 2026-09-07 — Stakeholder-facing milestone file created

**Prompted by:** user request for a milestone file to share with stakeholders — phases and what's covered only, deliberately no timelines, kept updated as a living reference as things change.
**Decision:** created `MILESTONES.md` in this folder, distinct from `../RELEASE_3_MILESTONES.md` (the technical engineering tracker created earlier). Nine phases (0–8): Discovery & Design (in progress), then Foundation, Purchasing & Suppliers, Receiving & Stock Movement, Requests/Issues/Returns, Equipment & Asset Management, Budgets & Approvals, Gate Pass/Consignment/Service Requests, Reporting — each phase written in plain language with what it covers and a todo list, no jargon, no dates. Library migration and the standalone-packaging question are called out as confirmed-but-not-yet-scheduled rather than folded into a numbered phase, so they aren't mistaken for forgotten.
**Impact:** both `MILESTONES.md` and `RELEASE_3_MILESTONES.md` cross-reference each other with an explicit instruction to keep them in sync — same work, two audiences, must not drift. `README.md`'s index updated. **Whoever updates one when a phase's status changes must update the other in the same pass.**

## 2026-09-07 — Phase 1 kickoff: Lab.room_id finding, delivery order, package/nav naming

**Prompted by:** user instruction to start Phase 1 (Foundation: Catalog, Stock Tracking & Locations) implementation.
**Precondition check run:** `SELECT count(*) FROM labs WHERE room_id IS NULL` against the local dev database returned **7 of 7 labs** — every local `Lab` row currently has no `room_id`. This confirms the precondition flagged in the "Resolved: Lab→Room FK question" entry above is real and, on this dataset, total. It does not block Catalog/Stock work; it blocks only the `InventoryItem` migration step.
**Decisions (from questions put to the user before writing code):**
1. **Delivery order:** Phase 1 is delivered as one vertical slice per todo — each slice (backend migrations/entities/services/controllers, frontend list+form, permissions, nav) finished and reviewable before the next starts. Order: Catalog & UOM → Stock Tracking → Physical Counts → `InventoryItem` migration last.
2. **`InventoryItem` migration:** explicitly deferred. **User's own words:** *"We need to build the core first, later we will think of migrating the existing data/records."* The new schema is built so the migration *could* happen later, but no migration script is written now, and the old `InventoryItem` module keeps running unchanged until that's revisited — same deferred posture as the Library migration.
3. **First slice scope, within Catalog & UOM:** starting with `Category` (self-referencing hierarchy, no `CategoryAttribute` yet) and `Uom` — the two dependency-free entities everything else in Catalog builds on. `Product` (with its aliases/attribute-values/images) follows as the next slice once these land.
4. **Package namespace:** new backend code lives under `com.cms.inventory.catalog` (model/repository/service/controller/dto), mirroring the existing `com.cms.spatial` module-namespaced package — per the "extraction approach: explicitly deferred" entry above, this is low-cost/reversible groundwork, not a resolution of the open code-boundary question.
5. **`Uom` primary key:** the ER doc (§2) modelled `Uom` with `UomCode` as its primary key. Implemented instead with the same surrogate `Long id` + unique `code` column every other master in this codebase uses (`RoomSubType`, `RoomPurposeCategory`, etc.) — keeps every FK in the app `Long`-typed and consistent; `code` is still enforced unique, so no capability is lost. Noted here as a small, deliberate deviation from the ER draft's exact wording, not an oversight.
6. **Permission naming:** `INVENTORY_CATEGORY_VIEW`/`INVENTORY_CATEGORY_MANAGE`, `INVENTORY_UOM_VIEW`/`INVENTORY_UOM_MANAGE` — checked for collision against the legacy `INVENTORY_VIEW`/`INVENTORY_CREATE`/etc. codes (the old lab-consumables feature); none.
7. **Nav placement:** a new top-level **"Stock Management"** sidenav group is added for these screens, per the earlier "new Inventory nav/menus... get their own top-level menu entries" decision — kept separate from the existing **"Inventory Management"** nav group, which is the legacy simple asset/equipment feature (`/inventory`, `/maintenance`) and stays exactly as-is until its own migration is scheduled. Reusing the "Inventory Management" label for the new module's nav would have collided with that existing, still-live group.
**Impact:** this entry is the record of the above; `MILESTONES.md` and `RELEASE_3_MILESTONES.md` status tables updated in the same change to show Phase 1 / R3-M1 in progress.

## 2026-09-07 — Product slice: CategoryAttribute nesting, image upload deferred, uniqueness scope

**Prompted by:** user instruction to continue Phase 1 into `Product` after Category/Uom shipped.
**Decisions:**
1. **`CategoryAttribute` gets its own nested CRUD** under Category (`/inventory/categories/{categoryId}/attributes`), reusing `INVENTORY_CATEGORY_VIEW`/`INVENTORY_CATEGORY_MANAGE` rather than new permission codes — an attribute has no lifecycle independent of the category that defines it, so it isn't a distinct "operation" in the Operation-wise permission mapping sense.
2. **`ProductAlias` and `ProductAttributeValue` are managed as child collections**, replaced wholesale on every `Product` create/update rather than getting their own CRUD endpoints/permissions — small nested lists edited only in the context of their parent Product form.
3. **`ProductImage` explicitly deferred**, along with the MinIO upload plumbing it needs (multipart endpoint, frontend dropzone/thumbnail picker — real, separately-scoped work; `FloorPlanService`/`MinioStorageService` are the precedent to follow when it's built). Not modelled in this migration at all, to avoid a dangling unused table — added in its own later pass instead. `Product` core + aliases + attribute values ship now.
4. **Uniqueness scope:** `ProductCode` is globally unique (the natural catalog identifier); `ProductName` is unique only within its `Category` (mirrors `Category`'s own sibling-name scoping) — not global, since `ProductAlias` already exists precisely to let "the same item" carry different display names across locations, so forcing global name uniqueness would fight that.
5. **`CategoryAttribute.dataType = ENUM`** needs a way to define its allowed values, which the ER draft didn't specify a mechanism for. Added a simple `enum_options` (comma-separated string) column rather than a separate options table — lightweight, sufficient for a picklist, revisit only if a real need for per-option metadata shows up.
6. **No attribute inheritance from ancestor categories.** A Product only sees `CategoryAttribute`s defined directly on its own `Category`, not ones defined on that category's parents. Simpler, and matches the ER draft's plain reading; can be revisited if a real use case needs it.

## 2026-09-07 — Stock Tracking slice: Room-only locations, movement types, decrease valuation

**Prompted by:** user instruction to continue Phase 1 into Stock Tracking after the Product/CategoryAttribute slice shipped.
**Decisions:**
1. **`InventoryLocation` wraps a `Room` only in this pass — no `Zone`-level locations yet.** The ER draft's §4 modelled `InfraNodeType [Room/Zone]`; the doc's own wording already says Inventory uses Zone only "occasionally." Implemented as a real, non-nullable FK to `rooms(id)` rather than the polymorphic `entityType`/`entityId` soft-reference pattern `VirtualLocation` uses for its five link kinds — a real FK is strictly better than a soft reference when there's exactly one possible target table, which is the case once Zone support is dropped for now. `VirtualName` is globally unique (not scoped per Room), since multiple `InventoryLocation` rows can legitimately point at the same Room (two Labs sharing a physical Room, each with its own distinct virtual name) but should never collide with each other in the picker UI. Add Zone-level support later as a genuine, reviewed extension if a real need shows up — don't quietly reintroduce the polymorphic-reference approach without re-checking this reasoning.
2. **Room/Zone picking reuses the existing Infra frontend service** (`CampusInfrastructureService.getAllActiveZones()` + `getRoomsByZone()`), not the existing `cms-room-picker` shared component — that component requires a `purposeCategoryId` (it's built for venue-linking screens like Lab/Classroom/ClinicalVenue), which Inventory has no equivalent of and shouldn't invent one just to reuse the component.
3. **Movement types exposed in this pass: `RECEIPT`, `ADJUSTMENT`, `DISPOSAL` only** — `ISSUE`, `TRANSFER`, `RETURN`, `CONSIGNMENT_CONSUMPTION` all belong to workflows (Requisition, Stock Transfer, Vendor Consignment) that don't exist yet; exposing them now would let someone record a movement with no corresponding real-world transaction backing it. The `StockLedger.txnType` column itself still allows the full ER-drafted enum, so later phases don't need a migration to add values — only today's UI/API surface is narrowed.
4. **Decrease-side valuation uses the current weighted-average unit cost, not FIFO/FEFO.** For a `DISPOSAL` or a decreasing `ADJUSTMENT`, if the caller doesn't supply a unit cost, the value removed is computed from the existing `StockBalance`'s own `valueOnHand / qtyOnHand`. Real FIFO/FEFO issue valuation is explicitly Phase 3 scope per the ER doc (§3's GRN/batch work) — this is a deliberately simple stand-in, not an attempt at real costing.
5. **A movement that would drive `qtyOnHand` negative is rejected.** Defensive validation, not from the ER doc — prevents silently-wrong stock data before any reconciliation/cycle-count tooling exists to catch it (that's Phase 1's next todo).
6. **`StockBatch` has no dedicated CRUD screen.** A batch/serial number + expiry date are optional fields on the "Record Stock Movement" form itself; a matching `StockBatch` row is created on the fly the first time a given batch/serial number is used for a product, exactly like `ProductAlias`/`ProductAttributeValue` are managed as part of their parent's form rather than as their own master.
7. **Stock Ledger movement history has no browsing screen yet** — only the "Record Movement" write path and the `StockBalance` read (current on-hand qty/value) are built. The backend read endpoint for ledger history is *not* added preemptively either, unlike some other deferred pieces — there's no consumer for it yet, so it's simple to add together with its screen in a later pass rather than carry unused surface area now.
**Impact:** new tables `inventory_locations`, `stock_batches`, `stock_ledger`, `stock_balances`; new permissions `INVENTORY_LOCATION_VIEW/MANAGE`, `INVENTORY_STOCK_VIEW/MANAGE`.

## 2026-09-08 — Cycle Count slice: full-location + ad-hoc scope, blind count, unbatched-only auto-post

**Prompted by:** user instruction to continue Phase 1 into "Physical stock counts / reconciliation" after the Stock Tracking slice shipped; three scoping questions put to the user first (count scope, blind vs visible system quantity, variance resolution), each explained with a concrete example before the user answered.
**Decisions:**
1. **Count scope — full location + ad-hoc, together.** Starting a count against a location defaults its sheet to every product currently holding any balance there, but products can still be added (even zero-balance ones) or removed before counting starts. Real ABC-driven auto-scheduling stays deferred, same posture as other scheduling features in this module.
2. **Blind count.** The count-entry screen never shows the system's expected quantity — the counter enters only what they physically found, with the comparison and variance revealed only after submission. Standard stock-take practice, and the one thing GAP-09 explicitly called out as missing from the SRS. Implemented API-side (not just hidden in the UI): `CycleCountLineResponse.systemQtySnapshot`/`varianceQty` come back `null` from every endpoint the count-entry screen reads while the header is still DRAFT, one response shape reused across both screens rather than a second blind-only DTO.
3. **Variance resolution — separate approve permission, auto-post.** Only nonzero-variance lines need sign-off; a zero-variance line auto-closes (`MATCHED`) with no approval step. `INVENTORY_CYCLE_COUNT_APPROVE` is its own permission, distinct from `INVENTORY_CYCLE_COUNT_MANAGE` (per the operation-wise permission mapping rule) — approving immediately posts an `ADJUSTMENT` through the existing `StockMovementService`, exactly as a manual adjustment would; rejecting dismisses the variance as a counting error with no stock change.
4. **A line's `systemQtySnapshot` is fixed once, at line-creation time** (count creation for a FULL_LOCATION line, or the moment a product is added ad-hoc) — never re-fetched later, so a movement recorded elsewhere mid-count can't quietly move the baseline out from under an in-progress count.
5. **Snapshot is summed across every `StockBalance` row for the product/location (batched and unbatched), matching what a person physically counting the shelf actually sees — but posting an approved variance only ever adjusts the unbatched balance row**, the same simplification spirit as this module's earlier "no batch CRUD screen, no FIFO/FEFO" calls. If a product's stock is genuinely spread across named batches, auto-posting can hit the existing negative-stock guard in `StockMovementService`; `CycleCountService.approveLine` catches that and surfaces a clear message pointing the user at Record Stock Movement (against the specific batch) instead of silently misattributing the adjustment. Real per-batch cycle counting is out of scope until batch-level workflows exist.
6. **Header lifecycle:** DRAFT (building/counting, blind) → SUBMITTED (variances computed, nonzero ones await approve/reject) → COMPLETED (every line in a terminal state — MATCHED, APPROVED, or REJECTED). CANCELLED is reachable only from DRAFT — once submitted, the workflow must run to completion via approve/reject on each line rather than allow a partial-rollback of already-posted adjustments.
7. **New permissions:** `INVENTORY_CYCLE_COUNT_VIEW` / `_MANAGE` / `_APPROVE`, seeded to DEV_ADMIN/SUPPORT_ADMIN/ADMIN/COLLEGE_ADMIN plus the DEV_ADMIN/SUPPORT_ADMIN catch-all sync, matching V427's pattern.
8. **Shared `CmsStatusBadgeComponent` extended**, not forked: `MATCHED` (success), `PENDING_REVIEW` (warning, alongside the existing `UNDER_REVIEW`), and `PENDING_COUNT` (pending) added to its `resolveClass()` switch rather than building a parallel badge system, per this repo's badge-consistency gate — `DRAFT`/`SUBMITTED`/`COMPLETED`/`CANCELLED`/`APPROVED`/`REJECTED` already existed in that switch and needed no change.
**Impact:** new tables `cycle_counts`, `cycle_count_lines` (V428); new permissions (V429); `StockBalanceRepository` gained two read-only aggregate queries (`sumQtyByProductForLocation`, `sumQtyForProductAndLocation`) used only by the new `CycleCountService`, not by `StockMovementService`'s existing write path. `MILESTONES.md` and `RELEASE_3_MILESTONES.md` updated in the same change — Phase 1 / R3-M1's "Physical stock counts" todo is now done, closing out Phase 1 except the still-deferred `InventoryItem` migration.

## 2026-09-08 — Phase 2 kickoff: delivery order, no PO approval gate yet, minimal TaxRule, rate-lookup-only price comparison

**Prompted by:** user instruction to start Phase 2 (Purchasing & Suppliers) after Phase 1 closed out (bar the deferred `InventoryItem` migration); four scoping questions put to the user first.
**Decisions:**
1. **Delivery order:** same vertical-slice style as Phase 1. Order: Supplier + RateContract + TaxRule (dependency-free masters) → VendorProductMapping (vendor rates per product) → Purchase Requisition → Purchase Order + line items. This entry covers only the first slice.
2. **No Purchase Order approval gate this phase.** A PO will ship with a plain status field (Draft/Sent/Closed-style), not even a single-approver placeholder — real multi-level/parallel approval routing is explicitly Phase 6 scope and gets built once, not twice.
3. **`TaxRule` built as a real minimal master now** (name, rate %, active), referenced by FK rather than a raw percentage on `PoLineItem`/`VendorProductMapping` — more correct long-term than a plain percent field, without attempting the full pluggable tax engine (jurisdiction, inclusive/exclusive, reverse-charge) GAP-02/03 describe; that's deferred until there's an actual second regime or accounting connector to design against.
4. **"Price comparison" scope is rate-lookup only this phase** — a product's `VendorProductMapping` rates across suppliers will be visible on the product/supplier screens once that entity is built (next slice), not a dedicated side-by-side "compare quotes for this requisition" workflow. That's closer to Phase 8 (Reporting) or a real RFQ flow and would be premature before Requisition/PO exist.
5. **`PurchaseOrder.CurrencyCode`/`ExchangeRate`** (per-PO foreign-vendor currency) will be included as plain fields with no conversion/revaluation engine — distinct from the deferred org-level multi-book GAP-04, and a low-risk inclusion flagged to (not objected by) the user rather than asked as a full question.
6. **Supplier approval is its own permission** (`INVENTORY_SUPPLIER_APPROVE`), separate from `INVENTORY_SUPPLIER_MANAGE` — same operation-wise permission mapping pattern as Cycle Count's approve/manage split. `isApproved`/`approvalDate` are settable only through the dedicated approve endpoint, never via the regular create/update path.
7. **Bank/tax-registration masking implemented, not left as a forward-looking commitment.** No existing masking utility was found to reuse (checked Agent/Faculty bank-detail fields — plain, unmasked columns with no masking code anywhere in the codebase), so `SupplierService` masks `bankAccountNumber`/`taxRegistrationId`/`legalRegistrationNo` to their last 4 characters for a caller holding only `INVENTORY_SUPPLIER_VIEW` (not `_MANAGE`), using the existing `PermSecurityBean` (`@perm` bean) injected directly into the service — no new generic masking framework built, just this one field-level check, since a reusable framework wasn't asked for and would be premature with only one masked entity so far.
8. **`RateContract` gets its own permission pair** (`INVENTORY_RATE_CONTRACT_VIEW`/`_MANAGE`), separate from `INVENTORY_SUPPLIER_*`, even though both live under the same new "Purchasing & Suppliers" nav group — negotiating rate contracts is a genuinely distinct operation from managing supplier master data.
9. **`Supplier.portalAccessEnabled` is a reserved flag only** — no vendor-portal login is built against it in this slice, same "flag now, build later" precedent as `Product.isLoanable` before `LoanableItemIssue` existed.
10. **New top-level nav group "Purchasing & Suppliers"** (Suppliers, Rate Contracts, Tax Rules), separate from "Stock Management" — matches the established convention that each new Inventory sub-area gets its own top-level entry rather than folding into an existing one.
**Found in passing:** `product-form`/`classroom-form`/`escort-rotation-dialog` all reference a `field-checkbox` CSS class on a bare native checkbox with no definition anywhere in the codebase (only `curriculum-version-form.component.scss` actually defines it locally) — an existing unstyled-checkbox bug, same class of issue as the badge/CSS-variable gate already in `CLAUDE.md`. Not fixed here (outside this session's touched files) but the new Supplier form's own checkbox defines `field-checkbox` locally rather than repeating the bug.
**Impact:** new tables `tax_rules`, `suppliers`, `rate_contracts` (V430); new permissions (V431). `MILESTONES.md` and `RELEASE_3_MILESTONES.md` updated in the same change — Phase 2 / R3-M2 now in progress.

---

## 2026-09-08 — VendorProductMapping slice: standalone master, RateContractLine override, product picker

**Prompted by:** user instruction to continue Phase 2 into VendorProductMapping after the TaxRule/Supplier/RateContract slice shipped; six scoping questions put to the user first, asked one specialist group at a time via multiple-choice per the user's standing preference for how @Partner specialist rounds are conducted.
**Decisions:**
1. **Scope stays rate-lookup only**, confirming decision #4 of the "Phase 2 kickoff" entry — no RFQ/best-quote comparison workflow.
2. **`isPreferred` included now** (not deferred) — a boolean flag on the mapping marking the go-to supplier for a product, for a future Purchase Requisition to auto-suggest against.
3. **Standalone master**, not nested under Supplier's or Product's form — its own list+form under the existing "Purchasing & Suppliers" nav group, consistent with this phase's other masters. At most one active mapping per (supplier, product) pair, enforced by a partial unique index (`WHERE is_active`) plus an app-level check surfaced as an inline "already exists" validation error.
4. **`RateContract` gained a real per-product rate — `RateContractLine`** (new child collection, replaced wholesale on save, same pattern as `ProductAlias`). Originally `RateContract` had no product-level rate at all (only a value cap/terms), so a mapping's "contract overrides price" decision had nothing to override with until this was added. A `RateContractLine`'s `negotiatedRate` overrides a linked mapping's `unitPrice` **only while the parent contract is `isActive` and today falls within its start/end date window** — resolved at read time in `VendorProductMappingService`, never stored, so a lapsing contract or an edited line is reflected immediately.
5. **`RateContractLine` has no permission of its own** — stays under the existing `INVENTORY_RATE_CONTRACT_MANAGE`, per the operation-wise permission mapping rule's "no lifecycle independent of its parent" carve-out (same reasoning as `CategoryAttribute` under `Category`).
6. **New reusable `cms-product-picker` shared component** (`frontend/src/app/shared/product-picker/`) — a debounced, server-searched autocomplete against the existing `ProductService.getPage`, since no product-picker existed yet and the catalog can be large (unlike `cms-room-picker`'s plain client-filtered `<select>`, appropriate for its much smaller Room lists). Plain `[(selectedProductId)]` binding, matching `cms-room-picker`'s existing plain-binding convention rather than a full `ControlValueAccessor`. Reused by both the new VendorProductMapping form and the new Product Rate Lines section on the RateContract form.
7. **VendorProductMapping's uniqueness check reuses the shared `uniqueFieldValidator`** rather than a bespoke async validator — the backend's `/pair-exists` endpoint's query params were shaped to fit that helper's existing `value`/`excludeId`/scoped-extra-param convention (`value` = the productId being checked, `supplierId` = the scope), the same way Product's own name-exists-within-category check already scopes by `categoryId`.
8. **Fields kept to the originally proposed list**: `unitPrice`, `currencyCode` (plain field, no conversion engine — same posture as the already-flagged `PurchaseOrder.CurrencyCode`), `uom` (optional; blank means "same as product's base UOM"), `minOrderQty`, `leadTimeDays`, `isPreferred`, plus the optional `rateContract` link.
**Impact:** new tables `vendor_product_mappings`, `rate_contract_lines` (V432); new permissions `INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW`/`_MANAGE` (V433). `RateContractRequest`/`RateContractResponse` gained a `lines` field (backward-compatible addition, not a breaking change to the already-shipped Rate Contract screen). `MILESTONES.md` and `RELEASE_3_MILESTONES.md` updated in the same change.

---

## 2026-09-08 — Reference architecture pivot: mirror the IHMS pharmacy app's proven patterns

**Prompted by:** user surfaced an existing, mature pharmacy/procurement application (IHMS — internal Bitbucket project `IHMS`, repo `ihms`; `pharmacy` backend service, `onepharmacy-ui`/`onestore-ui` Angular frontends) mid-session and asked how much it should inform the design of the Inventory Management module's remaining phases.
**Review findings:** IHMS's domain model (~190 JHipster entities) and screen inventory map closely onto Phases 2–7 of this module's plan — `PurchaseOrder`/`PurchaseOrderItem` with a receipt-progress status lifecycle (`PENDING → ORDERED → IN_PROGRESS → PARTIALLY_COMPLETED → COMPLETED`/`FORCE_CLOSED`, no approval gate — validating this module's own "no PO approval gate this phase" call), `Purchase`/`PurchaseItem` (GRN) with a two-step save→confirm draft workflow and a `Delivery Challan → GRN` conversion path, a two-tier requisition split (`StockIndent` for branch-to-branch transfer requests vs `IssueLocationIndent` for department-level issue within a branch — matching this module's own planned Phase 4 split), `Consignment`/`ConsignmentItem` for vendor-owned stock that converts into an owned `Purchase` as it's consumed (Phase 7), and rack/bin-scoped `PhysicalStock` counts.
**One divergence flagged, not reversed:** IHMS's `SupplierRateContract` is flat — one row per (supplier, product, rate, date range) directly. This module's already-shipped `RateContract` (supplier-level: value cap/terms/renewal) + `RateContractLine` (per-product negotiated rate, child collection) + `VendorProductMapping` (default per-product rate) is more normalized. Left as shipped rather than reworked, since it predates this instruction — recorded here as the reference precedent for how future divergences from IHMS's shape should be called out (a conscious, logged decision, not a default).
**Decision:** Going forward, every remaining Inventory Management phase's design starts from checking IHMS's corresponding entities/screens first, and adopts its status lifecycles, workflow shapes, and field sets as the strong default — diverging is a conscious, logged choice, not an unexamined one. This does not relax the standing "no vertical branding" rule — IHMS's pharmacy-specific fields (drug/molecule/dosage-form concepts, patient/HIS/insurance coupling) are not ported; only the generic operational patterns (PO/GRN lifecycle, indent/issue split, consignment conversion, etc.) are.
**Impact:** Informs every subsequent phase's design from here on; no code changed by this entry itself. See the auto-memory note (`inventory-mirrors-ihms-pharmacy-app`) for the session-independent version of this rule.

---

## 2026-09-08 — Purchase Requisition slice: no PO/Wanted-List dependency yet, per-line approval, mirrors CycleCount

**Prompted by:** user instruction to continue Phase 2 after reviewing IHMS; five scoping questions put to the user first, one specialist group at a time via multiple-choice.
**Decisions:**
1. **Given IHMS has no separate "Purchase Requisition" document** (only an auto-computed, reorder-triggered "wanted list" and a Direct PO entry that skips past any request step), the user chose to build **both**: a real human-submitted Purchase Requisition (this entry) *and* a reorder-triggered Wanted List as its own later slice — not a straight IHMS mirror here.
2. **Delivery order:** given the resulting scope now spans three linked pieces (Requisition, Wanted List, Purchase Order), the user chose to ship **Purchase Requisition alone first**, standing on its own — Wanted List and Purchase Order remain separate future slices, continuing this phase's established vertical-slice-at-a-time delivery style.
3. **Purchase Requisition targets an `InventoryLocation`** (required) — where the requisitioned stock will land, matching how Stock Movement/Cycle Count already scope everything to a Location.
4. **Approve/reject happens per line, not just at the requisition header level** — mirrors IHMS's `StockRequirementItem` granularity, but the actual shape (header DRAFT→SUBMITTED→COMPLETED/CANCELLED, per-line PENDING→APPROVED/REJECTED with `resolvedBy`/`resolvedAt`/`resolutionNotes`) mirrors this module's own already-shipped `CycleCount`/`CycleCountLine` far more closely than anything in IHMS — a closer, more directly reusable in-repo precedent. `CANCELLED` is reachable only from `DRAFT`, same rule as CycleCount.
5. **PO status lifecycle decision (for the future PO slice) recorded now while fresh:** mirror IHMS's receipt-progress-driven states (`PENDING → ORDERED → IN_PROGRESS → PARTIALLY_COMPLETED → COMPLETED` + `FORCE_CLOSED`) rather than the earlier plain Draft/Sent/Closed plan — still no approval gate, per the original "Phase 2 kickoff" decision.
6. **Wanted List trigger decision (for the future slice) recorded now while fresh:** a scheduled background job (not an on-demand action) will compute reorder-level shortages once that slice is built.
7. **An `APPROVED` requisition line does not itself create anything** — no PO exists yet to create. It simply reaches a terminal state; picking up approved lines into a PO is explicitly the next slice's job, not retrofitted here.
8. **Manual actor/timestamp fields** (`createdBy`/`createdAt`, `submittedBy`/`submittedAt`, `completedAt`), not the generic auditing listener — same reasoning as `CycleCount`: this header has several distinct actor/timestamp pairs a single listener doesn't fit.
9. **New permissions:** `INVENTORY_PURCHASE_REQUISITION_VIEW`/`_MANAGE`/`_APPROVE`, seeded to DEV_ADMIN/SUPPORT_ADMIN/ADMIN/COLLEGE_ADMIN plus the DEV_ADMIN/SUPPORT_ADMIN catch-all sync, matching V429's (Cycle Count) pattern.
**Impact:** new tables `purchase_requisitions`, `purchase_requisition_items` (V434); new permissions (V435). `MILESTONES.md` and `RELEASE_3_MILESTONES.md` updated in the same change.

---

## 2026-09-08 — Wanted List slice: MRP-standard netting, not a straight IHMS mirror

**Prompted by:** user instruction to continue Phase 2 into the Wanted List; when the first specialist round tried to scope its actions/quantity narrowly off IHMS's own `StockRequirementItem`, the user pushed back explicitly: *"the ihms reference i gave regarding pharmacy is just an example of what we do already, it does not mean that would replicate or follow the same here. I want you to think globally like an ERP application and take the essence from pharmacy and build a cross-functional inventory management system, dont hesitate to add the current trending and standards that are followed in an Inventory Management application."* This reframes the "Reference architecture pivot" entry's mandate: IHMS is a starting reference to check, not a shape to replicate when a broader ERP-standard pattern fits better — diverging for a well-established industry-standard reason is exactly the "conscious, logged choice" that entry already called for.
**Decisions:**
1. **Real MRP netting, not a naive on-hand-vs-reorder-level check.** `NetRequirement = ReorderLevel − (QtyOnHand + QtyOnOrder)`, where `QtyOnOrder` sums quantity already sitting in open (non-DRAFT-header, non-REJECTED) Purchase Requisition lines for that product+location — the standard MRP "netting against the pipeline" rule, adapted since Purchase Order doesn't exist yet. Without this, the job would re-flag a shortage that's already been requested on every run.
2. **Suggested quantity = standard hybrid lot-sizing rule**: `max(Product.reorderQty, NetRequirement)` — always at least one fixed reorder lot, scaled up when the shortfall exceeds it. Both `ReorderLevel` and `QtyOnOrder`/`QtyOnHand` are captured as snapshots on the line at generation time (same reasoning as `CycleCountLine.systemQtySnapshot`), so a line stays self-explanatory even after the product's configured level or the location's stock later moves.
3. **A line is the ERP-standard MRP "Planned Order"**, and its only forward action that commits to real spend is **converting it into a Purchase Requisition** — reusing `PurchaseRequisitionService`'s create/addLine/submit rather than duplicating that logic, not IHMS's own flatter defer/reject-then-wait-for-a-separate-PO-step shape.
4. **Collective conversion**: several selected lines *for the same location* convert together into one new Purchase Requisition (one line per product), matching standard MRP "collective conversion" of Planned Orders — avoids one requisition per shortage line. The planner can adjust each line's quantity before it's firmed in.
5. **Line lifecycle**: `PENDING` (auto-computed) → `DEFERRED` (held for later; reopenable back to `PENDING`) or `REJECTED` (structured reason + optional notes — mirrors `CycleCountLine`'s resolution pattern) or `CONVERTED` (terminal, linked to the requisition/line it created). The shortage job skips any (product, location) pair that already has an unresolved (`PENDING`/`DEFERRED`) line — enforced by both a partial unique index and the service layer — so a pair only becomes eligible again once its line resolves.
6. **Manual "Run Now" trigger added, reopening the earlier scheduled-job-only decision.** Real ERP MRP runs (SAP/Oracle) standardly offer both a scheduled batch run and an on-demand one; the user explicitly approved reopening the prior "background job only" call for this reason. The nightly job stays too (`0 0 5 * * *`, mirroring `AcademicTermAlertService`'s `@Scheduled` pattern).
7. **Four permissions, following the operation-wise mapping rule**: `INVENTORY_WANTED_LIST_VIEW`, `_MANAGE` (defer/reject/reopen triage), `_CONVERT` (commits to a real requisition — split out the same way Purchase Requisition's `_APPROVE` is split from `_MANAGE`), `_RUN` (the manual trigger, a distinct system-wide operation) — all seeded to DEV_ADMIN/SUPPORT_ADMIN/ADMIN/COLLEGE_ADMIN plus the DEV_ADMIN/SUPPORT_ADMIN catch-all sync.
8. **Scope stays per-(product, location)**, only for pairs that already hold a `StockBalance` row (an established stocking relationship) — a location that has never received a product isn't inferred to need it, avoiding cross-join noise across every location for every reorder-configured product.
**Impact:** new table `wanted_list_items` (V436); new permissions (V437); `MILESTONES.md` and `RELEASE_3_MILESTONES.md` updated in the same change.

---

## 2026-09-08 — Purchase Order slice: closes Phase 2, made autonomously overnight

**Made autonomously overnight — flag for morning review if this reads wrong.** Prompted by the
user authorizing an unattended, no-confirmation overnight/next-day build session (see
`AUTONOMOUS_OVERNIGHT_PLAN.md`) to use up remaining token budget without waiting for the usual
per-slice specialist-question rounds. This entry follows the same judgment-call discipline those
rounds would have applied — established precedent first, ERP-standard default otherwise — rather
than inventing new patterns.
**Decisions:**
1. **Status lifecycle exactly as pre-recorded** in the "Purchase Requisition slice" entry's
   decision 5: `PENDING → ORDERED → IN_PROGRESS → PARTIALLY_COMPLETED → COMPLETED` +
   `FORCE_CLOSED`, no approval gate. `IN_PROGRESS`/`PARTIALLY_COMPLETED`/`COMPLETED` are not yet
   driven by anything in this slice — they activate once Phase 3's Goods Receipt slice starts
   posting against the already-added `PurchaseOrderItem.receivedQty` column; until then a PO only
   ever reaches `ORDERED` or `FORCE_CLOSED` from `PurchaseOrderService`.
2. **`FORCE_CLOSED` reachable from `PENDING` too** (an order never sent to the supplier), not only
   from `ORDERED`/`IN_PROGRESS`/`PARTIALLY_COMPLETED` — this lifecycle has no separate "cancel a
   not-yet-sent order" state of its own, and adding one would deviate from the exact enum set
   already recorded in the earlier entry without a real need.
3. **Lines are picked up from `APPROVED` `PurchaseRequisitionItem` rows for the order's own
   location** — mirrors `WantedListService.convert`'s "collective conversion" shape (pick several,
   commit together) rather than duplicating its code, since the source entity differs enough that
   direct reuse wasn't practical. A picked-up line moves the requisition line to a new terminal
   `ORDERED` status (widening `chk_purchase_requisition_items_status` via a new forward migration,
   V438 — not editing the already-shipped V434, per this repo's hard gate) so the same approved
   line can't be double-booked into two orders; removing a PO line before the order is sent
   reverts the requisition line back to `APPROVED`.
4. **Unit price defaults from `VendorProductMappingService`'s existing contract-aware effective-
   rate resolution** (a new `resolveEffectiveRate` method extracted from that service's own
   `toResponse` logic, not duplicated) — required to be supplied manually if no active mapping
   exists for that (supplier, product) pair, rather than silently defaulting to zero.
5. **`TaxRule` applied per line, tax amount and line total computed and stored at line-creation
   time** (not recomputed live) — same snapshot spirit as `CycleCountLine.systemQtySnapshot`, so a
   line stays self-explanatory even if the referenced `TaxRule`'s rate changes later.
6. **`currencyCode`/`exchangeRate` stay plain fields, no conversion engine** — same posture already
   flagged (not objected to) for `VendorProductMapping.currencyCode` in the "Phase 2 kickoff" entry.
7. **Permissions:** `INVENTORY_PURCHASE_ORDER_VIEW`/`_MANAGE`/`_FORCE_CLOSE` — force-close is its
   own permission per the operation-wise mapping rule, same pattern as Cycle Count's/Purchase
   Requisition's approve/manage splits, seeded to DEV_ADMIN/SUPPORT_ADMIN/ADMIN/COLLEGE_ADMIN plus
   the DEV_ADMIN/SUPPORT_ADMIN catch-all sync (V439).
8. **No backend unit test class added**, matching the established precedent for every other
   Inventory Management slice so far (`Supplier`, `RateContract`, `VendorProductMapping`,
   `PurchaseRequisition`, `WantedList` all shipped with manual test cases only, no
   `backend/src/test/java/com/cms/inventory/**` classes exist) — not a new decision, just followed
   consistently. Both `./gradlew compileJava` and `npx tsc -p tsconfig.app.json --noEmit` were run
   clean before committing, per this plan's own standing rules.
**Impact:** new tables `purchase_orders`, `purchase_order_items` (V438, which also widens
`purchase_requisition_items`'s status check constraint); new permissions (V439). **This closes
Phase 2 ("Purchasing & Suppliers") in full** — `MILESTONES.md` and `RELEASE_3_MILESTONES.md`
updated in the same change. See `AUTONOMOUS_OVERNIGHT_PLAN.md` for what's next (Phase 3 —
Receiving & Stock Movement).

## 2026-09-08 — Goods Receipt slice: Phase 3 kickoff, made autonomously overnight

**Made autonomously overnight — flag for morning review if this reads wrong.** Continues the same
unattended, no-confirmation build session as the "Purchase Order slice" entry above.
**Decisions:**
1. **Two-step save (`DRAFT`) → confirm (`CONFIRMED`)**, matching IHMS's own `Purchase`/
   `PurchaseItem` draft→confirm shape (per the "Reference architecture pivot" entry) rather than
   posting stock immediately on line-add. Only `confirm` posts anything.
2. **Every write to `StockLedger`/`StockBalance` still goes exclusively through
   `StockMovementService.recordMovement`** — confirming a receipt line calls it with `txnType:
   RECEIPT` rather than writing the ledger/balance tables directly, preserving that class's own
   "sole owner of every stock write" invariant from the Phase 1 "Stock Tracking slice" entry.
3. **Over-receipt is blocked outright, 0% tolerance** — not allowed-with-a-warning as the
   Autonomous Overnight Plan's own draft wording had first suggested. A real delivery running over
   what was ordered is common in practice, but without a documented tolerance policy to implement
   correctly, blocking is the safer default; a configurable tolerance is real, separately-scoped
   work to revisit if asked, not something to half-implement here.
4. **A receipt line always requires an existing `PurchaseOrderItem`** — no direct receipt with no
   PO in this slice (matches the plan's own scope; GAP-style "unplanned receipt" flows are not
   part of Phase 3's Goods Receipt as scoped).
5. **`PurchaseOrderItem.receivedQty` is updated directly by `GoodsReceiptService.confirm`**, and
   the PO's own status recompute is delegated to a new `PurchaseOrderService
   .recalculateReceiptProgress` method rather than `GoodsReceiptService` writing to `PurchaseOrder`
   itself — keeps "one service owns every write to its own aggregate" consistent
   (`StockMovementService` for the ledger, `PurchaseOrderService` for the order).
6. **Known, documented limitation:** blocking over-receipt while still `DRAFT` only checks this
   receipt's own draft lines against the PO line's confirmed `receivedQty` — it does not see
   quantity sitting in a *different* still-open DRAFT receipt against the same PO line. The real
   guard is the fresh re-check `confirm` performs against the PO line's live `receivedQty` at
   confirm time, which always holds regardless of how many drafts raced to get there — so no stock
   can ever actually over-post, but a draft can show a quantity that turns out invalid once another
   draft confirms first, surfaced as a clear error naming the now-current open quantity.
7. **Batch/serial number and expiry stay on the receipt line itself**, reusing the same optional
   fields the Phase 1 "Record Stock Movement" form already exposes — no second batch-entry UI, per
   that phase's own "batch fields live on the movement, not a dedicated screen" precedent.
8. **New package `com.cms.inventory.receiving`** (its own bounded context, matching the ER
   diagram's 12-context boundary list) rather than folding Goods Receipt into `.procurement` —
   Receiving is a distinct phase/bounded context from Procurement even though it reads
   `PurchaseOrder`/`PurchaseOrderItem` directly.
9. **Permissions:** `INVENTORY_GRN_VIEW`/`_MANAGE`/`_CONFIRM` — confirm is its own permission per
   the operation-wise mapping rule (it's the action with real stock/financial consequence), same
   pattern as every other approve/confirm split in this module so far.
**Impact:** new tables `goods_receipts`, `goods_receipt_lines` (V440); new permissions (V441).
`MILESTONES.md` and `RELEASE_3_MILESTONES.md` updated in the same change — Phase 3 / R3-M3 now in
progress. `./gradlew compileJava` and `npx tsc -p tsconfig.app.json --noEmit` both run clean
before committing.

## 2026-09-08 — Stock Transfer slice: unbatched only, value carried via resolved weighted-average

**Made autonomously overnight — flag for morning review if this reads wrong.** Continues the same
unattended, no-confirmation build session as the two entries above.
**Decisions:**
1. **Gives the `TRANSFER` `StockTxnType` (reserved since the Phase 1 "Stock Tracking slice" entry,
   unused until now) a real screen** — `StockMovementService.ALLOWED_TXN_TYPES` widened to include
   it, and its qty-delta handling shares `ADJUSTMENT`'s existing direction-based
   (`INCREASE`/`DECREASE`) branch rather than a new one, since the shape is identical.
2. **Unbatched stock only in this pass** — same simplification precedent `CycleCount`'s own
   posting step already established ("no batch-level workflow yet"). No `batchOrSerialNo` field
   on `StockTransferLine`. Real batch-aware transfers are real, separately-scoped work if a need
   for them surfaces.
3. **Value is carried across, not zeroed.** The naive approach — posting a `TRANSFER` with no
   unit cost — would leave the destination's `valueOnHand` at zero for a new balance row, silently
   losing value on every transfer. Instead `StockTransferService` resolves the source's own
   current weighted-average unit cost (`valueOnHand / qtyOnHand` on its unbatched `StockBalance`
   row — the exact formula `StockMovementService`'s own decrease-valuation already documents,
   duplicated here rather than extracted into a shared method since it's two lines and this is the
   only other caller so far) and passes that same resolved cost to *both* the decrease-at-source
   and increase-at-destination `recordMovement` calls, so the transferred stock's value is
   preserved end to end.
4. **No new validation duplicated for "not enough stock to transfer"** — `StockMovementService
   .recordMovement`'s existing negative-stock guard (rejects a movement that would leave
   `qtyOnHand` negative) already covers it when the DECREASE leg posts; `StockTransferService`
   does not pre-check the source balance itself.
5. **Simple `DRAFT → COMPLETED` lifecycle, no approval gate** — consistent with Purchase Order's
   own "no approval gate this phase" call (Phase 6 owns real approval routing). `CANCELLED`
   reachable only from `DRAFT`, matching every other DRAFT-first header in this module.
6. **Two permissions only** (`INVENTORY_STOCK_TRANSFER_VIEW`/`_MANAGE`), no third "complete"
   permission split out — unlike Goods Receipt's confirm (brings genuinely new stock into the
   system) or Purchase Order's force-close (an audit-worthy early stop), completing a transfer
   only moves stock already accounted for between two locations the same `MANAGE` permission
   already governs, so splitting out a narrower permission wouldn't gate anything meaningfully
   different.
7. **Lives in the existing `com.cms.inventory.stock` package**, not a new one — unlike Goods
   Receipt (a distinct Receiving bounded context reading into Procurement), Stock Transfer reads
   and writes nothing outside `stock`'s own `InventoryLocation`/`StockBalance`/
   `StockMovementService`, so it belongs alongside `CycleCount` and `InventoryLocation` rather
   than earning its own package.
**Impact:** new tables `stock_transfers`, `stock_transfer_lines` (V442); new permissions (V443);
`StockMovementService`'s `ALLOWED_TXN_TYPES` and qty-delta switch both widened (existing file, not
a migration — safe to edit). `MILESTONES.md` and `RELEASE_3_MILESTONES.md` updated in the same
change. `./gradlew compileJava` and `npx tsc -p tsconfig.app.json --noEmit` both run clean before
committing.

## 2026-09-08 — Return to Supplier slice: closes Phase 3, made autonomously overnight

**Made autonomously overnight — flag for morning review if this reads wrong.** Continues the same
unattended, no-confirmation build session as the three entries above.
**Decisions:**
1. **Raised against a `CONFIRMED` `GoodsReceipt` only** — a still-DRAFT receipt has posted no
   stock yet, so there is nothing real to return. Enforced at create time.
2. **`RETURN` given to `StockMovementService` as a decrease-only type, same handling as
   `DISPOSAL`** (no `direction` needed) — this slice's meaning is unambiguous (goods physically
   leaving to the supplier). Flagged in that class's own switch comment: Phase 4's different,
   not-yet-built "internal returns" concept (returning previously-*issued* stock back to a
   location) must not assume this same decrease-only handling still fits without re-checking —
   it may need its own direction-aware treatment or its own enum value.
3. **Nets back out of `PurchaseOrderItem.receivedQty`, not just the stock ledger** — a return means
   the true accepted quantity from the supplier is now lower, and the parent order's own status
   should reflect that. This required loosening `PurchaseOrderService.recalculateReceiptProgress`
   (previously a no-op once `COMPLETED`, treating it as terminal) to only treat `FORCE_CLOSED` as
   truly terminal — `COMPLETED` can now correctly revert to `PARTIALLY_COMPLETED`/`IN_PROGRESS`/
   `ORDERED` as a return lowers a line's received quantity. This is a behavior change to
   already-shipped Phase 3 code from earlier tonight, made in the same session, not a separate
   deviation requiring its own review round.
4. **Over-return blocked outright** (0% tolerance), same posture and same "own-draft-only guard +
   fresh re-check at complete time" shape as Goods Receipt's own over-receipt guard — deliberately
   consistent rather than inventing a different rule for the mirror-image operation.
5. **Structured `reason` (enum) is optional, not required** — `SupplierReturnReason` mirrors
   `WantedListRejectionReason`'s shape, but nothing in this module's precedent requires a reason
   be mandatory at header level; the return's own line notes can carry detail if the picklist
   doesn't fit.
6. **Two permissions only** (`INVENTORY_SUPPLIER_RETURN_VIEW`/`_MANAGE`), same reasoning as Stock
   Transfer — completing a return doesn't warrant its own narrower permission beyond `MANAGE`.
7. **Lives in `com.cms.inventory.receiving`**, alongside `GoodsReceipt` — reads directly into
   `GoodsReceiptLine`/`PurchaseOrderItem`, the same bounded context `GoodsReceiptService` already
   sits in.
**Impact:** new tables `supplier_returns`, `supplier_return_lines` (V444); new permissions (V445);
`StockMovementService.ALLOWED_TXN_TYPES` widened to include `RETURN`;
`PurchaseOrderService.recalculateReceiptProgress`'s terminal-state guard narrowed to
`FORCE_CLOSED` only, and its final "nothing received" branch now explicitly reverts to `ORDERED`
instead of silently leaving a stale status. `MILESTONES.md` and `RELEASE_3_MILESTONES.md` updated
in the same change — **this closes Phase 3 ("Receiving & Stock Movement") in full**. See
`AUTONOMOUS_OVERNIGHT_PLAN.md` for what's next (Phase 4 — Requests, Issues & Returns).
`./gradlew compileJava` and `npx tsc -p tsconfig.app.json --noEmit` both run clean before
committing.

## 2026-09-08 — Stock Issue Request slice: Phase 4 kickoff, made autonomously overnight

**Made autonomously overnight — flag for morning review if this reads wrong.** Continues the same
unattended, no-confirmation build session as the four entries above.
**Decisions:**
1. **Naming kept strictly distinct from Purchase Requisition** per the standing caution already
   recorded in `AUTONOMOUS_OVERNIGHT_PLAN.md` before this slice was written: `StockIssueRequest`/
   `StockIssueRequestItem`, its own `com.cms.inventory.issue` package, its own
   `INVENTORY_ISSUE_REQUEST_*` permission family, its own "Requests, Issues & Returns" nav group —
   nothing shares a name, permission, or package with `PurchaseRequisition`.
2. **Header/line lifecycle mirrors `PurchaseRequisition`/`PurchaseRequisitionItem` almost exactly**
   (closest in-repo precedent, same as that entity's own docs note about IHMS) — DRAFT ->
   SUBMITTED -> COMPLETED/CANCELLED, per-line PENDING -> APPROVED/REJECTED, `CANCELLED` reachable
   only from DRAFT, header auto-completes once every line is resolved.
3. **The one real difference: approving a line here posts a live stock consequence.** Unlike
   Purchase Requisition's `approveLine` (which only reaches a terminal sign-off state — nothing
   downstream exists to act on it yet), `StockIssueRequestService.approveLine` calls {@code
   StockMovementService.recordMovement} with a new `ISSUE` transaction type in the same
   transaction as the status change. If the issuing location doesn't have enough on hand,
   `recordMovement`'s existing negative-stock guard rejects the whole approval and the line stays
   `PENDING` — no partial posting, no separate stock-availability check duplicated here.
4. **`ISSUE` added to `StockMovementService` as decrease-only** (same handling as `DISPOSAL`/
   `RETURN`, no direction) — posts only at the issuing location. The requesting location is
   **not** credited a matching increase in this slice: a `locationRole = REQUESTING_POINT`
   location is treated as a consuming cost-center with no stock ledger of its own, not a second
   stock-holding point (unlike `StockTransfer`, which moves stock between two genuinely
   stock-holding locations). If a real future need for the requester to also track received-but-
   not-yet-consumed stock shows up, this would need to become a two-leg movement like Transfer —
   revisit then, don't assume single-leg still fits.
5. **No separate "confirm" step beyond approve** — approving a line *is* the posting action
   (unlike Goods Receipt's separate draft-then-confirm), since there's no multi-line "delivery"
   concept here to batch before committing — each line is independently approved or rejected.
6. **Three permissions** (`INVENTORY_ISSUE_REQUEST_VIEW`/`_MANAGE`/`_APPROVE`), matching Purchase
   Requisition's own split exactly — approve is the one action with real consequence.
**Impact:** new tables `stock_issue_requests`, `stock_issue_request_items` (V446); new permissions
(V447); `StockMovementService.ALLOWED_TXN_TYPES` widened to include `ISSUE`. `MILESTONES.md` and
`RELEASE_3_MILESTONES.md` updated in the same change — Phase 4 / R3-M4 now in progress.
`./gradlew compileJava` and `npx tsc -p tsconfig.app.json --noEmit` both run clean before
committing.

## 2026-09-08 — Auto-restocking (OC-206) skipped: needs real product-policy input

**Made autonomously overnight — flag for morning review.** Per `AUTONOMOUS_OVERNIGHT_PLAN.md`'s
own standing rule 7 ("stop and leave a clear note instead of guessing... skip to the next
independent slice"), OC-206 ("Auto-restocking when items run low") was not implemented as
originally sketched in that plan file. On closer look while starting it, the plan's own wording —
"extend the Wanted List shortage job so it also nets against open `StockIssueRequest` lines the
same way it already nets against open Purchase Requisition lines" — doesn't hold up: a pending
internal issue request against location B doesn't reduce location A's *own* need to buy more
total stock into the system; it only reflects B's stock being drawn down. Netting them together
would incorrectly suppress a real purchase-shortage signal. A version of "auto-restocking" that
would make sense — e.g. preferring an internal transfer from a location with surplus over
recommending a new purchase — requires real deployment policy (which locations are internal
stores vs. requesting points for this purpose, sourcing-preference rules, whether that's even
desired behavior) that no ERP-standard default settles cleanly. **Left unimplemented, flagged
here rather than guessed at or silently skipped.** Revisit with the user's actual input before
building it — do not reopen this without asking first, autonomous session or not.

## 2026-09-08 — Internal Return slice: RETURN widened to direction-based, made autonomously overnight

**Made autonomously overnight — flag for morning review if this reads wrong.** Continues the same
unattended, no-confirmation build session as the entries above (skipping OC-206 per the entry
just above, moving to OC-207).
**Decisions:**
1. **`StockMovementService`'s `RETURN` transaction type widened from decrease-only to
   direction-based** (joining `ADJUSTMENT`/`TRANSFER`'s existing `INCREASE`/`DECREASE` handling)
   — exactly the re-check the "Return to Supplier slice" entry's own code comment said would be
   needed once this concept showed up. `SupplierReturnService`'s existing call was updated to
   pass `direction: "DECREASE"` explicitly (previously implicit/always-negate) so its behavior is
   unchanged; the new Internal Return action passes `"INCREASE"`.
2. **No new document/entity for the return itself** — unlike `SupplierReturn` (its own header +
   line table, because a supplier return can bundle several receipt lines), an internal return is
   a simple accumulating action directly against an already-`APPROVED` `StockIssueRequestItem`:
   a new `returnedQty` running-total column on that same table (same "running total on the line
   itself" shape as `PurchaseOrderItem.receivedQty`), and a single `returnLine` service method —
   no draft/confirm two-step, since there's no multi-line document to batch first.
3. **Only an `APPROVED` (i.e. actually issued) line can be returned**, and only up to `requestedQty
   - returnedQty` — enforced both in the service and via a DB check constraint
   (`returned_qty <= requested_qty`).
4. **Its own permission** (`INVENTORY_ISSUE_REQUEST_RETURN`), separate from `_APPROVE` — per the
   operation-wise permission mapping rule, returning stock is a distinct, audit-worthy action from
   approving the original issue.
**Impact:** `StockMovementService`'s qty-delta switch changed (RETURN moved from the
`DISPOSAL`-grouped decrease-only branch to the `ADJUSTMENT`/`TRANSFER`-grouped direction-based
branch); `SupplierReturnService` updated to pass explicit direction; new columns/constraint on
`stock_issue_request_items` (V448); new permission (V449). `MILESTONES.md` and
`RELEASE_3_MILESTONES.md` updated in the same change. `./gradlew compileJava` and `npx tsc -p
tsconfig.app.json --noEmit` both run clean before committing.

## 2026-09-08 — Loanable Item Issue slice: made autonomously overnight, Phase 4 otherwise complete

**Made autonomously overnight — flag for morning review if this reads wrong.** Continues the same
unattended, no-confirmation build session as the entries above.
**Decisions:**
1. **Deliberately not integrated with `StockLedger`/`StockBalance`.** A loanable item (sports
   equipment, hostel items) isn't consumed — it's expected back. Modeling a loan as an `ISSUE`/
   `RETURN` stock movement pair would be wrong: either it permanently removes the item from
   on-hand stock (incorrect — it's coming back), or it requires inventing a proper "on-loan
   quantity" concept alongside on-hand quantity in `StockBalance` — real, separately-scoped
   design work, not something to half-build inside this slice. `LoanableItemIssue` is a
   standalone tracking record instead: who has it, since when, expected back when, condition at
   each end.
2. **No borrower entity** — captured as plain text (`borrowerName` + optional `borrowerContact`),
   per the standing "no vertical branding" rule: a college's students and a hospital's staff are
   both just "a borrower" to this generic core, and building a real borrower-lookup entity would
   be premature without a concrete need.
3. **"Overdue" computed at read time, never stored** — derived from `expectedReturnDate` vs.
   today in the response mapper (and as a query predicate for the "overdue only" list filter), so
   it's never stale and needs no background job to keep in sync.
4. **Issue and return are each a single direct action** — no DRAFT/submit workflow, since there's
   nothing to build up first (unlike `StockIssueRequest`'s multi-line sheet).
5. **A product must be flagged `isLoanable`** (the reserved flag from the "Phase 2 kickoff" entry,
   unused until now) to be issued this way — enforced at create time with a clear error.
6. **Three permissions** (`INVENTORY_LOAN_ISSUE_VIEW`/`_MANAGE`/`_RETURN`) — return is its own
   permission per the operation-wise mapping rule, marking an item returned (with a condition
   assessment) being a distinct action from issuing it.
**Impact:** new table `loanable_item_issues` (V450); new permissions (V451); `status-badge
.component.ts` extended for `ISSUED` (`RETURNED` already existed). `MILESTONES.md` and
`RELEASE_3_MILESTONES.md` updated in the same change. **Phase 4 ("Requests, Issues & Returns")
is now otherwise complete** — its one remaining item, Auto-restocking (OC-206), is deliberately
deferred pending real product-policy input rather than shipped in a guessed form; see that
entry above. `./gradlew compileJava` and `npx tsc -p tsconfig.app.json --noEmit` both run clean
before committing.

## 2026-09-08 — Asset register slice: Phase 5 kickoff, made autonomously overnight

**Made autonomously overnight — flag for morning review if this reads wrong.** Continues the same
unattended, no-confirmation build session as the entries above.
**Decisions:**
1. **Could not check IHMS's own asset-status shape**, as the "Reference architecture pivot"
   entry's standing instruction asks for every remaining phase — this autonomous session has no
   access to the IHMS codebase (an external Bitbucket repo, not present here). Used the plan's own
   4-state sketch (`IN_USE`/`UNDER_MAINTENANCE`/`RETIRED`/`DISPOSED`) plus one small, defensible
   addition — an initial `AVAILABLE` state — since the plan's states have no "registered but not
   yet deployed" starting point otherwise. A future session with real IHMS access should verify
   this against IHMS's actual asset entity before assuming it's the final shape.
2. **`Asset` tracks one physical, individually-tracked unit** (asset tag as its unique identity),
   distinct from `Product`/`StockBalance`'s aggregate quantity tracking — the two coexist: a
   `Product` can be both stock-tracked in bulk (consumables) and separately have individual
   `Asset` rows registered for units that need per-unit tracking (a `Product` isn't required to be
   "asset-only" or "stock-only").
3. **`goodsReceiptLine` link is optional** — supports both a "received through the normal
   procure→receive flow" onboarding path and a standalone "already-owned, being onboarded" entry
   with no receipt behind it (per the plan's own wording for this slice).
4. **`purchaseValue`/`purchaseDate`/`usefulLifeMonths`/`salvageValue` captured now as asset master
   data**, even though nothing computes depreciation from them yet — that's the still-to-come
   "Depreciation slice"; capturing the fields now avoids a later migration just to add them.
5. **Status changes are open-ended, no state-machine validation** — unlike every DRAFT/SUBMITTED-
   style workflow elsewhere in this module, real asset status doesn't move through a fixed
   sequence (an asset can cycle `IN_USE` ↔ `UNDER_MAINTENANCE` many times before an eventual
   `RETIRED`/`DISPOSED`), so `updateStatus` only validates the target is a real enum value.
6. **Asset tag gets the mandatory real-time uniqueness check** (`uniqueFieldValidator` + `/asset-
   tag-exists`), per `CLAUDE.md`'s master-screen uniqueness pattern — this is a master-like screen
   (list + combined new/edit form), not a header/line workflow, so it follows that pattern instead
   of the DRAFT/SUBMITTED shape most of this session's other slices have used.
7. **Two permissions this slice** (`INVENTORY_ASSET_VIEW`/`_MANAGE`) — the plan's own text already
   calls out that Disposal (a later Phase 5 slice) gets its own `INVENTORY_ASSET_DISPOSE`
   permission when built; status changes and edits both stay under `_MANAGE` for now since there's
   no other audit-worthy action yet to split out.
**Impact:** new table `assets` (V452); new permissions (V453). `MILESTONES.md` and
`RELEASE_3_MILESTONES.md` updated in the same change — Phase 5 / R3-M5 now in progress.
`./gradlew compileJava` and `npx tsc -p tsconfig.app.json --noEmit` both run clean before
committing.

## 2026-09-08 — Maintenance & Service Contracts slice: made autonomously overnight

**Made autonomously overnight — flag for morning review if this reads wrong.** Continues the same
unattended, no-confirmation build session as the entries above. Also verified two infrastructure
facts worth recording: (1) this module's `OC-XXX` numbers are a local commit-message convention
only, not real Jira tickets (`scripts/jira.sh info OC-200` returns "Issue Does Not Exist" against
a working, reachable Jira instance) — consistent with how this module has always worked, not a
gap introduced tonight; (2) a stronger cross-session continuation mechanism already exists in
this repo (`scripts/r2-autonomous-run.sh`, a system-crontab-triggered headless `claude -p`
process) but was deliberately not adopted for tonight since the user committed to keeping this
terminal open, and running two autonomous processes against the same plan file risks real
number-collision races. Both are recorded in `AUTONOMOUS_OVERNIGHT_PLAN.md`'s new
"Infrastructure notes" section for any future session.
**Decisions (feature slice):**
1. **`AssetMaintenanceSchedule.recurrenceIntervalDays` is a plain day-count**, not a frequency
   enum (`MONTHLY`/`QUARTERLY`/...) plus a custom-value escape hatch — one field covers every
   cadence uniformly, simplest thing that works, matching this module's repeated "simple unless
   there's a real need" bias.
2. **`markPerformed` advances `nextDueDate` from the performed date, not the old due date** —
   standard preventive-maintenance practice (a visit that happens late doesn't compress the next
   interval); a `ONE_OFF` schedule deactivates instead, since there's nothing to recur to.
3. **`AssetServiceContract` links the existing `Supplier` master** rather than a free-text vendor
   name or a new vendor entity — same reuse-over-duplicate discipline already applied to the Infra
   hierarchy and `audit_log`; a maintenance vendor is the same kind of thing a purchasing vendor
   is. Shape (supplier link, coverage window, renewal reminder, active flag) mirrors
   `RateContract`'s own fields, the closest in-repo "standing agreement with a supplier" precedent.
4. **One permission pair covers both entities** (`INVENTORY_ASSET_MAINTENANCE_VIEW`/`_MANAGE`) —
   the plan's own wording already bundled "Maintenance scheduling and service contracts" as one
   slice/bullet with one permission pair, not two independent operations.
5. **"Overdue" (schedules) and "expired" (contracts) are both computed at read time**, never
   stored — same pattern `LoanableItemIssue`'s overdue flag already established, extended here to
   a second, analogous case (a contract past its `endDate`).
**Impact:** new tables `asset_maintenance_schedules`, `asset_service_contracts` (V454); new
permissions (V455). `MILESTONES.md` and `RELEASE_3_MILESTONES.md` updated in the same change.
`./gradlew compileJava` and `npx tsc -p tsconfig.app.json --noEmit` both run clean before
committing.

## 2026-09-08 — Depreciation slice: made autonomously overnight, no new schema

**Made autonomously overnight — flag for morning review if this reads wrong.** Continues the same
unattended, no-confirmation build session as the entries above.
**Decisions:**
1. **No new table or entity.** `Asset.purchaseValue`/`purchaseDate`/`usefulLifeMonths`/
   `salvageValue` were already captured as master data in the "Asset register slice" specifically
   so this slice wouldn't need a migration — confirmed correct. Depreciation is computed live in
   `AssetService.toResponse`, never stored, and never posted anywhere — the plan's own text
   already scoped this to "no GL posting/connector work," consistent with the already-deferred
   posting-connector decision from Phase 1's Backend Architect round.
2. **Standard straight-line only**, per the plan's own explicit instruction not to attempt
   double-declining/units-of-production without a real need. `monthlyDepreciation =
   (purchaseValue − salvageValue) / usefulLifeMonths`; whole calendar months elapsed (via `java
   .time.Period`, not a naive day-count divide) multiplied by that rate, capped so accumulated
   depreciation never exceeds the depreciable base and current book value never drops below
   salvage value once the useful life has fully elapsed.
3. **`depreciationApplicable` is `false` whenever any of the three required inputs (purchase
   value, purchase date, useful life) is missing** — the response then carries `null` for both
   computed fields rather than a misleading `0`, and the frontend shows "—" instead of a zero
   that would read as "worthless" for an asset that simply has incomplete master data. A missing
   salvage value is treated differently — defaulted to zero, not blocking, since zero salvage is
   a completely normal real value, not missing data.
4. **No new permission** — this is a computed, read-only addition to the existing `AssetResponse`
   already gated by `INVENTORY_ASSET_VIEW`/`_MANAGE`, not a new operation.
**Impact:** `AssetResponse` gained `depreciationApplicable`/`accumulatedDepreciation`/
`currentBookValue`; no migration. Asset Register list gained a "Book Value" column. `MILESTONES
.md` and `RELEASE_3_MILESTONES.md` updated in the same change. `./gradlew compileJava` and `npx
tsc -p tsconfig.app.json --noEmit` both run clean before committing.

*Next entry goes here — do not insert above this line.*
