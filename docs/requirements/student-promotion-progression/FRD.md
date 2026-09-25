# Functional Requirements Document — Student Promotion / Progression

**Module slug:** `student-promotion-progression`
**Source BR:** BR-52 · **Milestone:** R1-M6.2

## 1. Overview

The sole mechanism that moves an existing student's enrollment from one term to the next,
enforcing subject-wise arrears, a Final-Year arrear-clearance gate, a max-duration cap, and
per-subject attendance detention, with a mandatory preview before an irreversible bulk commit.

## 2. Actors & Permissions

| Permission | Category | Gates | Auto-granted to |
|---|---|---|---|
| `STUDENT_PROMOTION_VIEW` | `EXAMINATION` | active-terms, suggested-next-term, preview, history | existing `EXAM_RESULT_VIEW` holders |
| `STUDENT_PROMOTION_MANAGE` | `EXAMINATION` | execute | existing `EXAM_RESULT_MANAGE` holders |

Both are dedicated, new permissions (V286), per the project's operation-wise permission mapping
rule — neither reuses `EXAM_RESULT_*` directly. Role assignment is DB-only.

## 3. Screens & UI Behavior

`student-promotion.component` (`select` → `preview` → `result` steps, `Step` signal):

- **Select step:** Cohort dropdown. On change, `GET /student-promotions/active-terms?cohortId=`
  populates a list of currently-active term(s) for that cohort (each showing enrolled-student
  count); selecting one auto-fetches `GET /student-promotions/suggested-next-term` for the
  destination. An "advanced"/manual toggle reveals the full academic-year → term cascade (both From
  and To) for cases with no detectable active term or a non-standard rollover.
- **Preview step:** Table (`displayedColumns`: student, attendance, arrears, block, outcome,
  remarks) — one editable row per previewed student. `outcomeOptionsFor(row)` constrains the
  dropdown to outcomes legal given that row's `blockReasons` (e.g. `PROMOTED`/`GRADUATED` excluded
  if `ARREARS_AT_FINAL_YEAR_GATE` or `MAX_DURATION_EXCEEDED` is present). Remarks is free text per
  row. Two checkboxes: "Generate course registrations" / "Generate fee demands" for the destination
  term (both default true).
- **Execute:** `executeAll()` posts all rows' current outcome selections; on success, moves to the
  **result** step showing counts per outcome and any rejected decisions with reasons.
- No name/code uniqueness check on this screen — it is a workflow/wizard, not a master-data form,
  so the mandatory `uniqueFieldValidator` pattern doesn't apply here.

## 4. Functional Workflows

**Preview (`POST /student-promotions/preview`):**
1. Load cohort, from-term, to-term; filter `ENROLLED` enrollments for that cohort+from-term.
2. Per student: gather subjects registered this term (`CourseRegistrationRepository
   .findByStudentTermEnrollmentId`, excluding `DROPPED`); fetch per-subject attendance
   (`AttendanceService.getAttendanceReport`) and flag `lowAttendance` subjects.
3. Compute latest `PUBLISHED` `ExamResult` per subject across the student's whole history (ties
   broken by exam date, then by result id).
4. `newArrearIds` = this-term subjects that are low-attendance OR whose latest published result is
   `FAIL`. `carriedArrears` = subjects with a latest `FAIL` result that are **not** in this term's
   registration set (i.e. failed previously, not retaken yet). `totalArrears` = union of both.
5. `isEnteringFinalOrBeyond` = `toSemesterNumber >= program.totalTerms`. Block reasons:
   `MAX_DURATION_EXCEEDED` if `(referenceTerm academic year start - admission year) >=
   program.durationYears * 2`; `ARREARS_AT_FINAL_YEAR_GATE` if entering final-or-beyond with any
   arrear.
6. Recommended outcome: blocked → `null`; `toSemesterNumber > totalTerms` → `GRADUATED`; has
   arrears → `PROMOTED_WITH_ARREARS`; else → `PROMOTED`.

**Execute (`POST /student-promotions/execute`):**
1. Re-run `previewPromotion` server-side (ignores any client-supplied eligibility) and index by
   `studentId`.
2. Per submitted decision: reject if the student isn't in the fresh preview, or if the row is
   blocked and the chosen outcome is `PROMOTED`/`PROMOTED_WITH_ARREARS`/`GRADUATED`.
3. Apply per outcome:
   - `EXCLUDED` — skip entirely, no decision row, counted only.
   - `DETAINED_REPEAT` — enrollment stays `ENROLLED`; counted only.
   - `GRADUATED` — enrollment → `COMPLETED`; `Student.status = GRADUATED`; no new enrollment.
   - `PROMOTED`/`PROMOTED_WITH_ARREARS` — enrollment → `COMPLETED`; find-or-create the destination
     `StudentTermEnrollment` (`semesterNumber + 1`, `yearOfStudy` computed from `AssessmentPattern`
     — `YEARLY` uses semesterNumber directly, else `ceil(semesterNumber / 2.0)`); `Student.semester`
     updated to the new enrollment's `yearOfStudy`.
