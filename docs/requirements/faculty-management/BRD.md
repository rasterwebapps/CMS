# Business Requirements Document — Faculty Management

**Client:** SKSCON / SKS College Of Nursing | **App:** OneCMS / College Management System | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective
Faculty Management is the institution's master record of every teaching and non-teaching staff member — the identity a Course Offering, Lab assignment, Timetable placement, or referral-commission record ultimately points back to. What began (R1-M2.5) as a minimal CRUD (name, code, contact, speciality, designation) has grown into a full HR-adjacent profile (BR-42: identity/demographics, bank details, address, experience breakdown) with a configurable document-verification workflow (BR-26) so the college can track compliance documents (ID proofs, certificates, nursing-council registration) per staff member without a code change every time the required-document list changes.

## 2. Stakeholders
- **College Admin / Admin** — creates and maintains Faculty records, reviews/verifies uploaded documents, configures which document types are required.
- **Faculty** — appears as the subject of the record; also consumed as a dropdown selection across Lab Incharge assignment, Subject eligible-faculty, Course Offering instructor, Timetable staffing, and Referral (Faculty-referral enquiry) screens.
- **Timetable / Capacity Planning modules** — consume Faculty's speciality, designation, and per-faculty workload-cap overrides as inputs to staffing/scheduling constraint checks.
- **Referral & Commission Management** — Faculty can be the `referredFacultyId` on an enquiry (Faculty Referral, ₹500 default commission) and carries its own `commissionAmount` field.

## 3. Business Rules
No dedicated BR-N section exists for baseline Faculty CRUD in `docs/BUSINESS_REQUIREMENTS.md` (BR-26 and BR-42 cover only the document-review and extended-profile additions); the following are derived from the shipped code and the two BRs:

- **BR-FAC-1:** A Faculty's `employeeCode` and `email` must each be unique across the institution (DB-level `UNIQUE` constraint, also re-checked at the service layer on create/update).
- **BR-FAC-2:** A Faculty's `nrtsNumber` (nursing-council registration) must be unique **when present**. All three near-unique identifiers (`employeeCode`, `email`, `nrtsNumber`) now have a live "as-you-type" uniqueness check exposed to the frontend, fixed 2026-09-24 for `employeeCode`/`email` — see Known Gaps history.
- **BR-FAC-3 (BR-42):** A document type is required for a given Faculty if **any** configured rule in `faculty_document_type_requirements` matches **any one** of that Faculty's designation, speciality, or highest qualification (an OR match across rules and across the three match dimensions, not a requiring-all AND).
- **BR-FAC-4 (BR-26):** Faculty document review status must be derived and surfaced on Faculty discovery screens (list, in both card and table view) rather than requiring a reviewer to open a separate document-verification queue screen — that standalone queue screen has been retired in favor of working from the selected Faculty's own Documents tab.
- **BR-FAC-5 (BR-26):** Document review must never create, replace, or overload `FacultyStatus` — employment status (Active/On Leave/Resigned/etc.) stays fully independent of document-verification status.
- **BR-FAC-6 (BR-26):** Document review must use DB-driven permissions only; no screen or API may hardcode a role name for document actions.
- **BR-FAC-7:** Role/permission assignment for who can manage Faculty, review documents, or view workload is entirely DB-driven via the Role Management module — never hardcoded.
- **BR-FAC-8:** A Faculty's advisory weekly/daily/continuous teaching-hour cap resolves by override precedence: per-faculty override (on the Faculty record) → designation default → flat institution-wide config → no cap. The "Raise Cap" action on Faculty Detail is a minimal, single-field PATCH specifically so a coordinator can bump one faculty member's daily cap without touching the rest of their profile.

## 4. Business Process / Workflow
1. Admin navigates to Faculty List and clicks "Add Faculty."
2. Admin fills core fields (employee code, name, email, phone, speciality, designation, joining date, status) and, optionally at the same time or later via edit, the extended profile (identity, bank, address, experience, qualification).
3. On submit, `POST /faculty` validates required fields and uniqueness (employee code/email/NRTS); on success the Faculty becomes immediately selectable everywhere a Faculty dropdown appears.
4. Separately, an Admin opens the Faculty's Documents tab and uploads scanned documents per required type (as computed from the Faculty Doc Config rules); each upload starts at a review status and is verified or rejected by a reviewer, with remarks and a verification lock preventing accidental overwrite of already-reviewed evidence.
5. The Faculty List's document-review badge (derived counts: total, required, pending, rejected, missing-required, verified-required) lets an Admin spot incomplete files without opening each record.
6. If a Timetable capacity check finds a Faculty consistently over their resolved cap, a coordinator can use the Faculty Detail "Raise Cap" flyout to set a per-faculty daily-hour override without a full profile edit.
7. To retire a Faculty member, Admin uses `DELETE /faculty/{id}` — there is no soft-deactivate lifecycle; `FacultyStatus` values (ON_LEAVE, RESIGNED, RETIRED, TERMINATED) serve the "no longer actively teaching" case without removing the record.

## 5. Success Criteria
Not formally defined in project documentation — inferred from feature completeness: Faculty CRUD, extended profile, document-review workflow, and workload-cap override are all implemented and consumed by Lab, Curriculum, Timetable, and Referral modules, exceeding the original milestone's acceptance checklist (`R1-2.5.1`–`R1-2.5.7`, all marked complete) with two additional shipped BRs (BR-26, BR-42) layered on top.

## 6. Assumptions & Constraints
- Faculty is assumed to be an admin-managed master; there is no faculty self-service registration flow.
- The extended profile (BR-42) is assumed entirely optional at the data-model level (nullable columns) — a Faculty record created via the original minimal core-fields flow remains valid without ever filling in identity/bank/address data.
- Document requirement rules (Faculty Doc Config) are assumed to be a relatively low-churn admin configuration, not something re-evaluated live against every in-flight document.
- `LabInChargeAssignment` (Lab Setup module) is assumed, by naming convention only, to reference a Faculty id — there is no enforced FK, so this assumption is not guaranteed to hold for stale data.

## 7. Known Gaps / Deferred
- ~~No real-time uniqueness validation endpoint for `employeeCode`/`email`~~ **Fixed 2026-09-24:** `/faculty/employee-code-exists` and `/faculty/email-exists` now exist (gated `FACULTY_MANAGE`), wired to the Faculty Form the same way `nrtsNumber` already was, closing this project's mandatory master-screen uniqueness-check gap for the two remaining fields.
- ~~Three seeded permissions (`FACULTY_CREATE`, `FACULTY_EDIT`, `FACULTY_DELETE`) are unused by the backend~~ **Fixed 2026-09-24:** each now gates its matching operation, additively alongside `FACULTY_MANAGE`.
- No pre-delete dependency guard on Faculty deletion; deleting a Faculty referenced elsewhere (Course Offerings, Lab assignments, Class Schedules) is not explicitly blocked at the service layer in the code reviewed.
