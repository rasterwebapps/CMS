# Business Requirements Document — Examination Management

**Product:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective
SKSCON needs to record subject-level examinations, capture student marks, and let students/guardians view published results without exposing raw academic administration screens to them. The shipped system delivers exactly this (Subsystem 1) plus feeds a PASS/FAIL signal into the Student Promotion module for arrear (carry-forward subject) detection under the INC / Dr. MGR Medical University promotion model. A second, more structurally rigorous term-based exam architecture (sessions/events/batch marks/computed semester results) was also built at the database and service layer but was never connected to any screen, so it delivers no business value today.

## 2. Stakeholders
- Exam Cell / Academic Admin staff (create exams, enter/publish results)
- Students (self-service result viewing)
- Parents/Guardians (ward result viewing via Parent Portal)
- Student Promotion module (consumes `ExamOutcome` for arrear detection)
- Raster support/dev admins (full access via DEV_ADMIN/SUPPORT_ADMIN)

## 3. Business Rules

No `BR-N` entry in `docs/BUSINESS_REQUIREMENTS.md` is dedicated to examinations or results (a Table-of-Contents grep for exam/result/grade/marks/transcript terms returned none). The following are derived from the shipped code and its inline comments, and stated here as implicit rules:

- **BR-EXAM-1 (derived):** A result's `outcome` (PASS/FAIL) is computed only when its `status` is `PUBLISHED`, using `marksObtained >= 50% of Examination.maxMarks`. PENDING and WITHHELD results carry no outcome and do not count toward promotion arrear detection. (Source: `ExamResultService.deriveOutcome`, code comment.)
- **BR-EXAM-2 (derived):** Student Promotion arrear detection uses only the **latest published** result per student per subject — a later supplementary/re-exam result supersedes an earlier FAIL. (Source: `StudentPromotionServiceImpl.latestPublishedPerSubject`.)
- **BR-EXAM-3 (derived):** V284's migration comment states "External/university marks only for v1 — internal/CIA marks don't exist yet," meaning this subsystem models only final/university-style exams, not continuous internal assessment (that's the separate, unconnected Subsystem 2 marks model).
- **BR-EXAM-4 (derived):** A student can only ever see their own results; a guardian can only see results of a student who server-side is actually their registered ward — enforced identically to the attendance self-service pattern (`GuardianService.assertIsMyWard`), not left to client-side trust.
- **BR-EXAM-5 (derived, Subsystem 2 only, unused by any screen):** Marks cannot be entered or modified once the owning `ExamSession` is `LOCKED`; a `SemesterResult` that is locked cannot be recomputed; a term's semester results can only be computed once every exam session in that term is `LOCKED`.

## 4. Business Process / Workflow Narrative
1. Exam Cell staff creates an `Examination` for a subject (name, type, date, duration, max marks).
2. Staff enters a `ExamResult` per student against that examination (marks, free-text grade, status).
3. When staff sets status to `PUBLISHED`, the system silently derives PASS/FAIL and stores it.
4. The student (or their guardian) can now see that result in the self-service portal.
5. Separately, the Student Promotion process reads each student's latest published result per subject to decide promotion eligibility / arrear carry-forward, independent of any explicit "publish to promotion" action by staff.
6. Lab continuous evaluation marks (record/viva/performance/total) can be entered per experiment per student, independently of the examination/result flow above — there is no visible screen for this, so in practice it can only be exercised via direct API calls.

## 5. Success Criteria
Not formally defined — no KPI, target, or acceptance metric is stated in the milestone tracker or business requirements doc for this module. Success is inferred from feature completeness against R1-M5.1's checklist, which is itself only partially matched by the shipped frontend (see Known Gaps).

## 6. Assumptions & Constraints
- Assumes one exam type per subject-level Examination; no notion of an "exam series" per term at the Subsystem-1 level (that concept exists only in the unused Subsystem 2).
- Assumes staff, not an automated scheduler, decides when to publish a result — there is no batch "publish all results for this examination" action; each result is published individually via its status field.
- Grade is a free-text string with no server-side grade-scale validation.

## 7. Known Gaps / Deferred
- The richer term-based exam session/semester-result architecture (Subsystem 2) is fully built server-side but has no business process wrapped around it — no staff workflow can reach it today.
- No GPA/CGPA computation exists, despite being referenced in the milestone tracker's description of the module's original intent.
- A likely field-name mismatch between frontend and backend (`courseId` vs `subjectId`) may currently block the "Add Examination" business process from working end-to-end from the UI — see FRD §7 for the code evidence; needs a live functional check to confirm impact, since this document is based on static code review only.
