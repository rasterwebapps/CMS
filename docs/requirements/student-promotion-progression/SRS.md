# Software Requirements Specification — Student Promotion / Progression

**Module slug:** `student-promotion-progression`
**Product:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Source BR:** BR-52 (docs/BUSINESS_REQUIREMENTS.md), Milestone R1-M6.2 (docs/RELEASE_1_MILESTONES.md, lines 653–668)
**Basis:** Reverse-engineered from shipped code as of 2026-09-24.

## 1. Introduction

### 1.1 Purpose
Documents the mechanism that moves an existing student from one academic term to the next,
following the INC (Indian Nursing Council) / Dr. MGR Medical University promotion model:
subject-wise arrears carried forward, a hard block at Final Year until arrears clear, a max-duration
cap, per-subject attendance detention, and a mandatory preview before an irreversible bulk commit.

### 1.2 Scope
Covers `StudentPromotionService`/`Impl`, `StudentPromotionDecision` audit trail, `ExamResult.outcome`,
the removal of the prior blind auto-advance from `TermInstanceService`'s `OPEN` transition, and the
`features/student-promotion` frontend screen.

### 1.3 References
- `docs/BUSINESS_REQUIREMENTS.md` BR-52 (primary), BR-49 (attendance thresholds consumed here),
  BR-53 (Term Lifecycle, the transition this module's predecessor logic used to hook into)
- `docs/RELEASE_1_MILESTONES.md` R1-M6.2
- Change Log entries 2026-07-16 and 2026-07-17 in BUSINESS_REQUIREMENTS.md

## 2. Overall Description

### 2.1 Product Perspective
Prior to this feature, no code path moved an *existing* student to the next term —
`AcademicYearService.create()` only created brand-new cohorts for new admissions. A second,
independently discovered defect — `StudentTermEnrollmentServiceImpl.generateEnrollmentsForTermInstance()`
auto-advancing every active student by calendar years-since-admission the moment a term opened,
with zero eligibility check — has been removed as part of this feature. Student Promotion is now
the sole path that creates the next term's `StudentTermEnrollment`, `CourseRegistration`, and
`FeeDemand` rows (course-offering generation is unaffected, since it is curriculum-driven, not
per-student).

### 2.2 Actors / User Classes
- **Academic admin** (`STUDENT_PROMOTION_VIEW`/`STUDENT_PROMOTION_MANAGE`, auto-granted to
  existing `EXAM_RESULT_VIEW`/`EXAM_RESULT_MANAGE` holders) — runs preview and executes promotion
  for a cohort.

### 2.3 Operating Environment
Angular (`frontend/src/app/features/student-promotion`), Spring Boot
(`com.cms.service.StudentPromotionServiceImpl`, `com.cms.controller.StudentPromotionController`),
PostgreSQL via Flyway (V284–V286).

### 2.4 Constraints / Assumptions
- Pass/fail is **external-marks-only for v1** — `ExamResult.outcome` is derived from
  `marksObtained >= 50%` of `Examination.maxMarks` once `PUBLISHED`. Internal/Continuous
  Assessment (CIA) marks do not exist anywhere in this system yet; INC's rule requiring internal
  and external to each be passed separately cannot be implemented until CIA marks are built.
- `execute` always re-validates every submitted decision against a freshly recomputed server-side
  preview — client-supplied eligibility is never trusted.
- Cohort-level `GRADUATED` status stays a separate manual action; promotion only ever sets
  `Student.status = GRADUATED` for individual students.
- Max-duration breach is block-only; no automatic terminal `Student.status` (e.g.
  `WITHDRAWN`/`EXPELLED`) is applied.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-PROM-1 | `GET /student-promotions/active-terms?cohortId=` auto-detects the term instance(s) a cohort currently has `ENROLLED` students in. | Must | Cohort, StudentTermEnrollment |
| FR-PROM-2 | `GET /student-promotions/suggested-next-term?fromTermInstanceId=` auto-suggests the chronologically next term (ODD→EVEN same year, or EVEN→next year's ODD). | Must | FR-PROM-1 |
| FR-PROM-3 | A manual academic-year/term cascade remains available as a fallback ("Choose different terms manually") when auto-detection finds nothing or the admin opts in. | Must | — |
| FR-PROM-4 | `POST /student-promotions/preview` computes, per enrolled student: attendance (per component, via BR-49 thresholds), per-subject exam outcomes, carried arrears (prior FAIL subjects not in the current term), new arrears (current-term subjects that are low-attendance or FAIL), total arrears, a recommended outcome, and block reasons. Read-only, no mutation. | Must | ExamResult.outcome, AttendanceService |
| FR-PROM-5 | `ARREARS_AT_FINAL_YEAR_GATE` — hard, non-overridable block when a student would enter the program's final term/year while still carrying any arrear. | Must | FR-PROM-4 |
| FR-PROM-6 | `MAX_DURATION_EXCEEDED` — hard, non-overridable block when the reference term's academic-year start year minus the cohort's admission year reaches `program.durationYears * 2`. | Must | FR-PROM-4 |
| FR-PROM-7 | Recommended outcome logic: blocked → none recommended; entering beyond the program's total terms → `GRADUATED`; has arrears (and not blocked) → `PROMOTED_WITH_ARREARS`; else → `PROMOTED`. Admin can override to any outcome not forbidden by an active block. | Must | FR-PROM-5, FR-PROM-6 |
| FR-PROM-8 | `POST /student-promotions/execute` re-validates every decision against a freshly recomputed preview (never trusts client input), applies enrollment/registration/status changes per outcome, writes one `StudentPromotionDecision` audit row per non-`EXCLUDED` decision, and optionally triggers course-registration/fee-demand generation for the destination term. | Must | FR-PROM-4 |
| FR-PROM-9 | `DETAINED_REPEAT` outcome leaves the current enrollment `ENROLLED` (no new term row); the student reappears unchanged in the next promotion cycle's preview. | Must | — |
| FR-PROM-10 | `GRADUATED` is only legal with zero arrears; sets `Student.status = GRADUATED` and completes the enrollment; does not touch `Cohort.status`. | Must | FR-PROM-5 |
| FR-PROM-11 | `EXCLUDED` outcome is skipped entirely — no decision row is written, but the count is reported back for transparency. | Must | — |
| FR-PROM-12 | `GET /student-promotions/history?cohortId=` / `?studentId=` returns the full `StudentPromotionDecision` audit trail. | Should | FR-PROM-8 |
| FR-PROM-13 | The blind auto-advance previously wired into `TermInstanceService`'s `OPEN` transition (`generateEnrollmentsForTermInstance`/`generateRegistrationsForTermInstance`/`generateDemandsForTermInstance`) is removed; only `generateOfferingsForTermInstance` still runs automatically on `OPEN`. | Must | — |

## 4. External Interface Requirements

### 4.1 Screens
`student-promotion.component` — a select → preview → result wizard: cohort picker (auto-detects
active term(s), suggests next term, manual cascade fallback available via "Choose different terms
manually" / advanced toggle), a per-student editable decision table (attendance, arrears, block
reason, outcome dropdown constrained to legal options, remarks), and a bulk "Execute" action with
optional course-registration/fee-demand generation checkboxes, followed by a result summary (counts
per outcome + rejected decisions).

### 4.2 API Endpoints (high level)
See FRD.md §5 for full request/response shapes.

### 4.3 Key DB Entities
`student_promotion_decisions` (+ `student_promotion_decision_arrears` element collection),
`exam_results.outcome`, `students.status`, `student_term_enrollments`.

## 5. Non-Functional Requirements

- **Performance:** Preview iterates every `ENROLLED` enrollment in the from-term/cohort, computing
  attendance per registered subject — O(students × subjects) per preview call; no pagination
  documented (cohort-sized, not institution-sized, so acceptable at current scale).
- **Security/RBAC:** `STUDENT_PROMOTION_VIEW` gates read/preview/history; `STUDENT_PROMOTION_MANAGE`
  gates execute. Both are dedicated, new permissions (not reused).
- **Auditability:** Every non-excluded decision is persisted with `decidedBy`/`decidedAt`/
  `remarks`/an arrear-subject snapshot at decision time — the system's first lifecycle-transition
  audit trail (per BR-52's own text).
- **Data integrity:** `execute` never trusts client-supplied preview data — it recomputes preview
  server-side and rejects any decision whose row can no longer be found or is newly blocked.

## 6. Known Gaps / Not Yet Implemented

- **Internal/Continuous Assessment (CIA) marks** — do not exist in this system; promotion's
  pass/fail is external-marks-only for v1. INC's separate internal+external pass rule cannot be
  implemented until CIA marks are built.
- **Automatic `Cohort.status = GRADUATED`** — remains a manual, separate admin action.
- **Automatic terminal student status on max-duration breach** — `execute` only blocks the
  transition; `WITHDRAWN`/`EXPELLED` is a manual follow-up action outside this feature.
- **`COURSE_REGISTRATION_GENERATE` "Backfill Course Registrations"** (V525, Course Offering
  screen) deliberately bypasses Promotion's eligibility checks (arrears/max-duration/exam outcome)
  — documented in that migration as existing only to backfill stragglers left `ENROLLED` without
  registrations outside the normal promotion flow, never as a primary path. Worth flagging as a
  bypass surface relative to this module's guarantees.
