# Software Requirements Specification — Program & Course Management

**Module:** Program & Course Management (OneCMS / College Management System)
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Milestone:** R1-M2.2 (Release 1, Milestone 2 — Core Academic & Lab Mapping)

## 1. Introduction

### 1.1 Purpose
Specifies requirements for the Program and Course masters as actually shipped, reverse-engineered from `ProgramController`/`ProgramService`, `CourseController`/`CourseService`, their models/DTOs, and migration history.

### 1.2 Scope
Covers the `Program` master (e.g. "B.Sc Nursing", 4-year duration) and the `Course` master, which today represents a **specialization/stream track offered under a Program** (e.g. a particular admission stream with its own roll-number code) — **not** a credit-bearing teachable subject. Does not cover Curriculum Versions, Curriculum-Semester-Course mappings, or Subjects (the credit-bearing teachable unit) — these are documented under the Curriculum & Lab-Curriculum Mapping module.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md`, R1-M2.2 (lines 171-186)
- `backend/src/main/java/com/cms/controller/ProgramController.java`, `CourseController.java`
- `backend/src/main/java/com/cms/model/Program.java`, `Course.java`
- Migrations: `V2__create_programs_table.sql`, `V5__create_courses_table.sql`, `V37__restructure_academic_hierarchy.sql`, `V44__remove_degree_type_add_program_duration_and_course_specialization.sql`, `V47__remove_program_level_reseed_programs.sql`, `V48__drop_program_departments_table.sql`, `V78__add_status_to_programs.sql`, `V91__document_types_and_program_mapping.sql`, `V114__add_assessment_pattern_to_programs.sql`, `V175__add_age_restriction_to_programs.sql`, `V228__add_is_active_to_courses.sql`, `V416__add_uses_clinical_shift_scheduling_to_programs.sql`
- `docs/manual-test-cases/program-management.md`, `program-course-management.md`, `document-types-program-mapping.md`

### 1.4 Important Terminology Note
The original milestone (R1-2.2.2) specified a `Course` entity with `credits`, `theoryCredits`, `labCredits`, `program`, and `semester` fields — i.e. a teachable subject. During a later restructuring (`V37`, `V44`, `V50`, `V277`/`V278`) the academic hierarchy was split in two:
- **`Course`** (this module, current shape) became a **Program specialization/stream** — `name`, `code`, `specialization`, `rollNumberCode`, `program` (FK), `isActive` — used for roll-number generation and admission-stream differentiation, no credit fields.
- **`Subject`** (table `subjects`, a separate entity not created by this milestone) inherited the original credit-bearing shape — `credits`, `theoryCredits`, `labCredits`, `speciality`, `semester` (`termNumber`) — and is what `Syllabus`/`Experiment`/curriculum mapping now attach to (see Curriculum & Lab-Curriculum Mapping module).

This SRS documents `Program`/`Course` as they exist **today**; the credit-bearing "Course" concept from the original milestone text is covered under the Curriculum module as `Subject`.

## 2. Overall Description

### 2.1 Product Perspective
`Program` is the top-level academic offering (e.g. B.Sc Nursing, GNM, ANM), each with a fixed `durationYears` and an `AssessmentPattern` (`TERM_BASED` or `YEARLY`) that determines `getTotalTerms()` (durationYears × 2, or × 1 for yearly). `Course` sits one level below Program as a named specialization/stream with its own 2-character `rollNumberCode` used by the roll-number generation sequence.

### 2.2 User Classes
- **Admin / College Admin** — manage Program/Course masters (`PROGRAM_MANAGE`, `COURSE_MANAGE`).
- **Any authenticated user** — read Program/Course lists for use in Enquiry, Admission, Fee Structure, and Curriculum screens.

### 2.3 Operating Environment
Angular (`features/program`, `features/course`), Spring Boot REST (`/programs`, `/courses`), PostgreSQL (`programs`, `courses`, `program_document_types`), Keycloak JWT auth, DB-driven RBAC.

### 2.4 Constraints / Assumptions
- `Program.code` and `Course.code` are both globally unique.
- `Course.program` is a mandatory FK — a Course cannot exist without a parent Program.
- `Program` carries admission-age-eligibility fields (`minimumAgeYears`, `ageCutoffDay`, `ageCutoffMonth`) and a `usesClinicalShiftScheduling` flag consumed by the (separately documented) Timetable module — both are extensions layered onto Program well after the original R1-M2.2 milestone shipped.
- `Program.documentRequirements` (mandatory/optional document types per program, `ProgramDocumentRequirement`) is an extension (V91) not in the original milestone scope, feeding the Admission module's required-document checklist.

## 3. Functional Requirements

| ID | Requirement | Priority | Dependencies |
|---|---|---|---|
| FR-PROGCOURSE-1 | System shall allow creation of a Program with name, code, duration (years), seat capacity, assessment pattern, status, and age-eligibility rules. | Must | — |
| FR-PROGCOURSE-2 | System shall allow listing all Programs and fetching one by ID. | Must | FR-1 |
| FR-PROGCOURSE-3 | System shall allow updating a Program's fields, and toggling its status (`ACTIVE`/`INACTIVE`) via a dedicated status endpoint. | Must | FR-1 |
| FR-PROGCOURSE-4 | System shall allow deleting a Program. | Should | FR-1 |
| FR-PROGCOURSE-5 | System shall allow configuring a Program's mandatory/optional admission document-type requirements. | Should | FR-1 |
| FR-PROGCOURSE-6 | System shall provide paginated/searchable Program listing (`/programs/page`, with `activeOnly` filter) and real-time name/code-uniqueness checks. | Must | FR-1 |
| FR-PROGCOURSE-7 | System shall allow creation of a Course scoped to a parent Program, with name, code, specialization, and a 2-character roll-number code. | Must | FR-1 |
| FR-PROGCOURSE-8 | System shall allow listing all Courses, fetching by ID, and listing Courses filtered by Program. | Must | FR-7 |
| FR-PROGCOURSE-9 | System shall allow updating a Course, toggling its active status, and deleting it. | Must | FR-7 |
| FR-PROGCOURSE-10 | System shall provide paginated/searchable Course listing (filterable by Program) and real-time name/code-uniqueness checks. | Must | FR-7 |

## 4. External Interface Requirements

### 4.1 Screens
- **Program List / Form** (`features/program`) — CRUD with status toggle, active-only filter.
- **Course List / Form** (`features/course`) — CRUD scoped to a selected Program, status toggle.

### 4.2 API Endpoints (high level)
`/programs` (CRUD + `/page`, `/{id}/document-types`, `/{id}/status`, `/name-exists`, `/code-exists`); `/courses` (CRUD + `/page`, `/program/{programId}`, `/{id}/status`, `/name-exists`, `/code-exists`).

### 4.3 Key DB Entities
`programs`, `program_document_types` (element collection), `courses` (FK `program_id`).

## 5. Non-Functional Requirements
- **Performance:** Both masters support server-side pagination; Course listing supports Program-scoped filtering to avoid loading the full catalog on Program-scoped screens.
- **Security/RBAC:** Writes gated by `PROGRAM_MANAGE`/`COURSE_MANAGE`; reads are authentication-only.
- **Auditability:** JPA-audited `created_at`/`updated_at` on both entities; no separate change-history table.

## 6. Known Gaps / Not Yet Implemented
- The "Course" concept described in the original R1-M2.2 milestone (credits/theoryCredits/labCredits/semester) does not exist under this entity today — it lives on the `Subject` entity (see Curriculum & Lab-Curriculum Mapping module's SRS). Anyone reading only the milestone tracker without checking shipped code would mis-expect credit fields on `Course`.
- `Course.specialization` is a free-text field, not a managed lookup.
- No dependency guard was found preventing deletion of a Program/Course that has downstream Curriculum Versions, Subjects, or Students already referencing it — this should be verified against production risk before relying on hard delete.
