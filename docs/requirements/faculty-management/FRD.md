# Functional Requirements Document — Faculty Management

## 1. Overview
Faculty Management provides CRUD for the `Faculty` master (core identity/employment plus an optional extended HR profile), per-faculty document upload/verification against admin-configured document-type requirement rules, and a "Raise Cap" action for per-faculty workload-cap overrides consumed by Timetable. Faculty Availability, Absence/Substitute, Session Swap, and Workload Rules (the scoped config editor) are separate Timetable-module features documented under the Timetable BRs, not here.

## 2. Actors & Permissions
| Permission | Grants |
|---|---|
| `FACULTY_MANAGE` | Create, update, delete a Faculty; raise/lower a Faculty's daily-cap override |
| `FACULTY_EXPORT` | Export the Faculty list (Excel/PDF) |
| `FACULTY_WORKLOAD_VIEW` | View a Faculty's term workload / schedule workload (Faculty Detail "Courses" tab, and the list's workload-summary batch call) |
| `FACULTY_DOC_CONFIG_MANAGE` | Upload/verify/reject/delete a Faculty document; create/delete a document-type requirement rule |
| `FACULTY_DOC_CONFIG_VIEW` | Seeded (V247) but not observed gating any endpoint directly reviewed in this pass — required-types and document-history reads carry no `@PreAuthorize` in `FacultyDocumentController` |
| *(none — authentication only)* | List/get Faculty, list by speciality/status, `/nrts-exists` |
| `FACULTY_MANAGE` *(also gates)* | `/employee-code-exists`, `/email-exists` — added 2026-09-24; gated rather than left open like `/nrts-exists`, since reaching the Faculty Form already requires `FACULTY_MANAGE` |
| `FACULTY_CREATE` / `FACULTY_EDIT` / `FACULTY_DELETE` | Seeded (V242); as of 2026-09-24 each gates its matching mutation as an `hasAny(granular, FACULTY_MANAGE)` alternative — create/update/delete respectively |

Legacy naming note: `FACULTY_DOC_CONFIG_*` permission codes were introduced by V247 alongside the Faculty Doc Config screen and are reused as the document-workflow gate rather than minting separate `FACULTY_DOCUMENT_*` codes.

## 3. Screens & UI Behavior

### 3.1 Faculty List (`faculty-list.component`)
- Dual view: card grid and Material table (`mat-paginator`, server-side sort via `matSortActive`/`matSortDirection`/`onSortChange`, `cmsResizableColumns`).
- Filters: speciality, status, document-review state; search.
- Card view shows a speciality badge (`cms-badge--soft-accent`), `<cms-status-badge [status]="item.status">` for employment status, and a term-workload badge (`cms-badge--soft-red`/`--soft-green` by `overCapacity`).
- Table view adds an Employee Code, Phone, Email, Designation columns, a Documents column (derived review summary), and the same status/workload badges.

### 3.2 Faculty Form (`faculty-form.component`)
- Required: Employee Code (≤50, **live uniqueness check** against `/faculty/employee-code-exists`, fixed 2026-09-24), First/Last Name (≤100 each), Email (valid email, ≤255, **live uniqueness check** against `/faculty/email-exists`, fixed 2026-09-24), Speciality, Designation, Joining Date.
- Optional core: Phone (≤20), Specialization (≤255), Lab Expertise (≤1000), Status (enum), Faculty Type (TEACHING/NON_TEACHING).
- Optional extended profile: Highest Qualification, NRTS Number (≤50, live uniqueness check against `/faculty/nrts-exists`), PAN, Aadhaar, DOB, Gender, Marital Status, Nationality, Religion, Blood Group, full bank-detail block, postal address, six experience-years fields (teaching/clinical × UG/PG/PhD), commission amount, and the three workload-cap override fields (weekly/daily/continuous, each ≥0).
- All three uniqueness checks use the same bespoke debounced (400ms) `AsyncValidatorFn` pattern in `faculty-form.component.ts` (not the shared `uniqueFieldValidator` helper) — `employeeCodeTaken`/`emailTaken`/`nrtsTaken` error keys, each with a "Checking…" pending hint in the template.

### 3.3 Faculty Detail (`faculty-detail.component`)
- Full profile read view, Documents tab (upload, verify/reject, download, re-upload-resets-status), Courses tab (term workload + schedule workload, gated by `FACULTY_WORKLOAD_VIEW`).
- "Raise Cap" flyout (`raise-cap-flyout.component`) — single-field PATCH for `plannedDailyHoursOverride`, distinct from the full edit form so a coordinator can act quickly without touching the rest of the profile.

### 3.4 Faculty Doc Config (`faculty-doc-config.component`)
- Admin screen listing/creating/deleting `FacultyDocumentTypeRequirement` rules: document type + any of designation/speciality/qualification.

## 4. Functional Workflows

### 4.1 Create Faculty
1. Admin opens Faculty Form, fills required + any extended-profile fields.
2. As Employee Code, Email, or NRTS Number is typed, an async validator calls `/faculty/employee-code-exists`, `/faculty/email-exists`, or `/faculty/nrts-exists` respectively.
3. On submit, `POST /faculty` with `FacultyRequest`; server re-validates employee code/email/NRTS uniqueness.
4. On `201 Created`, list refreshes; new Faculty is immediately available in every downstream Faculty dropdown.

