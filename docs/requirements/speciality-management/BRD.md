# Business Requirements Document — Speciality Management

**Client:** SKSCON / SKS College Of Nursing | **App:** OneCMS / College Management System | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective
SKS College of Nursing organizes its academic offering around specialities (e.g. General Nursing, Cardiology-focused tracks). The Speciality Management module gives administrators a single, DB-managed master list of specialities so that Programs, Labs, Faculty, and Subjects can all be consistently scoped and reported against a speciality, instead of specialities being free-text on each downstream record.

## 2. Stakeholders
- **College Admin / Admin** — creates and maintains the speciality master, assigns HOD.
- **Faculty** — is scoped to a speciality; sees it on their own profile.
- **Lab Setup, Faculty Management, Curriculum modules** — all consume the Speciality master as a lookup/filter dimension.

## 3. Business Rules
No dedicated BR-N section exists for Speciality in `docs/BUSINESS_REQUIREMENTS.md`; the following are derived from the shipped code:

- **BR-SPECIALITY-1:** A Speciality's `code` must be unique across the institution (enforced at the DB level).
- **BR-SPECIALITY-2:** A Speciality's `name` must be unique (enforced at the service/API level via the `/name-exists` check, not a DB constraint).
- **BR-SPECIALITY-3:** A Speciality can be deactivated (`isActive=false`) rather than deleted, to preserve historical references from Labs/Faculty/Subjects while removing it from active-selection dropdowns (`activeOnly=true` filter).
- **BR-SPECIALITY-4:** A Speciality optionally records a Head of Department both as a free-text name (`hodName`) and a soft reference to a Faculty record (`hodFacultyId`) — the two are not required to stay in sync by any enforced constraint.
- **BR-SPECIALITY-5:** Hard delete (`DELETE /specialities/{id}`) remains available alongside soft-deactivation; the service layer's delete-time referential behavior (whether it blocks deletion when Labs/Faculty reference the speciality) was not independently verified in this pass and should be confirmed before relying on it in production.

## 4. Business Process / Workflow
1. Admin navigates to Speciality List.
2. Admin clicks "Add" → Speciality Form opens; name and code are validated for uniqueness as the admin types.
3. On save, `POST /specialities` creates the record; it becomes immediately available in every downstream Speciality dropdown (Lab, Faculty, Subject forms).
4. To retire a speciality without breaking historical records, admin uses the status toggle (`PATCH /specialities/{id}/status`) rather than delete.
5. Downstream modules (Lab, Faculty) filter their own speciality dropdowns to `activeOnly=true` so a deactivated speciality stops appearing as a selectable option for new records while remaining valid on existing ones.

## 5. Success Criteria
Not formally defined in project documentation — inferred from feature completeness: Speciality CRUD, uniqueness validation, and status lifecycle are all implemented and consumed by three downstream modules (Lab, Faculty, Subject), matching the milestone's original acceptance checklist (`R1-2.1.1` through `R1-2.1.13`, all marked complete).

## 6. Assumptions & Constraints
- Speciality is assumed to be a relatively low-churn, admin-only master (no self-service creation by faculty or students).
- The system assumes at most one "current" HOD per speciality at a time (single `hodFacultyId` field, not a history table).

## 7. Known Gaps / Deferred
- No formal BR document exists for Speciality in `BUSINESS_REQUIREMENTS.md`; this BRD is derived entirely from code and migration inspection.
- No enforced link between `hodFacultyId` and the Faculty record's own speciality assignment (a Faculty member could be named HOD of a Speciality they are not themselves assigned to).
