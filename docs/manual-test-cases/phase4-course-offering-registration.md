# Phase 4 — Course Offering and Registration: Manual Test Cases

## TC-CO-001: Generate Course Offerings for an OPEN Term Instance

**Preconditions:**
- At least one Program with active CurriculumVersion and CurriculumSemesterCourse records exists
- At least one active Cohort exists under that Program
- A TermInstance with status OPEN exists for a valid AcademicYear

**Steps:**
1. Send `POST /api/course-offerings/generate?termInstanceId={id}` where `{id}` is an OPEN TermInstance ID
2. Verify the response status is `200 OK`
3. Check `{"offeringsCreated": N}` where N > 0 (if curriculum subjects exist)
4. Send `GET /api/course-offerings?termInstanceId={id}`
5. Verify the list contains offerings for the correct semester numbers (ODD term → odd semesters; EVEN term → even semesters)

**Expected Result:**
- Offerings are created for every CurriculumSemesterCourse matching the term type (ODD/EVEN)
- Response includes `offeringsCreated` count

**Status:** NOT TESTED

---

## TC-CO-002: Idempotent Generate Course Offerings

**Preconditions:**
- Offerings already generated for a TermInstance (run TC-CO-001 first)

**Steps:**
1. Send `POST /api/course-offerings/generate?termInstanceId={id}` again (same term)
2. Verify the response status is `200 OK`
3. Check `{"offeringsCreated": 0}` — no duplicate offerings created

**Expected Result:**
- Second call creates 0 offerings (idempotent)

**Status:** NOT TESTED

---

## TC-CO-003: Get Course Offerings Filtered by Semester Number

**Preconditions:**
- Multiple course offerings exist for a TermInstance across different semester numbers

**Steps:**
1. Send `GET /api/course-offerings?termInstanceId={id}&semesterNumber=1`
2. Verify only offerings with `semesterNumber=1` are returned
3. Verify the response status is `200 OK`

**Expected Result:**
- Filtered list contains only offerings for semester 1

**Status:** NOT TESTED

---

## TC-CO-004: Update Course Offering (Assign Faculty and Section)

**Preconditions:**
- A CourseOffering with `id={coId}` exists

**Steps:**
1. Send `PUT /api/course-offerings/{coId}` with body `{"facultyId": 42, "sectionLabel": "A"}`
2. Verify the response status is `200 OK`
3. Verify the response body shows `facultyId: 42` and `sectionLabel: "A"`
4. Send `GET /api/course-offerings/{coId}` to confirm persistence

**Expected Result:**
- Faculty and section are updated on the offering

**Status:** NOT TESTED

---

## TC-CO-005: Deactivate a Course Offering (Soft Delete)

**Preconditions:**
- A CourseOffering with `id={coId}` exists and `isActive=true`

**Steps:**
1. Send `DELETE /api/course-offerings/{coId}`
2. Verify the response status is `204 No Content`
3. Send `GET /api/course-offerings/{coId}`
4. Verify `isActive=false` in response

**Expected Result:**
- Offering is soft-deleted (isActive=false), not removed from DB

**Status:** NOT TESTED

---

## TC-CO-006: Auto-generate on TermInstance OPEN transition

**Preconditions:**
- TermInstance is in PLANNED status
- Active cohorts and curriculum exist

**Steps:**
1. Send `PUT /api/term-instances/{id}` with body `{"status": "OPEN"}`
2. Verify the response status is `200 OK`
3. Send `GET /api/course-offerings?termInstanceId={id}`
4. Verify course offerings were auto-generated

**Expected Result:**
- Changing status to OPEN triggers automatic generation of course offerings

**Status:** NOT TESTED

---

## TC-CO-007: Auto-deactivate offerings on TermInstance LOCKED transition

**Preconditions:**
- TermInstance is OPEN with existing course offerings

**Steps:**
1. Send `PUT /api/term-instances/{id}` with body `{"status": "LOCKED"}`
2. Verify the response status is `200 OK`
3. Send `GET /api/course-offerings?termInstanceId={id}`
4. Verify all offerings have `isActive=false`

**Expected Result:**
- All active offerings are deactivated when term is locked

**Status:** NOT TESTED

---

## TC-CR-001: Generate Course Registrations for a TermInstance

**Preconditions:**
- StudentTermEnrollments (ENROLLED) exist for the TermInstance
- CourseOfferings (active) exist for the same TermInstance matching the enrollment semester numbers

**Steps:**
1. Send `POST /api/course-registrations/generate?termInstanceId={id}`
2. Verify response status is `200 OK`
3. Check `{"registrationsCreated": N}` where N > 0

