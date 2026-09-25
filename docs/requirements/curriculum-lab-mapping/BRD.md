# Business Requirements Document — Curriculum & Lab-Curriculum Mapping

**Client:** SKSCON / SKS College Of Nursing | **App:** OneCMS / College Management System | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective
A nursing college's academic offering must satisfy regulatory (INC) curriculum-hours requirements and demonstrate outcome-based education — that every lab practical (Experiment) traceably contributes to a Course Outcome, Program Outcome, or Program-Specific Outcome. This module gives Admin/Curriculum Coordinators the data model to plan that: subjects with credit/hour splits, versioned curriculum plans per Program+Course that can evolve year over year without losing history, syllabus documents that are never silently rewritten, and an explicit Experiment→Outcome mapping (the CO/PO/PSO matrix) that downstream accreditation reporting and the Timetable auto-scheduler both depend on.

## 2. Stakeholders
- **College Admin / Curriculum Coordinator** — defines Subjects, builds/clones Curriculum Versions, places Subjects into terms, authors Syllabus versions, defines Experiments, and maps them to outcomes.
- **Faculty** — consumes the Subject/Syllabus/Experiment data as the basis for what they teach; a Subject's `eligibleLabs`/`eligibleClinicalVenues`/`eligibleFaculty` fields (admin-curated) widen who/where a subject can be staffed/scheduled.
- **Timetable / Capacity Planning module** — consumes `CurriculumSemesterCourse`'s theory/lab/clinical hours as the term's total teaching-hour demand, and `Subject.labSessionBlockPeriods`/`clinicalSessionBlockPeriods` to decide whether a lab session must be placed as one unbroken multi-period block.
- **Progress Tracking module (BR-58)** — consumes `SyllabusUnit`'s planned-hours-per-unit as the baseline against which portion-completion is measured.
- **INC Curriculum Compliance module (BR-49)** — consumes `CurriculumSemesterCourse`'s hour/type/elective columns directly (same table, different concern) and owns the `CurriculumElectiveGroup` entity that `CurriculumSemesterCourse.electiveGroupId` optionally points to.

## 3. Business Rules
No dedicated BR-N section exists for baseline Curriculum/Subject/Experiment/Mapping CRUD in `docs/BUSINESS_REQUIREMENTS.md` (BR-49 covers only the INC-compliance hour/elective layer built on top of the same tables); the following are derived from the shipped code:

- **BR-CLM-1:** A Subject's `code` must be unique institution-wide; `credits`, `theoryCredits`, `labCredits` must each be ≥1, **except** exactly two system-managed subjects (`SYSTEM-LIBRARY`, `SYSTEM-SPORTS`) which are pinned to 0 credits as a deliberate sentinel the Timetable auto-scheduler recognizes for advisory Library/Sports filler placement — this is not a general-purpose "zero-credit subject" allowance.
- **BR-CLM-2:** A Curriculum Version is scoped to exactly one Program + Course + effective-from Academic Year; its name must be unique within that Program/Course scope (not institution-wide).
- **BR-CLM-3:** Cloning a Curriculum Version creates a new version (new name, new effective year) as a starting point for the next revision, rather than requiring the admin to manually re-enter every term's subject placement — this is the shipped answer to "how does next year's curriculum get built from this year's."
- **BR-CLM-4:** A Subject can be placed at most once into a given term of a given Curriculum Version (`curriculum_version_id + term_number + subject_id` unique).
- **BR-CLM-5:** A Syllabus is append-only history, not an editable document: once created, its content (objectives/content/textbooks/course outcomes) can never be changed — only its `isActive` flag can be toggled. A content revision is captured by creating an entirely new Syllabus version against the same Curriculum-Semester-Course, never overwritten. There is no delete.
- **BR-CLM-6:** A Lab-Curriculum Mapping (Experiment → Outcome) is unique per (experiment, outcome type, outcome code) — the same Experiment cannot be double-mapped to the identical outcome within the same outcome type (COURSE_OUTCOME/PROGRAM_OUTCOME/PROGRAM_SPECIFIC_OUTCOME), though it can validly map to the same outcome *code* under two different outcome types, or to many distinct outcome codes.
- **BR-CLM-7:** An Experiment belongs to a Subject, not to a specific Curriculum Version or term — so the same practical remains reusable/referenceable across every curriculum version that ever teaches that Subject, without needing to be redefined each time the curriculum is revised.
- **BR-CLM-8:** Role/permission assignment for who can manage Subjects, Curriculum Versions, Syllabus, Experiments, or Mappings is entirely DB-driven via the Role Management module.

