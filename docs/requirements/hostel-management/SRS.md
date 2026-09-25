# Software Requirements Specification — Hostel Management

**Application:** OneCMS / College Management System
**Company:** Raster / Raster Images Pvt. Ltd.
**Client:** SKSCON / SKS College Of Nursing
**Module:** Hostel Management (Release 2, Milestone 4 — R2-M4)
**Status as of this document:** 🟡 **PARTIAL / IN PROGRESS.** Room infrastructure, room-type master, room preferences, and room allocation are shipped. Hostel fee wiring, mess management, and hostel attendance/leave are not built. See Section 6 for the full gap list.

---

## 1. Introduction

### 1.1 Purpose

This SRS documents the requirements that are **actually implemented** in the Hostel Management module of OneCMS as of this writing, reverse-engineered from the shipped backend (Spring Boot/PostgreSQL/Flyway) and frontend (Angular) code. It intentionally does not describe the full original R2-M4 scope as if it were complete — only what has shipped is recorded as a met requirement. Deferred/blocked work is called out separately so this document cannot be mistaken for a finished-module spec.

### 1.2 Scope

In scope for what's documented here:
- Campus Infrastructure physical hierarchy (Organization → Branch → Block → Floor → Zone → Room) to the extent it is the shared spine Hostel builds on, and the Hostel-specific `HostelRoom` attachment on top of it.
- `HostelRoomType` master (pricing/sharing/AC master data).
- `RoomPreference` (non-binding pre-allocation request) capture at Enquiry/Admission.
- `RoomAllocation` (binding, capacity-consuming room assignment) and the occupancy dashboard.
- `Student.studentType` persistence (read-only in this phase).

Out of scope (not built, see Section 6): hostel fee recalculation/wiring, student-type mid-course conversion workflow, mess management, hostel attendance, hostel leave/out-pass approval workflow.

### 1.3 References

- `docs/RELEASE_2_MILESTONES.md` — R2-M4 (lines ~138–228) and the Release 2 progress tracker (line ~425)
- `docs/BUSINESS_REQUIREMENTS.md` — BR-30 (Fee Structure dimensions incl. `studentType`/HOSTEL_FEE), BR-60 (Core Physical Infrastructure & Spatial Visualization Engine, the shared Campus Infrastructure spine)
- Flyway migrations `V297`–`V310`, `V341`–`V344` (Room Purpose Classification)
- Source: `backend/src/main/java/com/cms/model/{Organization,Branch,Block,Floor,Zone,Room,HostelRoom,HostelRoomType,RoomPreference,RoomAllocation}.java`, `backend/src/main/java/com/cms/service/{CampusInfrastructureService,HostelRoomTypeService,RoomPreferenceService,RoomAllocationService}.java`, `frontend/src/app/features/hostel/**`

### 1.4 Intended Audience

Product Owner, engineering team (backend/frontend), QA, and Raster/SKSCON stakeholders tracking Release 2 milestone status.

---

## 2. Overall Description

### 2.1 Product Perspective

Hostel Management is one of twelve planned Release 2 modules (Module 10) inside OneCMS, a single-tenant Spring Boot + Angular monolith. It is gated behind the `HOSTEL` feature-module flag (`app.modules.enabled`) — when disabled for a deployment, its nav entries, routes, and API endpoints are all blocked (`MODULE_NOT_ENABLED`).

Architecturally, Hostel Management does **not** introduce its own room/location entities. Per a 2026-07-22 scoping decision, it reuses a new **shared, campus-wide physical hierarchy** (`Organization → Branch → Block → Floor → Zone → Room`) that is intended to also eventually back Classroom/Lab location data and a future Stores/Inventory module — none of which is built yet. Hostel-specific data hangs off this shared spine via a thin `HostelRoom` join entity, so "is this room a hostel room" is an attachment, not a property of `Room` itself.

Hostel fees were scoped to reuse the existing Finance pipeline (`FeeType.HOSTEL_FEE`, `FeeStructureGroup`, `FeeDemand`) rather than a standalone fee entity — this reuse has not been wired yet (Section 6).

### 2.2 Actors / User Classes

All access is governed by the DB-driven role/permission system (Keycloak-authenticated users, permissions assigned to `app_roles` via `role_permissions`). Based on permission naming and role seeding found in migrations, the intended actors are:
- **Admin-tier staff** (`DEV_ADMIN`, `SUPPORT_ADMIN`, `ADMIN`, `COLLEGE_ADMIN`) — the only roles granted the module's permissions today, per the migrations (`V299`, `V307`, `V310`). No finer-grained "warden" or "front-office" role has been seeded for this module yet, even though the milestone notes anticipated different staff populations per screen (front-office for preferences, warden/hostel-admin for allocations) — role assignment itself remains DB-only, so this is a matter of DB role-permission configuration, not code.
- **Students** — not yet exposed to any hostel-specific self-service screen. `studentType` is not present on `StudentResponse`, and no student-facing preference/allocation view exists.

