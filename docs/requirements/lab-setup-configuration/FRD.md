# Functional Requirements Document — Lab Setup & Configuration

## 1. Overview
Lab Setup & Configuration provides CRUD for the `Lab` master (the definitional venue record — name, type, speciality, location, capacity, status, optional physical Room link) and CRUD for `LabInChargeAssignment` (assigning a Faculty/Technician as Lab Incharge or Technician to a Lab). It is the foundational master consumed by Lab Scheduling, Lab-Curriculum Mapping, and Subject's lab-preference fields.

## 2. Actors & Permissions
| Permission | Grants |
|---|---|
| `LAB_MANAGE` | Create, update, delete a Lab; assign/remove Lab Incharge or Technician staff (`PUT /labs/{id}` fixed 2026-09-24 to require this instead of `LAB_VIEW` — see Edge Cases) |
| `LAB_VIEW` | Read Lab list/detail only |
| *(none — authentication only)* | List Labs by speciality, list a Lab's current assignments |

There is no `ROLE_LAB_INCHARGE`-specific permission in the shipped system; role/permission assignment is entirely DB-driven (Role Management module), consistent with project convention.

## 3. Screens & UI Behavior

### 3.1 Lab List (`lab-list.component`)
- Material table with filters by Speciality, Lab Type, and Status.
- Columns include name, type, speciality, building/room, capacity, status.
- No uniqueness pre-check on name is wired into the form (see Edge Cases).

### 3.2 Lab Form (`lab-form.component`)
- Fields: Name (required, ≤255), Lab Type (required, enum dropdown), Speciality (required), Building (optional, ≤255), Room Number (optional, ≤50), Capacity (optional, positive integer), Status (required, enum dropdown), Room (optional link to a physical Campus Setup Room).
- No real-time async uniqueness validator on Name — deviates from this project's mandatory master-screen pattern (see Known Gaps).
- Short form — Save/Cancel at the bottom.

### 3.3 Lab Detail (`lab-detail.component`)
- Shows full Lab record plus its current staff assignments (Lab Incharge/Technician, assignment date).
- Assign-staff and remove-assignment actions available from this screen.

## 4. Functional Workflows

### 4.1 Create Lab
1. Admin opens Lab Form, fills required fields.
2. `POST /labs` with `LabRequest` body → `201 Created`.
3. New Lab appears in list and becomes selectable in every downstream Lab dropdown immediately (no activation step).

### 4.2 Assign Lab Incharge / Technician
1. Admin opens Lab Detail → "Assign Staff."
2. Selects a staff member (assigneeId/assigneeName captured as a plain snapshot, not a live FK), role (`LAB_INCHARGE` or `TECHNICIAN`), and assignment date.
3. `POST /labs/{id}/assign` creates the `LabInChargeAssignment` row.
4. Multiple assignments (including duplicate roles) are permitted — no uniqueness constraint blocks it.

### 4.3 Update / Delete Lab
1. `PUT /labs/{id}` updates the record — gated by `LAB_MANAGE` (fixed 2026-09-24; previously `LAB_VIEW`, see Edge Cases).
2. `DELETE /labs/{id}` (gated by `LAB_MANAGE`) hard-deletes the Lab with no pre-delete dependency check.

## 5. API Endpoints

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/labs` | `LabRequest` | `201` Lab | `LAB_MANAGE` |
| GET | `/labs` | — | `200` `List<Lab>` | authenticated |
| GET | `/labs/page` | search/filter params | `200` `Page<Lab>` | authenticated |
| GET | `/labs/{id}` | — | `200` Lab | authenticated |
| GET | `/labs/speciality/{specialityId}` | — | `200` `List<Lab>` | authenticated |
| PUT | `/labs/{id}` | `LabRequest` | `200` Lab | `LAB_MANAGE` |
| DELETE | `/labs/{id}` | — | `204` | `LAB_MANAGE` |
| POST | `/labs/{id}/assign` | assignee id/name, role, date | `201` assignment | `LAB_MANAGE` |
| DELETE | `/labs/{labId}/assignments/{assignmentId}` | — | `204` | `LAB_MANAGE` |
| GET | `/labs/{id}/assignments` | — | `200` `List<assignment>` | authenticated |

## 6. Data Model

**Table:** `labs`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| name | VARCHAR NOT NULL | no DB or app-level uniqueness constraint |
| lab_type | VARCHAR NOT NULL | enum: COMPUTER/PHYSICS/CHEMISTRY/ELECTRONICS/BIOLOGY/LANGUAGE/MECHANICAL/OTHER |
| speciality_id | BIGINT NOT NULL FK → specialities | |
| building | VARCHAR | nullable |
| room_number | VARCHAR | nullable |
| capacity | INTEGER | nullable |
| room_id | BIGINT | nullable FK → rooms (Campus Infrastructure), added V473 |
| status | VARCHAR NOT NULL | enum: ACTIVE/AVAILABLE/INACTIVE/UNDER_MAINTENANCE |
| created_at / updated_at | TIMESTAMPTZ | JPA-audited |

**Table:** `lab_incharge_assignments`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| lab_id | BIGINT NOT NULL FK → labs | |
| assignee_id | BIGINT NOT NULL | plain value, no FK to `faculty` |
| assignee_name | VARCHAR NOT NULL | name snapshot at assignment time |
| role | VARCHAR NOT NULL | enum: LAB_INCHARGE/TECHNICIAN |
| assigned_date | DATE NOT NULL | |
| created_at / updated_at | TIMESTAMPTZ | JPA-audited |

## 7. Edge Cases & Validation Rules
- ~~**Update permission mismatch:** `PUT /labs/{id}` requires only `LAB_VIEW`...~~ **Fixed 2026-09-24:** `PUT /labs/{id}` now requires `LAB_MANAGE`, consistent with create/delete/assign and with the frontend's own `canManageLabs()` gate.
- **No delete dependency guard:** deleting a Lab that still has active `LabInChargeAssignment`s, Lab Schedule entries, Lab-Curriculum Mappings, or is referenced in a Subject's `eligibleLabs` set is not blocked at the service layer; behavior depends on DB-level FK constraints (which may reject the delete with a raw FK violation rather than a friendly error, or cascade, depending on the constraint — not independently verified in this pass).
- **No name uniqueness:** two Labs can be created with the identical name; nothing in `LabController`/`LabService` exposes a `/name-exists` check, unlike Speciality and other masters.
- **assigneeId not validated against Faculty:** `LabInChargeAssignment.assigneeId` accepts any `Long` — there is no existence check against the `faculty` table, so an assignment can reference a non-existent or since-deleted faculty id.
- **Room capacity not cross-checked:** linking a Lab to a Room does not validate the Lab's own `capacity` field against the Room's physical capacity — the two are independent numbers that can diverge.

## 8. Known Gaps / Deferred
- No real-time uniqueness validation on Lab name (deviates from this project's mandatory master-screen `uniqueFieldValidator` pattern).
- No pre-delete dependency guard on Lab deletion.
- `LabInChargeAssignment.assigneeId` has no referential integrity to the Faculty table.
