# Functional Requirements Document — Curriculum & Lab-Curriculum Mapping

## 1. Overview
This module covers seven related entities: **Subject** (the teachable unit), **CurriculumVersion** and **CurriculumSemesterCourse** (the versioned curriculum plan and its per-term subject placements), **Syllabus** and **SyllabusUnit** (free-text and structured per-unit syllabus content), **Experiment** (lab practicals under a Subject), and **LabCurriculumMapping** (the Experiment → Course/Program/Program-Specific Outcome correlation matrix). `CurriculumElectiveGroup` (elective scheduling) is a related but out-of-scope entity documented under `inc-curriculum-compliance`.

## 2. Actors & Permissions
| Permission | Grants |
|---|---|
| `SUBJECT_MANAGE` | Create/update/delete a Subject, toggle its active status, add an eligible venue, `/name-exists` & `/code-exists` checks |
| `CURRICULUM_CREATE` | Create/clone a Curriculum Version — `hasAny(CURRICULUM_CREATE, CURRICULUM_MANAGE)` as of 2026-09-24 |
| `CURRICULUM_EDIT` | Update a Curriculum Version — `hasAny(CURRICULUM_EDIT, CURRICULUM_MANAGE)` as of 2026-09-24 |
| `CURRICULUM_DELETE` | Delete a Curriculum Version — `hasAny(CURRICULUM_DELETE, CURRICULUM_MANAGE)` as of 2026-09-24 |
| `CURRICULUM_VIEW` | Reads on `CurriculumVersionController` (list/page/by-id/name-exists) — added 2026-09-24, alongside the create/edit/delete/manage permissions (any of the four now suffices) |
| `CURRICULUM_MANAGE` | Add/update/remove a Curriculum-Semester-Course placement; create/update/delete a Lab-Curriculum Mapping; also now a full fallback for every Curriculum Version operation above |
| `SYLLABUS_MANAGE` | Create a new Syllabus version, toggle a version's active flag |
| `SYLLABUS_UNIT_VIEW` | List Syllabus Units for a course |
| `SYLLABUS_UNIT_MANAGE` | Create/update/delete a Syllabus Unit, `/unit-number-exists` check |
| `EXPERIMENT_MANAGE` | Create/update/delete an Experiment |
| *(none — authentication only)* | Read Subject list/page/by-course/by-speciality, `/curriculum-semester-courses` reads, `/syllabi` reads, `/experiments` reads + `/name-exists`, `/curriculum-mappings` reads |

Naming note, corrected 2026-09-24: `CurriculumVersionController` correctly uses the finer-grained `CURRICULUM_CREATE`/`_EDIT`/`_DELETE` triad (V242 granular permissions) because Curriculum Version is the one entity in this family V242 minted a dedicated permission for; `CurriculumSemesterCourseController` and `LabCurriculumMappingController` correctly fall back to the coarser `CURRICULUM_MANAGE` (V88) because V242 never created a dedicated granular permission for those two entities specifically — each controller was already using the most specific permission available to it. The originally-flagged "inconsistency" was this difference in available granularity, not a bug in either controller. What *was* a real gap (now fixed): `CurriculumVersionController` didn't accept `CURRICULUM_MANAGE` as a fallback for its own operations, so a role holding only the coarser permission couldn't touch Curriculum Versions at all even though it could manage everything else in the family — see Known Gaps history below. `CurriculumVersionController`'s reads (list/page/by-id/name-exists) were also unguarded until 2026-09-24 — see Known Gaps.

## 3. Screens & UI Behavior

### 3.1 Curriculum Version List / Form (`curriculum-version-list`, `curriculum-version-form`)
- List: search + filter by Program/active status, server-side pagination.
- Form: Program (required), Course (required), Version Name (required, ≤100, **real-time uniqueness check** scoped to Program+Course via `/curriculum-versions/name-exists`), Effective-From Academic Year (required), Active flag.
- Clone dialog (`curriculum-version-clone-dialog`): new version name + new effective academic year, calls `POST /curriculum-versions/{id}/clone`.

### 3.2 Curriculum Map (`curriculum-map.component`)
- Term-by-term view of a Curriculum Version's Subject placements (`GET /curriculum-semester-courses?curriculumVersionId=`), add/edit/remove a placement inline (Subject, term, hour split, subject type, elective flag/group).

