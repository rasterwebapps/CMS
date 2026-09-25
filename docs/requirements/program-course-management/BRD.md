# Business Requirements Document — Program & Course Management

**Client:** SKSCON / SKS College Of Nursing | **App:** OneCMS / College Management System | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective
SKS College of Nursing offers multiple academic programs (e.g. B.Sc Nursing, GNM, ANM), each of which may itself be split into named specialization streams for admission and roll-numbering purposes. This module gives administrators DB-managed masters for both levels, replacing what would otherwise be hardcoded or spreadsheet-tracked program/stream definitions, and feeds Enquiry, Admission, Fee Structure, and Curriculum with a consistent Program/Course reference.

## 2. Stakeholders
- **College Admin / Admin** — defines Programs and Courses, sets admission-age rules and required documents per Program.
- **Front Office / Admission staff** — select Program/Course on Enquiry and Admission forms.
- **Fee/Accounting** — Fee Structures are scoped per Program/Course (see `docs/BUSINESS_REQUIREMENTS.md` BR-1/BR-30).
- **Curriculum team** — defines Curriculum Versions against a Program+Course pair.

## 3. Business Rules
No dedicated BR-N section documents Program/Course in isolation; the following are derived from code:

- **BR-PROGCOURSE-1:** `Program.code` and `Course.code` must each be globally unique.
- **BR-PROGCOURSE-2:** A `Course` always belongs to exactly one `Program` (mandatory FK); there is no cross-program Course.
- **BR-PROGCOURSE-3:** `Program.durationYears` combined with `assessmentPattern` (`TERM_BASED` = 2 terms/year, `YEARLY` = 1 term/year) determines the total number of terms a student in that program will progress through (`getTotalTerms()`).
- **BR-PROGCOURSE-4:** Each `Course` carries a 2-character `rollNumberCode` (enforced `length=2`, `NOT NULL` since V184) that feeds the Number Sequence / Roll Number Generation system (see BR-41), so every admitted student's roll number encodes which Course stream they belong to.
- **BR-PROGCOURSE-5:** A Program records minimum admission age and a cutoff day/month (defaults 17 years, Dec 31) used by Admission eligibility checks (added V175, after the original milestone).
- **BR-PROGCOURSE-6:** A Program can declare mandatory vs. optional document types required at admission (`program_document_types`, V91) — this feeds the Admission module's checklist, not something in the original R1-M2.2 milestone scope.
- **BR-PROGCOURSE-7:** Both Program and Course follow the same `PATCH /{id}/status` activate/deactivate lifecycle contract used elsewhere in the app (BR-32's pattern), though neither is in BR-32's own documented scope table.

## 4. Business Process / Workflow
1. Admin creates a Program (name, code, duration, assessment pattern).
2. Admin optionally configures the Program's mandatory/optional admission document types.
3. Admin creates one or more Courses under that Program, each with a unique 2-character roll-number code.
4. Downstream: Enquiry/Admission forms select Program then Course; Fee Structure Groups are scoped per Program+Course+quota+state+gender+student-type (BR-30); Curriculum Versions are built against a Program+Course pair; Students admitted under a Course receive roll numbers encoded with that Course's `rollNumberCode`.

## 5. Success Criteria
Not formally defined — inferred from feature completeness: all milestone checklist items (R1-2.2.1 through R1-2.2.10) are marked complete, and Program/Course are actively consumed by Enquiry, Admission, Fee Structure, and Curriculum Version modules in production code.

## 6. Assumptions & Constraints
- The business assumes a relatively small, admin-managed set of Programs/Courses (dozens, not thousands) — pagination exists but the UI patterns (dropdowns) assume manageable list sizes.
- Course specialization is descriptive free text, not a further-normalized taxonomy.

## 7. Known Gaps / Deferred
- The credit-bearing "Course" concept originally planned in R1-M2.2 (credits, theory/lab credit split, semester) shipped under a differently-named entity (`Subject`) during a later restructuring — see the Curriculum & Lab-Curriculum Mapping BRD. This is a **terminology/scope drift** from the original milestone text, not a missing feature.
- No explicit business rule found governing what happens to existing Students/Curriculum Versions if a Program or Course is deleted (as opposed to deactivated).
