# Functional Requirements Document — Speciality Management

## 1. Overview
Speciality Management provides full CRUD, uniqueness validation, and activation-lifecycle control for the `Speciality` master, backing every Lab/Faculty/Subject screen's speciality dropdown.

## 2. Actors & Permissions
| Permission | Grants |
|---|---|
| `DEPT_MANAGE` | Create, update, delete, status-toggle a Speciality; also gates `/name-exists` and `/code-exists` checks |
| *(none — authentication only)* | List, get-by-id, paginated search |

Legacy naming note: the permission code retains `DEPT_` (from the module's original "Department" name, pre-V200-rename) rather than `SPECIALITY_`.

## 3. Screens & UI Behavior

### 3.1 Speciality List
- Material table with columns: Name, Code, Description, HOD, Status badge, Actions.
- Server-side search + pagination via `GET /specialities/page?search=&page=&size=&sort=`.
- Status badge reflects `isActive` (Active/Inactive), toggled via a status action calling `PATCH /{id}/status`.

### 3.2 Speciality Form (Create/Edit)
- Fields: Name (required), Code (required), Description (optional, max 1000 chars), HOD Name (optional).
- **Real-time uniqueness validation** on Name and Code fields using the `uniqueFieldValidator` directive against `GET /specialities/name-exists` and `GET /specialities/code-exists`, excluding the current record's ID when editing (mandatory pattern for all master screens per project standards).
- Short form — Save/Cancel actions at the bottom (not a sticky footer, per project convention for short forms).

## 4. Functional Workflows

### 4.1 Create Speciality
1. Admin opens Speciality Form.
2. As Name/Code are typed, async validators call `/name-exists`/`/code-exists`.
3. On submit, `POST /specialities` with `SpecialityRequest` body.
4. On `201 Created`, list refreshes and new row appears.

### 4.2 Deactivate Speciality
1. Admin clicks the status toggle on a list row.
2. Confirmation captures an optional `reason`.
3. `PATCH /specialities/{id}/status` with `{ isActive: false, reason }`.
4. Row's badge updates to Inactive; speciality drops out of `activeOnly=true` dropdowns elsewhere in the app.

## 5. API Endpoints

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/specialities` | `SpecialityRequest` (name, code, description, hodFacultyId, hodName) | `201` `SpecialityResponse` | `DEPT_MANAGE` |
| GET | `/specialities?activeOnly=` | — | `200` `List<SpecialityResponse>` | authenticated |
| GET | `/specialities/{id}` | — | `200` `SpecialityResponse` | authenticated |
| PUT | `/specialities/{id}` | `SpecialityRequest` | `200` `SpecialityResponse` | `DEPT_MANAGE` |
| DELETE | `/specialities/{id}` | — | `204` | `DEPT_MANAGE` |
| PATCH | `/specialities/{id}/status` | `ActiveStatusUpdateRequest` (isActive, reason) | `200` `ActiveStatusUpdateResponse` | `DEPT_MANAGE` |
| GET | `/specialities/page?search=&page=&size=&sort=` | — | `200` `Page<SpecialityResponse>` | authenticated |
| GET | `/specialities/name-exists?value=&excludeId=` | — | `200` boolean | `DEPT_MANAGE` |
| GET | `/specialities/code-exists?value=&excludeId=` | — | `200` boolean | `DEPT_MANAGE` |

## 6. Data Model

**Table:** `specialities`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| name | VARCHAR NOT NULL | uniqueness enforced at service layer |
| code | VARCHAR NOT NULL UNIQUE | DB-level uniqueness |
| description | VARCHAR(1000) | nullable |
| hod_faculty_id | BIGINT | nullable, soft reference to `faculty.id`, no FK constraint |
| hod_name | VARCHAR | nullable, free text |
| is_active | BOOLEAN NOT NULL DEFAULT true | added by V229 |
| created_at / updated_at | TIMESTAMPTZ | JPA-audited |

**Relationships (inbound, referenced by):** `Lab.speciality_id`, `Faculty.speciality_id`, `Subject.speciality_id` (nullable).

## 7. Edge Cases & Validation Rules
- Creating a Speciality with a duplicate `code` fails at the DB constraint level (`nameExists`/`codeExists` checks are meant to prevent this from ever reaching the DB via the UI).
- Deactivating a Speciality does not cascade or block based on active Labs/Faculty/Subjects referencing it — no dependency guard was found in `SpecialityService`, unlike the `ACTIVE_REFERENCE_EXISTS` guard pattern documented for other masters in BR-32.
- Deleting a Speciality that is referenced by Lab/Faculty/Subject rows (all of which declare `nullable = false` FKs to `speciality_id`) will fail at the DB level with a foreign-key violation rather than a friendly application error, since no explicit pre-delete guard was found in `SpecialityService`.

## 8. Known Gaps / Deferred
- No `ACTIVE_REFERENCE_EXISTS`-style guard on deactivation/deletion (see Edge Cases above) — behavior differs from the BR-32-documented masters (Referral Type, Agent, Staff Referrer) that explicitly block deactivation when active usage exists.
- No dedicated view-only permission; any authenticated user (including Student/Parent portal roles, if they hold a valid token) can read the full Speciality list.
