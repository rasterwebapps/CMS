# Student Roll Number Generation & Registration Fields

## TC-STU-RN-001: Verify Roll Number Pattern

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- System Configuration has `ROLL_NUMBER_COLLEGE_CODE` set to `959`
- At least one Course exists with `roll_number_code` = `65` (e.g., B.Sc. Nursing)
- At least one Student exists under that course without a roll number

**Steps:**
1. Navigate to Students → Roll Numbers (or use POST `/students/generate-roll-numbers`)
2. Select the course and academic year (e.g., 2026)
3. Select students to assign
4. Click Generate

**Expected Result:**
- Roll numbers are generated in the format: `959` + `65` + `2026` + `001` = `959652026001`
- Each subsequent student in the batch gets `959652026002`, `959652026003`, etc.
- Students are sorted alphabetically before assignment

**Status:** NOT TESTED

---

## TC-STU-RN-002: Preview Roll Numbers Before Assignment

**Preconditions:**
- Same as TC-STU-RN-001

**Steps:**
1. Call POST `/students/preview-roll-numbers` with `{ courseId, studentIds, academicYear }`
2. Verify the response contains the expected roll numbers

**Expected Result:**
- Response shows roll numbers that will be assigned (not yet saved)
- Numbers follow the `[CollegeCode][CourseCode][Year][Sequence]` pattern

**Status:** NOT TESTED

---

## TC-STU-RN-003: Sequence Rolls Over Correctly

**Preconditions:**
- Course with `roll_number_code` = `65` already has 3 students with roll numbers for 2026

**Steps:**
1. Generate a roll number for a 4th student in the same course and year

**Expected Result:**
- Roll number is `959652026004`
- Sequence is taken from the `roll_number_sequences` table (persisted)

**Status:** NOT TESTED

---

## TC-STU-REG-001: Create Student with University Registration Number

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN

**Steps:**
1. Navigate to Students → Add Student
2. Fill in required fields (Name, Email, Program, Semester, Admission Date, Roll Number)
3. Enter `2026MCA65001` in the "University Reg. No." field
4. Enter `UMIS2026001` in the "UMIS Number" field
5. Click Create Student

**Expected Result:**
- Student is created successfully
- Response includes `universityRegistrationNumber: "2026MCA65001"` and `umisNumber: "UMIS2026001"`

**Status:** NOT TESTED

---

## TC-STU-REG-002: View University Registration Number and UMIS in Student Detail

**Preconditions:**
- A student with university registration number and UMIS number exists

**Steps:**
1. Navigate to Students → select a student
2. Open the student detail page
3. Check the Profile tab

**Expected Result:**
- "University Reg. No." displays the value (or `—` if not set)
- "UMIS Number" displays the value (or `—` if not set)

**Status:** NOT TESTED

---

## TC-STU-REG-003: Update Student's University Registration and UMIS Numbers

**Preconditions:**
- A student exists without university registration number or UMIS number

**Steps:**
1. Navigate to Students → select a student → Edit
2. Enter values in "University Reg. No." and "UMIS Number" fields
3. Click Update Student

**Expected Result:**
- Student is updated successfully
- The new numbers are persisted and visible in the detail view

**Status:** NOT TESTED

---

## TC-STU-REG-004: Duplicate University Registration Number Rejected

**Preconditions:**
- Student A has `universityRegistrationNumber = "2026MCA65001"`

**Steps:**
1. Try to create or update Student B with the same `universityRegistrationNumber`

**Expected Result:**
- Server returns 409 Conflict or 400 Bad Request (unique constraint violation)

**Status:** NOT TESTED

---

## TC-STU-REG-005: University Registration Number and UMIS Optional

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Create a student without filling in "University Reg. No." or "UMIS Number"

**Expected Result:**
- Student is created successfully
- Both fields are `null`/`—` in the response and detail view

**Status:** NOT TESTED
