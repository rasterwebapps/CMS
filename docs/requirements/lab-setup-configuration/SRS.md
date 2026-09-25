# Software Requirements Specification — Lab Setup & Configuration

**Module:** Lab Setup & Configuration (OneCMS / College Management System)
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Milestone:** R1-M2.4 (Release 1, Milestone 2 — Core Academic & Lab Mapping; wider README Module 7.1)

## 1. Introduction

### 1.1 Purpose
Specifies requirements for the Lab master and its in-charge/technician staff assignment, as shipped. This is the **Lab Setup & Configuration** module — the definitional master for a lab as a physical/virtual venue — distinct from Lab Scheduling (`features/lab-schedule`, timetabling), Lab Curriculum Mapping (CO/PO, documented under Curriculum & Lab-Curriculum Mapping), Equipment/Inventory (separate modules), and Lab Safety (Safety Guidelines/PPE/Incident Reports, separate modules).

### 1.2 Scope
Covers the `Lab` entity (name, type, speciality, location, capacity, status, optional link to a physical Room) and `LabInChargeAssignment` (assigning a Faculty/Technician as Lab Incharge or Technician to a Lab).

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md`, R1-M2.4 (lines 201-224)
- `backend/src/main/java/com/cms/controller/LabController.java`
- `backend/src/main/java/com/cms/model/Lab.java`, `LabInChargeAssignment.java`
- `backend/src/main/java/com/cms/model/enums/LabType.java`, `LabStatus.java`, `LabInChargeRole.java`
- Migrations: `V7__create_labs_table.sql`, `V8__create_lab_incharge_assignments_table.sql`, `V473__assign_rooms_to_labs.sql`, `V474__assign_placeholder_room_to_computer_lab.sql`
- `docs/manual-test-cases/lab-setup.md`

## 2. Overall Description

### 2.1 Product Perspective
A `Lab` is scoped to exactly one `Speciality` and optionally linked to a physical `Room` from the Campus Infrastructure hierarchy (added later, V473, per BR-54's room taxonomy work — nullable, non-unique). A Lab has a type (COMPUTER/PHYSICS/CHEMISTRY/ELECTRONICS/BIOLOGY/LANGUAGE/MECHANICAL/OTHER) and a status (ACTIVE/AVAILABLE/INACTIVE/UNDER_MAINTENANCE). One or more staff (Faculty or Technician) can be assigned as Lab Incharge or Technician via `LabInChargeAssignment`.

### 2.2 User Classes
- **Admin / College Admin** — full Lab CRUD, in-charge assignment (`LAB_MANAGE`).
- **Lab Incharge role** — per the original milestone text, intended to also update Lab details (`ROLE_LAB_INCHARGE` listed in the milestone's endpoint table); the shipped code has no Lab-Incharge-specific permission, so this remains covered by `LAB_MANAGE` like the rest of Lab CRUD (`PUT /labs/{id}` was incorrectly gated by `LAB_VIEW` until fixed 2026-09-24 — see Known Gaps).
- **Any authenticated user** — read Lab list/detail (used by Faculty/Curriculum/Timetable screens needing a Lab dropdown).

### 2.3 Operating Environment
Angular (`features/lab`), Spring Boot REST (`/labs`), PostgreSQL (`labs`, `lab_incharge_assignments`), Keycloak JWT auth, DB-driven RBAC.

### 2.4 Constraints / Assumptions
- `Lab.speciality` is a mandatory FK.
- `Lab.room` is optional and does not enforce capacity/uniqueness against the Room's own physical capacity.
- `LabInChargeAssignment.assigneeId`/`assigneeName` are stored as a plain `Long`/`String` pair, not a JPA relation to `Faculty` — no DB-level referential integrity to the Faculty table.

## 3. Functional Requirements

| ID | Requirement | Priority | Dependencies |
|---|---|---|---|
| FR-LAB-1 | System shall allow creation of a Lab with name, type, speciality, building, room number, capacity, and status. | Must | Speciality Management |
| FR-LAB-2 | System shall allow listing all Labs, paginated/searchable/filterable by speciality, type, and status. | Must | FR-1 |
| FR-LAB-3 | System shall allow fetching a Lab by ID, and listing all Labs under a given Speciality. | Must | FR-1 |
| FR-LAB-4 | System shall allow updating a Lab's details. | Must | FR-1 |
| FR-LAB-5 | System shall allow deleting a Lab. | Should | FR-1 |
| FR-LAB-6 | System shall allow assigning a staff member (Faculty or Technician) as Lab Incharge or Technician to a Lab, with an assignment date. | Must | FR-1 |
| FR-LAB-7 | System shall allow removing a staff assignment from a Lab, and listing all current assignments for a Lab. | Must | FR-6 |

## 4. External Interface Requirements

### 4.1 Screens
- **Lab List** (`lab-list.component`) — Material table with filters by speciality/type/status.
- **Lab Form** (`lab-form.component`) — create/edit.
- **Lab Detail** (`lab-detail.component`) — detail view showing assigned staff.

### 4.2 API Endpoints (high level)
`POST /labs`, `GET /labs`, `GET /labs/page`, `GET /labs/{id}`, `GET /labs/speciality/{specialityId}`, `PUT /labs/{id}`, `DELETE /labs/{id}`, `POST /labs/{id}/assign`, `DELETE /labs/{labId}/assignments/{assignmentId}`, `GET /labs/{id}/assignments`.

### 4.3 Key DB Entities
`labs` (FK `speciality_id`, nullable FK `room_id`), `lab_incharge_assignments` (FK `lab_id`).

## 5. Non-Functional Requirements
- **Performance:** Server-side pagination on the Lab list, filterable to avoid loading the full lab catalog.
- **Security/RBAC:** Create/update/delete/assign all gated by `LAB_MANAGE` (update was incorrectly gated by `LAB_VIEW` until fixed 2026-09-24 — see Known Gaps); reads are authentication-only.
- **Auditability:** JPA-audited timestamps on both entities.

## 6. Known Gaps / Not Yet Implemented
- ~~The original milestone explicitly listed `PUT /labs/{id}` as requiring `ROLE_ADMIN, ROLE_LAB_INCHARGE`...~~ **Fixed 2026-09-24:** `PUT /labs/{id}` was gated by `@perm.has('LAB_VIEW')` — a view-only permission that let any user with read access mutate a Lab, inconsistent with every sibling mutating endpoint in `LabController` (create/delete/assign all require `LAB_MANAGE`) and with the frontend's own `canManageLabs()` check (`lab-detail.component.ts`), which already gated the Edit UI action on `LAB_MANAGE`. Backend now requires `LAB_MANAGE`, matching the frontend. No dedicated Lab-Incharge-only permission exists (or is needed) — this is consistent with the app's DB-driven, permission-not-role model (BR-24); a Lab Incharge is simply granted `LAB_MANAGE` via Role Management if that access is wanted for that role.
- No dependency guard was found preventing deletion of a Lab that still has active `LabInChargeAssignment`s, Lab Schedules, or Lab Curriculum Mappings referencing it.
- `LabInChargeAssignment` does not enforce that `assigneeId` corresponds to a real, active Faculty record.
