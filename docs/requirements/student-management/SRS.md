# Software Requirements Specification — Student Management

**Module:** Student Management (core student master record)
**App:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Source milestone:** R1-M3.1 (`docs/RELEASE_1_MILESTONES.md`, historical scope only — current code is source of truth)

---

## 1. Introduction

### 1.1 Purpose
Defines the requirements for the core Student Management module: the `Student` master record, the Student Explorer/list screen, CRUD operations, roll-number management, and program transfer. This document is reverse-engineered from the shipped backend (`backend/src/main/java/com/cms/{model,controller,service,repository,dto}/Student*.java`) and frontend (`frontend/src/app/features/student/*`).

### 1.2 Scope
In scope: the `Student` entity itself, its CRUD/search/explorer screens, roll-number generation/assignment, and program transfer. Out of scope (documented separately by this effort): Enquiry-to-Admission workflow, Student Type/fee dimensions, Student Promotion, Student Scholarship, Student Fee, Student Marks — these are built on top of or alongside `Student` but are separate modules per the task's module boundary.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md` lines 261–316 (R1-M3.1, historical)
- `docs/BUSINESS_REQUIREMENTS.md` — BR-11 (Student Explorer), BR-21 (First-Graduate tracking), BR-27 (Admission Number), BR-41 (Roll Number / Number Sequences)
- `backend/src/main/java/com/cms/model/Student.java`, `StudentController.java`, `StudentService.java`, `StudentRepository.java`
- `frontend/src/app/features/student/*`

## 2. Overall Description

### 2.1 Product Perspective
`Student` is a core entity referenced across nearly every other module (Fee, Attendance, Timetable/Course Registration, Examination, Library, Scholarship, Promotion). It is created either through the Enquiry→Admission conversion workflow (a separate module) or directly via the Student CRUD API. Once created, this module owns the student's master demographic/academic/bank record and its lifecycle status.

### 2.2 Actors / User Classes
- **Admin / College Admin** — full CRUD, roll-number generation/assignment, program transfer, export.
- **Faculty** — view students (roster context), no create/delete.
- **Accounting / Front-office roles** — view for fee-collection context.
- **Student (self-service)** — no direct access to this module's CRUD; self-service is handled by separate profile/self-service screens (not part of this module).

### 2.3 Operating Environment
Angular SPA (`frontend/src/app/features/student/`) calling a Spring Boot REST API (`/students`) backed by PostgreSQL (`students` table, Flyway-managed). Auth via Keycloak JWT; authorization via the DB-driven permission system (`@perm.has(...)`).

### 2.4 Constraints / Assumptions
- `rollNumber`, `admissionNumber`, `universityRegistrationNumber`, `umisNumber`, and `email` are each unique at the DB level (`UNIQUE` column constraints).
- `admissionNumber` is generated only by the Admission workflow (BR-27) and is treated as immutable here — not editable through this module's update endpoint in practice (no UI field for it).
- `rollNumber` is optional at creation (nullable since V53) and is usually assigned later via the Roll Number Assignment screen (BR-41).
- Aadhar number is stored as plain `String` on `Student` — CLAUDE.md/milestone doc references "encrypted at rest" as an original design intent; the current entity shows no column-level encryption annotation, so this is flagged as a gap (see §6).

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-STUDENT-1 | Create a student record with core, personal, demographic, family, address, and bank-detail fields | Must | Program, Course, Speciality masters |
| FR-STUDENT-2 | Update an existing student record | Must | FR-STUDENT-1 |
| FR-STUDENT-3 | List/filter students by program, status, lab batch, academic year, fee status, active-only | Must | — |
| FR-STUDENT-4 | Student Explorer — paginated, sortable, multi-filter (program, course, academic year, status, student type, free-text search) list | Must | BR-11 |
| FR-STUDENT-5 | Export the Explorer result set to Excel/PDF with the same filters and sort applied | Should | FR-STUDENT-4 |
| FR-STUDENT-6 | Fetch a single student by ID or by roll number | Must | — |
| FR-STUDENT-7 | List students without a roll number, optionally filtered by course/program | Must | FR-STUDENT-9 |
| FR-STUDENT-8 | Manually assign/correct a single student's roll number | Must | — |
| FR-STUDENT-9 | Bulk-generate and assign roll numbers for every student in a course/year missing one, with a preview step before commit | Must | BR-41, `RollNumberGeneratorService` |
| FR-STUDENT-10 | Delete a student record | Should | — |
| FR-STUDENT-11 | Analyze and execute a program transfer for a student (impact analysis before commit; history retained) | Should | — |
| FR-STUDENT-12 | View a student's program-transfer history | Should | FR-STUDENT-11 |

## 4. External Interface Requirements