### 4.2 Upload & Review a Faculty Document
1. Admin/reviewer opens Faculty Detail → Documents tab; required types are resolved from `GET /faculty/{facultyId}/documents/required-types` (OR-match against designation/speciality/qualification).
2. `POST /faculty/{facultyId}/documents/upload` (multipart) stores the file in MinIO and creates/updates the `faculty_documents` row.
3. Reviewer verifies or rejects via `PUT /faculty/{facultyId}/documents/{id}` (remarks, `verifiedBy`, `verifiedAt` recorded); action logged to `faculty_document_history`.
4. A re-upload of an already-verified document resets its status, requiring re-review (verification lock prevents silent overwrite of reviewed evidence).
5. The Faculty List's document-review badge reflects total/required/pending/rejected/missing-required/verified-required counts without ever writing to `FacultyStatus`.

### 4.3 Raise Faculty Daily-Hour Cap
1. Coordinator opens Faculty Detail → "Raise Cap."
2. `PATCH /faculty/{id}/daily-cap` with `{ plannedDailyHoursOverride }` — a minimal single-field update, gated by `FACULTY_MANAGE`.
3. Timetable's `checkWithinWorkloadCaps` resolves this override ahead of the designation default and flat institution config the next time it evaluates this faculty's daily cap.

### 4.4 Delete Faculty
1. `DELETE /faculty/{id}` (gated by `FACULTY_MANAGE`) — existence check only; no explicit downstream-reference guard found in `FacultyService.delete()`.

