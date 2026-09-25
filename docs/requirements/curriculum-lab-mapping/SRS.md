# Software Requirements Specification — Curriculum & Lab-Curriculum Mapping

**Module:** Curriculum & Lab-Curriculum Mapping (OneCMS / College Management System; wider README Module 4)
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Milestone:** R1-M2.6 (Release 1, Milestone 2 — Core Academic & Lab Mapping)

## 1. Introduction

### 1.1 Purpose
Specifies requirements for the curriculum data model and lab-curriculum outcome-mapping feature as actually shipped, reverse-engineered from the `Subject`, `CurriculumVersion`, `CurriculumSemesterCourse`, `Syllabus`, `SyllabusUnit`, `Experiment`, and `LabCurriculumMapping` entities/controllers/services, and migration history. The milestone text (`R1-2.6.1`–`R1-2.6.9`) named only `Syllabus`, `Experiment`, and `LabCurriculumMapping`; this SRS also documents `Subject`, `CurriculumVersion`, and `CurriculumSemesterCourse` because `program-course-management/SRS.md` (§1.2) explicitly defers them here as "the credit-bearing teachable unit" and its curriculum-plan container, and because `Syllabus`/`Experiment` structurally depend on them (FKs to `CurriculumSemesterCourse`/`Subject`).

### 1.2 Scope
Covers:
- **Subject** — the credit-bearing teachable unit (theory + lab credits, term number, timetable-relevant session-block-size fields).
- **CurriculumVersion** — a versioned curriculum plan scoped to one Program + Course, effective from a given Academic Year, clonable to seed a new version.
- **CurriculumSemesterCourse** — the join placing a Subject into a specific term of a CurriculumVersion, carrying theory/lab/clinical hour splits and subject type (CORE/FOUNDATIONAL/ELECTIVE/CO_CURRICULAR).
- **Syllabus** — an immutable, versioned free-text syllabus document (objectives/content/textbooks/course outcomes) attached to one `CurriculumSemesterCourse`.
- **SyllabusUnit** — a later (V319) structured per-unit breakdown of a `CurriculumSemesterCourse`'s planned hours, primarily consumed by the Progress Tracking module (BR-58) rather than this milestone.
- **Experiment** — a lab practical defined under a Subject (procedure, apparatus, learning outcomes).
- **LabCurriculumMapping** — maps an Experiment to a Course Outcome / Program Outcome / Program-Specific Outcome at a correlation level (LOW/MEDIUM/HIGH), i.e. the CO/PO/PSO matrix data.