### 2.3 Operating Environment

- Backend: Spring Boot (Java), PostgreSQL, Flyway-managed schema.
- Frontend: Angular, served as part of the same OneCMS SPA, routes under `frontend/src/app/features/hostel/` and `frontend/src/app/features/hostel/campus-infrastructure/`.
- Auth: Keycloak; authorization: DB-driven permission checks (`@perm.has('...')` on every controller method).

### 2.4 Constraints and Assumptions

- **Warden = `Faculty` + `DesignationMaster`.** No generic Staff entity exists in this codebase, so `Zone.warden` is a nullable FK to `Faculty`, not a new Staff/Warden entity.
- **Hostel occupancy is authoritative via `HostelRoomType.sharingCapacity`**, not `Room.capacity` (which is generic/informational and shared with any future non-hostel use of `Room`).
- **A room must be classified under a `RoomPurposeCategory` with `isResidential = true`** before it can be designated a `HostelRoom` (`CampusInfrastructureService.assignHostelRoom`) — this gate was added after the initial Campus Infrastructure build, as part of a later Room Purpose Classification pass (`V341`–`V344`).
- **Gender restriction is a filter, not a hard server-side block.** `RoomPreferencePickerComponent` and the Room Allocation dashboard filter the *candidate* zones/rooms shown by student gender vs. `Zone.genderRestriction`; `RoomAllocationService.create()` itself performs no gender check.
- Assumption: this document assumes the reader has R2-M4's specialist-review scoping note (in `RELEASE_2_MILESTONES.md`) as background for *why* the hierarchy looks the way it does (non-binding preference vs. binding allocation, room-type-based pricing instead of a `FeeStructureGroup` dimension, etc.).

---

## 3. Functional Requirements (Implemented Only)

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-HOSTEL-1 | System shall provide a full CRUD master for `HostelRoomType` (name, code, sharing capacity, AC flag, fee-per-year, description, active flag), with real-time async name/code uniqueness validation while typing. | High | Master-screen uniqueness pattern (`uniqueFieldValidator` + `/name-exists`, `/code-exists`) |
| FR-HOSTEL-2 | System shall provide a 7-level physical infrastructure hierarchy (`Organization → Branch → Block → Floor → Zone → Room`) with full CRUD at every level, each level's name/code uniqueness scoped to its immediate parent (Block/code unique per Branch, Room number unique per Zone, etc.). | High | None (foundational) |
| FR-HOSTEL-3 | Creating a `Block` shall cascade-create one default `Floor`, which cascade-creates one default `Zone`, which cascade-creates one default `Room`, so a simple single-building/single-room structure requires zero extra manual steps. | Medium | FR-HOSTEL-2 |
| FR-HOSTEL-4 | `isHostel` and `genderRestriction` shall be settable at Block, Floor, or Zone level; setting either on a Block or Floor cascades the same value down to every level underneath (overwriting, not merging), and a child may subsequently be re-edited independently of its parent's last cascade. | Medium | FR-HOSTEL-2 |
| FR-HOSTEL-5 | A `Room` shall be designatable as a `HostelRoom` by attaching a `HostelRoomType`, only if the `Room`'s Room Purpose Category has `isResidential = true`; a `Room` may hold at most one `HostelRoom` attachment. | High | FR-HOSTEL-1, FR-HOSTEL-2, Room Purpose Classification (`V341`–`V344`) |
| FR-HOSTEL-6 | System shall capture a non-binding `RoomPreference` (preferred `HostelRoomType`, optional preferred `Zone`, status, remarks) linked to exactly one of an `Enquiry` or a `Student`, embeddable in the Enquiry form and both create/edit paths of the Admission form when Student Type = Hosteler. | High | FR-HOSTEL-1, FR-HOSTEL-2 |
| FR-HOSTEL-7 | When an Enquiry with a `RoomPreference` converts to a `Student`, the same preference row shall be carried forward (its `student_id` populated) rather than duplicated. | Medium | FR-HOSTEL-6 |
| FR-HOSTEL-8 | System shall provide a staff-facing Room Preferences queue screen (table view, status filter, mark-fulfilled/cancel row actions). | Medium | FR-HOSTEL-6 |
| FR-HOSTEL-9 | System shall provide a `RoomPreferencePickerComponent` that filters the offered Zone list to zones whose `genderRestriction` is compatible with the student's gender (unrestricted zones always offered; `OTHER`-gender students see only unrestricted zones), showing a hint when no compatible zone exists. | Medium | FR-HOSTEL-6 |
| FR-HOSTEL-10 | System shall create a binding `RoomAllocation` (student, hostel room, start/end date, status) only when the target student's `Student.studentType == HOSTELER`; creation must be rejected (not silently auto-converted) otherwise. | High | FR-HOSTEL-5, `Student.studentType` (FR-HOSTEL-13) |
| FR-HOSTEL-11 | `RoomAllocation` creation shall reject a student who already holds an `ACTIVE` allocation, and shall reject allocation to a room whose current `ACTIVE` occupant count has reached `HostelRoomType.sharingCapacity`. | High | FR-HOSTEL-10 |
| FR-HOSTEL-12 | System shall provide a Room Allocation occupancy-map dashboard (room cards grouped/filterable by room type, zone, and gender; occupancy bar; allocate flyout with debounced HOSTELER-only student search; per-occupant cancel). | High | FR-HOSTEL-10, FR-HOSTEL-11 |
| FR-HOSTEL-13 | `Student` shall persist a `studentType` (`DAY_SCHOLAR`/`HOSTELER`) column, copied from `Enquiry.studentType` at the point of student conversion; nullable, and — in this phase — **read-only** (not exposed on `StudentResponse`, not settable via the generic student-update endpoint). | High | None |

