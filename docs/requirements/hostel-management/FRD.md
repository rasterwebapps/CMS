# Functional Requirements Document — Hostel Management

**Application:** OneCMS / College Management System
**Company:** Raster / Raster Images Pvt. Ltd.
**Client:** SKSCON / SKS College Of Nursing
**Module:** Hostel Management (Release 2, Milestone 4 — R2-M4)
**Status:** 🟡 PARTIAL / IN PROGRESS

---

## 1. Overview

This document describes, at implementation detail, the parts of Hostel Management that are actually shipped: the shared Campus Infrastructure hierarchy (as far as it relates to hostel rooms), the Hostel Room Type master, Room Preferences, and Room Allocation. It explicitly does **not** describe Mess Management, Hostel Attendance & Leave, or Hostel Fee billing — none of that is built (Section 8).

## 2. Actors & Permissions

Permission codes below are the exact strings found in Flyway migrations and enforced via `@PreAuthorize("@perm.has('<code>')")` on each controller method. All are seeded only to `DEV_ADMIN`, `SUPPORT_ADMIN`, `ADMIN`, `COLLEGE_ADMIN` (no distinct warden/front-office role exists yet) and synced to the DEV_ADMIN/SUPPORT_ADMIN catch-all block on each migration.

| Permission code | Screen/action | Migration |
|---|---|---|
| `HOSTEL_ROOM_TYPE_VIEW` | View Hostel Room Types | V299 |
| `HOSTEL_ROOM_TYPE_MANAGE` | Manage Hostel Room Types (create/edit/delete/status/uniqueness checks) | V299 |
| `CAMPUS_INFRASTRUCTURE_VIEW` | View Organization→Room hierarchy | V307 |
| `CAMPUS_INFRASTRUCTURE_MANAGE` | Manage Organization→Room hierarchy | V307 |
| `HOSTEL_ROOM_VIEW` | View a Room's hostel-room (type) designation | V307 |
| `HOSTEL_ROOM_MANAGE` | Assign/unassign a Room's hostel-room designation | V307 |
| `HOSTEL_ROOM_PREFERENCE_VIEW` | View Room Preferences | V310 |
| `HOSTEL_ROOM_PREFERENCE_MANAGE` | Create/update/delete Room Preferences | V310 |
| `HOSTEL_ROOM_ALLOCATION_VIEW` | View Room Allocations / occupancy | V310 |
| `HOSTEL_ROOM_ALLOCATION_MANAGE` | Create/update-status/delete Room Allocations | V310 |

`HOSTEL_ROOM_VIEW`/`MANAGE` were deliberately kept separate from `CAMPUS_INFRASTRUCTURE_VIEW`/`MANAGE` per the operation-wise permission mapping gate — assigning a room type to a room is treated as a hostel-domain action, not general facilities/campus-admin work. The whole module is additionally gated by the `HOSTEL` feature-module flag.

## 3. Screens & UI Behavior

### 3.1 Hostel Room Types (`/hostel-room-types`)

- List screen (card/table view), Add/Edit form, standard masters pattern (mirrors `DesignationMaster`).
- Fields: `name` (required, ≤100 chars, unique), `code` (required, ≤50 chars, unique), `sharingCapacity` (required, integer ≥1), `isAc` (boolean), `feeAmountPerYear` (required, BigDecimal ≥0), `description` (optional, ≤500 chars), `isActive`.
- Real-time async uniqueness on both `name` and `code` via `uniqueFieldValidator` against `GET /hostel-room-types/name-exists` and `/code-exists` (both take `value` + optional `excludeId` for edit mode).
- Status toggle (active/inactive) via `PATCH /hostel-room-types/{id}/status`.
- Server-side paginated list (`GET /hostel-room-types/page`, default size 25, sorted by `name`), plus an un-paginated `GET /hostel-room-types?activeOnly=` for dropdown consumers.

### 3.2 Campus Infrastructure (`/campus-infrastructure`)