**Explicitly out of scope** (documented elsewhere):
- **Program, Course** — `program-course-management` module.
- **CurriculumElectiveGroup** and elective-scheduling logic — `inc-curriculum-compliance` module (BR-49), which owns the `curriculum_term_courses` elective columns and the elective-group entity even though `CurriculumSemesterCourse.electiveGroupId` lives in the same table documented here.
- **Lab** (the venue master) — `lab-setup-configuration` module; only referenced here via `Subject.eligibleLabs`.
- **Progress Tracking / portion-completion** consumption of `SyllabusUnit` — Timetable BR-58.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md`, R1-M2.6 (lines 240-256)
- `docs/requirements/program-course-management/SRS.md` §1.2 (scope hand-off)
- `docs/requirements/inc-curriculum-compliance/SRS.md` (elective-group scope hand-off)
- `backend/src/main/java/com/cms/controller/SubjectController.java`, `CurriculumVersionController.java`, `CurriculumSemesterCourseController.java`, `SyllabusController.java`, `SyllabusUnitController.java`, `ExperimentController.java`, `LabCurriculumMappingController.java`
- `backend/src/main/java/com/cms/model/Subject.java`, `CurriculumVersion.java`, `CurriculumSemesterCourse.java`, `Syllabus.java`, `SyllabusUnit.java`, `Experiment.java`, `LabCurriculumMapping.java`
- `backend/src/main/java/com/cms/model/enums/SubjectType.java`, `OutcomeType.java`, `MappingLevel.java`
- Migrations: `V9__create_syllabi_table.sql`, `V10__create_experiments_table.sql`, `V11__create_lab_curriculum_mappings_table.sql`, `V63__create_curriculum_versions_table.sql`, `V276__add_subject_permissions.sql`, `V319__create_syllabus_units_table.sql`, plus later hour/type/elective columns on `curriculum_term_courses` (see `inc-curriculum-compliance`)
- `docs/manual-test-cases/curriculum-management.md`

## 2. Overall Description

### 2.1 Product Perspective
A `CurriculumVersion` is scoped to exactly one `Program` + `Course` pair and one "effective from" `AcademicYear`; it can be cloned into a new version (new name, new effective year) to seed the next revision without hand-rebuilding every semester's subject list. Each `CurriculumSemesterCourse` row places one `Subject` into one term of one `CurriculumVersion` (unique per version+term+subject), carrying its own theory/lab/clinical hour split and subject type — these hours are independent of the `Subject`'s own `theoryCredits`/`labCredits`, allowing the same Subject's actual scheduled hours to vary per curriculum version/term. A `Syllabus` is an append-only versioned document attached to a `CurriculumSemesterCourse` (never edited in place — a content change creates a new version; the only mutable field post-creation is `isActive`). `SyllabusUnit` is a separate, later-added structured plan (one row per teaching unit, with its own planned hours and THEORY/LAB/CLINICAL component type) layered on the same `CurriculumSemesterCourse`. An `Experiment` belongs to a `Subject` (not a `CurriculumSemesterCourse`), so the same Experiment is available regardless of which curriculum version/term currently teaches that Subject. A `LabCurriculumMapping` row then maps one `Experiment` to one Course/Program/Program-Specific Outcome code at a correlation strength, unique per (experiment, outcome type, outcome code).

### 2.2 User Classes
- **Admin / College Admin / Curriculum Coordinator** — full CRUD on Subject, Curriculum Version, Curriculum-Semester-Course placement, Syllabus (create new version / toggle active), Syllabus Unit, Experiment, and CO/PO/PSO mapping (`SUBJECT_MANAGE`, `CURRICULUM_CREATE`/`_EDIT`/`_DELETE`/`_MANAGE`, `SYLLABUS_MANAGE`, `SYLLABUS_UNIT_MANAGE`, `EXPERIMENT_MANAGE`).
- **Any authenticated user** — read Subject/Curriculum Version/Syllabus/Experiment/Mapping lists and details (used by Timetable, Progress Tracking, and reporting screens).

### 2.3 Operating Environment
Angular (`features/curriculum`), Spring Boot REST (`/subjects`, `/curriculum-versions`, `/curriculum-semester-courses`, `/syllabi`, `/syllabus-units`, `/experiments`, `/curriculum-mappings`), PostgreSQL (`subjects`, `curriculum_versions`, `curriculum_term_courses`, `syllabi`, `syllabus_units`, `experiments`, `lab_curriculum_mappings`), Keycloak JWT auth, DB-driven RBAC.

### 2.4 Constraints / Assumptions
- `Subject.code` is DB-unique; `Subject.credits`/`theoryCredits`/`labCredits` must be ≥1 for ordinary subjects, except two allowlisted system-managed subjects (`SYSTEM-LIBRARY`, `SYSTEM-SPORTS`) pinned to exactly 0 credits (a deliberate sentinel for Timetable's advisory Library/Sports filler scheduling, not a general "zero-credit subject" feature).
- `CurriculumSemesterCourse` is unique per (`curriculum_version_id`, `term_number`, `subject_id`) — the same Subject cannot be placed twice in the same term of the same version.
- `Syllabus` is immutable once created; `PUT /syllabi/{id}` only toggles `isActive`, never edits content. There is no delete endpoint — syllabus history is permanent and append-only.
- `LabCurriculumMapping` is unique per (`experiment_id`, `outcome_type`, `outcome_code`) — the same Experiment cannot be mapped twice to the identical outcome code within the same outcome type.
- The milestone's "CO/PO mapping matrix UI component" (R1-2.6.9) shipped as a flat, sortable/paginated Material-table list of individual experiment↔outcome mapping rows (`co-po-mapping.component`), not a visual rows-of-experiments × columns-of-outcomes grid — see Known Gaps.

## 3. Functional Requirements

| ID | Requirement | Priority | Dependencies |
|---|---|---|---|
| FR-CLM-1 | System shall allow creation/update/deactivation of a Subject with name, code, credits (total/theory/lab), speciality, term number, and lab/clinical session-block-size. | Must | Speciality Management |
| FR-CLM-2 | System shall reject a duplicate Subject name or code, validated in real time as the admin types. | Must | FR-1 |
| FR-CLM-3 | System shall allow creation of a Curriculum Version scoped to one Program + Course + effective Academic Year, and cloning an existing version into a new one. | Must | Program & Course Management, Academic Year & Calendar |
| FR-CLM-4 | System shall reject a duplicate Curriculum Version name within the same Program/Course scope, validated in real time. | Must | FR-3 |
| FR-CLM-5 | System shall allow placing a Subject into a specific term of a Curriculum Version, with its own theory/lab/clinical hour split and subject type (CORE/FOUNDATIONAL/ELECTIVE/CO_CURRICULAR), rejecting a duplicate (version, term, subject) placement. | Must | FR-1, FR-3 |
| FR-CLM-6 | System shall allow creating a new, immutable, versioned Syllabus (objectives, content, textbooks, reference books, course outcomes) for a Curriculum-Semester-Course, and toggling an existing version's active flag. | Must | FR-5 |
| FR-CLM-7 | System shall allow defining structured Syllabus Units (unit number, component type, title, planned hours) under a Curriculum-Semester-Course, rejecting a duplicate unit number within the same course. | Should | FR-5 |
| FR-CLM-8 | System shall allow CRUD of Experiments under a Subject (experiment number, name, description, aim, apparatus, procedure, expected outcome, learning outcomes, estimated duration), rejecting a duplicate Experiment name within the same Subject. | Must | FR-1 |
| FR-CLM-9 | System shall allow CRUD of Lab-Curriculum Mappings, each linking one Experiment to one Course/Program/Program-Specific Outcome code at a LOW/MEDIUM/HIGH correlation level with an optional justification, rejecting a duplicate (experiment, outcome type, outcome code) mapping. | Must | FR-8 |

## 4. External Interface Requirements

### 4.1 Screens
- **Curriculum Version List / Form** (`curriculum-version-list`, `curriculum-version-form`) — versioned curriculum-plan CRUD, plus a Clone dialog (`curriculum-version-clone-dialog`).
- **Curriculum Map** (`curriculum-map.component`) — per-version term-by-term Subject placement view (backs `curriculum-semester-courses`).
- **Syllabus List / Form** (`syllabus-list`, `syllabus-form`) — versioned syllabus CRUD; **Syllabus Unit dialog** (`syllabus-unit-dialog`) for the structured per-unit plan.
- **Experiment List / Form** (`experiment-list`, `experiment-form`).
- **CO/PO Mapping** (`co-po-mapping`, `co-po-mapping-form`) — Lab-Curriculum Mapping list/form (see Known Gaps re: "matrix" naming).

### 4.2 API Endpoints (high level)
`GET/POST/PUT/DELETE/PATCH /subjects*`; `GET/POST/PUT/DELETE /curriculum-versions*`, `POST /curriculum-versions/{id}/clone`; `POST/PUT/DELETE /curriculum-semester-courses*`; `GET/POST/PUT /syllabi*` (no delete); `GET/POST/PUT/DELETE /syllabus-units*`; `GET/POST/PUT/DELETE /experiments*`; `GET/POST/PUT/DELETE /curriculum-mappings*`.

### 4.3 Key DB Entities
`subjects` (FK `speciality_id`; M2M `subject_eligible_labs`, `subject_eligible_clinical_venues`), `curriculum_versions` (FK `program_id`, `course_id`, `effective_from_academic_year_id`), `curriculum_term_courses` (FK `curriculum_version_id`, `subject_id`, nullable `elective_group_id`), `syllabi` (FK `curriculum_term_course_id`), `syllabus_units` (FK `curriculum_term_course_id`), `experiments` (FK `subject_id`), `lab_curriculum_mappings` (FK `experiment_id`).

## 5. Non-Functional Requirements
- **Performance:** Server-side pagination/search on Subject and Curriculum Version lists (`/page` endpoints); Experiment, Syllabus, and Lab-Curriculum Mapping lists are unpaginated (`GET` returns a full `List`, filterable by parent id).
- **Security/RBAC:** All mutations DB-permission-gated per entity (`SUBJECT_MANAGE`, `CURRICULUM_CREATE`/`_EDIT`/`_DELETE`, `CURRICULUM_MANAGE`, `SYLLABUS_MANAGE`, `SYLLABUS_UNIT_MANAGE`/`_VIEW`, `EXPERIMENT_MANAGE`); reads are otherwise authentication-only except `SyllabusUnitController`'s `GET`, which requires `SYLLABUS_UNIT_VIEW`.
- **Auditability:** JPA-audited timestamps on all seven entities; Syllabus's append-only versioning is itself a form of audit trail (no in-place content edits ever overwrite history).
- **Data integrity:** DB-level unique constraints back FR-2, FR-4/duplicate-name checks (Subject code, Curriculum-Semester-Course version+term+subject, Lab-Curriculum-Mapping experiment+outcome), each paired with a real-time `/name-exists`-style frontend check.

## 6. Known Gaps / Not Yet Implemented
- The milestone's "CO/PO mapping matrix UI component" (R1-2.6.9) shipped as a flat sortable table of individual mapping rows, not a visual matrix (rows = experiments, columns = outcome codes) — a UI-shape deviation from the milestone text, though functionally complete for CRUD.
- `SyllabusUnitController`'s document-history and required-types-style reads were not found to carry inconsistent permission gates the way Faculty's did, but `SYLLABUS_UNIT_VIEW` and `SYLLABUS_UNIT_MANAGE` were introduced later (V320) than the base `SYLLABUS_VIEW`/`SYLLABUS_MANAGE` (V88) — the two features (free-text Syllabus vs. structured Syllabus Units) are functionally separate despite both hanging off the same `CurriculumSemesterCourse` FK.
- No cross-check was found ensuring a `CurriculumSemesterCourse`'s `theoryHours`/`labHours`/`clinicalHours` sum reconciles with its `Subject`'s own `theoryCredits`/`labCredits` — the two hour systems can diverge without any warning.
- `Experiment` is scoped to `Subject`, not to a specific `CurriculumVersion`/term — an Experiment defined while a Subject was taught in one curriculum version remains visible/reusable even after that Subject is removed from all active curriculum versions, with no orphan-check surfaced in the UI reviewed.