---

## 4. External Interface Requirements

### 4.1 Screens (implemented)

- **Hostel Room Types** — `/hostel-room-types` (list) and `/hostel-room-types/new`, `/hostel-room-types/:id/edit` (form) — under the "Hostel Management" nav group.
- **Room Preferences** — `/room-preferences` — staff queue screen; also embedded as a picker widget inside the Enquiry form and the Admission form (create and edit).
- **Room Allocation** — `/room-allocations` — occupancy-map dashboard.
- **Campus Infrastructure** — `/campus-infrastructure` — 6-level accordion/skyline builder for Organization→Room (shared, not hostel-exclusive; lives under a separate "Core Infrastructure" nav group), plus the Room form's hostel-room designation (checkbox + room-type select) and Room Purpose Category/Sub-Type screens (`room-purpose-category`, `room-sub-type`).

### 4.2 API Endpoints (implemented, high level)

All endpoints below require the stated permission via `@PreAuthorize("@perm.has('...')")`.

| Method + Path | Purpose | Permission |
|---|---|---|
| `POST/GET/PUT/DELETE /hostel-room-types`, `/hostel-room-types/{id}`, `/hostel-room-types/page`, `/hostel-room-types/{id}/status` | Hostel Room Type CRUD, pagination, status toggle | `HOSTEL_ROOM_TYPE_VIEW` / `_MANAGE` |
| `GET /hostel-room-types/name-exists`, `/hostel-room-types/code-exists` | Async uniqueness check | `HOSTEL_ROOM_TYPE_MANAGE` |
| `GET/POST/PUT/DELETE /campus-infrastructure/organizations`, `.../branches`, `.../blocks`, `.../floors`, `.../zones`, `.../rooms` (nested per level) | Campus Infrastructure hierarchy CRUD, name/code-exists, reorder | `CAMPUS_INFRASTRUCTURE_VIEW` / `_MANAGE` |
| `GET/PUT/DELETE /campus-infrastructure/rooms/{roomId}/hostel-room` | Assign/view/unassign a room's `HostelRoomType` designation | `HOSTEL_ROOM_VIEW` / `HOSTEL_ROOM_MANAGE` |
| `POST /room-preferences`, `GET /room-preferences/{id}`, `/enquiry/{enquiryId}`, `/student/{studentId}`, `/page`, `PUT /room-preferences/{id}`, `DELETE /room-preferences/{id}` | Room Preference CRUD, lookups by enquiry/student, paginated list | `HOSTEL_ROOM_PREFERENCE_VIEW` / `_MANAGE` |
| `POST /room-allocations`, `GET /room-allocations/{id}`, `/student/{studentId}`, `/page`, `/occupancy`, `PATCH /room-allocations/{id}/status`, `DELETE /room-allocations/{id}` | Room Allocation CRUD, per-student lookup, paginated list, occupancy map, status update | `HOSTEL_ROOM_ALLOCATION_VIEW` / `_MANAGE` |

### 4.3 Key DB Entities (implemented)

`organizations`, `branches`, `blocks`, `floors`, `zones`, `rooms`, `hostel_rooms`, `hostel_room_types`, `room_preferences`, `room_allocations`, plus a `student_type` column added to `students` and a pre-existing `student_type` column on `enquiries`. Full column detail is in `FRD.md` Section 6.