### 3.3 Syllabus List / Form (`syllabus-list`, `syllabus-form`)
- List: filterable by Subject/active-only.
- Form (create only — content is immutable post-creation): Curriculum-Semester-Course, Version (int), Objectives/Content/Text Books/Reference Books/Course Outcomes (long free text).
- Activate/deactivate a version via `PUT /syllabi/{id}` (activation-only body, not content).
- **Syllabus Unit dialog** (`syllabus-unit-dialog`): Unit Number (required, real-time uniqueness check scoped to the parent course via `/syllabus-units/unit-number-exists`), Component Type (THEORY/LAB/CLINICAL), Title (≤200), Planned Hours, Description, Sort Order.

### 3.4 Experiment List / Form (`experiment-list`, `experiment-form`)
- List: filterable by Subject/active-only.
- Form: Subject (required), Experiment Number (required), Name (required, **real-time uniqueness check** scoped to Subject via `/experiments/name-exists`), Description, Aim, Apparatus, Procedure, Expected Outcome, Learning Outcomes, Estimated Duration (minutes).

### 3.5 CO/PO Mapping (`co-po-mapping`, `co-po-mapping-form`)
- Material table of individual `LabCurriculumMapping` rows: Experiment Name, Experiment Number, Subject Name, Outcome Type, Outcome Code, Outcome Description, Mapping Level, actions. Sortable/resizable columns, paginated.
- Form: Experiment (required), Outcome Type (COURSE_OUTCOME/PROGRAM_OUTCOME/PROGRAM_SPECIFIC_OUTCOME, required), Outcome Code (required), Outcome Description, Mapping Level (LOW/MEDIUM/HIGH, required), Justification.
- Shipped as a flat list, not a visual experiment×outcome grid — see FRD §8.

## 4. Functional Workflows

### 4.1 Build a Curriculum Version
1. Admin creates a Curriculum Version (`POST /curriculum-versions`, `CURRICULUM_CREATE`) for a chosen Program+Course+effective Academic Year — real-time name-uniqueness check as they type.
2. Admin opens Curriculum Map, adds Subjects term by term (`POST /curriculum-semester-courses`, `CURRICULUM_MANAGE`) — each placement rejects a duplicate (version, term, subject) combination.
3. To start the next revision, Admin clones the current version (`POST /curriculum-versions/{id}/clone`, `CURRICULUM_CREATE`) rather than re-entering every placement by hand; the clone still requires manual review/adjustment since hour splits are copied as-is.

### 4.2 Author a Syllabus
1. Admin selects a Curriculum-Semester-Course placement and creates a new Syllabus version (`POST /syllabi`, `SYLLABUS_MANAGE`).
2. Content is entered once; there is no edit-in-place — any future content change is a brand-new version (`version` integer incremented by the admin/service).
3. Admin toggles which version is `isActive` via `PUT /syllabi/{id}` — the only mutable field post-creation.
4. Optionally, Admin adds structured Syllabus Units under the same course for Progress Tracking's later use, each with a real-time unit-number uniqueness check.

### 4.3 Define Experiments and Map to Outcomes
1. Admin creates Experiments under a Subject (`POST /experiments`, `EXPERIMENT_MANAGE`) — real-time name uniqueness scoped to that Subject.
2. Admin opens CO/PO Mapping, creates a `LabCurriculumMapping` row per Experiment↔Outcome pairing (`POST /curriculum-mappings`, `CURRICULUM_MANAGE`) — a duplicate (experiment, outcome type, outcome code) is rejected by the DB unique constraint.
3. The resulting mapping rows are the CO/PO/PSO matrix data consumed by accreditation-facing reporting elsewhere in the system (not itself part of this module).

