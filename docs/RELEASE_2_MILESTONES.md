# 🚀 Release 2 — Milestone Tracker

> **College Management System — Release 2** covers the extended modules: lab safety, communication portals, library, hostel, transport, research, placement, events, LMS, feedback, security/compliance, and mobile/integration.
>
> This corresponds to **Phase 6** from the [Master Development Plan](DEVELOPMENT_PLAN.md).
>
> **Prerequisite:** [Release 1](RELEASE_1_MILESTONES.md) must be complete before starting Release 2.

---

## 📋 Table of Contents

- [Release 2 Scope](#-release-2-scope)
- [R2-M1: Lab Safety & Compliance](#-r2-m1-lab-safety--compliance)
- [R2-M2: Communication & Portals](#-r2-m2-communication--portals)
- [R2-M3: Library Management](#-r2-m3-library-management)
- [R2-M4: Hostel Management](#-r2-m4-hostel-management)
- [R2-M5: Transport Management](#-r2-m5-transport-management)
- [R2-M6: Research & Publication](#-r2-m6-research--publication)
- [R2-M7: Placement & Career](#-r2-m7-placement--career)
- [R2-M8: Event & Activity Management](#-r2-m8-event--activity-management)
- [R2-M9: Online Learning / LMS](#-r2-m9-online-learning--lms)
- [R2-M10: Feedback & Survey](#-r2-m10-feedback--survey)
- [R2-M11: Security & Compliance](#-r2-m11-security--compliance)
- [R2-M12: Mobile & Integration](#-r2-m12-mobile--integration)
- [Release 2 Definition of Done](#-release-2-definition-of-done)
- [Release 2 Progress Tracking](#-release-2-progress-tracking)

---

## 🎯 Release 2 Scope

| Milestone | Phase Origin | Module | Key Outcome |
|-----------|-------------|--------|-------------|
| **R2-M1** | Phase 6.1 | Module 7.8 | Lab safety guidelines, incident reporting, PPE tracking |
| **R2-M2** | Phase 6.2 | Module 12 | Notice board, messaging, student/faculty/parent portals |
| **R2-M3** | Phase 6.3 | Module 9 | Book cataloging, issue/return, digital library |
| **R2-M4** | Phase 6.4 | Module 10 | Room allocation, hostel fees, mess management |
| **R2-M5** | Phase 6.5 | Module 11 | Routes, vehicles, transport fees, driver management |
| **R2-M6** | Phase 6.6 | Module 14 | Research projects, publications, patents, grants |
| **R2-M7** | Phase 6.7 | Module 15 | Placement drives, job portal, resume builder |
| **R2-M8** | Phase 6.8 | Module 17 | Events, clubs, certificate generation |
| **R2-M9** | Phase 6.9 | Module 19 | LMS content delivery, assignments, virtual labs |
| **R2-M10** | Phase 6.10 | Module 21 | Student feedback, 360° feedback, surveys, grievances |
| **R2-M11** | Phase 6.11 | Module 18 | Audit logs, GDPR compliance, document management |
| **R2-M12** | Phase 6.12 | Module 20 | Mobile API optimization, biometric/RFID, IoT integration |

---

## 🛡️ R2-M1: Lab Safety & Compliance

> **Module 7.8** — Safety guidelines, incident reporting, and PPE tracking.

- [ ] **R2-1.1** Backend: Create safety guideline entities and APIs
  - Safety guidelines management (per lab, per speciality)
  - PPE (Personal Protective Equipment) tracking per lab
- [ ] **R2-1.2** Backend: Create incident reporting entities and APIs
  - Incident report entity (`id`, `lab`, `reportedBy`, `incidentDate`, `description`, `severity`, `status`, `actionTaken`)
  - Incident workflow (Report → Investigate → Resolve → Close)
- [ ] **R2-1.3** Backend: Create safety training and audit entities and APIs
  - Safety training records (student/faculty training completion)
  - Safety audit scheduling and results
- [ ] **R2-1.4** Frontend: Create safety management components
  - Safety guidelines list and editor
  - Incident reporting form and tracker
  - PPE inventory dashboard
  - Training records view
- [ ] **R2-1.5** Write unit + controller tests (95% coverage)
- [ ] **R2-1.6** Create manual test cases: `docs/manual-test-cases/lab-safety.md`

---

## 📢 R2-M2: Communication & Portals

> **Module 12** — Notice board, internal messaging, and role-specific portals.

- [ ] **R2-2.1** Backend: Create notice board / announcements API
  - Announcement entity (`id`, `title`, `content`, `targetAudience`, `speciality`, `publishDate`, `expiryDate`, `createdBy`)
  - Filter announcements by role, speciality, date
- [ ] **R2-2.2** Backend: Create internal messaging system
  - Message entity with sender, recipients, subject, body, read status
  - Inbox, sent, and archived message endpoints
- [ ] **R2-2.3** Frontend: Create student portal dashboard
  - Personalized view: enrolled courses, attendance, lab schedule, fee status, results
- [ ] **R2-2.4** Frontend: Create faculty portal dashboard
  - Personalized view: assigned courses, lab schedules, attendance marking, student lists
- [ ] **R2-2.5** Frontend: Create parent portal (read-only access to ward's progress)
  - Ward's attendance, results, fee payment status, announcements
  - Restricted by `ROLE_PARENT`
- [ ] **R2-2.6** Frontend: Create announcement and messaging components
  - Announcement list, detail, and creation form
  - Messaging inbox, compose, and thread views
- [ ] **R2-2.7** Write unit + controller tests (95% coverage)
- [ ] **R2-2.8** Create manual test cases: `docs/manual-test-cases/communication-portals.md`

---

## 📚 R2-M3: Library Management

> **Module 9** — Book cataloging, issue/return workflows, and digital library.

- [x] **R2-3.1** Backend: Create library entities and APIs
  - `LibraryBook` entity — full accession register (accession no., title, authors, ISBN, call no., shelf, subject, source, price, status)
  - `LibraryIssue` entity — circulation (student/faculty issue, return, renew, overdue auto-marking via nightly `@Scheduled` job)
  - `LibraryFine` entity — overdue fine tracking (auto-created on return; waive/collect endpoints)
  - `LibraryPeriodical` entity — journal/periodical register (national/international, subscription status)
  - `LibrarySetting` entity — configurable loan days, max books, fine rate, max renewals
  - Book catalogue CRUD + accession-number uniqueness endpoint
  - Issue/return/renew workflow with business-rule guards (max books, max renewals, active-issue lock)
  - Fine management: `GET /library/fines`, `POST /{id}/waive`, `POST /{id}/collect`
  - Periodicals CRUD
  - Settings get/update
  - Reports: overdue, fines summary, issue history, accession register
  - Book import: Excel validate/execute/template endpoints
  - V196 (schema) + V197 (11 permissions, LIBRARIAN role) migrations complete
- [ ] **R2-3.2** Backend: Create digital library APIs *(deferred — out of scope for nursing college)*
  - Digital resource management (e-books, journals, papers)
  - Access control by student/faculty role
- [x] **R2-3.3** Frontend: Create library management components
  - Book Catalogue — list + add/edit form, async accession-number uniqueness validator
  - Issue Desk — full list, issue form (student/faculty), return/renew with confirm modal
  - Fine Management — filterable list, waive/collect with confirm modal, summary cards
  - My Library — student/faculty portal: active issues, borrow history, catalogue search, fine status
  - Journals/Periodicals — list + add/edit form
  - Reports — overdue, fines, issue history, accession register
  - Book Import — validate → preview → execute flow
  - Library Settings — configurable defaults
  - All 13 routes + LIBRARIAN sidebar nav group wired with permission guards
- [x] **R2-3.4** Write unit tests for library services
  - `LibraryBookServiceTest` — create (happy path, duplicate accession, auto-generate), delete (available/issued/not-found)
  - `LibraryFineServiceTest` — findAll (no filter / status filter), waive (happy/non-pending), collect (happy/non-pending)
  - `LibraryIssueServiceTest` — issue (student happy, book not found, not available, student not found, max books, missing ID), returnBook (on-time/overdue fine creation/already returned/lost), renew (happy/max renewals/already returned), markOverdueIssues (marks past-due / skips when none)
  - ⚠️ 40 pre-existing test compilation failures in unrelated modules (Speciality constructor, ProgramResponse, FeePaymentRepository) block test execution — fix is a separate cleanup task
- [x] **R2-3.5** Create manual test cases: `docs/manual-test-cases/library-management.md`

---

## 🏠 R2-M4: Hostel Management

> **Module 10** — Room allocation, hostel fees, mess management, and hostel attendance.
>
> **Scoped 2026-07-22** (specialist review, full R2-M4 in one pass — not phased). Key reuse decisions, driven by what already exists in the codebase rather than building parallel systems:
> - **Warden = `Faculty` + `DesignationMaster`**, not a new Staff entity. No generic Staff entity exists in this codebase; Faculty already carries a `designation` FK (same pattern as Lab In-Charge Assignment). `Room.warden` FKs to `Faculty`.
> - **Hostel fees reuse the existing Finance pipeline**, not a standalone hostel fee entity. `FeeType.HOSTEL_FEE` already exists and flows through `FeeStructureGroup` → `SemesterFee` → `FeeDemand` → `PaymentReceipt` since BR-30. `StudentFeeAllocation.hasHostelFee` already exists too — currently set once, at admission finalization (`FeeFinalizationService`), sourced from `Enquiry.studentType`, and never revisited afterward.
> - **New prerequisite surfaced by this scoping, not in the original R2-M4 draft:** `studentType` (HOSTELER/DAY_SCHOLAR) today only lives on `Enquiry` and is discarded after admission — `Student` has no such field. The Hostel module needs it as a first-class, mutable attribute of an admitted student, including **mid-course conversion** (Hosteler ⇄ Day Scholar) with fee recalculation applied **from the current term forward only** — past/already-generated `SemesterFee`/`FeeDemand`/receipt rows must never be touched. This is a Finance-domain change, tracked as **R2-M4.0** below, and is a hard dependency for R2-4.2. Recommend documenting as its own BR (next available: **BR-54**) once implementation starts, since it's a genuine new business rule, not internal plumbing.

### R2-M4.0 — Student Type Persistence & Conversion (prerequisite, Finance-domain)

- [x] **R2-4.0.1** Add `studentType` (`DAY_SCHOLAR`/`HOSTELER`) to `Student` — **shipped 2026-07-22.** `Student.studentType` (V297), copied from `Enquiry.studentType` at the exact point `EnquiryService.convertToStudent()` constructs the `Student` (same place `admissionCategory` is copied from `enquiry.getAdmissionQuota()`). Migration backfills already-admitted students via `enquiries.converted_student_id`, same link `FeeFinalizationService` already uses for `hasHostelFee`. Nullable — read-only for now (not yet exposed on `StudentResponse`/settable via the generic student-update endpoint; wiring that in is bundled with R2-4.0.2's conversion UI so it isn't touched twice).
- [ ] **R2-4.0.2** Conversion endpoint/workflow: change an admitted student's `studentType` mid-course — **blocked pending a design decision, see finding below.** Needs its own audit trail (mirrors `student_promotion_decisions`/BR-52) and a dedicated `STUDENT_TYPE_CONVERSION_MANAGE` permission (operation-wise mapping gate).
- [ ] **R2-4.0.3** Migration(s) + tests for the recalculation logic

> **⚠️ Finding 2026-07-22, blocks R2-4.0.2 — the live per-term billing engine does not work the way this milestone assumed.** While tracing where to hook in "recalculate current+future term HOSTEL_FEE," found that `FeeDemand` (the actual per-term invoice, generated by `FeeDemandServiceImpl.generateDemandsForTermInstance()` at term-OPEN) does **not** derive its amount from `StudentFeeAllocation`/`SemesterFee`/`hasHostelFee` at all. It derives `FeeDemand.totalAmount` fresh from `FeeStructureGroup` → `FeeStructure` → `FeeStructureYearAmount`, scoped only by `(programId, academicYearId, yearOfStudy)` — via `FeeStructureGroupRepository.findByProgramIdAndAcademicYearId`, which does **not** filter by quota/feeState/gender/course the way the group's own `findExact()` lookup (used elsewhere, e.g. `FeeStructureService`) does. `StudentFeeAllocation`/`SemesterFee`/`hasHostelFee` appear to be admission-time-only (used by `FeeFinalizationService` for the finalized total shown at admission), a separate calculation path from the one that actually bills each term.
>
> Also found, from migration `V170__remove_student_type_from_fee_structure_groups.sql`'s own comment: **`studentType` was deliberately removed as a `FeeStructureGroup` dimension** ("Student type is no longer a fee dimension. Day scholar vs hosteler cost is implicit: HOSTEL_FEE row = hosteler surcharge.") — meaning BR-30's documented "4 dimensions including studentType" in `BUSINESS_REQUIREMENTS.md` is stale relative to the actual schema; `FeeStructureGroup` today has 6 fields (program, academicYear, course, quota, feeState, gender), no studentType. How institutions are actually meant to keep HOSTEL_FEE off a day scholar's bill under this design isn't obvious from the code alone — worth separately checking with the user whether this is a known limitation of the current live system before layering hostel-fee recalculation on top of it.
>
> **This is unrelated to Hostel Management specifically** — it's a pre-existing characteristic (possibly a latent bug) of `FeeDemandServiceImpl`. Not fixed or touched here; flagging rather than guessing, since it's real production billing logic. R2-4.0.2 needs a decision on which mechanism the recalculation should actually target before any code is written.

**Open questions resolved 2026-07-22:**
1. **Room-type/sharing-based pricing** (not flat) — needs a new `HostelRoomType` master (see R2-M4.1a), not a `FeeStructureGroup` dimension. `FeeStructureGroup`'s existing `HOSTEL_FEE` line becomes a pre-allocation *estimate* only (shown at enquiry/admission time before a room exists); the real, binding amount comes from the allocated room's type once assigned.
2. **Conversion must happen before allocation** — a room can only be allocated to a student already flagged `HOSTELER`. Room Allocation screen should block/redirect to the conversion action if attempted on a `DAY_SCHOLAR`, not silently auto-convert.
3. **Leave requests require warden approval** — request → approve/reject workflow (mirrors the existing Scholarship application / Elective Assignment approval pattern), not a warden-logged-only in/out register.

### R2-M4.1a — Hostel Room Type Master (new, prerequisite for R2-4.1/R2-4.2)

- [x] **R2-4.1a.1** `HostelRoomType` master entity — **shipped 2026-07-22.** `name`, `code`, `sharingCapacity`, `isAc`, `feeAmountPerYear` (BigDecimal, per-year cadence to match `FeeStructureYearAmount`), `description`, `isActive`. Migration V298 (table) + V299 (`HOSTEL_ROOM_TYPE_VIEW`/`HOSTEL_ROOM_TYPE_MANAGE` permissions, DEV_ADMIN/SUPPORT_ADMIN catch-all sync). Full CRUD service/controller/repository following the `DesignationMaster` pattern exactly (async name/code uniqueness via `uniqueFieldValidator` + `/name-exists`/`/code-exists`, paginated `/page` endpoint, status toggle). Frontend: card+table list view, add/edit form, wired into `app.routes.ts` and the Preferences nav group as "Hostel Room Types". Backend unit tests (`HostelRoomTypeServiceTest`) pass; `./gradlew compileJava compileTestJava` and `ng build` both clean.
  - Note: sits under the **Preferences** nav group for now (alongside other masters) since it's the only Hostel-module screen that exists yet — revisit grouping once R2-M4.1 (Rooms) ships and a dedicated Hostel nav group makes sense.

### R2-M4.1 — Campus Infrastructure (Organization → Branch → Block → Floor → Zone → Room → HostelRoom) & Allocation

> **Re-scoped 2026-07-22.** The original flat `Hostel`/`Room` design (below, struck through) was replaced with a proper campus-wide physical hierarchy after discussion — driven by three real requirements: (1) room *type* must be shareable with students at enquiry/admission, meaning the infra must be defined ahead of admission, not built ad hoc per allocation; (2) a student's room request can change across enquiry → admission → later, subject to availability, so a non-binding **preference** must be distinct from a binding **allocation**; (3) hostel blocks can be fully separate by gender, or a shared block with mixed-gender wings — gender restriction had to live below the block level. `Hostel` (the original entity) is dropped entirely — `Block` + `Zone` already cover it, and `Zone.warden` replaces `Hostel.warden` with per-wing granularity.
>
> Chosen scope: a **shared, campus-wide** infrastructure hierarchy (not hostel-only) — Classroom/Lab's existing free-text `building` field can migrate onto it later as a separate, deliberately deferred pass, since that touches live production data in two already-shipped screens and isn't required to unblock Hostel.
>
> **Expanded 2026-07-22, same day** — a separate spec proposed a "Location-Independent Inventory & Asset Tracking Architecture" for a future standalone Stores/inventory product sitting alongside CMS. Resolved: (1) **same database, logically decoupled module** — this is a single monolith with no multi-service infra to build on yet; a shared DB avoids distributed-transaction problems for things like stock decrement + allocation confirm as one atomic step, and a clean module boundary now makes a future physical split easy without paying the operational cost today; (2) **`Organization` → `Branch` added above `Block`** (migrations renumbered — Block/Floor/Zone/Room/HostelRoom shifted from V300–V305 to V302–V307 to let Organization/Branch land first as V300/V301 — safe since none of these had shipped to any environment yet); (3) **a Zone always auto-creates one default `Room` ("Main")** on creation, so Stores/inventory attachment always has a `room_id` to target uniformly, even for a lab/space spanning a whole zone with no internal partitions. This `Block`/`Floor`/`Zone`/`Room` hierarchy is now the intended shared spine both CMS (Classroom/Lab/HostelRoom) and a future Stores module (SubStore/FixedAsset/InventoryStock) would reference by `room_id` — the Stores-specific tables themselves are **not yet scoped or built**.

- [x] **R2-4.1.1** `Organization` → `Branch` → `Block` → `Floor` → `Zone` → `Room` → `HostelRoom` migrations + entities — **shipped 2026-07-22.**
  - `Organization` (V300): `name`, `code`, `description` — root of the hierarchy, no isHostel/gender (a whole org is never meaningfully single-gender)
  - `Branch` (V301): belongs to one `Organization`; `name`, `code` unique per organization — same reasoning, no isHostel/gender
  - `Block` (V302): belongs to one `Branch`; `name`, `code` (now unique per-branch, not globally), `description`, `isHostel`, `genderRestriction`
  - `Floor` (V303): belongs to one `Block`; `name`, `floorNumber` (display ordering), `isHostel`, `genderRestriction`; unique per block on name+number
  - `Zone` (V304): belongs to one `Floor`; `name`, `isHostel`, `genderRestriction` (`BOYS`/`GIRLS`/null=unrestricted — new `GenderRestriction` enum, deliberately not reusing the person-level `Gender` enum since values differ), optional `warden` → FK `Faculty` (independent per zone, so one floor can have separate wardens per wing); **auto-creates one default `Room` named "Main"** on creation
  - `Room` (V305): belongs to one `Zone`; `roomNumber`, `capacity` (generic/informational — kept for future Classroom/Lab reuse), `description` — deliberately has no `isHostel`/gender of its own, see cascade note below
  - `HostelRoom` (V306): thin join — `room` (FK, unique) + `roomType` (FK → `HostelRoomType`, R2-4.1a). Location comes from the room's zone chain; pricing/sharing/AC comes from `HostelRoomType`; **`HostelRoomType.sharingCapacity` is authoritative for hostel occupancy, not `Room.capacity`**.
  - **`isHostel`/`genderRestriction` cascade** (Block/Floor/Zone only, not Organization/Branch): settable at Block, Floor, *or* Zone — whichever level matches reality (a whole boys-only block, one girls-only floor in a mixed block, or just one wing on a shared floor). Setting either on a Block or Floor **cascades the same values down to every level underneath** (`CampusInfrastructureService.cascadeBlockToChildren`/`cascadeFloorToChildren`), overwriting children. Not an enforced invariant — a child can be independently re-edited afterward to differ from its parent's last cascade ("not permanent for a single gender"). This is a coarse organizational label, separate from the binding `HostelRoom` designation that actually drives allocation/fees.
- [x] **R2-4.1.2** Repositories, services, controller, and CRUD frontend for the full 7-entity hierarchy — **shipped 2026-07-22.** One `CampusInfrastructureService`/`CampusInfrastructureController` (mirrors `IndiaLocationService`/`IndiaLocationController`'s nested-path-per-level shape: `/campus-infrastructure/organizations`, `/organizations/{id}/branches`, `/branches/{id}/blocks`, `/blocks/{id}/floors`, `/floors/{id}/zones`, `/zones/{id}/rooms`, `/rooms/{id}/hostel-room`). Permissions: `CAMPUS_INFRASTRUCTURE_VIEW`/`MANAGE` covers Organization through Room (one coherent hierarchy-management concern); `HOSTEL_ROOM_VIEW`/`MANAGE` stays separate (the room↔room-type attachment) per the operation-wise mapping gate — assigning a room type is a hostel-domain action, not general facilities/campus-admin work. Frontend: one 6-level accordion list screen (`campus-infrastructure-list`, no table-view toggle — doesn't fit this data shape) plus 6 forms (Organization/Branch/Block/Floor/Zone/Room), each lower-level form gaining cascading Organization→Branch→...→parent selects; the hostel-room designation stays folded into the Room form (checkbox + room-type select). Backend tests (`CampusInfrastructureServiceTest`, including explicit cascade-behavior and auto-default-room tests) pass; `./gradlew compileJava compileTestJava` and `ng build` both clean.
- [x] **R2-4.1.3** `RoomPreference` entity (non-binding; `enquiryId`/`studentId`, `preferredRoomType` FK, optional `preferredZone` FK, `status`) — captured at enquiry and/or admission, shown to the student as available options — **shipped 2026-07-22.** Migration V308. Same row is carried forward (`student_id` populated) when the enquiry converts, via a hook in `EnquiryService.convertToStudentWithData`, rather than duplicated. Frontend: embedded `RoomPreferencePickerComponent`, self-contained (owns its own load/save calls), dropped into the existing Enquiry form (shown when Student Type = Hosteler) and both branches of the Admission form (from-enquiry create + edit). Plus a staff-facing `room-preferences` list/queue screen (table view, status filter, mark-fulfilled/cancel row actions).
- [x] **R2-4.1.4** `RoomAllocation` entity (binding, capacity-consuming; `student`, `hostelRoom` FK, dates, `status`) — creation rejected (not auto-converted) if `Student.studentType != HOSTELER` (conversion, R2-M4.0, must already be done) — **shipped 2026-07-22.** Migration V309. `RoomAllocationService.create` gates on `studentType == HOSTELER`, rejects a second concurrent `ACTIVE` allocation per student, and checks occupancy (`COUNT(ACTIVE) < HostelRoomType.sharingCapacity`) before allowing a new one. Frontend: `room-allocations` occupancy-map dashboard (room cards grouped/filterable by room type and zone, occupancy bar, allocate flyout with a debounced HOSTELER-only student search, per-occupant cancel). **Explicitly not wired in this pass:** the R2-4.0.2/R2-4.2 fee-recalculation trigger on allocate/cancel — that stays blocked on the billing-engine finding above; RoomAllocation only reads `studentType`, it doesn't touch fee recalculation. Also not enforced: cross-checking the allocated room's `Zone.genderRestriction` against `Student.gender` — the data exists (via the room's zone chain) but no validation was added, since it wasn't part of the original spec and `Gender.OTHER` has no corresponding `GenderRestriction` value to map to; flagging as a gap rather than guessing a policy.
- [ ] ~~`Hostel` entity (`id`, `name`, `type` [BOYS/GIRLS], `totalRooms`, `warden`)~~ — dropped, superseded by `Block`/`Zone` above
- [ ] ~~Flat `Room` entity (`id`, `hostel`, `roomNumber`, `roomType`, `occupants`)~~ — superseded by the `Block`/`Floor`/`Zone`/`Room`/`HostelRoom` chain above

**Not yet scoped:** the Stores/inventory side of the "Location-Independent Inventory & Asset Tracking" spec — `SubStore`/`FixedAsset`/`InventoryStock` tables (all would reference `room_id`), stock movement workflow, and the unified Assets+Consumables valuation query. `InventoryItem`/`Equipment` today are both hardcoded to `lab_id NOT NULL` (Lab-only, not generic-location); `InventoryItem` has no price field at all (can't value consumables yet); `Equipment` has `purchasePrice` but no depreciation tracking. All of that is real gap work for whenever Stores scoping resumes — not touched in this pass.

### R2-M4.2 — Hostel Fees (reuses Finance pipeline, priced by room type)

- [ ] **R2-4.2** Backend: Wire hostel fee collection through the existing Finance pipeline — no new fee entity
  - `RoomAllocation` creation triggers the R2-4.0.2 recalculation path, but sources the HOSTEL_FEE amount from `room.roomType.feeAmountPerYear` (current + future terms only) instead of the generic `FeeStructureGroup` estimate
  - `RoomAllocation` removal/room-change (e.g., room-type upgrade/downgrade) re-triggers recalculation the same way, current-and-future terms only
  - Fee Explorer / receipt screens should be able to show which `HostelRoomType` a hosteler's HOSTEL_FEE line derives from, for audit clarity

### R2-M4.3 — Mess Management

- [ ] **R2-4.3a** Backend: Mess menu management and meal tracking APIs (new — no existing analog in the codebase)

### R2-M4.4 — Hostel Attendance & Leave

- [ ] **R2-4.3b** Backend: Hostel in/out attendance APIs (distinct from academic `Attendance`/`LabAttendance` — new entity, not a reuse of the THEORY/LAB attendance types)
- [ ] **R2-4.3c** Backend: Leave request workflow — student/warden-initiated request → **warden approval/rejection** required before an out-pass is considered granted; audit trail of decision (mirrors the `student_promotion_decisions`/BR-52 approval-audit pattern); warden resolved via the student's allocated `Room.hostel.warden`

### R2-M4.5 — Frontend

- [ ] **R2-4.4** Frontend: Create hostel management components
  - Hostel Room Type master screen (list + add/edit, async uniqueness — standard masters pattern)
  - Room allocation dashboard with occupancy map, grouped/filterable by room type
  - Student type conversion action (R2-M4.0) surfaced from the student profile screen, with a confirmation step showing the fee impact before committing — and gates the room-allocation screen to `HOSTELER`-only students per decision 2 above
  - Hostel fee management (reuses Fee Explorer/Fee Structure UI patterns, filtered to HOSTEL_FEE, showing room-type derivation)
  - Mess menu and meal schedule
  - Hostel attendance tracker + leave request/approval screen (warden-facing approve/reject queue, student-facing request + status)

### R2-M4.6 — Tests & Docs

- [ ] **R2-4.5** Write unit + controller tests (95% coverage)
- [ ] **R2-4.6** Create manual test cases: `docs/manual-test-cases/hostel-management.md`

---

## 🚌 R2-M5: Transport Management

> **Module 11** — Routes, vehicles, transport fees, and driver management.

- [ ] **R2-5.1** Backend: Create transport entities and APIs
  - `Route` entity (`id`, `name`, `startPoint`, `endPoint`, `stops`, `distance`)
  - `Vehicle` entity (`id`, `registrationNumber`, `type`, `capacity`, `route`, `driver`)
  - `TransportAllocation` entity (student ↔ route mapping)
  - Transport fee structure and payment tracking
  - Driver management and assignment
- [ ] **R2-5.2** Frontend: Create transport management components
  - Route management with stop configuration
  - Vehicle fleet dashboard
  - Student transport allocation
  - Transport fee management
- [ ] **R2-5.3** Write unit + controller tests (95% coverage)
- [ ] **R2-5.4** Create manual test cases: `docs/manual-test-cases/transport-management.md`

---

## 🔬 R2-M6: Research & Publication

> **Module 14** — Research projects, publications, patents, and grant management.

- [ ] **R2-6.1** Backend: Create research entities and APIs
  - `ResearchProject` entity (`id`, `title`, `principalInvestigator`, `coPIs`, `speciality`, `fundingSource`, `amount` [BigDecimal], `startDate`, `endDate`, `status`)
  - `Publication` entity (`id`, `title`, `authors`, `journal`, `year`, `doi`, `type` [JOURNAL/CONFERENCE/BOOK_CHAPTER])
  - `Patent` entity (`id`, `title`, `inventors`, `filingDate`, `status`, `patentNumber`)
  - Grant tracking and expenditure management
- [ ] **R2-6.2** Frontend: Create research management components
  - Research project dashboard with status tracking
  - Publication list with filters and citation metrics
  - Patent tracker
  - Grant expenditure reports
- [ ] **R2-6.3** Write unit + controller tests (95% coverage)
- [ ] **R2-6.4** Create manual test cases: `docs/manual-test-cases/research-publication.md`

---

## 💼 R2-M7: Placement & Career

> **Module 15** — Placement drives, job portal, and resume builder.

- [ ] **R2-7.1** Backend: Create placement entities and APIs
  - `PlacementDrive` entity (`id`, `company`, `date`, `eligiblePrograms`, `minCGPA`, `packageOffered` [BigDecimal], `status`)
  - `PlacementApplication` entity (student ↔ drive mapping with status tracking)
  - `PlacementOffer` entity (offer details, acceptance status)
  - Job portal with company profiles
  - Resume/CV builder data endpoints
- [ ] **R2-7.2** Frontend: Create placement management components
  - Placement drive calendar and details
  - Student application and tracking portal
  - Company profile management
  - Placement statistics dashboard
  - Resume builder interface
- [ ] **R2-7.3** Write unit + controller tests (95% coverage)
- [ ] **R2-7.4** Create manual test cases: `docs/manual-test-cases/placement-career.md`

---

## 🎉 R2-M8: Event & Activity Management

> **Module 17** — Events, clubs, and certificate generation.

- [ ] **R2-8.1** Backend: Create event entities and APIs
  - `Event` entity (`id`, `name`, `type`, `date`, `venue`, `organizer`, `speciality`, `budget` [BigDecimal], `status`)
  - `Club` entity (`id`, `name`, `type`, `faculty_advisor`, `president`, `speciality`)
  - `EventRegistration` entity (student ↔ event mapping)
  - Certificate generation endpoints (participation, achievement)
- [ ] **R2-8.2** Frontend: Create event management components
  - Event calendar and registration
  - Club management dashboard
  - Certificate template manager and generation
  - Event budget tracker
- [ ] **R2-8.3** Write unit + controller tests (95% coverage)
- [ ] **R2-8.4** Create manual test cases: `docs/manual-test-cases/event-management.md`

---

## 🎓 R2-M9: Online Learning / LMS

> **Module 19** — LMS content delivery, assignment submission, and virtual lab integration.

- [ ] **R2-9.1** Backend: Create LMS entities and APIs
  - `LMSCourse` entity (course content structure with modules/topics)
  - `LMSContent` entity (`id`, `course`, `module`, `title`, `contentType` [VIDEO/DOCUMENT/QUIZ], `contentUrl`, `orderIndex`)
  - `Assignment` entity (`id`, `course`, `title`, `description`, `dueDate`, `maxMarks`)
  - `AssignmentSubmission` entity (student ↔ assignment with file upload, grade)
  - Virtual lab integration endpoints
  - Content progress tracking per student
- [ ] **R2-9.2** Frontend: Create LMS components
  - Course content browser with module navigation
  - Video/document viewer
  - Assignment submission portal
  - Student progress tracker
  - Virtual lab launcher
- [ ] **R2-9.3** Write unit + controller tests (95% coverage)
- [ ] **R2-9.4** Create manual test cases: `docs/manual-test-cases/lms.md`

---

## 📋 R2-M10: Feedback & Survey

> **Module 21** — Student feedback, 360° feedback, surveys, and grievance system.

- [ ] **R2-10.1** Backend: Create feedback entities and APIs
  - `FeedbackForm` entity (`id`, `title`, `targetType` [COURSE/FACULTY/LAB/INSTITUTION], `semester`, `startDate`, `endDate`, `isAnonymous`)
  - `FeedbackQuestion` entity (questions with rating/text/MCQ types)
  - `FeedbackResponse` entity (student responses with anonymity support)
  - 360° feedback for faculty (from students, peers, HOD)
  - Survey creation and response collection
  - Grievance submission and tracking workflow
- [ ] **R2-10.2** Frontend: Create feedback and survey components
  - Feedback form builder (admin)
  - Feedback submission interface (students)
  - Feedback analytics dashboard (charts, averages, trends)
  - Survey creation and response viewer
  - Grievance submission form and status tracker
- [ ] **R2-10.3** Write unit + controller tests (95% coverage)
- [ ] **R2-10.4** Create manual test cases: `docs/manual-test-cases/feedback-survey.md`

---

## 🔒 R2-M11: Security & Compliance

> **Module 18** — Audit logs, GDPR compliance, and document management.

- [ ] **R2-11.1** Backend: Create security and compliance entities and APIs
  - `AuditLog` entity (`id`, `userId`, `action`, `entityType`, `entityId`, `timestamp`, `ipAddress`, `details`)
  - Automatic audit logging via AOP/interceptor for all write operations
  - GDPR data export and deletion endpoints (student data portability)
  - `Document` entity for institutional document management
  - Data retention policies and automated cleanup
- [ ] **R2-11.2** Frontend: Create security and compliance components
  - Audit log viewer with filters (user, action, date range, entity)
  - GDPR data request management interface
  - Document management portal (upload, version, share)
  - Compliance dashboard
- [ ] **R2-11.3** Write unit + controller tests (95% coverage)
- [ ] **R2-11.4** Create manual test cases: `docs/manual-test-cases/security-compliance.md`

---

## 📱 R2-M12: Mobile & Integration

> **Module 20** — Mobile API optimization, biometric/RFID integration, third-party APIs, and IoT.

- [ ] **R2-12.1** Backend: API optimization for mobile clients
  - Lightweight response DTOs for mobile consumption
  - Pagination and field selection support
  - Push notification integration hooks
- [ ] **R2-12.2** Backend: Biometric/RFID integration hooks
  - Attendance integration via biometric devices
  - RFID-based lab access control endpoints
  - Device registration and management APIs
- [ ] **R2-12.3** Backend: Third-party API integration endpoints
  - ERP system integration (data sync endpoints)
  - Government portal integration (regulatory reporting)
  - Payment gateway integration hooks
- [ ] **R2-12.4** Backend: IoT integration for lab sensors
  - Lab environment monitoring endpoints (temperature, humidity, power)
  - Sensor data ingestion and alerting
  - Equipment usage tracking via IoT
- [ ] **R2-12.5** Write unit + controller tests (95% coverage)
- [ ] **R2-12.6** Create manual test cases: `docs/manual-test-cases/mobile-integration.md`

---

## ✅ Release 2 Definition of Done

Every task/milestone is considered **complete** only when ALL of the following are met:

| Criteria | Description |
|----------|-------------|
| **Code Complete** | All backend and frontend code is written and functional |
| **Backend Tests** | Unit + controller tests pass with ≥95% code coverage (JaCoCo) |
| **Build Passes** | `./gradlew check` (backend) and `ng build` (frontend) succeed |
| **Flyway Migration** | Database migration script created (for PostgreSQL profile) |
| **DTOs** | All data transfer objects are Java records with Jakarta validation |
| **Role-Based Access** | `@PreAuthorize` annotations applied to all controller methods |
| **Error Handling** | All errors return standardized `ErrorResponse` via GlobalExceptionHandler |
| **Manual Test Cases** | Manual test case document created in `docs/manual-test-cases/` |
| **Business Documentation** | Any business/workflow changes documented in `docs/BUSINESS_REQUIREMENTS.md` |
| **Code Review** | Pull request reviewed and approved |
| **CHANGELOG** | `CHANGELOG.md` updated with the new feature |
| **Release 1 Stable** | All Release 1 features remain functional (regression-free) |

---

## 📊 Release 2 Progress Tracking

| Milestone | Status | Progress |
|-----------|--------|----------|
| R2-M1: Lab Safety & Compliance | ⬜ Not Started | 0% |
| R2-M2: Communication & Portals | ⬜ Not Started | 0% |
| R2-M3: Library Management | ✅ Complete | 100% — R2-3.1 ✅ R2-3.2 deferred R2-3.3 ✅ R2-3.4 ✅ R2-3.5 ✅ |
| R2-M4: Hostel Management | 🟡 In Progress | Shipped 2026-07-22: R2-4.1a (Hostel Room Type master), R2-4.0.1 (studentType on Student), R2-4.1.1+4.1.2 (full Block/Floor/Zone/Room/HostelRoom CRUD), R2-4.1.3+4.1.4 (RoomPreference + RoomAllocation, backend + frontend, incl. new dedicated "Hostel" nav module). R2-4.0.2 (conversion + fee recalc) blocked on a billing-engine finding, see note above — this also keeps R2-4.2 (fee wiring) blocked. R2-M4.3 (Mess), R2-M4.4 (Attendance/Leave), rest of R2-M4.5 (Frontend) not started |
| R2-M5: Transport Management | ⬜ Not Started | 0% |
| R2-M6: Research & Publication | ⬜ Not Started | 0% |
| R2-M7: Placement & Career | ⬜ Not Started | 0% |
| R2-M8: Event & Activity Management | ⬜ Not Started | 0% |
| R2-M9: Online Learning / LMS | ⬜ Not Started | 0% |
| R2-M10: Feedback & Survey | ⬜ Not Started | 0% |
| R2-M11: Security & Compliance | ⬜ Not Started | 0% |
| R2-M12: Mobile & Integration | ⬜ Not Started | 0% |

---

> **Note:** This release tracker is aligned with the [Master Development Plan](DEVELOPMENT_PLAN.md) Phase 6. Milestones can be prioritized and reordered based on institutional needs. All Release 2 work assumes [Release 1](RELEASE_1_MILESTONES.md) is complete and stable.