4. Every non-`EXCLUDED` outcome writes a `StudentPromotionDecision` row (student, cohort,
   from/to term, outcome, snapshot of `totalArrearSubjects` ids, `decidedBy` = the authenticated
   principal's name or `"system"`, `decidedAt`, `remarks`).
5. If `generateCourseRegistrations` and at least one student was promoted (with or without
   arrears), calls `CourseRegistrationService.generateRegistrationsForTermInstance(toTerm)`.
6. If `generateFeeDemands` similarly, calls `FeeDemandService.generateDemandsForTermInstance(toTerm)`.

## 5. API Endpoints

| Method | Path | Request | Response | Permission |
|---|---|---|---|---|
| GET | `/student-promotions/active-terms?cohortId=` | — | `List<CohortTermOption>` `{termInstanceId, termLabel, enrolledCount}` | `STUDENT_PROMOTION_VIEW` |
| GET | `/student-promotions/suggested-next-term?fromTermInstanceId=` | — | `CohortTermOption` or 204 | `STUDENT_PROMOTION_VIEW` |
| POST | `/student-promotions/preview` | `PromotionPreviewRequest {cohortId, fromTermInstanceId, toTermInstanceId}` | `PromotionPreviewResponse {cohortId, cohortCode, fromTermInstanceId, fromTermLabel, toTermInstanceId, toTermLabel, programTotalTerms, maxDurationYears, students: [StudentPromotionPreviewRow...]}` | `STUDENT_PROMOTION_VIEW` |
| POST | `/student-promotions/execute` | `PromotionExecuteRequest {cohortId, fromTermInstanceId, toTermInstanceId, decisions: [{studentId, outcome, remarks}], generateCourseRegistrations, generateFeeDemands}` | `PromotionExecuteResponse {promotedCount, promotedWithArrearsCount, detainedCount, graduatedCount, excludedCount, rejectedDecisions, courseRegistrationsGenerated, feeDemandsGenerated}` | `STUDENT_PROMOTION_MANAGE` |
| GET | `/student-promotions/history?cohortId=` or `?studentId=` | — | `List<StudentPromotionDecisionDto>` | `STUDENT_PROMOTION_VIEW` |

`StudentPromotionPreviewRow`: `studentId, studentName, rollNumber, enrollmentId,
subjectAttendance: [AttendanceReportResponse...], subjectExamOutcomes: [SubjectExamOutcome...],
carriedArrearSubjects, newArrearSubjects, totalArrearSubjects: [PromotionArrearSubject...],
recommendedOutcome, blockReasons: [string...]`.

## 6. Data Model

- **`exam_results.outcome`** (V284, `VARCHAR(10)`) — `PASS`/`FAIL`, computed at `create`/`update`
  when a result is `PUBLISHED`; backfilled for pre-existing published rows via a one-time
  `UPDATE ... FROM examinations` join comparing `marks_obtained * 100 >= max_marks * 50`.
- **`student_promotion_decisions`** (V285): `student_id`, `cohort_id`, `from_term_instance_id`
  (NOT NULL), `to_term_instance_id` (nullable — null for `DETAINED_REPEAT`), `outcome`
  (`PromotionOutcome` enum: `PROMOTED`/`PROMOTED_WITH_ARREARS`/`DETAINED_REPEAT`/`GRADUATED`/
  `EXCLUDED`), `decided_by`, `decided_at`, `remarks`.
- **`student_promotion_decision_arrears`** (V285, `@ElementCollection`): `decision_id`,
  `subject_id` — snapshot of arrear subjects at decision time, independent of later curriculum
  changes.
- Relationships: `StudentPromotionDecision` → `Student`, `Cohort`, `TermInstance` (×2, from/to).

## 7. Edge Cases & Validation Rules

- `suggestNextTerm` for an `ODD` from-term looks for the same academic year's `EVEN` term; for an
  `EVEN` from-term it looks at the next academic year (by start date) and its `ODD` term. Returns
  no suggestion (204) if neither exists.
- A student whose enrollment row can't be found in the freshly recomputed preview at execute time
  is rejected with "Student not enrolled in this cohort/term" rather than silently skipped.
- A blocked row submitted with an advancing outcome (`PROMOTED`/`PROMOTED_WITH_ARREARS`/
  `GRADUATED`) is rejected with the specific block reason(s) joined into the message — blocks are
  enforced server-side even if the client UI somehow allowed selecting a forbidden outcome.
- `GRADUATED` is only computed as the *recommended* outcome when `toSemesterNumber > totalTerms`
  (i.e. genuinely past the program's last term) — an admin can still manually pick `GRADUATED`
  earlier, but it will be blocked if arrears exist and the row is flagged final-year-or-beyond.
  (Blocking is driven by `blockReasons`, computed independent of the chosen outcome.)
- `yearOfStudy` computation differs by `AssessmentPattern`: `YEARLY` programs use the semester
  number directly as year; others (`SEMESTER`) use `ceil(semesterNumber / 2.0)`.
- Re-running `execute` for a student already advanced would find-or-create against the existing
  destination enrollment (`findByStudentIdAndTermInstanceId`) rather than duplicating it.

## 8. Known Gaps / Deferred

See SRS.md §6 and BRD.md §7: CIA/internal marks not implemented; `Cohort.status = GRADUATED` and
terminal student status on max-duration breach both remain manual, separate actions; the V525
"Backfill Course Registrations" action on Course Offerings can create registrations for stragglers
outside this module's eligibility gates.