## 5. API Endpoints

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| POST | `/subjects` | `SubjectRequest` | `201` `SubjectResponse` | `SUBJECT_MANAGE` |
| GET | `/subjects?activeOnly=` | — | `200` `List<SubjectResponse>` | authenticated |
| GET | `/subjects/page?search=&courseId=` | — | `200` `Page<SubjectResponse>` | authenticated |
| GET | `/subjects/{id}` | — | `200` `SubjectResponse` | authenticated |
| GET | `/subjects/course/{courseId}` | — | `200` `List<SubjectResponse>` | authenticated |
| GET | `/subjects/speciality/{specialityId}` | — | `200` `List<SubjectResponse>` | authenticated |
| PUT | `/subjects/{id}` | `SubjectRequest` | `200` `SubjectResponse` | `SUBJECT_MANAGE` |
| PATCH | `/subjects/eligible-venues` | `AddEligibleVenueRequest` | `204` | `SUBJECT_MANAGE` |
| PATCH | `/subjects/{id}/status` | `ActiveStatusUpdateRequest` | `200` `ActiveStatusUpdateResponse` | `SUBJECT_MANAGE` |
| DELETE | `/subjects/{id}` | — | `204` | `SUBJECT_MANAGE` |
| GET | `/subjects/name-exists?value=&excludeId=` | — | `200` boolean | `SUBJECT_MANAGE` |
| GET | `/subjects/code-exists?value=&excludeId=` | — | `200` boolean | `SUBJECT_MANAGE` |
| POST | `/curriculum-versions` | `CurriculumVersionRequest` | `201` `CurriculumVersionDto` | `CURRICULUM_CREATE` or `CURRICULUM_MANAGE` (fallback added 2026-09-24) |
| GET | `/curriculum-versions?programId=` | — | `200` `List<CurriculumVersionDto>` | `CURRICULUM_VIEW`/`_CREATE`/`_EDIT`/`_MANAGE` (fixed 2026-09-24; previously unguarded) |
| GET | `/curriculum-versions/page?search=&programId=&isActive=` | — | `200` `Page<CurriculumVersionDto>` | `CURRICULUM_VIEW`/`_CREATE`/`_EDIT`/`_MANAGE` (fixed 2026-09-24; previously unguarded) |
| GET | `/curriculum-versions/name-exists?programId=&courseId=&value=&excludeId=` | — | `200` boolean | `CURRICULUM_VIEW`/`_CREATE`/`_EDIT`/`_MANAGE` (fixed 2026-09-24; previously unguarded) |
| GET | `/curriculum-versions/{id}` | — | `200` `CurriculumVersionDto` | `CURRICULUM_VIEW`/`_CREATE`/`_EDIT`/`_MANAGE` (fixed 2026-09-24; previously unguarded) |
| PUT | `/curriculum-versions/{id}` | `CurriculumVersionRequest` | `200` `CurriculumVersionDto` | `CURRICULUM_EDIT` or `CURRICULUM_MANAGE` (fallback added 2026-09-24) |
| DELETE | `/curriculum-versions/{id}` | — | `204` | `CURRICULUM_DELETE` or `CURRICULUM_MANAGE` (fallback added 2026-09-24) |
| POST | `/curriculum-versions/{id}/clone?newVersionName=&newEffectiveAcademicYearId=` | — | `201` `CurriculumVersionDto` | `CURRICULUM_CREATE` or `CURRICULUM_MANAGE` (fallback added 2026-09-24) |
| POST | `/curriculum-semester-courses` | `CurriculumSemesterCourseRequest` | `201` `CurriculumSemesterCourseDto` | `CURRICULUM_MANAGE` |
| PUT | `/curriculum-semester-courses/{id}` | `CurriculumSemesterCourseRequest` | `200` `CurriculumSemesterCourseDto` | `CURRICULUM_MANAGE` |
| DELETE | `/curriculum-semester-courses/{id}` | — | `204` | `CURRICULUM_MANAGE` |
| GET | `/curriculum-semester-courses?curriculumVersionId=&termNumber=` | — | `200` list (term) or `CurriculumFullViewDto` (whole version) | authenticated |
| POST | `/syllabi` | `SyllabusRequest` | `201` `SyllabusResponse` | `SYLLABUS_MANAGE` |
| GET | `/syllabi?subjectId=&activeOnly=` | — | `200` `List<SyllabusResponse>` | authenticated |
| GET | `/syllabi/{id}` | — | `200` `SyllabusResponse` | authenticated |
| PUT | `/syllabi/{id}` | `SyllabusActivationRequest` | `200` `SyllabusResponse` | `SYLLABUS_MANAGE` — activation-only, no content edit, no delete |
| POST | `/syllabus-units` | `SyllabusUnitRequest` | `201` `SyllabusUnitDto` | `SYLLABUS_UNIT_MANAGE` |
| GET | `/syllabus-units?curriculumTermCourseId=` | — | `200` `List<SyllabusUnitDto>` | `SYLLABUS_UNIT_VIEW` |
| PUT | `/syllabus-units/{id}` | `SyllabusUnitRequest` | `200` `SyllabusUnitDto` | `SYLLABUS_UNIT_MANAGE` |
| DELETE | `/syllabus-units/{id}` | — | `204` | `SYLLABUS_UNIT_MANAGE` |
| GET | `/syllabus-units/unit-number-exists?value=&curriculumTermCourseId=&excludeId=` | — | `200` boolean | `SYLLABUS_UNIT_MANAGE` |
| POST | `/experiments` | `ExperimentRequest` | `201` `ExperimentResponse` | `EXPERIMENT_MANAGE` |
| GET | `/experiments?subjectId=&activeOnly=` | — | `200` `List<ExperimentResponse>` | authenticated |
| GET | `/experiments/{id}` | — | `200` `ExperimentResponse` | authenticated |
| GET | `/experiments/name-exists?subjectId=&value=&excludeId=` | — | `200` boolean | authenticated |
| PUT | `/experiments/{id}` | `ExperimentRequest` | `200` `ExperimentResponse` | `EXPERIMENT_MANAGE` |
| DELETE | `/experiments/{id}` | — | `204` | `EXPERIMENT_MANAGE` |
| POST | `/curriculum-mappings` | `LabCurriculumMappingRequest` | `201` `LabCurriculumMappingResponse` | `CURRICULUM_MANAGE` |
| GET | `/curriculum-mappings?experimentId=&outcomeType=&outcomeCode=` | — | `200` `List<LabCurriculumMappingResponse>` | authenticated |
| GET | `/curriculum-mappings/{id}` | — | `200` `LabCurriculumMappingResponse` | authenticated |
| PUT | `/curriculum-mappings/{id}` | `LabCurriculumMappingRequest` | `200` `LabCurriculumMappingResponse` | `CURRICULUM_MANAGE` |
| DELETE | `/curriculum-mappings/{id}` | — | `204` | `CURRICULUM_MANAGE` |