---

## 5. Non-Functional Requirements

### 5.1 Performance

- List endpoints (`/hostel-room-types/page`, `/room-preferences/page`, `/room-allocations/page`) are paginated (`Pageable`, default page size 25) rather than returning full tables.
- No module-specific caching, background jobs, or async processing exists; all operations are synchronous request/response.

### 5.2 Security / RBAC

- Every controller method is individually gated by a `@PreAuthorize("@perm.has('<PERMISSION_CODE>')")` check — no endpoint is left unauthenticated or permission-less.
- Permissions follow the operation-wise mapping gate: View and Manage are separate permission rows per screen/entity group (`HOSTEL_ROOM_TYPE_VIEW`/`_MANAGE`, `HOSTEL_ROOM_PREFERENCE_VIEW`/`_MANAGE`, `HOSTEL_ROOM_ALLOCATION_VIEW`/`_MANAGE`, `CAMPUS_INFRASTRUCTURE_VIEW`/`_MANAGE`, `HOSTEL_ROOM_VIEW`/`_MANAGE`), each seeded to `DEV_ADMIN`, `SUPPORT_ADMIN`, `ADMIN`, `COLLEGE_ADMIN` and synced into the DEV_ADMIN/SUPPORT_ADMIN catch-all block.
- Role assignment beyond the seeded admin-tier roles is DB-only (Role Management module), per project standard — not hardcoded anywhere in this module's code.
- The whole module is additionally gated behind the `HOSTEL` feature-module flag at nav, route, and API layers.
- No hostel-specific PII beyond what already exists on `Student`/`Enquiry` is introduced; no new file-upload or document-handling surface exists in this module.

### 5.3 Auditability

- All Hostel entities use `@CreatedDate`/`@LastModifiedDate` (`created_at`/`updated_at`) via `AuditingEntityListener` — standard entity-level audit timestamps.
- There is **no dedicated audit-trail/decision-log table** for this module (e.g. no equivalent of `student_promotion_decisions`) — the milestone notes anticipated one for student-type conversion (R2-4.0.2) and for warden leave approval (R2-M4.4), but neither of those features is built yet, so no such audit table exists.

---

## 6. Known Gaps / Not Yet Implemented

This module is **partial**. The following are explicitly **not** implemented, per the Release 2 progress tracker and confirmed absent from the codebase (no matching entities, controllers, or frontend routes found):

1. **R2-4.0.2 — Student-type mid-course conversion workflow.** No conversion endpoint, no `STUDENT_TYPE_CONVERSION_MANAGE` permission, no confirmation-step UI exists anywhere in the codebase. `Student.studentType` is currently write-once (set only at admission conversion) and not user-editable.
2. **R2-4.0.2 / R2-4.2 — Fee recalculation and Hostel Fee wiring, both blocked on the same finding.** Tracing where to hook "recalculate current+future term HOSTEL_FEE" found that `FeeDemandServiceImpl.generateDemandsForTermInstance()` — the code that actually generates the per-term invoice — derives its amount fresh from `FeeStructureGroup → FeeStructure → FeeStructureYearAmount` scoped only by `(programId, academicYearId, yearOfStudy)`, and does **not** read `StudentFeeAllocation`/`SemesterFee`/`hasHostelFee` at all. Those fields are admission-time-only display values, not the live billing path. This is a pre-existing characteristic of the billing engine, unrelated to Hostel Management specifically, but it blocks any recalculation-on-allocate/convert logic until a design decision is made on which mechanism to target. `RoomAllocation` create/cancel in the current code **does not** trigger any fee recalculation — it only reads `studentType` as a gate.
3. **R2-M4.3 — Mess Management.** No entity, migration, controller, or frontend screen exists for mess menus or meal tracking. Confirmed absent by codebase search.
4. **R2-M4.4 — Hostel Attendance & Leave.** No hostel in/out attendance entity (distinct from academic `Attendance`) and no leave-request/warden-approval workflow exist. Confirmed absent by codebase search.
5. **Remaining R2-4.5 Frontend items not built:** student-type conversion action with fee-impact preview (depends on gap #1/#2); Hostel Fee management screen (depends on gap #2); Mess menu/meal schedule screens (depends on gap #3); Hostel attendance tracker and leave request/approval screens (depends on gap #4).
6. **No student-facing self-service surface.** `studentType` is not on `StudentResponse`; there is no student view of their own room preference, allocation, or (once built) leave requests.
7. **R2-4.6 — Manual test cases document** (`docs/manual-test-cases/hostel-management.md`) does not exist yet.

None of the above should be read as "planned but present" — they are absent from the shipped code as of this document.