## 4. Business Process / Workflow
1. Admin defines the Subject catalog (name, code, credits, speciality, term number) — a one-time-per-subject setup independent of any specific curriculum version.
2. Admin creates a Curriculum Version for a Program+Course, effective from a chosen Academic Year (or clones a prior version to save re-entry).
3. Admin opens the Curriculum Map and places Subjects into each term of the version, setting each placement's theory/lab/clinical hour split and subject type (CORE/FOUNDATIONAL/ELECTIVE/CO_CURRICULAR).
4. Admin authors the Syllabus for each Curriculum-Semester-Course placement (objectives, content, textbooks, course outcomes) and, optionally, a structured Syllabus Unit breakdown (per-unit planned hours) for Progress Tracking to measure against later.
5. Admin defines Experiments under each Subject that has lab hours (procedure, apparatus, aim, expected/learning outcomes).
6. Admin maps each Experiment to the Course/Program/Program-Specific Outcomes it demonstrates, at a correlation strength (LOW/MEDIUM/HIGH) with an optional justification — this is the CO/PO/PSO matrix data consumed by accreditation-facing reporting.
7. Downstream, Timetable's auto-scheduler reads each term's `CurriculumSemesterCourse` hours as its demand input, and Subject's `eligibleLabs`/session-block-size fields as scheduling preferences/constraints.

## 5. Success Criteria
Not formally defined in project documentation — inferred from feature completeness: Subject, Curriculum Version (with clone), Curriculum-Semester-Course placement, Syllabus (versioned), Syllabus Unit, Experiment, and Lab-Curriculum Mapping CRUD are all implemented and consumed by Timetable and (for Syllabus Units) Progress Tracking, matching the milestone's original acceptance checklist (`R1-2.6.1`–`R1-2.6.9`, all marked complete) plus later extensions (Syllabus Units, V319) not in the original milestone text.

## 6. Assumptions & Constraints
- Curriculum authoring is assumed to be an Admin/Coordinator-only activity; no Faculty self-service editing of Syllabus/Experiment content was found.
- The append-only Syllabus versioning model assumes historical syllabus content must remain retrievable indefinitely (e.g. for accreditation audit trails spanning multiple academic years) rather than being pruned.
- The two hour-tracking systems on `CurriculumSemesterCourse` (its own theory/lab/clinical hours) versus `Subject` (theoryCredits/labCredits) are assumed to be independently admin-maintained; the system does not assume or enforce that they reconcile.
- `Experiment`'s Subject-level (not curriculum-version-level) scoping assumes lab practicals are relatively stable across curriculum revisions for the same subject — a genuinely different practical for a new curriculum version is assumed to be modeled as a new Experiment record, not a version-scoped override of an existing one.

## 7. Known Gaps / Deferred
- No formal BR document exists for baseline Curriculum/Subject/Experiment/Mapping CRUD in `BUSINESS_REQUIREMENTS.md`; this BRD is derived entirely from code and migration inspection (BR-49 covers only the INC-compliance layer built on the same tables).
- No system-level reconciliation check between `CurriculumSemesterCourse`'s hour fields and `Subject`'s own credit fields.
- No orphan-detection surfaced in the UI for Experiments whose Subject has been removed from every active Curriculum Version.
- The "CO/PO mapping matrix UI" from the original milestone text shipped as a list, not a visual matrix — see FRD Known Gaps for detail; flagged here as a business-facing deviation worth confirming was accepted, not silently treated as equivalent.