## 6. Data Model

**Table:** `subjects`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| name | VARCHAR NOT NULL | |
| code | VARCHAR NOT NULL UNIQUE | |
| credits, theory_credits, lab_credits | INTEGER NOT NULL | ≥1 for ordinary subjects; 0 for the two system-managed allowlist subjects only |
| speciality_id | BIGINT | nullable FK → specialities |
| term_number | INTEGER NOT NULL | (0 allowed only for system-managed subjects) |
| is_active | BOOLEAN NOT NULL DEFAULT true | |
| lab_session_block_periods, clinical_session_block_periods | INTEGER NOT NULL DEFAULT 1 | consumed by Timetable auto-scheduler |
| (M2M) subject_eligible_labs, subject_eligible_clinical_venues, subject_eligible_faculty | join tables | admin-curated widening, additive only |

**Table:** `curriculum_versions`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| program_id | BIGINT NOT NULL FK → programs | |
| course_id | BIGINT NOT NULL FK → courses | |
| version_name | VARCHAR(100) NOT NULL | unique within program+course scope (app-level) |
| effective_from_academic_year_id | BIGINT NOT NULL FK → academic_years | |
| is_active | BOOLEAN NOT NULL DEFAULT true | |
| created_at / updated_at | TIMESTAMPTZ | |

**Table:** `curriculum_term_courses` (unique on `curriculum_version_id, term_number, subject_id`)
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| curriculum_version_id | BIGINT NOT NULL FK → curriculum_versions | |
| term_number | INTEGER NOT NULL | |
| subject_id | BIGINT NOT NULL FK → subjects | |
| sort_order | INTEGER | nullable |
| theory_hours, lab_hours, clinical_hours | INTEGER NOT NULL DEFAULT 0 | independent of Subject's own credit fields |
| subject_type | VARCHAR(20) NOT NULL DEFAULT 'CORE' | enum CORE/FOUNDATIONAL/ELECTIVE/CO_CURRICULAR |
| is_elective | BOOLEAN NOT NULL DEFAULT false | |
| elective_group_id | BIGINT | nullable FK → curriculum_elective_groups (owned by `inc-curriculum-compliance`) |

**Table:** `syllabi`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| curriculum_term_course_id | BIGINT NOT NULL FK → curriculum_term_courses | |
| version | INTEGER NOT NULL | |
| objectives (2000), content (4000), text_books (2000), reference_books (2000), course_outcomes (2000) | VARCHAR | all immutable post-create |
| is_active | BOOLEAN | |

