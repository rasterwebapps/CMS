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

---

## TC-DUP-012: Edit equipment with an asset code that already exists

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Two equipment items exist: "Dell Computer" (ASSET001) and "HP Computer" (ASSET002)

**Steps:**
1. Navigate to Equipment and click Edit on "Dell Computer"
2. Change the Asset Code field to "ASSET002"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "Equipment with asset code 'ASSET002' already exists"
- Equipment is NOT updated

**Status:** NOT TESTED

---

## TC-DUP-013: Edit equipment keeping the same asset code (no conflict)

**Preconditions:**
- Equipment "Dell Computer" (ASSET001) exists

**Steps:**
1. Navigate to Equipment and click Edit on "Dell Computer"
2. Change the Name only, keep Asset Code as "ASSET001"
3. Click Save

**Expected Result:**
- Equipment is updated successfully (same asset code — not treated as a conflict with itself)

**Status:** NOT TESTED

---

## TC-DUP-014: Edit faculty with an employee code that already exists

**Preconditions:**
- Two faculty members exist: "John Doe" (EMP001) and "Jane Smith" (EMP002)

**Steps:**
1. Navigate to Faculty and click Edit on "John Doe"
2. Change the Employee Code field to "EMP002"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A faculty with employee code 'EMP002' already exists"
- Faculty is NOT updated

**Status:** NOT TESTED

---

## TC-DUP-015: Edit faculty with an email that already exists

**Preconditions:**
- Two faculty members exist: "John Doe" (john@college.edu) and "Jane Smith" (jane@college.edu)

**Steps:**
1. Navigate to Faculty and click Edit on "John Doe"
2. Change the Email to "jane@college.edu"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A faculty with email 'jane@college.edu' already exists"
- Faculty is NOT updated

**Status:** NOT TESTED

---

## TC-DUP-016: Edit agent with a name that already exists

**Preconditions:**
- Two agents exist: "North Zone Agent" and "South Zone Agent"

**Steps:**
1. Navigate to Agents and click Edit on "North Zone Agent"
2. Change the Name field to "South Zone Agent"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "An agent with the name 'South Zone Agent' already exists"
- Agent is NOT updated

**Status:** NOT TESTED

---

## TC-DUP-017: Edit fee structure with a fee type that already exists in the same group

**Preconditions:**
- Two fee structures exist for the same Program + Academic Year + Course: TUITION and LAB_FEE

**Steps:**
1. Navigate to Fee Structures and edit the TUITION entry for that combination
2. Change the Fee Type to "LAB_FEE"
3. Click Save

**Expected Result:**
- Save is rejected with an error alert: "A fee structure with fee type 'LAB_FEE' already exists for this program and academic year combination"
- Fee structure is NOT updated

**Status:** NOT TESTED
