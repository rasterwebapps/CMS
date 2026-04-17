# Manual Test Cases: Duplicate Validation on Master Entry Edit

These test cases verify that editing a master entry (Department, Program, Course, Academic Year,
Referral Type, Subject, Semester, Lab) is rejected when another entry with the same name or code
already exists.

---

## TC-DUP-001: Edit department with a name that already exists

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Two departments exist: "Computer Science" (CS) and "Mathematics" (MATH)

**Steps:**
1. Navigate to Departments and click Edit on "Computer Science"
2. Change the Name field to "Mathematics"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A department with the name 'Mathematics' already exists"
- Department is NOT updated

**Status:** NOT TESTED

---

## TC-DUP-002: Edit department with a code that already exists

**Preconditions:**
- Two departments exist: "Computer Science" (CS) and "Mathematics" (MATH)

**Steps:**
1. Navigate to Departments and click Edit on "Computer Science"
2. Change the Code field to "MATH"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A department with the code 'MATH' already exists"
- Department is NOT updated

**Status:** NOT TESTED

---

## TC-DUP-003: Edit department keeping the same name and code (no conflict)

**Preconditions:**
- Department "Computer Science" (CS) exists

**Steps:**
1. Navigate to Departments and click Edit on "Computer Science"
2. Change only the Description field
3. Click Save

**Expected Result:**
- Department is updated successfully (name/code unchanged — not treated as a conflict)

**Status:** NOT TESTED

---

## TC-DUP-004: Edit program with a name that already exists

**Preconditions:**
- Two programs exist: "Bachelor" (BACHELOR) and "Master" (MASTER)

**Steps:**
1. Edit the "Bachelor" program
2. Change the Name to "Master"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A program with the name 'Master' already exists"

**Status:** NOT TESTED

---

## TC-DUP-005: Edit program with a code that already exists

**Preconditions:**
- Two programs exist: "Bachelor" (BACHELOR) and "Master" (MASTER)

**Steps:**
1. Edit the "Bachelor" program
2. Change the Code to "MASTER"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A program with the code 'MASTER' already exists"

**Status:** NOT TESTED

---

## TC-DUP-006: Edit course with a name that already exists

**Preconditions:**
- Two courses exist with names "B.Sc. Nursing" (BSN) and "M.Sc. Nursing" (MSN)

**Steps:**
1. Edit "B.Sc. Nursing"
2. Change the Name to "M.Sc. Nursing"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A course with the name 'M.Sc. Nursing' already exists"

**Status:** NOT TESTED

---

## TC-DUP-007: Edit academic year with a name that already exists

**Preconditions:**
- Two academic years exist: "2024-2025" and "2023-2024"

**Steps:**
1. Edit "2024-2025"
2. Change the Name to "2023-2024"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "An academic year with the name '2023-2024' already exists"

**Status:** NOT TESTED

---

## TC-DUP-008: Edit semester with a name that already exists in the same academic year

**Preconditions:**
- Academic year "2024-2025" has two semesters: "Fall 2024" and "Spring 2025"

**Steps:**
1. Edit "Fall 2024"
2. Change the Name to "Spring 2025"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A semester with the name 'Spring 2025' already exists in this academic year"

**Status:** NOT TESTED

---

## TC-DUP-009: Edit referral type with a name that already exists

**Preconditions:**
- Two referral types exist: "Staff" (STAFF) and "Alumni" (ALUMNI)

**Steps:**
1. Edit "Staff"
2. Change the Name to "Alumni"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A referral type with the name 'Alumni' already exists"

**Status:** NOT TESTED

---

## TC-DUP-010: Edit lab with a name that already exists in the same department

**Preconditions:**
- Department "Computer Science" has two labs: "Computer Lab 1" and "Computer Lab 2"

**Steps:**
1. Edit "Computer Lab 1"
2. Change the Name to "Computer Lab 2"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A lab with the name 'Computer Lab 2' already exists in this department"

**Status:** NOT TESTED

---

## TC-DUP-011: Edit lab with a name that exists in a different department (allowed)

**Preconditions:**
- Department "Computer Science" has "Computer Lab 1"
- Department "Electronics" also has "Computer Lab 1"

**Steps:**
1. Edit the "Computer Lab 1" in "Computer Science" department
2. Keep the same name, change only Building
3. Click Save

**Expected Result:**
- Update is successful (same name in a different department is not a conflict)

**Status:** NOT TESTED
