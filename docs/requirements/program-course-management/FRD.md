# Functional Requirements Document — Program & Course Management

## 1. Overview
Program & Course Management provides CRUD, status lifecycle, uniqueness validation, and admission-document configuration for the `Program` and `Course` masters.

## 2. Actors & Permissions
| Permission | Grants |
|---|---|
| `PROGRAM_MANAGE` | Create/update/delete/status-toggle Program; manage document-type requirements; `/name-exists`, `/code-exists` |
| `PROGRAM_VIEW`, `PROGRAM_CREATE`, `PROGRAM_EDIT`, `PROGRAM_DELETE`, `PROGRAM_EXPORT` | Granular per-operation codes seeded (Permission Model V2, BR-39) — controller endpoints currently gate only on `PROGRAM_MANAGE` for all writes; the granular codes exist in the permission catalog but are not yet individually wired to distinct endpoints. |
| `COURSE_MANAGE` | Create/update/delete/status-toggle Course; `/name-exists`, `/code-exists` |
| `COURSE_VIEW`, `COURSE_CREATE`, `COURSE_EDIT`, `COURSE_DELETE`, `COURSE_EXPORT` | Same pattern as Program — seeded, not yet operation-split at the controller. |
| *(none)* | List/get/page for both Program and Course — authentication only |

## 3. Screens & UI Behavior

### 3.1 Program List
- Table: Name, Code, Duration (years), Assessment Pattern, Status badge, Actions.
- Search + pagination via `/programs/page`; `activeOnly` filter toggle.

### 3.2 Program Form
- Fields: Name, Code, Duration Years, Seat Capacity, Assessment Pattern (Term-based/Yearly), Minimum Age Years, Age Cutoff Day/Month, Uses Clinical Shift Scheduling (checkbox).
- Real-time name/code uniqueness validation (`uniqueFieldValidator` against `/programs/name-exists`, `/programs/code-exists`).
- A separate "Document Requirements" panel/dialog lets the admin toggle each `DocumentType` as Mandatory/Optional/Not Required for that Program.

### 3.3 Course List
- Scoped to a selected Program (or shows all with Program column); Table: Name, Code, Specialization, Roll Number Code, Status, Actions.

### 3.4 Course Form
- Fields: Program (dropdown, required), Name, Code, Specialization, Roll Number Code (exactly 2 characters).
- Real-time name/code uniqueness validation.

## 4. Functional Workflows

### 4.1 Create Program → Add Courses
1. Admin creates Program (`POST /programs`).
2. Admin optionally sets document requirements (`PUT /programs/{id}/document-types`).
3. Admin creates one or more Courses under that Program (`POST /courses`, `program` FK required).
4. Course becomes selectable in Enquiry/Admission/Fee Structure/Curriculum Version screens, filtered by the chosen Program (`GET /courses/program/{programId}`).

### 4.2 Deactivate Program/Course
1. Admin toggles status on a list row.
2. `PATCH /{resource}/{id}/status` with `{ isActive/status }` payload (Program uses `ProgramStatusUpdateRequest` with `ProgramStatus` enum `ACTIVE`/`INACTIVE`; Course uses a boolean-style status update consistent with `Course.isActive`).
3. Deactivated Program/Course drops out of `activeOnly` filtered dropdowns.

## 5. API Endpoints

### Program (`/programs`)
| Method | Path | Permission |
|---|---|---|
| POST | `/programs` | `PROGRAM_MANAGE` |
| GET | `/programs` | authenticated |
| GET | `/programs/{id}` | authenticated |
| PUT | `/programs/{id}` | `PROGRAM_MANAGE` |
| PATCH | `/programs/{id}/status` | `PROGRAM_MANAGE` |
| DELETE | `/programs/{id}` | `PROGRAM_MANAGE` |
| GET | `/programs/{id}/document-types` | authenticated |
| PUT | `/programs/{id}/document-types` | `PROGRAM_MANAGE` |
| GET | `/programs/page?search=&activeOnly=` | authenticated |
| GET | `/programs/name-exists`, `/code-exists` | `PROGRAM_MANAGE` |

### Course (`/courses`)
| Method | Path | Permission |
|---|---|---|
| POST | `/courses` | `COURSE_MANAGE` |
| GET | `/courses` | authenticated |
| GET | `/courses/page?search=&programId=` | authenticated |
| GET | `/courses/{id}` | authenticated |
| GET | `/courses/program/{programId}` | authenticated |
| PUT | `/courses/{id}` | `COURSE_MANAGE` |
| PATCH | `/courses/{id}/status` | `COURSE_MANAGE` |
| DELETE | `/courses/{id}` | `COURSE_MANAGE` |
| GET | `/courses/name-exists`, `/code-exists` | `COURSE_MANAGE` |

## 6. Data Model

**`programs`**: id, name, code (UNIQUE), duration_years, seat_capacity, status (`ProgramStatus` ACTIVE/INACTIVE), assessment_pattern (`AssessmentPattern` TERM_BASED/YEARLY, default TERM_BASED), minimum_age_years (default 17), age_cutoff_day (default 31), age_cutoff_month (default 12), uses_clinical_shift_scheduling (boolean), created_at, updated_at.

**`program_document_types`** (element collection, joined on `program_id`): document_type, category (`MANDATORY`/`OPTIONAL`).

**`courses`**: id, name, code (UNIQUE), specialization (free text, nullable), roll_number_code (CHAR(2) NOT NULL), program_id (FK NOT NULL → programs), is_active, created_at, updated_at.

## 7. Edge Cases & Validation Rules
- `Course.rollNumberCode` must be exactly 2 characters (tightened by `V184__tighten_roll_number_code_to_2_chars.sql`) and `NOT NULL` (`V183`) — a Course cannot be saved without it, since it drives roll-number sequence generation (BR-41).
- A Program's `getTotalTerms()` is computed, not stored — changing `durationYears` or `assessmentPattern` after Curriculum Versions/Term Instances already exist for that Program does not retroactively adjust already-generated terms.
- No explicit uniqueness scoping was found preventing two Courses under different Programs from sharing the same `specialization` text (specialization is not itself unique — only `code` is).

## 8. Known Gaps / Deferred
- Program/Course granular permission codes (`PROGRAM_CREATE`/`_EDIT`/`_DELETE`/`_EXPORT`, `COURSE_CREATE`/`_EDIT`/`_DELETE`/`_EXPORT`) exist in the seeded permission catalog (Permission Model V2, BR-39) but every write endpoint still checks only the single `PROGRAM_MANAGE`/`COURSE_MANAGE` code — the granular split has not been wired into the controllers for this module.
- No export endpoint was found for Program or Course despite `PROGRAM_EXPORT`/`COURSE_EXPORT` permission codes existing in the catalog.
