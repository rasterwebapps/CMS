# Software Requirements Specification — Examination Management

**Module:** Examination Management (R1-M5.1)
**Product:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Introduction

### 1.1 Purpose
Documents the requirements actually implemented for subject-level examination scheduling, mark/result entry, and student/guardian result self-service, reverse-engineered from the shipped backend (`backend/src/main/java/com/cms/`), Flyway migrations, and frontend (`frontend/src/app/features/examination/`).

### 1.2 Scope
Covers two parallel, independently-built subsystems that both persist "exam results" data:
1. **Legacy Examination/ExamResult subsystem** — subject-scoped exams, manually entered results, PASS/FAIL outcome used by Student Promotion arrear detection. **This is the only part with a frontend UI.**
2. **Term-based Exam Session subsystem** (`ExamSession` → `ExamEvent` → `StudentMark` → `SemesterResult`) — a newer, course-offering-scoped architecture with session lifecycle (DRAFT → PUBLISHED → LOCKED), batch mark entry, and computed semester PASS/FAIL. **Backend-only; no frontend route or service consumes any of its 5 controllers.**

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md` lines 577–598 (R1-M5.1)
- `docs/manual-test-cases/examination-management.md`
- `docs/BUSINESS_REQUIREMENTS.md` (no dedicated BR-N section exists for examinations/results; see BRD.md for derived rules)

## 2. Overall Description

### 2.1 Product Perspective
Subsystem 1 integrates with Student Promotion (arrear/INC detection via `ExamOutcome`), the Student self-service portal, and the Parent/Guardian portal. Subsystem 2 integrates with `TermInstance`, `CourseOffering`, and `CourseRegistration` (the term-based academic model) but has no consumer beyond its own repositories/reports.

### 2.2 Actors / User Classes
- **Admin / Exam Cell staff** — create examinations, enter/edit/delete results (permission-gated).
- **Student** — views own published results (self-service).
- **Guardian/Parent** — views a linked ward's results (parent portal), server-validated ward ownership.
- Roles are assigned via the DB-driven Role Management module; no roles are hardcoded.

### 2.3 Operating Environment
Angular frontend, Spring Boot backend under context path `/api/v1`, PostgreSQL, Keycloak-issued JWT bearer auth, permission checks via `@perm.has(...)`/`@perm.hasAny(...)`.

### 2.4 Constraints / Assumptions
- Pass threshold differs between the two subsystems: Subsystem 1 uses 50% of `Examination.maxMarks`; Subsystem 2 (`SemesterResultServiceImpl`) uses a flat 40% threshold constant. No GPA/CGPA letter-grading exists anywhere despite the milestone tracker describing "GPA/CGPA calculation" — both subsystems only produce PASS/FAIL/percentage.
- Subsystem 1 has no exam-session concept — an `Examination` can be created/edited/deleted at any time with no lock/publish workflow protecting entered marks.
- ~~**Known defect:** the frontend `Examination`/`ExaminationRequest` model used field name `courseId`...~~ **Fixed 2026-09-24:** the frontend model, form, list, and tour copy were renamed to `subjectId`/`subjectName` throughout, and the form now sources its dropdown from `GET /subjects` instead of `/courses`. See FRD §7 for detail.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|----|-------------|----------|---------------|
| FR-EXAM-1 | Create/update/delete an `Examination` (name, subject, exam type THEORY/PRACTICAL/VIVA, date, duration, max marks) | Must | Subject master |
| FR-EXAM-2 | List/view examinations, filterable by subject | Must | FR-EXAM-1 |
| FR-EXAM-3 | Record a student's exam result (marks, grade text, status PENDING/PUBLISHED/WITHHELD) against an examination | Must | FR-EXAM-1 |
| FR-EXAM-4 | Derive PASS/FAIL outcome automatically when a result's status is set to PUBLISHED (50% of max marks threshold); outcome is null while PENDING/WITHHELD | Must | FR-EXAM-3 |
| FR-EXAM-5 | Student self-service: authenticated student views only their own exam results | Must | FR-EXAM-3, self-service auth linkage |
| FR-EXAM-6 | Guardian self-service: authenticated guardian views a linked ward's results, ward ownership re-validated server-side | Must | FR-EXAM-3, Guardian/Parent Portal |
| FR-EXAM-7 | Record lab continuous evaluation marks (record/viva/performance/total) per experiment per student | Should | Experiment master (Curriculum) |
| FR-EXAM-8 | *(Backend-only, unreachable from UI)* Create an `ExamSession` per term+session-type (INTERNAL1/INTERNAL2/PRACTICAL/FINAL) with DRAFT→PUBLISHED→LOCKED lifecycle | Implemented, not exposed | Term Instance |
| FR-EXAM-9 | *(Backend-only)* Create an `ExamEvent` per course offering within a session (max/pass marks); enter `StudentMark` per course registration, blocked once the owning session is LOCKED | Implemented, not exposed | FR-EXAM-8 |
| FR-EXAM-10 | *(Backend-only)* Compute a `SemesterResult` (percentage, PASS/FAIL at 40% threshold) per student-term-enrollment once all term exam sessions are LOCKED; lock a computed result to freeze it | Implemented, not exposed | FR-EXAM-9 |
| FR-EXAM-11 | *(Backend-only)* Generate a per-student result sheet, per-cohort semester summary, and per-course-offering exam stats (present/absent/malpractice counts, average marks) | Implemented, not exposed | FR-EXAM-10 |

## 4. External Interface Requirements

### 4.1 Screens (Subsystem 1 only — the only UI-reachable part)
- **Examinations list** (`/examinations`) — Material table, columns Name/Course/Type/Date/Duration/Max Marks/Actions, search, sort, column picker, resizable columns.
- **Examination form** (`/examinations/new`, `/examinations/:id/edit`) — reactive form with live preview card and tips card.
- **Exam Results list** (`/exam-results`) — filtered by an examination picker (infinite-scroll select), columns Roll No./Student/Marks/Grade/Status (status rendered via `cms-status-badge`).

### 4.2 API Endpoints (high level; all under `/api/v1`)
- `GET|POST /examinations`, `GET|PUT|DELETE /examinations/{id}`, `GET /examinations/subject/{subjectId}`
- `GET|POST /exam-results`, `GET|PUT|DELETE /exam-results/{id}`, `GET /exam-results/examination/{id}`, `GET /exam-results/student/{id}`, `GET /exam-results/my`, `GET /exam-results/my-wards`
- `GET|POST /lab-evaluations`, `GET|PUT|DELETE /lab-evaluations/{id}`, `GET /lab-evaluations/experiment/{id}`, `GET /lab-evaluations/student/{id}`
- Backend-only: `/exam-sessions`, `/exam-events`, `/student-marks`, `/semester-results`, `/result-reports/*`

### 4.3 Key DB Entities
`examinations`, `exam_results` (+ `outcome` column added by V284), `lab_continuous_evaluations`, `exam_sessions`, `exam_events`, `student_marks`, `term_results` (entity `SemesterResult`).

## 5. Non-Functional Requirements
- **Performance:** no pagination on `/examinations` or `/exam-results/examination/{id}` GET-all endpoints — acceptable at current seed-data scale, unverified at production scale.
- **Security/RBAC:** all mutating endpoints require `EXAMINATION_MANAGE`/`EXAM_RESULT_MANAGE`. Read endpoints on `ExaminationController` (`findAll`, `findById`, `findBySubjectId`) previously had no `@PreAuthorize` at all; **fixed 2026-09-24** — they now require `EXAMINATION_VIEW` or `EXAMINATION_MANAGE`, matching every other read in this module. Self-service endpoints re-validate identity/ward ownership server-side, not just via route guard.
- **Auditability:** `createdAt`/`updatedAt` on every entity via JPA auditing; no dedicated audit-log trail for result edits/deletes beyond that.

## 6. Known Gaps / Not Yet Implemented
- **Frontend field-name mismatch bug** (see §2.4) likely breaks Add/Edit Examination submissions and always renders a blank Course column — needs verification against a live run.
- No GPA/CGPA calculation despite milestone tracker text; only percentage/PASS-FAIL.
- The entire term-based Exam Session architecture (Subsystem 2 — sessions, events, batch marks entry, computed semester results, result sheets/summaries/course stats) has zero frontend surface. The milestone tracker's "marks entry component (batch-wise)", "result view and transcript component" appear to describe this subsystem, but no such screens exist in `frontend/src/app/features/examination/`.
- Two inconsistent pass thresholds exist in the codebase (50% in `ExamResultService`, 40% in `SemesterResultServiceImpl`) with no shared configuration.
- ~~Granular permissions `EXAMINATION_CREATE/EDIT/DELETE` and `EXAM_RESULT_CREATE/EDIT/DELETE/EXPORT` were seeded (V242) but no controller enforces them~~ **Fixed 2026-09-24:** `ExaminationController` and `ExamResultController` now check `hasAny(granular, MANAGE)` on create/update/delete — purely additive (the `MANAGE` fallback is kept, and V242 already backfilled the granular codes onto every role that held `MANAGE` when it ran), so no existing access changed. `EXAM_RESULT_EXPORT` remains unwired — no export endpoint exists on `ExamResultController` to gate.
- No uniqueness validation (`uniqueFieldValidator`/`name-exists` endpoint) on the Examination form, though `Examination` functions as a master-like entity.
- `LabContinuousEvaluation.totalMarks` is client-supplied, not server-derived from record+viva+performance.