**Table:** `syllabus_units` (unique on `curriculum_term_course_id, unit_number`)
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| curriculum_term_course_id | BIGINT NOT NULL FK → curriculum_term_courses | |
| unit_number | INTEGER NOT NULL | |
| component_type | VARCHAR(20) NOT NULL DEFAULT 'THEORY' | reuses `AttendanceType` enum (THEORY/LAB/CLINICAL) |
| title | VARCHAR(200) NOT NULL | |
| planned_hours | INTEGER | nullable |
| description | VARCHAR(1000) | nullable |
| sort_order | INTEGER | nullable |
| is_active | BOOLEAN NOT NULL | |

**Table:** `experiments`
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| subject_id | BIGINT NOT NULL FK → subjects | |
| experiment_number | INTEGER NOT NULL | |
| name | VARCHAR NOT NULL | uniqueness checked at app level, scoped to subject |
| description (2000), aim (1000), apparatus (2000), procedure (4000), expected_outcome (1000), learning_outcomes (2000) | VARCHAR | nullable |
| estimated_duration_minutes | INTEGER | nullable |
| is_active | BOOLEAN | |

**Table:** `lab_curriculum_mappings` (unique on `experiment_id, outcome_type, outcome_code`)
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| experiment_id | BIGINT NOT NULL FK → experiments | |
| outcome_type | VARCHAR NOT NULL | enum COURSE_OUTCOME/PROGRAM_OUTCOME/PROGRAM_SPECIFIC_OUTCOME |
| outcome_code | VARCHAR NOT NULL | free-text code, e.g. "CO1" |
| outcome_description | VARCHAR(1000) | nullable |
| mapping_level | VARCHAR NOT NULL | enum LOW/MEDIUM/HIGH |
| justification | VARCHAR(500) | nullable |

## 7. Edge Cases & Validation Rules
- Creating a Subject with credits/theoryCredits/labCredits < 1 fails validation for every subject except the two allowlisted system-managed codes, which the service pins to exactly 0 regardless of what is submitted.
- Placing a duplicate (curriculum_version_id, term_number, subject_id) combination fails at the DB unique-constraint level.
- Attempting to edit a Syllabus's content fields after creation has no code path — `PUT /syllabi/{id}` only accepts a `SyllabusActivationRequest` (activation toggle), so any UI that tried to resubmit full content would be rejected/ignored by the DTO shape.
- Creating a duplicate (experiment_id, outcome_type, outcome_code) Lab-Curriculum Mapping fails at the DB unique-constraint level.
- No cross-check was found ensuring a `CurriculumSemesterCourse`'s hour fields reconcile with its `Subject`'s own theory/lab credits — they can diverge silently.
- No delete-dependency guard was found for `Subject`, `Experiment`, or `CurriculumVersion` beyond DB-level FK constraints; deleting a Subject referenced by active `CurriculumSemesterCourse`/`Experiment` rows, or a CurriculumVersion referenced by active placements/syllabi, is not explicitly pre-checked at the service layer in the code reviewed and may surface as a raw FK-violation error rather than a friendly message.
- ~~`LabCurriculumMappingController`/`CurriculumSemesterCourseController` gate all mutations under the single `CURRICULUM_MANAGE` permission, while `CurriculumVersionController` uses the finer `CURRICULUM_CREATE`/`_EDIT`/`_DELETE` triad — a role granted only `CURRICULUM_MANAGE` cannot create/edit/delete Curriculum Versions...`~~ **Fixed 2026-09-24:** `CurriculumVersionController` now accepts `CURRICULUM_MANAGE` as an additional fallback on every operation, so a role holding only the coarser permission can now manage Curriculum Versions too, same as it already could for semester-course placements and CO/PO mappings. `CurriculumSemesterCourseController`/`LabCurriculumMappingController` are unchanged — they were already correctly using `CURRICULUM_MANAGE` since neither entity has its own V242 granular permission.

## 8. Known Gaps / Deferred
- The original milestone's "CO/PO mapping matrix UI component" (R1-2.6.9) shipped as a flat, row-per-mapping Material table (`co-po-mapping.component`), not a visual rows×columns correlation grid — functionally complete for CRUD, but a UI-shape deviation from the milestone text worth confirming with a reviewer.
- No reconciliation check between `CurriculumSemesterCourse` hours and `Subject` credit fields.
- No pre-delete dependency guards found for Subject/Experiment/CurriculumVersion beyond DB-level FK constraints.