## 5. API Endpoints

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/faculty` | `FacultyRequest` | `201` `FacultyResponse` | `FACULTY_MANAGE` |
| GET | `/faculty?specialityId=&status=` | — | `200` `List<FacultyResponse>` | authenticated |
| GET | `/faculty/{id}` | — | `200` `FacultyResponse` | authenticated |
| GET | `/faculty/{id}/workload?termInstanceId=` | — | `200` `FacultyWorkloadDetail` | `FACULTY_WORKLOAD_VIEW` |
| GET | `/faculty/workload-summary?facultyIds=&termInstanceId=` | — | `200` `List<FacultyWorkloadSummary>` | `FACULTY_WORKLOAD_VIEW` |
| GET | `/faculty/{id}/schedule-workload?termInstanceId=` | — | `200` `FacultyScheduleWorkload` | `FACULTY_WORKLOAD_VIEW` |
| PUT | `/faculty/{id}` | `FacultyRequest` | `200` `FacultyResponse` | `FACULTY_MANAGE` |
| PATCH | `/faculty/{id}/daily-cap` | `{ plannedDailyHoursOverride }` | `200` `FacultyResponse` | `FACULTY_MANAGE` |
| DELETE | `/faculty/{id}` | — | `204` | `FACULTY_MANAGE` |
| GET | `/faculty/page?search=&specialityId=&status=&documentReview=` | — | `200` `Page<FacultyResponse>` | authenticated |
| GET | `/faculty/export?format=&...&sort=&direction=` | — | file bytes | `FACULTY_EXPORT` |
| GET | `/faculty/nrts-exists?value=&excludeId=` | — | `200` boolean | authenticated |
| GET | `/faculty/employee-code-exists?value=&excludeId=` | — | `200` boolean | `FACULTY_MANAGE` (added 2026-09-24) |
| GET | `/faculty/email-exists?value=&excludeId=` | — | `200` boolean | `FACULTY_MANAGE` (added 2026-09-24) |
| GET | `/faculty/{facultyId}/documents/required-types` | — | `200` `List<DocumentType>` | none observed |
| GET | `/faculty/{facultyId}/documents/{id}/history` | — | `200` `FacultyDocumentHistoryResponse` | none observed |
| PUT | `/faculty/{facultyId}/documents/{id}` | review fields | `200` `FacultyDocumentResponse` | `FACULTY_DOC_CONFIG_MANAGE` |
| DELETE | `/faculty/{facultyId}/documents/{id}` | — | `204` | `FACULTY_DOC_CONFIG_MANAGE` |
| POST | `/faculty/{facultyId}/documents/upload` | multipart file + type | `201` `FacultyDocumentResponse` | `FACULTY_DOC_CONFIG_MANAGE` |
| GET | `/faculty/{facultyId}/documents/{id}/download` | — | file bytes | none observed |
| POST | `/faculty-document-type-requirements` | `FacultyDocumentTypeRequirementRequest` | `201` | `FACULTY_DOC_CONFIG_MANAGE` |
| DELETE | `/faculty-document-type-requirements/{id}` | — | `204` | `FACULTY_DOC_CONFIG_MANAGE` |

## 6. Data Model

**Table:** `faculty`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| employee_code | VARCHAR NOT NULL UNIQUE | no `/exists` endpoint |
| first_name / last_name | VARCHAR NOT NULL | |
| email | VARCHAR NOT NULL UNIQUE | no `/exists` endpoint |
| phone | VARCHAR | nullable |
| bio | VARCHAR(500) | nullable, added V137 |
| emergency_contact_name/relationship/phone | VARCHAR | nullable, added V144 |
| speciality_id | BIGINT NOT NULL FK → specialities | |
| designation_id | BIGINT NOT NULL FK → designations | (originally a plain `VARCHAR designation`, replaced V201) |
| specialization | VARCHAR | nullable |
| lab_expertise | VARCHAR(1000) | nullable |
| joining_date | DATE NOT NULL | |
| status | VARCHAR NOT NULL | enum FacultyStatus |
| faculty_type | VARCHAR | nullable, enum TEACHING/NON_TEACHING |
| highest_qualification | VARCHAR(50) | nullable, enum FacultyQualification |
| nrts_number | VARCHAR UNIQUE | nullable-unique, V199 |
| pan_number / aadhaar_number | VARCHAR | nullable |
| date_of_birth | DATE | nullable |
| gender / marital_status | VARCHAR | nullable, enums |
| nationality / religion / blood_group | VARCHAR | nullable |
| bank_account_number / bank_ifsc_code / bank_branch / bank_name / bank_account_holder / bank_account_type | VARCHAR | nullable, V93 |
| (embedded Address: postal_address, street, city, district, state, pincode, country_id) | VARCHAR/FK | nullable, V93 + V159 |
| teaching_exp_ug/pg/phd_years, clinical_exp_ug/pg/phd_years | NUMERIC(5,1) | nullable, V93 |
| commission_amount | NUMERIC(12,2) | nullable, V219 |
| planned_weekly/daily/continuous_hours_override | INTEGER | nullable, V373/V387 |
| version | — | optimistic-locking column, V418 |
| created_at / updated_at | TIMESTAMPTZ | JPA-audited |

**Table:** `faculty_documents` (unique on `faculty_id, document_type`)
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| faculty_id | BIGINT NOT NULL FK → faculty (ON DELETE CASCADE) | |
| document_type | VARCHAR NOT NULL | enum DocumentType |
| status | VARCHAR NOT NULL DEFAULT 'NOT_UPLOADED' | enum DocumentVerificationStatus |
| remarks, verified_by, verified_at | — | nullable review metadata |
| file_name, content_type, file_size, storage_key (V249), uploaded_at | — | MinIO-backed; legacy `bytea` columns dropped V523 |

**Table:** `faculty_document_type_requirements`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| document_type | VARCHAR(80) NOT NULL | enum DocumentType |
| designation_id | BIGINT | nullable FK → designations |
| speciality_id | BIGINT | nullable FK → specialities |
| qualification | VARCHAR(50) | nullable enum FacultyQualification |
| created_at | TIMESTAMPTZ | |

A rule is a match for a Faculty if **any one** of its non-null `designation_id`/`speciality_id`/`qualification` equals that Faculty's corresponding field (BR-42's OR-match rule).

**Relationships (inbound, referenced by):** `Lab.speciality`/`LabInChargeAssignment.assigneeId` (soft, unenforced), `CourseOffering.facultyId`, `LabSchedule.faculty`, `Batch.coordinatorFacultyId`, `Enquiry.referredFacultyId` (Referral & Commission Management), `FacultyAvailability`/`FacultyAbsence`/`FacultySessionSwap` (Timetable module, documented separately).

## 7. Edge Cases & Validation Rules
- Creating/updating a Faculty with a duplicate employee code or email fails with a service-layer `IllegalArgumentException` (caught after full form entry) rather than being caught live as the admin types, unlike NRTS Number.
- `nrtsNumber` uniqueness is only enforced when the value is non-null; multiple Faculty with a null NRTS number are all valid simultaneously.
- Deleting a Faculty performs only an existence check; no verified guard against active Course Offering/Lab/Class Schedule references was found in `FacultyService`.
- `LabInChargeAssignment.assigneeId` (Lab Setup module) has no FK to `faculty.id` — a Faculty deletion will not cascade or block on it, silently orphaning the assignment's identity.
- The Faculty Doc Config screen lets an Admin define a requirement rule referencing a Designation or Speciality that could later itself be deleted/deactivated; no cross-check was found preventing an orphaned rule.
- `FacultyRequest.credits`-style min/max validation does not apply here (that pattern belongs to `SubjectRequest`); Faculty's own numeric fields (experience years, workload overrides) are validated only for non-negativity, not for internally consistent totals (e.g. nothing caps the sum of the six experience-year fields).

## 8. Known Gaps / Deferred
- ~~`FACULTY_CREATE`/`FACULTY_EDIT`/`FACULTY_DELETE` permissions are seeded... but never enforced~~ **Fixed 2026-09-24** — see SRS §6.
- No pre-delete dependency guard on Faculty deletion.
- `FACULTY_DOC_CONFIG_VIEW` is seeded but was not observed gating any read endpoint in `FacultyDocumentController` (required-types, history, download all carry no `@PreAuthorize`) — effectively those reads are open to any authenticated caller regardless of holding this permission.