**Expected Result:**
- Each enrolled student gets registrations for all active course offerings matching their semester number

**Status:** NOT TESTED

---

## TC-CR-002: Idempotent Generate Course Registrations

**Preconditions:**
- Registrations already generated (run TC-CR-001 first)

**Steps:**
1. Send `POST /api/course-registrations/generate?termInstanceId={id}` again
2. Verify response status is `200 OK`
3. Check `{"registrationsCreated": 0}`

**Expected Result:**
- Second call creates 0 registrations (idempotent)

**Status:** NOT TESTED

---

## TC-CR-003: Get Course Registrations by Enrollment

**Preconditions:**
- CourseRegistrations exist for enrollment `{enrollmentId}`

**Steps:**
1. Send `GET /api/course-registrations?enrollmentId={enrollmentId}`
2. Verify response status is `200 OK`
3. Verify all returned registrations belong to the specified enrollment

**Expected Result:**
- List of courses the student is registered in for that term

**Status:** NOT TESTED

---

## TC-CR-004: Get Course Registrations by Course Offering (Class List)

**Preconditions:**
- Multiple students registered in course offering `{courseOfferingId}`

**Steps:**
1. Send `GET /api/course-registrations?courseOfferingId={courseOfferingId}`
2. Verify response status is `200 OK`
3. Verify multiple registrations are returned (one per student)

**Expected Result:**
- Class list showing all students registered in the course offering

**Status:** NOT TESTED

---

## TC-CR-005: Drop a Course Registration

**Preconditions:**
- A CourseRegistration with `id={crId}` exists with status `REGISTERED`

**Steps:**
1. Send `PUT /api/course-registrations/{crId}/drop`
2. Verify response status is `200 OK`
3. Verify `status: "DROPPED"` in response body

**Expected Result:**
- Registration status changes to DROPPED

**Status:** NOT TESTED

---

## TC-CO-UI-001: View Course Offerings Section on Academic Year Detail Page

**Preconditions:**
- Academic Year with an OPEN or LOCKED TermInstance exists
- Course offerings have been generated

**Steps:**
1. Navigate to `/academic-years/{id}` in the browser
2. Scroll to "Course Offerings" section
3. Verify the table shows: Subject Name, Subject Code, Semester #, Curriculum Version, Faculty, Section, Status
4. Change the "Filter by Semester" dropdown to a specific semester
5. Verify only offerings for that semester are shown

**Expected Result:**
- Course offerings table renders with correct data and filter works

**Status:** NOT TESTED

---

## TC-CO-UI-002: Generate Offerings Button on Academic Year Detail Page

**Preconditions:**
- Academic Year with an OPEN TermInstance exists
- Curriculum exists with subjects

**Steps:**
1. Navigate to `/academic-years/{id}` in the browser
2. Click "Generate Offerings" button
3. Verify toast notification appears with success message
4. Verify the offerings table populates with generated offerings

**Expected Result:**
- Offerings are generated and displayed

**Status:** NOT TESTED

---

## TC-CO-UI-003: Inline Edit Faculty and Section on Course Offering Row

**Preconditions:**
- Course Offerings exist for an OPEN TermInstance

**Steps:**
1. Navigate to Academic Year Detail page
2. In the Course Offerings table, click the edit (pencil) button on a row
3. Enter a Faculty ID and Section Label
4. Click "Save"
5. Verify the row updates with the new values

**Expected Result:**
- Faculty ID and Section Label are saved and reflected in the table

**Status:** NOT TESTED

---

## TC-CR-UI-001: View Course Registrations Tab on Student Detail Page

**Preconditions:**
- Student with term enrollments and course registrations exists

**Steps:**
1. Navigate to `/students/{id}` in the browser
2. Click the "Course Registrations" tab
3. Verify each term enrollment shows a sub-table of registered courses
4. Columns: Subject, Code, Semester, Status

**Expected Result:**
- Course Registrations tab shows all registered courses grouped by term enrollment

**Status:** NOT TESTED

---

## TC-CR-UI-002: Generate Registrations Button on Academic Year Detail Page

**Preconditions:**
- Academic Year with an OPEN TermInstance exists
- Course offerings have been generated

**Steps:**
1. Navigate to `/academic-years/{id}` in the browser
2. Scroll to "Course Registrations" section
3. Click "Generate Registrations" button
4. Verify toast notification appears with success message
5. Verify the registrations count in the summary updates

**Expected Result:**
- Registrations are generated and summary count updates

**Status:** NOT TESTED