- One 6-level accordion/skyline builder screen (no table-view toggle — doesn't fit this data shape) covering Organization, Branch, Block, Floor, Zone, Room.
- Each lower-level form has cascading parent selects (Organization → Branch → ... → immediate parent).
- Uniqueness: Organization/Branch name+code unique; Block code unique per Branch; Floor name+floorNumber unique per Block; Room number unique per Zone — each with its own async `*-exists` endpoint.
- Drag-to-reorder supported at Block (within Branch), Zone (within Floor), and Room (within Zone) level via dedicated `.../reorder` endpoints.
- The Room form includes the hostel-room designation inline: a checkbox + `HostelRoomType` select, calling `PUT /campus-infrastructure/rooms/{roomId}/hostel-room`. This is blocked server-side unless the Room's Purpose Category is `isResidential = true`.
- Room Purpose Category / Room Sub-Type are separate small master screens (`/room-purpose-category`, `/room-sub-type`) feeding the Room form's classification dropdowns.

### 3.3 Room Preferences

- **Embedded picker** (`RoomPreferencePickerComponent`): dropped into the Enquiry form (shown when Student Type = Hosteler) and both branches of the Admission form (from-enquiry create + edit). Self-contained — owns its own load/save calls, not routed through the parent form's submit.
  - Takes a `gender` input (`'MALE' | 'FEMALE' | 'OTHER' | null`), used to filter the offered Zone dropdown: a zone with `genderRestriction = null` is always shown; `MALE` sees `BOYS`-restricted + unrestricted zones; `FEMALE` sees `GIRLS`-restricted + unrestricted zones; `OTHER` sees only unrestricted zones (a hint is shown if none exist).
  - If gender becomes known/changes after a zone was already picked and that zone is no longer compatible, the stale selection is cleared automatically.
- **Staff queue screen** (`/room-preferences`): table view, status filter (`PENDING`/`FULFILLED`/`CANCELLED`), mark-fulfilled/cancel row actions, search by enquiry name or student first/last name.

### 3.4 Room Allocation (`/room-allocations`)

- Occupancy-map dashboard: room cards grouped/filterable by room type, zone, and gender (`HostelRoomOccupancyResponse.genderRestriction`, sourced from the room's Zone), each card showing an occupancy bar (`occupiedCount` / `sharingCapacity`) and a Boys/Girls badge.
- "Allocate" flyout: debounced student search restricted to `studentType == HOSTELER` students, start date (required) and optional end date, optional remarks.
- Per-occupant cancel action from the room card (updates allocation status, does not hard-delete unless explicitly deleted via the delete endpoint).

## 4. Functional Workflows

### 4.1 Set up hostel-eligible physical space (staff)
1. Create/select Organization → Branch → Block → Floor → Zone → Room via Campus Infrastructure.
2. Optionally mark Block/Floor/Zone `isHostel` + `genderRestriction` (cascades downward on save).
3. Classify the target Room's Purpose Category as Residential (and optionally a Sub-Type).
4. Attach a `HostelRoomType` to the Room (`PUT .../rooms/{roomId}/hostel-room`) — this is rejected with `IllegalArgumentException` if the Room isn't Residential-classified.

### 4.2 Capture a room preference (staff, at Enquiry or Admission)
1. Staff set Student Type = Hosteler on the Enquiry (or Admission) form; the Room Preference Picker becomes visible.
2. Staff pick a preferred `HostelRoomType` (required) and optionally a gender-compatible `Zone`.
3. `POST /room-preferences` with `enquiryId` (or `studentId`) + `preferredRoomTypeId` (+ optional `preferredZoneId`, `remarks`). Exactly one of `enquiryId`/`studentId` must be present; a second preference for the same enquiry/student is rejected ("update it instead").
4. On enquiry→student conversion, the existing preference row is updated in place (its `studentId` populated) rather than a new row created.

### 4.3 Allocate a room (staff, binding)
1. Staff open the Room Allocation dashboard, pick a room card with available capacity, open the Allocate flyout.
2. Search restricted to `HOSTELER` students; pick one, set start date (+ optional end date, remarks).
3. `POST /room-allocations` validates, in order: student exists → `student.studentType == HOSTELER` (else `IllegalStateException`) → student has no existing `ACTIVE` allocation (else `IllegalStateException`) → hostel room exists → current `ACTIVE` occupant count < `HostelRoomType.sharingCapacity` (else `IllegalStateException`).
4. On success, a `RoomAllocation` row is created with status `ACTIVE` (default) or an explicitly supplied status.

### 4.4 Cancel/end an allocation
1. Staff use the row/card cancel action → `PATCH /room-allocations/{id}/status` with a new `RoomAllocationStatus` (`COMPLETED`/`CANCELLED`), or `DELETE /room-allocations/{id}` to remove the record outright.
2. No side effects fire (no fee adjustment, no notification) — this is a bare status/row change today.

## 5. API Endpoints

### 5.1 Hostel Room Type (`/hostel-room-types`)

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/hostel-room-types` | `HostelRoomTypeRequest` | `HostelRoomTypeResponse` (201) | `HOSTEL_ROOM_TYPE_MANAGE` |
| GET | `/hostel-room-types?activeOnly=` | — | `List<HostelRoomTypeResponse>` | `HOSTEL_ROOM_TYPE_VIEW` |
| GET | `/hostel-room-types/{id}` | — | `HostelRoomTypeResponse` | `HOSTEL_ROOM_TYPE_VIEW` |
| PUT | `/hostel-room-types/{id}` | `HostelRoomTypeRequest` | `HostelRoomTypeResponse` | `HOSTEL_ROOM_TYPE_MANAGE` |
| DELETE | `/hostel-room-types/{id}` | — | 204 | `HOSTEL_ROOM_TYPE_MANAGE` |
| PATCH | `/hostel-room-types/{id}/status` | `ActiveStatusUpdateRequest` | `ActiveStatusUpdateResponse` | `HOSTEL_ROOM_TYPE_MANAGE` |
| GET | `/hostel-room-types/page?search=&page=&size=` | — | `Page<HostelRoomTypeResponse>` | `HOSTEL_ROOM_TYPE_VIEW` |
| GET | `/hostel-room-types/name-exists?value=&excludeId=` | — | `boolean` | `HOSTEL_ROOM_TYPE_MANAGE` |
| GET | `/hostel-room-types/code-exists?value=&excludeId=` | — | `boolean` | `HOSTEL_ROOM_TYPE_MANAGE` |

`HostelRoomTypeRequest`: `name`, `code`, `sharingCapacity` (Integer, ≥1), `isAc` (Boolean), `feeAmountPerYear` (BigDecimal, ≥0), `description`, `isActive`.

### 5.2 Campus Infrastructure (`/campus-infrastructure`) — hostel-relevant subset

| Method | Path | Permission |
|---|---|---|
| GET/PUT/DELETE | `/campus-infrastructure/rooms/{roomId}/hostel-room` | `HOSTEL_ROOM_VIEW` (GET) / `HOSTEL_ROOM_MANAGE` (PUT/DELETE) |

`HostelRoomRequest`: `roomTypeId` (required), `isActive`. Full generic hierarchy CRUD (`organizations`, `branches`, `blocks`, `floors`, `zones`, `rooms`, each with reorder + name/code-exists) exists under the same controller but is general Campus Infrastructure, not hostel-exclusive — see `CampusInfrastructureController.java`.

### 5.3 Room Preferences (`/room-preferences`)

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/room-preferences` | `RoomPreferenceRequest` | `RoomPreferenceResponse` (201) | `HOSTEL_ROOM_PREFERENCE_MANAGE` |
| GET | `/room-preferences/{id}` | — | `RoomPreferenceResponse` | `HOSTEL_ROOM_PREFERENCE_VIEW` |
| GET | `/room-preferences/enquiry/{enquiryId}` | — | `RoomPreferenceResponse` or 404 | `HOSTEL_ROOM_PREFERENCE_VIEW` |
| GET | `/room-preferences/student/{studentId}` | — | `RoomPreferenceResponse` or 404 | `HOSTEL_ROOM_PREFERENCE_VIEW` |
| GET | `/room-preferences/page?search=&status=` | — | `Page<RoomPreferenceResponse>` | `HOSTEL_ROOM_PREFERENCE_VIEW` |
| PUT | `/room-preferences/{id}` | `RoomPreferenceRequest` | `RoomPreferenceResponse` | `HOSTEL_ROOM_PREFERENCE_MANAGE` |
| DELETE | `/room-preferences/{id}` | — | 204 | `HOSTEL_ROOM_PREFERENCE_MANAGE` |

`RoomPreferenceRequest`: `enquiryId` (nullable), `studentId` (nullable — exactly one must be set), `preferredRoomTypeId` (required), `preferredZoneId` (nullable), `status`, `remarks` (≤500 chars).

### 5.4 Room Allocations (`/room-allocations`)

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/room-allocations` | `RoomAllocationRequest` | `RoomAllocationResponse` (201) | `HOSTEL_ROOM_ALLOCATION_MANAGE` |
| GET | `/room-allocations/{id}` | — | `RoomAllocationResponse` | `HOSTEL_ROOM_ALLOCATION_VIEW` |
| GET | `/room-allocations/student/{studentId}` | — | `List<RoomAllocationResponse>` | `HOSTEL_ROOM_ALLOCATION_VIEW` |
| GET | `/room-allocations/page?search=&status=` | — | `Page<RoomAllocationResponse>` | `HOSTEL_ROOM_ALLOCATION_VIEW` |
| GET | `/room-allocations/occupancy` | — | `List<HostelRoomOccupancyResponse>` | `HOSTEL_ROOM_ALLOCATION_VIEW` |
| PATCH | `/room-allocations/{id}/status` | `RoomAllocationStatusUpdateRequest` | `RoomAllocationResponse` | `HOSTEL_ROOM_ALLOCATION_MANAGE` |
| DELETE | `/room-allocations/{id}` | — | 204 | `HOSTEL_ROOM_ALLOCATION_MANAGE` |

`RoomAllocationRequest`: `studentId` (required), `hostelRoomId` (required), `startDate` (required), `endDate` (nullable), `status` (nullable, defaults to `ACTIVE`), `remarks` (≤500 chars).

## 6. Data Model

| Table | Key columns | Relationships |
|---|---|---|
| `organizations` | `name`, `code`, `description` | root of hierarchy |
| `branches` | `organization_id`, `name`, `code` (unique per org) | belongs to `organizations` |
| `blocks` | `branch_id`, `name`, `code` (unique per branch), `description`, `is_hostel`, `gender_restriction`, `is_active`, `order_index` | belongs to `branches` |
| `floors` | `block_id`, `name`, `floor_number`, `is_hostel`, `gender_restriction`, `is_basement`, `is_active` | belongs to `blocks`; unique on (block, name, floor_number) |
| `zones` | `floor_id`, `name`, `is_hostel`, `gender_restriction` (`BOYS`/`GIRLS`/null), `warden_id` (FK → `faculty`, nullable), `is_active`, `order_index` | belongs to `floors` |
| `rooms` | `zone_id`, `room_number`, `capacity` (generic, informational), `purpose_category_id` (FK, nullable), `sub_type_id` (FK, nullable), `description`, `is_active`, `order_index` | belongs to `zones`; unique room_number per zone |
| `hostel_rooms` | `room_id` (FK, unique), `room_type_id` (FK → `hostel_room_types`), `is_active` | thin join, one row per hostel-designated Room |
| `hostel_room_types` | `name` (unique), `code` (unique), `sharing_capacity`, `is_ac`, `fee_amount_per_year`, `description`, `is_active` | flat master, referenced by `hostel_rooms`, `room_preferences` |
| `room_preferences` | `enquiry_id` (FK, nullable), `student_id` (FK, nullable — exactly one populated), `preferred_room_type_id` (FK), `preferred_zone_id` (FK, nullable), `status` (`PENDING`/`FULFILLED`/`CANCELLED`), `remarks` | non-binding |
| `room_allocations` | `student_id` (FK, required), `hostel_room_id` (FK, required), `start_date`, `end_date` (nullable), `status` (`ACTIVE`/`COMPLETED`/`CANCELLED`), `remarks` | binding, capacity-consuming |
| `students.student_type` | `VARCHAR(20)`, nullable, values `DAY_SCHOLAR`/`HOSTELER` | added by V297; backfilled from `enquiries.student_type` via `enquiries.converted_student_id` |

All entities carry `created_at`/`updated_at` via `AuditingEntityListener`.

Migrations: V297 (`students.student_type`), V298–V299 (`hostel_room_types` + permissions), V300–V307 (Organization→Room hierarchy + permissions), V308 (`room_preferences`), V309 (`room_allocations`), V310 (Room Preference/Allocation permissions), V341–V344 (Room Purpose Category/Sub-Type + the Residential gate on `assignHostelRoom`, added after the initial hierarchy build).

## 7. Edge Cases & Validation Rules

- **Allocation blocked for non-hostelers.** `RoomAllocationService.create()` throws `IllegalStateException` if `student.studentType != HOSTELER` — the message explicitly tells staff to convert the student first (even though the conversion action itself doesn't exist yet — see Section 8).
- **No double-active-allocation.** A student with an existing `ACTIVE` `RoomAllocation` cannot be allocated again until that allocation's status changes.
- **Capacity enforcement.** Allocation is rejected once `count(ACTIVE allocations for this HostelRoom) >= HostelRoomType.sharingCapacity`; `Room.capacity` is never consulted for this check.
- **Residential gate.** `assignHostelRoom` throws `IllegalArgumentException` if the target Room has no `purposeCategory` or its category's `isResidential` flag is not `true`.
- **One preference per enquiry/student.** `RoomPreferenceService.create()` rejects a second preference for the same `enquiryId` or `studentId` ("update it instead").
- **Preference must link to something.** `RoomPreferenceRequest` with both `enquiryId` and `studentId` null throws `IllegalArgumentException`.
- **Gender filtering is advisory, not enforced.** Both the Room Preference picker and the Room Allocation dashboard filter *displayed* zones/rooms by gender compatibility, but neither `RoomPreferenceService` nor `RoomAllocationService` performs a server-side gender check — a mismatched allocation is not technically rejected by the backend if forced via direct API call.
- **`HostelRoom.room_id` is unique** — a physical Room can be attached to at most one `HostelRoomType` at a time (re-designating updates the existing row rather than creating a second one).
- **Cascade overwrite, not merge.** Setting `isHostel`/`genderRestriction` on a Block or Floor unconditionally overwrites the same fields on every Floor/Zone underneath — there is no "only cascade if unset" behavior.

## 8. Known Gaps / Deferred

Not implemented — no entities, controllers, services, migrations, or frontend routes exist for any of the following (confirmed by codebase search):

1. **Student-type conversion (R2-4.0.2).** No endpoint, no `STUDENT_TYPE_CONVERSION_MANAGE` permission, no audit-trail table (would-be analog of `student_promotion_decisions`), no frontend confirmation-step UI. `Student.studentType` is currently set once (at admission conversion) and never updated afterward by any code path.
2. **Hostel Fee wiring (R2-4.2)** and the fee-recalculation half of R2-4.0.2 — both blocked on the same finding: the live per-term billing path (`FeeDemandServiceImpl.generateDemandsForTermInstance()`) doesn't read `StudentFeeAllocation`/`hasHostelFee` at all, so there is no confirmed hook to make allocation/conversion change what a student is billed. `RoomAllocationService` today performs zero fee-related side effects.
3. **Mess Management (R2-M4.3).** No `Mess`-prefixed or meal-related entity, table, controller, or screen exists.
4. **Hostel Attendance & Leave (R2-M4.4).** No hostel-specific in/out attendance entity (distinct from academic `Attendance`/`LabAttendance`) and no leave-request/warden-approval workflow exist.
5. **Remaining R2-4.5 Frontend:** student-type conversion action + fee-impact preview; Hostel Fee management screen (Fee Explorer filtered to `HOSTEL_FEE` showing room-type derivation); Mess menu/meal schedule UI; Hostel attendance tracker + leave request/approval UI (warden-facing queue + student-facing request/status).
6. **Student self-service.** No student-facing view of their own room preference, allocation, or (future) leave status; `studentType` is not exposed on `StudentResponse`.
7. **Manual test cases (R2-4.6)** for Hostel Management have not been written (`docs/manual-test-cases/hostel-management.md` does not exist).
