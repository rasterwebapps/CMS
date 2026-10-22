# Subject & Curriculum Load — Conversation Log (2026-10-21)

> Credentials shared during the session (SSH, Postgres, devadmin) are intentionally **redacted** in this file.

Target: `cms-postgres` container on `172.17.1.243`, database `CMS_MANUALTESTING`.
Source: `/home/raster/SANKAR/SEMESTER_WISE_ALL_SUBJECTS_THEORY_CALCULATED_HOURS.xlsx`

---

## 1. Request: review the Excel and create term-wise subjects

**User:** Review the Excel file; create the term-wise subjects in the `cms-postgres` container, database `CMS_MANUALTESTING`.

**Assistant:** Parsed the workbook (7 semesters, 37 subject rows; columns: prescribed/allotted theory hrs, prescribed/allotted skill-lab hrs). Direct Postgres access failed (password required) and SSH was refused; credentials were requested.

**User:** Provided SSH and Postgres credentials *(redacted)*.

**Assistant:** Reached the DB via `ssh → docker exec cms-postgres psql`. Inspected schema: `subjects`, `curriculum_term_courses`, `curriculum_versions` (B.Sc – V1, id 1), `programs`, `courses`. Inserted 31 unique subjects and 34 term mappings (allotted hours, credits 0) directly with SQL.

Notes reported: three subjects repeat across terms (Elective Personality Development T5/T6, Community Health Nursing – I T5/T7, OBG Nursing – I T6/T7). **User:** keep as it is.

## 2. Error: "Failed to add subject" (Term 3, NURSING_FOUNDATION_II)

Screenshot showed Clinical = 50h ticked. Code review of `CurriculumSemesterCourseService.applyDetails`:
Lab hours > 0 requires ≥1 eligible Lab; Clinical hours > 0 requires ≥1 eligible Clinical Venue. The subject had neither, so the API rejected it. The frontend swallows the backend message.

**User:** Term 2 already has the same subject with lab hours — how?
**Assistant:** My direct SQL inserts bypassed the API validation. The check only runs through the app's add/edit API. Rows loaded with lab hours but no linked lab would also fail on edit.

## 3. Master data & configuration needed for valid mapping

Findings from the application validations:

| Area | Rule |
|---|---|
| Subject | name ≤255 unique; code ≤50 unique; credits 1–20; theory/lab credits 0–20; term 1–12 |
| Speciality | optional on subject; required on Lab; name/code unique |
| Lab | name unique per speciality; lab type enum; status enum; capacity ≥1; optional room (Academic category) |
| Clinical venue | name unique; capacity >0; only needed if clinical hours > 0 |
| Mapping | term 1..program total terms (8); lab hrs>0 ⇒ eligible lab; clinical hrs>0 ⇒ eligible venue; elective ⇒ elective group of same version+term |
| Enums | SubjectType: CORE/FOUNDATIONAL/ELECTIVE/CO_CURRICULAR; LabType: COMPUTER/PHYSICS/CHEMISTRY/ELECTRONICS/BIOLOGY/LANGUAGE/MECHANICAL/OTHER; LabStatus: ACTIVE/AVAILABLE/INACTIVE/UNDER_MAINTENANCE |

Problems in the first SQL load: credits = 0, some codes > 50 chars, lab hours without labs, electives without groups.

## 4. Where credits take effect

- `credits`: validation (≥1) + display (subject list badge/column, form preview).
- `theoryCredits`: only functional use — workload weight in `CourseOfferingSectionFacultyService.autoAssignTheory`.
- `labCredits`: stored/displayed only; unused elsewhere.
- No cross-check that credits = theory + lab; hours come from curriculum mapping, not credits.

## 5. Final Excel template + reload through the API

Decisions: (1) keep repeated subjects as-is; (2) use **prescribed** hours; (3) create labs from prescribed lab hours.

Template: `/home/raster/SANKAR/ONECMS_SUBJECT_CURRICULUM_TEMPLATE.xlsx`
Sheets: Instructions, Specialities (9), Labs (10), Subjects (31), Subject_Lab_Links (10), Elective_Groups (4), Curriculum_Mapping (34).

Assumptions: theory credits = round(theory hrs/15, min 1); lab credits = round(lab hrs/30); credits = sum; lab capacity 30, status ACTIVE; new specialities FON, MSN, MHN, NEM, BAS, HUM (existing CHN, GN, PN); long codes shortened ≤50.

Load (devadmin via Keycloak token, REST `/api/v1`): deleted old 34 mappings + 31 subjects; created specialities, 10 labs, 31 subjects (with eligible labs), 4 elective groups, 34 mappings — **0 errors**. DB verification: no lab-hour row without a lab; no elective without a group.

Per-term prescribed hours loaded (theory / lab): T1 400/80, T2 260/160, T3 230/80, T4 250/40, T5 290/80, T6 210/40, T7 200/120.

Note: an empty pre-existing group "Elective — Term 4" (id 1, created by the user via UI) was left untouched.

## 6. Faculty requirement for Term 1

Inputs: ODD term 2026-11-01 → 2027-04-30 = 26 weeks; faculty caps: max 30 sessions/week, 5/day, 2 continuous; min 18/week (advisory); cohort 60 seats.

Final parameters from user: 45-min period; 60 per classroom; 30 per lab; 60 per clinical venue.
→ 1 theory section, 2 lab batches.

Weekly sessions (hours×60/45 ÷ 26 weeks, rounded up per subject): Theory 26 (English 3, Anatomy 4, Physiology 4, Sociology 4, Psychology 4, Nursing Foundation 7) + Lab 10 (NF 5 × 2 batches) = **36 sessions/week**.

Faculty: by workload ≥ 2 (36/30; 36/18); by speciality matching **3** (HUM 11, BAS 8, FON 17 sessions/week). Fewer is possible via the subject's Eligible Faculty list. DB currently has 2 active faculty. ODD term also runs terms 3, 5, 7 on the same pool.

## 7. Is a faculty required for every speciality?

**No.** `faculty.speciality_id` is mandatory for a faculty member, but a speciality may have none, and HOD is optional. Subject speciality is optional: without one, any faculty may teach it. With one, only same-speciality faculty or the subject's Eligible Faculty list may be assigned (`FacultyEligibility`); otherwise the subject can't be staffed / auto-assign skips it. Labs must keep a speciality.
