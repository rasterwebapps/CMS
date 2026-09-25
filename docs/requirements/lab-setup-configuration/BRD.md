# Business Requirements Document — Lab Setup & Configuration

**Client:** SKSCON / SKS College Of Nursing | **App:** OneCMS / College Management System | **Built by:** Raster / Raster Images Pvt. Ltd.

## 1. Executive Summary / Business Objective
SKS College of Nursing runs practical/lab sessions (Computer, Physics, Chemistry, Electronics, Biology, Language, Mechanical labs, etc.) across its specialities. Before any lab can be scheduled, staffed, equipped, or mapped to curriculum, it must first exist as a defined venue with a known type, capacity, location, and status. The Lab Setup & Configuration module provides that single master record — the "what and where" of a lab — plus the ability to name a Lab Incharge and/or Technician responsible for it. Every downstream module (Lab Scheduling/Timetable, Lab-Curriculum Mapping, Equipment/Inventory, Lab Safety) depends on this master existing first.

## 2. Stakeholders
- **Admin / College Admin** — creates and maintains the Lab master, assigns Lab Incharge/Technician staff.
- **Lab Incharge / Technician** — the staff member named as responsible for a Lab via `LabInChargeAssignment`; the original milestone intended them to also be able to update Lab details, but the shipped permission model does not gate `PUT /labs/{id}` on this role specifically (see FRD Known Gaps).
- **Faculty / Curriculum / Timetable modules** — consume the Lab list as a dropdown/lookup wherever a lab needs to be selected (Lab-Curriculum Mapping, Lab Scheduling, Subject's eligible-labs preference).
- **Speciality Management** — upstream dependency; every Lab must belong to exactly one Speciality.

## 3. Business Rules
No dedicated BR-N section exists for Lab Setup in `docs/BUSINESS_REQUIREMENTS.md`; the following are derived from the shipped code (`LabController`, `LabService`, `Lab`/`LabInChargeAssignment` entities):

- **BR-LAB-1:** A Lab must belong to exactly one Speciality (`speciality_id NOT NULL`) — there is no institution-wide/speciality-agnostic Lab.
- **BR-LAB-2:** A Lab has a fixed type from a closed enum (`COMPUTER`, `PHYSICS`, `CHEMISTRY`, `ELECTRONICS`, `BIOLOGY`, `LANGUAGE`, `MECHANICAL`, `OTHER`) — not a DB-driven master like Speciality or Designation.
- **BR-LAB-3:** A Lab optionally links to exactly one physical `Room` from the Campus Infrastructure hierarchy (added later by V473/V474) — this link is nullable and non-unique (a Room is not prevented from being linked to more than one Lab), and does not cross-validate the Lab's declared `capacity` against the Room's own physical capacity.
- **BR-LAB-4:** A Lab's status is one of `ACTIVE`, `AVAILABLE`, `INACTIVE`, `UNDER_MAINTENANCE` — a plain enum, not the `isActive` boolean lifecycle pattern used by DB-driven masters like Speciality.
- **BR-LAB-5:** One or more staff (identified only by a `Long` id + `String` name snapshot, not a live FK to Faculty) can be assigned to a Lab as `LAB_INCHARGE` or `TECHNICIAN` via `LabInChargeAssignment`, each with an assignment date. Multiple concurrent assignments of either role to the same Lab are not prevented.
- **BR-LAB-6:** There is no uniqueness constraint or real-time uniqueness check on Lab `name` — two Labs may share the same name (unlike the DB-driven masters that follow the `uniqueFieldValidator` + `/name-exists` pattern).

## 4. Business Process / Workflow
1. Admin navigates to the Lab list and clicks "Add Lab."
2. Admin fills in name, type, speciality, building, room number, capacity, status, and optionally links a physical Room.
3. On save, `POST /labs` creates the record (`LAB_MANAGE`); the Lab is immediately available to Curriculum, Scheduling, and Subject-eligibility dropdowns.
4. Admin (or, per the shipped permission model, anyone holding `LAB_MANAGE`) opens a Lab and assigns a staff member as Lab Incharge or Technician via `POST /labs/{id}/assign`, recording the assignment date.
5. The Lab Detail screen shows the Lab's current assigned staff, sourced from `GET /labs/{id}/assignments`.
6. An assignment can be removed (`DELETE /labs/{labId}/assignments/{assignmentId}`) without deleting the Lab itself.
7. A Lab can be edited at any time (`PUT /labs/{id}`) or, if no longer needed, hard-deleted (`DELETE /labs/{id}`, `LAB_MANAGE`) — there is no soft-deactivate-with-reason lifecycle as seen on other masters.

## 5. Success Criteria
Not formally defined in project documentation — inferred from feature completeness: Lab CRUD, speciality-scoped filtering, and staff-assignment CRUD are all implemented and consumed by downstream modules (Lab-Curriculum Mapping via `Experiment`/`Subject.eligibleLabs`, Lab Scheduling), matching the milestone's original acceptance checklist (`R1-2.4.1` through `R1-2.4.12`, all marked complete in `docs/RELEASE_1_MILESTONES.md`).

## 6. Assumptions & Constraints
- Lab is assumed to be a relatively low-churn, admin-only master; no self-service creation by Lab Incharge/Faculty.
- `LabInChargeAssignment.assigneeId` is assumed by convention to reference a Faculty (or Technician-role Faculty) record, but this is not enforced by any FK or existence check at write time.
- The optional Room link (V473/V474) assumes one Lab maps to at most one physical Room at a time; there is no history of past Room links.

## 7. Known Gaps / Deferred
- No formal BR document exists for Lab Setup & Configuration in `BUSINESS_REQUIREMENTS.md`; this BRD is derived entirely from code and migration inspection, consistent with the sibling SRS.md's own "Known Gaps" section.
- ~~`PUT /labs/{id}` (update) is gated by `LAB_VIEW`...~~ **Fixed 2026-09-24:** now gated by `LAB_MANAGE`, matching create/delete/assign and the frontend's own `canManageLabs()` gate. There is still no Lab-Incharge-specific permission (the original milestone text listed `ROLE_ADMIN, ROLE_LAB_INCHARGE`) — consistent with this project's DB-driven, permission-not-role model; a Lab Incharge role gets update access by being granted `LAB_MANAGE` via Role Management.
- No dependency guard was found preventing deletion of a Lab still referenced by active `LabInChargeAssignment`s, Lab Schedules, Lab-Curriculum Mappings, or Subject `eligibleLabs` entries.
- No name-uniqueness validation exists for Lab, unlike the mandatory `uniqueFieldValidator`/`/name-exists` pattern this project applies to other masters (e.g. Speciality) — two Labs can share an identical name.