### 4.1 Screens
- **Student List / Explorer** (`student-list.component`) — Material table, server-paginated/sorted, column picker, resizable columns, export button.
- **Student Form** (`student-form`) — create/edit.
- **Student Detail** (`student-detail`) — read view.
- **Roll Number Assignment** (`roll-number-assignment`) — preview/bulk-assign screen.
- **Program Transfer Dialog** (`program-transfer-dialog`) — analysis + execution.
- **Retro Admit** (`retro-admit`) — legacy/direct-admit path (per BR-43; not detailed here as it belongs to the Enquiry/Admission module).

### 4.2 API Endpoints (base `/students`)
| Method | Path | Purpose | Permission |
|---|---|---|---|
| POST | `/students` | Create | `STUDENT_CREATE` |
| GET | `/students` | List with ad-hoc filters | none explicit (authenticated) |
| GET | `/students/explorer` | Paginated/sorted explorer | none explicit |
| GET | `/students/export` | Export Excel/PDF | `STUDENT_EXPORT` |
| GET | `/students/without-roll-number` | Students missing roll number | none explicit |
| GET | `/students/{id}` | Get by ID | none explicit |
| GET | `/students/roll-number/{rollNumber}` | Get by roll number | none explicit |
| PUT | `/students/{id}` | Update | `STUDENT_VIEW` (see §6 — likely intended `STUDENT_EDIT`) |
| PATCH | `/students/{id}/roll-number` | Assign one roll number | `STUDENT_CREATE` |
| POST | `/students/bulk-assign-roll-numbers` | Bulk assign | `STUDENT_CREATE` |
| POST | `/students/generate-roll-numbers` | Generate + assign | `STUDENT_CREATE` |
| POST | `/students/preview-roll-numbers` | Preview only | `STUDENT_CREATE` |
| DELETE | `/students/{id}` | Delete | `STUDENT_CREATE` (see §6) |
| GET | `/students/{id}/program-transfer-analysis` | Impact analysis | `STUDENT_EDIT` |
| POST | `/students/{id}/program-transfer` | Execute transfer | `STUDENT_EDIT` |
| GET | `/students/{id}/program-transfers` | Transfer history | `STUDENT_VIEW` or `STUDENT_EDIT` |

### 4.3 Key DB Entities
- `students` (table, V12, extended by many later migrations — see FRD Data Model)
- `roll_number_sequences`, `number_series_definitions` / `number_sequence_counters` (roll/admission number generation, BR-41)
- `student_program_transfers` (V128)

## 5. Non-Functional Requirements

- **Performance:** Explorer endpoint is paginated server-side (default page size 25) to avoid loading the full student set; export runs synchronously against the same filtered/sorted query.
- **Security / RBAC:** All create/mutate/export endpoints are gated by DB-driven permissions (`STUDENT_CREATE`, `STUDENT_EDIT`, `STUDENT_EXPORT`); several read endpoints (`GET /students`, `GET /students/{id}`, update) have no explicit `@PreAuthorize` beyond authentication — see Known Gaps.
- **Auditability:** `createdAt`/`updatedAt` are tracked via JPA auditing (`@CreatedDate`/`@LastModifiedDate`); no dedicated audit-log entries for student field changes (unlike the Timetable module's `AuditLogService`).

## 6. Known Gaps / Not Yet Implemented

- **`PUT /students/{id}` is gated by `STUDENT_VIEW`, not `STUDENT_EDIT`.** A view-only permission holder can currently update any student record. `STUDENT_EDIT` exists as a permission (used elsewhere, e.g. program-transfer endpoints) but is not applied to the core update endpoint — likely an oversight, flagged rather than silently assumed correct.
- **`GET /students`, `GET /students/{id}`, `GET /students/roll-number/{rollNumber}`, `GET /students/without-roll-number` have no `@PreAuthorize` annotation** — accessible to any authenticated user regardless of permission grants.
- **`DELETE /students/{id}` is gated by `STUDENT_CREATE`**, not a dedicated `STUDENT_DELETE` (a `STUDENT_DELETE` permission is seeded in V88 but not referenced by this controller) — a mismatch between the seeded permission catalog and actual enforcement.
- **No `uniqueFieldValidator`/`/name-exists`-style async uniqueness check on the Student Form** for email/roll number/admission number — uniqueness is only enforced at DB-constraint/service-validation time, not live-typing feedback. (Student is a transactional record rather than a "master" in the strict sense CLAUDE.md's mandatory-pattern targets, so this may be an intentional scope boundary rather than a defect — not fixed here since this is a documentation-only task.)
- **Aadhar number encryption-at-rest**, described as a design intent in the original milestone doc, is not visible in the current `Student` entity (plain `String` column, no encryption converter) — could not confirm at the JPA layer; may be handled at a different layer (DB TDE, etc.) not visible from the codebase.
