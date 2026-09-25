# Business Requirements Document — Student Management

**App:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Company:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective

The Student Management module is the single source of truth for every enrolled student's master record — identity, academic placement (program/course/speciality/semester/cohort), demographics required for statutory/nursing-council reporting, family/guardian contact, bank details for scholarship/refund payouts, and lifecycle status. Every other module (fees, attendance, timetable/course registration, examinations, library, scholarships, promotion) reads from this record rather than duplicating student identity data.

## 2. Stakeholders

- **College Admin / Registrar** — primary owner: creates, corrects, and maintains student records; runs roll-number generation; manages program transfers.
- **Faculty** — consumes student records for rosters, attendance, marks, and correspondence.
- **Accounts/Finance team** — consumes student records for fee allocation and collection.
- **Students/Parents** — indirect beneficiaries via downstream self-service portals (not part of this module).
- **SKSCON management** — consumes the Student Explorer for institutional reporting (enrollment counts by program/year/status).

## 3. Business Rules

- **BR-STUDENT-1 (mapped from BR-11 — Student Explorer with Filters).** All students, regardless of admission channel, must be visible in a single filterable Student Explorer (program, speciality, academic year, semester, status, fee status, free-text search), server-paginated and sortable, with each row navigating to the student's detail profile.
- **BR-STUDENT-2 (mapped from BR-27 — Permanent Admission Number).** Every student gets a permanent, immutable, academic-year-scoped admission number (`ADM-2526-0001` format) generated only on successful admission completion — never reserved on a failed attempt. Roll number remains optional until generated separately.
- **BR-STUDENT-3 (mapped from BR-41 — Roll Number Generation).** Roll numbers follow `[CollegeCode][CourseCode][Year][Sequence]`, generated from a per-course-per-year counter with pessimistic locking against concurrent bulk-generate races. Staff may preview a batch before committing, or manually assign/correct a single student's roll number outside generation entirely.
- **BR-STUDENT-4 (implicit, from entity constraints).** `rollNumber`, `admissionNumber`, `universityRegistrationNumber`, `umisNumber`, and `email` must each be unique across all students (DB `UNIQUE` constraints on `students`).
- **BR-STUDENT-5 (implicit, from `StudentProgramTransfer`).** A student's program can be changed after enrollment via an explicit "Program Transfer" action that first runs an impact analysis (e.g. fee/curriculum implications) before commit, and every transfer is retained as history rather than overwritten.
- **BR-STUDENT-6 (implicit, from `firstGraduate`/`fatherEducation`/`motherEducation` fields — related to BR-21).** First-generation-graduate status and parent education level are captured on the student record to feed scholarship-eligibility determination in the Scholarship module.

## 4. Business Process / Workflow

1. A student record is created either (a) automatically when an Enquiry is converted to an Admission (separate module, generates the admission number per BR-27), or (b) directly by an admin through the Student Form.
2. The admin (or Registrar) periodically runs **Generate Roll Numbers** for a course/year, previewing the proposed sequence before committing; any student can also have their roll number manually assigned/corrected at any time.
3. Staff maintain the record over time — status transitions (ACTIVE → ON_LEAVE/SUSPENDED/WITHDRAWN/GRADUATED/EXPELLED/INACTIVE), contact and bank-detail updates, cohort assignment.
4. If a student needs to move programs, the admin opens **Program Transfer**, reviews the analysis (target program eligibility/impact), and executes the transfer; the prior program assignment is preserved in `student_program_transfers`.
5. Any user holding `STUDENT_EXPORT` can pull a filtered/sorted Excel or PDF extract of the Explorer view for institutional reporting.
6. Downstream modules (Fee, Attendance, Examination, Library, Course Registration) reference the student by ID and read status/program/cohort/semester as needed — this module does not push data outward; it is read by others.

## 5. Success Criteria

Not formally defined with quantitative KPIs in the codebase or `docs/BUSINESS_REQUIREMENTS.md`. Inferred from feature completeness: every enrolled student has exactly one record with a unique admission number; roll numbers are assignable without collision under concurrent bulk operations; the Explorer can find any student via at least one of program/status/academic-year/free-text search within the paginated UI.

## 6. Assumptions & Constraints

- A student always belongs to exactly one `Program` (`programId` is `NOT NULL`); `Course` and `Speciality` are optional.
- `semester`/`yearOfStudy` is a plain integer counter on the student record, not itself derived from cohort/term enrollment (`StudentTermEnrollment` is a separate, related entity tracking per-term enrollment — outside this module's scope).
- Address is stored as a JPA `@Embedded` value object on the student row (not a separate table).
- Bank details exist on the student record specifically to support OneBook payouts (refunds, scholarships) — not for general banking/payroll purposes.

## 7. Known Gaps / Deferred

- The core update endpoint's permission gating (`STUDENT_VIEW` instead of `STUDENT_EDIT`) and several unguarded GET endpoints are inconsistent with the rest of the permission model — see SRS §6 for the full list. This is a code-level finding surfaced during documentation, not a business-rule gap; no code was changed as part of this documentation task.
- No explicit uniqueness pre-check (live "name/code already exists" feedback) on the Student Form; uniqueness is enforced only at save time via DB constraints.
