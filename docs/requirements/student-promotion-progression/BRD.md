# Business Requirements Document — Student Promotion / Progression

**Module slug:** `student-promotion-progression`
**Source BR:** BR-52 · **Milestone:** R1-M6.2

## 1. Executive Summary / Business Objective

Move an existing student from one academic term to the next in a way that actually reflects the
real INC/Dr. MGR Medical University promotion model — subject-wise arrears, a Final Year clearance
gate, a max-duration cap, and per-subject attendance detention — replacing a previously undiscovered
blind auto-advance that promoted *every* active student purely by calendar years-since-admission
with no eligibility check at all. The objective is both a new capability (structured promotion) and
a correctness fix (removing an unsafe hidden behavior).

## 2. Stakeholders

- **Academic admin** — runs promotion at the end of each term cycle; the sole actor with
  `STUDENT_PROMOTION_MANAGE`.
- **Students** — directly affected: arrears, detention, and graduation eligibility are decided
  here.
- **Fee/Admissions office** — downstream consumers of the fee-demand/course-registration generation
  this module can trigger for the destination term.
- **Exam cell** — `ExamResult.outcome` (PASS/FAIL) is the direct input to arrear detection.

## 3. Business Rules

| ID | Rule | Rationale |
|---|---|---|
| BR-PROM-1 | A student can be promoted to the next year while carrying a failed subject forward (`PROMOTED_WITH_ARREARS`). | Matches real INC practice — a single failed subject doesn't have to repeat the whole year. |
| BR-PROM-2 | All arrears must be cleared before a student may enter the program's Final Year term — a hard, non-overridable block. | INC requires arrear-free status before final-year eligibility. |
| BR-PROM-3 | A student cannot be promoted/graduated past double the program's normal duration (e.g. 8 years for a 4-year B.Sc.). Hard, non-overridable block. | Regulatory maximum-duration cap. |
| BR-PROM-4 | Low attendance (per BR-49's configurable per-component thresholds, no hardcoded percentage) creates an arrear for that specific subject; it does not by itself force a whole-year repeat. A manual `DETAINED_REPEAT` override remains available for admin discretion. | Attendance shortfall is a subject-level problem, not automatically a year-level one; admin retains override for edge cases (e.g. a max-duration breach). |
| BR-PROM-5 | Pass/fail is external-marks-only for v1 (`marksObtained >= 50%` of `maxMarks`, once `PUBLISHED`). | CIA/internal marks don't exist in the system yet — documented as an accepted v1 limitation, not an oversight. |
| BR-PROM-6 | `POST /preview` (read-only) must be reviewed before `POST /execute` (irreversible bulk mutation); `execute` always re-validates every decision against a freshly recomputed preview, never trusting client-supplied eligibility. | Prevents an admin acting on stale or tampered eligibility data for a bulk, irreversible operation. |
| BR-PROM-7 | Picking a cohort is the only mandatory input for the common case — the system auto-detects the active term and suggests the next one; the full manual cascade is a fallback. | Removes meaningless repeat clicking through two full year→term cascades for the common single-active-term case. |
| BR-PROM-8 | Cohort-level `GRADUATED` status stays a manual, separate decision; promotion only ever sets individual `Student.status = GRADUATED`. | Cohort graduation is a broader administrative event than any one promotion run. |
| BR-PROM-9 | Max-duration breach blocks the transition only — no automatic `WITHDRAWN`/`EXPELLED` status change. | That terminal outcome is a distinct, deliberate admin decision outside this feature's scope. |

## 4. Business Process / Workflow

1. Term cycle ends; academic admin opens Student Promotion, selects a cohort.
2. System auto-detects the term(s) that cohort has `ENROLLED` students in and suggests the
   chronologically next destination term (falls back to manual academic-year/term cascade if
   ambiguous or a new cohort with no next term created yet).
3. Admin requests a **preview** — a read-only, per-student computation of attendance, exam
   outcomes, carried/new/total arrears, a recommended outcome, and any hard-block reasons.
4. Admin reviews/edits each student's outcome (constrained to outcomes not forbidden by an active
   block), optionally adds remarks, and chooses whether to also generate course registrations and/or
   fee demands for the destination term.
5. Admin **executes**. The system recomputes preview server-side per student (defense against
   stale/tampered client state), applies the enrollment/status transition per outcome, and writes
   an audit row (`student_promotion_decisions`) for every non-`EXCLUDED` decision.
6. Result screen reports counts per outcome plus any rejected decisions (e.g. a student whose
   eligibility changed between preview and execute).
7. `DETAINED_REPEAT` students simply reappear, unchanged, in the next cycle's preview for that
   cohort/term.

## 5. Success Criteria

Not formally defined with a KPI — inferred from feature completeness: (a) the previously-existing
blind auto-advance is fully removed from the term-open transition, (b) every term-to-term
enrollment change for an existing student now goes through this reviewed, audited path, and (c)
subject-wise arrears, Final-Year gating, and max-duration are enforced as hard blocks exactly as
specified.

## 6. Assumptions & Constraints

- `StudentTermEnrollmentService.generateEnrollmentsForTermInstance()` and its downstream
  registration/fee-demand generation are no longer called automatically on term `OPEN` — this was
  a deliberate, verified removal (confirmed in `TermInstanceService.updateTermInstance`), not an
  unintended side effect.
- `ExamResult.outcome` is computed and persisted (not computed on read) at create/update time, so
  "latest published result per student+subject" queries stay simple filters and a later
  supplementary result naturally supersedes an old FAIL.
- Course offering generation on term `OPEN` is unaffected by this change since it is
  curriculum-driven, not per-student.

## 7. Known Gaps / Deferred

- **CIA/internal marks** — do not exist; the INC rule that internal and external marks must each
  be passed separately cannot be implemented until they do.
- **Automatic `Cohort.status = GRADUATED`** — manual, separate action, by design.
- **Automatic terminal student status on max-duration breach** — manual follow-up, by design.
- **`COURSE_REGISTRATION_GENERATE` "Backfill Course Registrations"** bypass surface — see SRS.md
  §6; a later (V525) unrelated screen action can create registrations for stragglers outside this
  module's eligibility checks.
