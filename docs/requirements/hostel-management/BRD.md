# Business Requirements Document — Hostel Management

**Application:** OneCMS / College Management System
**Company:** Raster / Raster Images Pvt. Ltd.
**Client:** SKSCON / SKS College Of Nursing
**Module:** Hostel Management (Release 2, Milestone 4 — R2-M4)
**Status:** 🟡 PARTIAL / IN PROGRESS — see Section 7 for what remains deferred/blocked.

---

## 1. Executive Summary / Business Objective

SKSCON, like most residential nursing colleges, admits both day-scholar and hosteler students and needs to track where hostelers physically live, what type of room they occupy, and (eventually) bill them accordingly, feed them, and account for their in/out movement. R2-M4 set out to build this as Module 10 of OneCMS.

Scoping (2026-07-22) determined that a flat "one hostel, one room list" model was insufficient for SKSCON's real needs — room type must be known and shareable with a student before admission, a room *request* is not the same as a *binding assignment*, and hostel blocks can be single-gender or mixed with gender-restricted wings. The module was accordingly built around a shared campus-wide physical hierarchy plus a distinct preference/allocation split, rather than a purpose-built flat hostel schema.

**As shipped so far**, the module delivers the physical infrastructure setup, room-type pricing/sharing master, a non-binding room-preference capture flow, and a binding room-allocation/occupancy system. It does **not yet** bill hostel fees, manage mess operations, or track hostel attendance/leave — those remain open business needs.

## 2. Stakeholders

- **Front-office / Admission staff** — expected consumers of the Room Preference capture flow at Enquiry/Admission (per the milestone scoping note); currently reach it only via the same admin-tier permissions as everyone else, since no distinct front-office role has been seeded for this module yet.
- **Warden / Hostel Admin staff** — expected consumers of the Room Allocation dashboard and (once built) the Mess and Attendance/Leave screens; `Zone.warden` (FK to `Faculty`) already models "who is responsible for this wing," but no warden-specific approval workflow exists yet.
- **Students (Hostelers)** — the subject of room preference/allocation records; have no self-service visibility into any of this yet.
- **Finance team** — stakeholders in the still-blocked hostel fee wiring (R2-4.2), since Hostel Fees were scoped to reuse the existing Finance pipeline rather than a new billing surface.
- **Raster / Raster Images Pvt. Ltd. engineering team** — module owner, responsible for the still-open billing-engine finding blocking further Hostel Fee work.

## 3. Business Rules

`docs/BUSINESS_REQUIREMENTS.md`'s table of contents has no BR entry dedicated solely to "Hostel Management" as a named business requirement. The closest, directly reused rule is:

- **BR-60 — Core Physical Infrastructure & Spatial Visualization Engine.** Describes the `Organization → Branch → Block → Floor → Zone → Room` hierarchy (referred to internally as "BR-54" in some in-code comments, though the document's actual published entry is BR-60) that Hostel Management's `HostelRoom` attaches onto. This is a shared, domain-agnostic hierarchy, not hostel-exclusive — Classroom/Lab and a future Stores module are expected to reference the same `room_id` spine eventually.
- **BR-30 — Fee Structure Group dimensions.** Documents that `HOSTEL_FEE` is one of the fee types a `FeeStructureGroup` can carry, gated by `studentType` at enquiry time. `docs/BUSINESS_REQUIREMENTS.md` itself flags that this is now **stale**: migration `V170__remove_student_type_from_fee_structure_groups.sql`'s own comment states `studentType` was deliberately removed as a `FeeStructureGroup` dimension — "Day scholar vs hosteler cost is implicit: HOSTEL_FEE row = hosteler surcharge." This staleness is directly relevant to the R2-4.2 blocker (Section 7).

No Hostel-specific BR number (e.g. a would-be "BR-54" for student-type conversion, floated in the milestone notes as "next available" once implementation starts) has actually been written up in `BUSINESS_REQUIREMENTS.md` yet, since the conversion feature itself hasn't shipped.

Business rules derived directly from the shipped code (not yet formalized as numbered BRs):

- A room can only be designated a hostel room if its Room Purpose Category is flagged `isResidential = true` — enforced server-side in `CampusInfrastructureService.assignHostelRoom`.
- A student can only be allocated a room once their `studentType` is `HOSTELER` — a `DAY_SCHOLAR` cannot be allocated; the system does not silently convert them.
- A student may hold at most one `ACTIVE` room allocation at a time.
- A hostel room's occupancy ceiling is its `HostelRoomType.sharingCapacity`, not the generic `Room.capacity`.
- `isHostel`/`genderRestriction` cascades from Block → Floor → Zone but is not a permanent lock — any child level can be independently re-edited afterward.

## 4. Business Process / Workflow — What's Built

**Room infrastructure setup (staff, one-time/occasional):**
1. Staff create the physical hierarchy top-down: Organization → Branch → Block → Floor → Zone → Room, via the Campus Infrastructure screen (a 6-level accordion/skyline builder). Creating a Block auto-creates one default Floor/Zone/Room so a simple building needs no extra manual steps.
2. Staff optionally mark a Block, Floor, or Zone as hostel space with a gender restriction — the setting cascades downward from wherever it's applied.
3. Staff classify a Room's Purpose Category/Sub-Type (e.g. Residential); only Residential-classified rooms are eligible to become hostel rooms.
4. Staff define `HostelRoomType` records (e.g. "2-Sharing AC", "4-Sharing Non-AC") with sharing capacity, AC flag, and per-year fee.
5. Staff attach a `HostelRoomType` to a Residential Room, creating a `HostelRoom`.

**Room preference capture (staff, at Enquiry and/or Admission):**
1. When an Enquiry's Student Type is set to Hosteler, the embedded Room Preference Picker lets staff record the applicant's preferred `HostelRoomType` and optionally a preferred `Zone` — filtered to gender-compatible zones only.
2. The same widget appears on the Admission form (both the "convert from enquiry" and "edit" paths), so the preference can be captured or revised at either stage.
3. When an Enquiry converts to a Student, its `RoomPreference` row is carried forward (not duplicated) by populating the row's `student_id`.
4. A staff-facing Room Preferences queue (table, status filter, mark-fulfilled/cancel) lets staff track outstanding preferences across all enquiries/students.

**Room allocation (staff, binding assignment):**
1. Staff open the Room Allocation occupancy-map dashboard — room cards grouped/filterable by room type, zone, and gender, each showing an occupancy bar.
2. Staff use the "allocate" flyout, searching HOSTELER-only students (debounced), and creating a `RoomAllocation` with a start date (and optional end date).
3. The system rejects the allocation if: the student isn't `HOSTELER`, the student already holds an active allocation, or the target room is at full `sharingCapacity`.
4. Staff can cancel an individual occupant's allocation from the dashboard.

**What happens after allocation today:** nothing automatic. No fee is generated or recalculated as a result of allocation, cancellation, or room change — that wiring is blocked (Section 7).

## 5. Success Criteria

Not formally defined in any tracked document — inferred from feature completeness against the original R2-M4 scoping note. By that yardstick: the infrastructure/preference/allocation slice (R2-4.0.1, R2-4.1a, R2-4.1.1–4.1.4) is complete and usable end-to-end for its own scope; the module as a whole is not "successful" against its original ambition until fee wiring, mess, and attendance/leave are also delivered.

## 6. Assumptions & Constraints

- Single-tenant deployment model: Hostel Management is one of nine toggleable feature modules (`app.modules.enabled`); a non-college deployment can run without it entirely.
- Warden is modeled as an existing `Faculty` with a `designation`, not a new Staff/Warden entity — there is no generic Staff entity in the codebase to build one from.
- The shared Campus Infrastructure hierarchy is intentionally generic (not hostel-only); Hostel's needs (gender restriction, hostel-designation) are layered on top rather than baked into the base entities, which keeps the base hierarchy reusable but means Hostel-specific rules (e.g. the Residential-category gate) live in `CampusInfrastructureService`, not a Hostel-owned service.
- Gender-restriction filtering is a UI/query-level convenience, not a hard database constraint — deliberately decided this way (2026-07-22) rather than adding defense-in-depth server-side rejection.

## 7. Known Gaps / Deferred

**The billing-engine finding is the central blocker and affects two separate sub-items:**

> While scoping how to recalculate a student's `HOSTEL_FEE` on conversion/allocation, engineering found that the live per-term billing path (`FeeDemandServiceImpl.generateDemandsForTermInstance()`) does not derive its invoice amount from `StudentFeeAllocation`/`SemesterFee`/`hasHostelFee` at all — it derives fresh from `FeeStructureGroup → FeeStructure → FeeStructureYearAmount`, scoped only by program/academic year/year-of-study (no quota/state/gender/studentType filtering at that specific query, unlike the equivalent lookup used elsewhere in Finance). This is a pre-existing characteristic of the billing engine, not something Hostel Management caused, but it means there is currently no reliable hook to make "convert to Hosteler" or "allocate a room" actually change what a student is billed. A design decision on which mechanism to target is needed before this can be built.

This single finding blocks:
1. **R2-4.0.2 — Student-type mid-course conversion**, and its fee-recalculation requirement (current-and-future terms only, never touching already-generated `SemesterFee`/`FeeDemand`/receipt rows).
2. **R2-4.2 — Hostel Fee wiring**, which was meant to source the `HOSTEL_FEE` amount from `room.roomType.feeAmountPerYear` on allocate/room-change, current-and-future terms only.

**Not started at all (no code exists):**
3. **R2-M4.3 — Mess Management** (menu management, meal tracking APIs).
4. **R2-M4.4 — Hostel Attendance & Leave** (in/out attendance distinct from academic attendance; leave request → warden-approval workflow, resolved via the student's allocated room's zone warden).
5. **Remaining R2-4.5 Frontend items**: student-type conversion action + fee-impact preview; Hostel Fee management screen (Fee Explorer filtered to HOSTEL_FEE with room-type derivation shown); Mess menu/meal schedule UI; Hostel attendance tracker + leave request/approval UI.
6. **R2-4.6 — Manual test case document** for Hostel Management does not exist yet.

**Also worth flagging to stakeholders (not a Hostel Management defect, but adjacent):** `docs/BUSINESS_REQUIREMENTS.md`'s BR-30 text describing `studentType` as a `FeeStructureGroup` dimension is stale relative to the actual schema (it was removed in `V170`); how a day scholar is meant to be kept off a `HOSTEL_FEE` line under the current design isn't obvious from the code alone and was explicitly flagged in the milestone notes as worth checking with the business before layering more hostel-fee logic on top.
